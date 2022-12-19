package gr.serafeim

import com.mitchellbosecke.pebble.loader.ClasspathLoader
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.el.GreekAnalyzer
import org.apache.lucene.document.DateTools
import org.apache.lucene.document.Document
import org.apache.lucene.document.LongPoint
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.*
import org.apache.lucene.search.highlight.Highlighter
import org.apache.lucene.search.highlight.QueryScorer
import org.apache.lucene.search.highlight.SimpleHTMLFormatter
import org.apache.lucene.search.highlight.SimpleSpanFragmenter
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import org.mapdb.DBMaker
import org.mapdb.Serializer
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

data class Results(val results: List<Result>, val total: Int)

data class Result(
    val id: String,
    val text: String,
    val hfragments: List<String>,
    val name: String,
    val path: List<String>,
    val created: Date,
    val modified: Date,
    val accessed: Date
) {
    fun abstract(): String {
        val m = minOf(50, text.length - 1)
        return text.substring(0, m)
    }
}

data class SearchParams(
    val q: String,
    val n: Int,
    val p: Int,
    val path: String?,
    val createdFrom: Date?,
    val createdTo: Date?,
    val modifiedFrom: Date?,
    val modifiedTo: Date?,
    val accessedFrom: Date?,
    val accessedTo: Date?
)

fun fromDateString(s: String): Date {
    return DateTools.stringToDate(s)
}

fun dateToMillis(d: Date?): Long {
    if(d==null) {
        return 0
    }
    return d.time
}

fun addDateQuery(bqb: BooleanQuery.Builder, dateFrom: Date?, dateTo: Date?, what: String) {
    if (dateFrom != null || dateTo != null) {
        val fromMillis = dateToMillis(dateFrom)
        val toMillis = dateToMillis(dateTo)
        val query3: Query = LongPoint.newRangeQuery(what, fromMillis, toMillis)
        bqb.add(query3, BooleanClause.Occur.FILTER)
    }

}

fun search(sp: SearchParams): Results {
    val directory: Directory = FSDirectory.open(Paths.get("lucene_index"))
    val reader = DirectoryReader.open(directory)
    val indexSearcher = IndexSearcher(reader)
    val analyzer: Analyzer = GreekAnalyzer()
    // https://stackoverflow.com/questions/2005084/how-to-specify-two-fields-in-lucene-queryparser
    val query1 = QueryParser("text", analyzer).parse(sp.q)
    val bqb = BooleanQuery.Builder()
    bqb.add(query1, BooleanClause.Occur.SHOULD)
    val query2: Query = WildcardQuery(Term("name", sp.q))
    bqb.add(query2, BooleanClause.Occur.SHOULD)
    bqb.setMinimumNumberShouldMatch(1)

    val query2a = QueryParser("name_t", analyzer).parse(sp.q)
    bqb.add(query2a, BooleanClause.Occur.SHOULD)
    bqb.setMinimumNumberShouldMatch(1)

    addDateQuery(bqb, sp.createdFrom, sp.createdTo, "created_point")
    addDateQuery(bqb, sp.modifiedFrom, sp.modifiedTo, "modified_point")
    addDateQuery(bqb, sp.accessedFrom, sp.accessedTo, "accessed_point")

    if (sp.path != null && sp.path != "") {
        val query4: Query = WildcardQuery(Term("path", sp.path))
        bqb.add(query4, BooleanClause.Occur.FILTER)
    }

    val booleanQuery = bqb.build()
    val collector = TopScoreDocCollector.create(99999, 100)

    indexSearcher.search(booleanQuery, collector)

    val start = (sp.p - 1) * sp.n
    val howmany = sp.n

    // Highlight
    val formatter = SimpleHTMLFormatter("<span class='highlight'>", "</span>");
    val queryScorer = QueryScorer(booleanQuery);
    val highlighter = Highlighter(formatter, queryScorer);
    highlighter.textFragmenter = SimpleSpanFragmenter(queryScorer, Int.MAX_VALUE)
    highlighter.maxDocCharsToAnalyze = Int.MAX_VALUE

    val fragmentHighlighter = Highlighter(formatter, queryScorer);
    fragmentHighlighter.textFragmenter = SimpleSpanFragmenter(queryScorer, 30)
    fragmentHighlighter.maxDocCharsToAnalyze = Int.MAX_VALUE

    val results = collector.topDocs(start, howmany).scoreDocs.map {

        val doc: Document = indexSearcher.doc(it.doc)
        val id = doc.get("id")
        val path = doc.getValues("path").asList()
        val name = doc.get("name")
        val text = doc.get("text")

        val created = fromDateString(doc.get("created"))
        val accessed = fromDateString(doc.get("accessed"))
        val modified = fromDateString(doc.get("modified"))

        val fragments = fragmentHighlighter.getBestFragments(analyzer.tokenStream("text", text), text, 10)

        val htext = highlighter.getBestFragment(analyzer, "text", text)?:"";
        Result(
            id = id,
            text = htext,
            hfragments = fragments.toList(),
            name = name,
            path = path,
            accessed = accessed,
            modified = modified,
            created = created
        )
    }

    return Results(results = results, total = collector.totalHits)
}

fun toDate(s: String): Date? {
    if (s!="") {
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        return formatter.parse(s)
    }
    return null
}

fun nextPage(req: ApplicationRequest, p: Int, n: Int, total: Int): String {
    if (n * p < total) {
        var u = URLBuilder(req.uri)
        u.parameters["page"] = "${p + 1}"
        return u.build().fullPath
    } else {
        return "#"
    }
}

fun prevPage(req: ApplicationRequest, p: Int): String {
    if (p > 1) {
        var u = URLBuilder(req.uri)
        u.parameters["page"] = "${p - 1}"
        return u.build().fullPath
    } else {
        return "#"
    }
}

fun Application.module() {
    //val solrClient = HttpSolrClient.Builder("http://localhost:3456/solr/docs").build()
    val env = environment.config.propertyOrNull("ktor.environment")?.getString()
    val logger = LoggerFactory.getLogger(Application::class.java)
    val directory = environment.config.propertyOrNull("parser.directory")?.getString() ?: "."
    val interval = environment.config.propertyOrNull("parser.interval")?.getString()?.toInt() ?: 60
    var total = 0
    gr.serafeim.parser.init(directory, interval)

    install(Pebble) {
        loader(ClasspathLoader().apply {
            prefix = "templates"
        }) // .extension(PebblePageExtension())
    }

    routing {
        get("/keys") {
            logger.info("Hi from /keys")

            val map = DBHolder.map

            call.respond(
                PebbleContent(
                    "keys.html", mapOf(
                        "keys" to map.keys,
                        "keySize" to map.keys.size,
                    )
                )
            )
        }
        get("/download") {
            logger.info("Hi from /download")
            val path = call.request.queryParameters.get("path") ?: ""
            println(path)
            val map = DBHolder.map
            if (path in map.keys) {
                val file = File(path)
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, file.name)
                        .toString()
                )
                call.respondFile(file)
            }
        }
        get("/") {

            logger.info("Hi from /")
            val q = call.request.queryParameters.get("query") ?: ""
            val n = call.request.queryParameters.get("number") ?: "10"
            val p = call.request.queryParameters.get("page") ?: "1"
            val path = call.request.queryParameters.get("path") ?: ""
            val createdFromStr = call.request.queryParameters.get("created-from") ?: ""
            val createdToStr = call.request.queryParameters.get("created-to") ?: ""
            val modifiedFromStr = call.request.queryParameters.get("modified-from") ?: ""
            val modifiedToStr = call.request.queryParameters.get("modified-to") ?: ""
            val accessedFromStr = call.request.queryParameters.get("modified-from") ?: ""
            val accessedToStr = call.request.queryParameters.get("modified-to") ?: ""
            val createdFrom = toDate(createdFromStr)
            val createdTo = toDate(createdToStr)
            val modifiedFrom = toDate(modifiedFromStr)
            val modifiedTo = toDate(modifiedToStr)
            val accessedFrom = toDate(accessedFromStr)
            val accessedTo = toDate(accessedToStr)

            var results = listOf<Result>()
            if (q != "") {


                val sp = SearchParams(
                    q = q,
                    p = p.toInt(),
                    n = n.toInt(),
                    path = path,
                    createdFrom = createdFrom,
                    createdTo = createdTo,
                    modifiedFrom = modifiedFrom,
                    modifiedTo = modifiedTo,
                    accessedFrom = accessedFrom,
                    accessedTo = accessedTo,
                )
                try {
                    val rt = search(sp)
                    results = rt.results
                    total = rt.total
                } catch (e: org.apache.lucene.queryparser.classic.ParseException) {
                    logger.info("Error while trying to parse the query")
                }
            }
            call.request
            call.respond(
                PebbleContent(
                    "home.html", mapOf(
                        "results" to results,
                        "total" to total,
                        "q" to q,
                        "page" to p,
                        "n" to n,
                        "created_from" to createdFromStr,
                        "created_to" to createdToStr,
                        "modified_from" to modifiedFromStr,
                        "modified_to" to modifiedToStr,
                        "accessed_from" to accessedFromStr,
                        "accessed_to" to accessedToStr,
                        "path" to path,
                        "next_page" to nextPage(call.request, p.toInt(), n.toInt(), total),
                        "prev_page" to prevPage(call.request, p.toInt())
                    )
                )
            )
        }
    }
}

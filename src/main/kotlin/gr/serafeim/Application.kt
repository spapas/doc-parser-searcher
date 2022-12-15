package gr.serafeim

import com.mitchellbosecke.pebble.loader.ClasspathLoader
import io.ktor.server.application.*
import io.ktor.server.pebble.*
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
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

data class Result(val id: String, val text: String, val name: String, val path: List<String>, val created: Date, val modified: Date, val accessed: Date) {
    fun abstract(): String {
        val m = minOf(50, text.length-1)
        return text.substring(0, m)
    }
}

data class SearchParams(val q: String, val n: Int, val path: String?, val createdFrom: Date?, val createdTo: Date?)

fun fromDateString(s: String): Date {
    return DateTools.stringToDate(s)
}

fun search(sp: SearchParams): List<Result> {
    val directory: Directory = FSDirectory.open(Paths.get("lucene_index"))
    val reader = DirectoryReader.open(directory)
    val indexSearcher = IndexSearcher(reader)
    val analyzer: Analyzer = GreekAnalyzer()
    val query1 = QueryParser("text", analyzer).parse(sp.q)

    // https://stackoverflow.com/questions/2005084/how-to-specify-two-fields-in-lucene-queryparser

    val bqb = BooleanQuery.Builder()
    bqb.add(query1, BooleanClause.Occur.SHOULD)
    val query2: Query = WildcardQuery(Term("name", sp.q))
    bqb.add(query2, BooleanClause.Occur.SHOULD)
    bqb.setMinimumNumberShouldMatch(1)

    if (sp.createdTo != null || sp.createdFrom!=null) {
        val createdFromMillis = if (sp.createdFrom!=null) {
            sp.createdFrom.time
        } else {
            0
        }

        val createdToMillis = if (sp.createdTo!=null) {
            sp.createdTo.time
        } else {
            Long.MAX_VALUE
        }
        val query3: Query = LongPoint.newRangeQuery("created_point", createdFromMillis, createdToMillis)
        bqb.add(query3, BooleanClause.Occur.FILTER)
    }


    if (sp.path!=null && sp.path!="") {
        val query4: Query = WildcardQuery(Term("path", sp.path))
        bqb.add(query4, BooleanClause.Occur.FILTER)
    }
    // also created:[20221202 TO 20221203]
    //IntPoint.newRangeQuery()

    val booleanQuery = bqb.build()
    val collector = TopScoreDocCollector.create(99999, 100)

    indexSearcher.search(booleanQuery, collector)
    val start = 0
    val howmany = sp.n

    // Highlight
    val formatter = SimpleHTMLFormatter("<span class='highlight'>", "</span>");
    val queryScorer =  QueryScorer(booleanQuery);
    val highlighter = Highlighter(formatter, queryScorer);
    highlighter.textFragmenter = SimpleSpanFragmenter(queryScorer, Int.MAX_VALUE)
    highlighter.maxDocCharsToAnalyze = Int.MAX_VALUE


    return collector.topDocs(start, howmany).scoreDocs.map {

        val doc: Document = indexSearcher.doc(it.doc)
        val id = doc.get("id")
        val path = doc.getValues("path").asList()
        val name = doc.get("name")
        val text = doc.get("text")

        val created = fromDateString(doc.get("created"))
        val accessed = fromDateString(doc.get("accessed"))
        val modified = fromDateString(doc.get("modified"))

        val htext = highlighter.getBestFragment(analyzer, "text", text);
        Result(id = id, text = htext, name = name, path = path, accessed = accessed, modified = modified, created = created)
    }
}

fun toDate(s: String): Date {
    val formatter = SimpleDateFormat("yyyy-MM-dd")
    return formatter.parse(s)
}

fun Application.module() {
    //val solrClient = HttpSolrClient.Builder("http://localhost:3456/solr/docs").build()
    val env = environment.config.propertyOrNull("ktor.environment")?.getString()
    val logger = LoggerFactory.getLogger(Application::class.java)
    val directory = environment.config.propertyOrNull("parser.directory")?.getString()?:"."
    val interval = environment.config.propertyOrNull("parser.interval")?.getString()?.toInt()?:60
    gr.serafeim.parser.init(directory, interval)

    install(Pebble){
        loader(ClasspathLoader().apply {
            prefix = "templates"
        })
    }

    routing {
        get("/keys") {
            logger.info("Hi from /keys")
            val db = DBMaker.fileDB("map.db").readOnly().make()
            val map = db.hashMap("docs", Serializer.STRING, Serializer.LONG).createOrOpen()

            call.respond(PebbleContent("keys.html", mapOf(
                "keys" to map.keys,
                "keySize" to map.keys.size,

            )))
            map.close()
            db.close()
        }
        get("/") {

            logger.info("Hi from /")
            val q = call.request.queryParameters.get("query")?:""
            val n = call.request.queryParameters.get("number")?:"10"
            val path = call.request.queryParameters.get("path")?:""
            val created_from = call.request.queryParameters.get("created-from")?:""
            val created_to = call.request.queryParameters.get("created-to")?:""
            println(created_from)
            var results = listOf<Result>()
            if (q != "") {
                val createdTo = if(created_to!=null && created_to!="") {
                    toDate(created_to)
                } else {
                    null
                }
                val createdFrom = if(created_from!=null && created_from!="") {
                    toDate(created_from)
                } else {
                    null
                }

                val sp = SearchParams(q=q, n=n.toInt(), path=path, createdFrom = createdFrom, createdTo = createdTo)
                results = search(sp)
            }
            call.respond(PebbleContent("home.html", mapOf(
                "results" to results,
                "q" to q,
                "n" to n,
                "created_from" to created_from,
                "created_to" to created_to,
                "path" to path
            )))
        }
    }
}

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
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.common.SolrDocumentList
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.util.*


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

data class Result(val id: String, val text: String, val name: String, val path: List<String>, val created: Date, val modified: Date, val accessed: Date) {
    fun abstract(): String {
        return text.substring(100)
    }
}

fun fromDateString(s: String): Date {
    return DateTools.stringToDate(s)
}

fun search(q: String, n: Int): List<Result> {
    val directory: Directory = FSDirectory.open(Paths.get("lucene_index"))
    val reader = DirectoryReader.open(directory)
    val indexSearcher = IndexSearcher(reader)
    val analyzer: Analyzer = GreekAnalyzer()
    val query = QueryParser("text", analyzer).parse(q)

    val results = indexSearcher.search(query, n)

    return results.scoreDocs.map {

        val doc: Document = indexSearcher.doc(it.doc)
        val id = doc.get("id")
        val path = doc.getValues("path").asList()
        val name = doc.get("name")
        val text = doc.get("text")
        val created = fromDateString(doc.get("created"))
        val accessed = fromDateString(doc.get("accessed"))
        val modified = fromDateString(doc.get("modified"))
        Result(id = id, text = text, name = name, path = path, accessed = accessed, modified = modified, created = created)
    }


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
        get("/") {
            call.application.environment.log.info("Hello /")
            logger.info("Hi hi")
            var q = call.request.queryParameters.get("query")?:""
            var n = call.request.queryParameters.get("n")?:"10"
            var results = listOf<Result>()
            if (q != "") {
                results = search(q, n.toInt())
            }
            call.respond(PebbleContent("index.html", mapOf(
                "results" to results, "q" to q,
                "n" to n
            )))
        }
    }
}
/*
fun main() {
    val solrClient = HttpSolrClient.Builder("http://localhost:3456/solr/docs").build()

    embeddedServer(Netty, port = 8000, watchPaths = listOf("classes", "resources")) {
        install(Pebble) {
            loader(ClasspathLoader().apply {
                prefix = "templates"
            })
        }

        routing {
            get("/") {
                call.application.environment.log.info("Hello from /api/v1!")

                var q = call.request.queryParameters.get("query")?:""
                var n = call.request.queryParameters.get("n")?:"10"
                var results = SolrDocumentList()
                if (q != "") {
                    val query = SolrQuery()
                    query.setQuery(q)
                    query.setRows(n.toInt())
                    val response = solrClient.query(query)
                    results = response.results
                }
                call.respond(PebbleContent("index.html", mapOf(
                    "results" to results, "q" to q,
                    "n" to n
                )))
            }
        }
    }.start(wait = true)
}
*/
import com.mitchellbosecke.pebble.loader.ClasspathLoader
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.pebble.*

import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.common.SolrDocumentList


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

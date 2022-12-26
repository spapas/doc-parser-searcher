package gr.serafeim.web

import gr.serafeim.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*


data class SearchParams(
    val q: String,
    val n: Int,
    val p: Int,
    val path: String?,
    val ext: String?,
    val createdFrom: Date?,
    val createdTo: Date?,
    val modifiedFrom: Date?,
    val modifiedTo: Date?,
    val accessedFrom: Date?,
    val accessedTo: Date?
)

val logger = KotlinLogging.logger {}

fun Route.listKeysRoute() {
    get("/keys") {
        logger.info("Hi from /keys")

        val map = DBHolder.map
        call.respond(
            PebbleContent(
                "keys.html", mapOf(
                    "keys" to map.map { Pair(it.key, it.value)},
                    "keySize" to map.keys.size,
                )
            )
        )
    }
}

fun Route.downloadFile() {
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
}

fun Route.index(pageSize: Int) {

    get("/") {

        logger.info("Hi from /")
        val q = call.request.queryParameters.get("query") ?: ""
        val p = call.request.queryParameters.get("page")?.toInt() ?: 1
        val path = call.request.queryParameters.get("path") ?: ""
        val ext = call.request.queryParameters.get("ext") ?: ""
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
        var totalTime = 0L
        var total = 0
        var results = listOf<Result>()
        if (q != "") {
            logger.info("Searching for: $q")
            val sp = SearchParams(
                q = q,
                p = p,
                n = pageSize,
                path = path,
                ext = ext,
                createdFrom = createdFrom,
                createdTo = createdTo,
                modifiedFrom = modifiedFrom,
                modifiedTo = modifiedTo,
                accessedFrom = accessedFrom,
                accessedTo = accessedTo,
            )
            try {
                val startTime = System.nanoTime()
                val rt = SearchHolder.search(sp)
                results = rt.results
                total = rt.total
                val endTime = System.nanoTime()
                totalTime = endTime - startTime
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
                    "totalTime" to totalTime,
                    "showingFrom" to pageSize * (p-1) + 1,
                    "showingTo" to if (pageSize * p < total) { pageSize * p} else { total },
                    "created_from" to createdFromStr,
                    "created_to" to createdToStr,
                    "modified_from" to modifiedFromStr,
                    "modified_to" to modifiedToStr,
                    "accessed_from" to accessedFromStr,
                    "accessed_to" to accessedToStr,
                    "path" to path,
                    "ext" to ext,
                    "next_page" to nextPage(call.request, p.toInt(), pageSize, total),
                    "prev_page" to prevPage(call.request, p.toInt())
                )
            )
        )
    }
}

package gr.serafeim.web

import gr.serafeim.*
import gr.serafeim.conf.ConfigHolder
import gr.serafeim.search.Result
import gr.serafeim.search.SearchHolder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

import java.io.File
import java.util.*
import java.net.URLEncoder;

data class SearchParams(
    val q: String,
    val n: Int,
    val p: Int,
    val path: String? = null,
    val ext: String? = null,
    val createdFrom: Date? = null,
    val createdTo: Date? = null,
    val modifiedFrom: Date? = null,
    val modifiedTo: Date? = null,
    val accessedFrom: Date? = null,
    val accessedTo: Date? = null
)

fun Route.listKeysRoute() {
    get("/docs") {
        val docsmap = DBHolder.map
        val q = call.request.queryParameters.get("query") ?: ""
        val p = call.request.queryParameters.get("page")?.toInt() ?: 1
        val psize = call.request.queryParameters.get("page_size")?.toInt() ?: 10
        val docs = if (q != "") docsmap.filter { q in it.key } else docsmap

        call.respond(
            PebbleContent(
                "docs.html", mapOf(
                    "docs" to docs.map { Pair(it.key, it.value) }.drop(psize * (p - 1)).take(psize),
                    "docSize" to docsmap.keys.size,
                    "q" to q,
                    "page" to p,
                    "next_page" to nextPage(call.request, p.toInt(), psize, docsmap.keys.size),
                    "prev_page" to prevPage(call.request, p.toInt())
                )
            )
        )
    }
}

fun Route.statusRoute() {
    get("/status") {

        val map = DBHolder.map
        call.respond(
            PebbleContent(
                "status.html", mapOf(
                    "keySize" to map.keys.size,
                    "parsing" to StateHolder.parsing,
                    "config" to ConfigHolder.config
                )
            )
        )
    }
}


fun Route.aboutRoute() {
    get("/about") {

        val map = DBHolder.map
        call.respond(
            PebbleContent(
                "about.html", mapOf(
                )
            )
        )
    }
}

fun Route.downloadFile() {
    get("/download") {
        val path = call.request.queryParameters.get("path") ?: ""
        println(path)
        val map = DBHolder.map
        if (path in map.keys) {
            val file = File(path)
            val cdVal = ContentDisposition.Attachment.withParameter(
                ContentDisposition.Parameters.FileName,
                URLEncoder.encode(file.name)
            )
            call.response.header(
                HttpHeaders.ContentDisposition,
                cdVal.toString()
            )
            call.respondFile(file)
        }
    }
}

fun Route.index(pageSize: Int) {

    get("/") {
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
                e.printStackTrace()
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
                    "showingFrom" to pageSize * (p - 1) + 1,
                    "showingTo" to if (pageSize * p < total) {
                        pageSize * p
                    } else {
                        total
                    },
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

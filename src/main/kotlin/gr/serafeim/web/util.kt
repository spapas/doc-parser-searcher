package gr.serafeim.web

import io.ktor.http.*
import io.ktor.server.request.*
import org.apache.lucene.document.DateTools
import java.text.SimpleDateFormat
import java.util.*


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


fun fromDateString(s: String): Date {
    return DateTools.stringToDate(s)
}

fun dateToMillis(d: Date?, default: Long): Long {
    if(d==null) {
        return default
    }
    return d.time
}

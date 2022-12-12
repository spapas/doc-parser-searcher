package gr.serafeim

import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument
import org.apache.tika.Tika
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.util.*
import kotlinx.coroutines.*


fun getSolrClient(): HttpSolrClient {
    val solrUrl = "http://localhost:3456/solr"
    return HttpSolrClient.Builder(solrUrl)
        .withConnectionTimeout(10000)
        .withSocketTimeout(60000)
        .build();

}

fun toDate(ft: FileTime): Date {
    return Date(ft.toMillis())
}



fun main() {
    val client = getSolrClient()
    val start = System.currentTimeMillis()
    val dir = File("c:/users/serafeim/desktop")
    runBlocking {
        val jobs = mutableListOf<Job>()

        dir.walk(direction = FileWalkDirection.BOTTOM_UP).forEach {
            if (listOf("doc", "docx").contains(it.extension.lowercase())) {
                val job = GlobalScope.launch {
                    println(it.name)
                    val attrs = Files.readAttributes<BasicFileAttributes>(Paths.get(it.path), BasicFileAttributes::class.java)

                    val tika = Tika()
                    val content = tika.parseToString(it.absoluteFile)

                    val doc = SolrInputDocument()
                    doc.addField("id", it.path)
                    doc.addField("_text_", content)
                    doc.addField("name", it.name)
                    doc.addField("path", it.path.split(File.separator))
                    doc.addField("extension", it.extension)
                    doc.addField("created-date", toDate(attrs.creationTime()))
                    doc.addField("last-accessed-date", toDate(attrs.lastAccessTime()))
                    doc.addField("last-modified-date", toDate(attrs.lastModifiedTime()))
                    //println(doc)
                    val updateResponse: UpdateResponse = client.add("docs", doc)
                    println(updateResponse)
                }
                jobs.add(job)
            }
        }
        jobs.joinAll()
    }
    client.commit("docs")

    val end = System.currentTimeMillis()
    println("start: ${start}, end: ${end}, end-start: ${end-start}")


}
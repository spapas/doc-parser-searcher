package gr.serafeim.parser

import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import kotlinx.coroutines.*
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.el.GreekAnalyzer
import org.apache.lucene.document.*
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import org.apache.tika.Tika
import org.apache.tika.config.TikaConfig
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule


val logger = LoggerFactory.getLogger("LuceneParser")

fun init(directory: String, interval: Int) {
    logger.info("Lucene parser init, directory: ${directory}, interval: ${interval} minutes")
    Timer("Parser").schedule(
        0, TimeUnit.MINUTES.toMillis(interval.toLong())) {
        println("Parse START init....")
        parse()
    }
}

fun toDateString(ft: FileTime): String {
    return DateTools.timeToString(ft.toMillis(), DateTools.Resolution.MINUTE)
}



fun parse() {
    println("Parse START")
    //We open a File System directory as we want to store the index on our local file system.
    val directory: Directory = FSDirectory.open(Paths.get("lucene_index"))

    //The analyzer is used to perform analysis on text of documents and create the terms that will be added in the index.
    val analyzer: Analyzer = GreekAnalyzer()
    val indexWriterConfig = IndexWriterConfig(analyzer)
    val indexWriter = IndexWriter(directory, indexWriterConfig)

    var config = TikaConfig(object {}.javaClass.getResourceAsStream("/tika-config.xml"))
    val tika = Tika(config)
    val dir = File("c:/users/serafeim/desktop")
    runBlocking {
        val jobs = mutableListOf<Job>()

        dir.walk(direction = FileWalkDirection.TOP_DOWN).forEach {
            if (listOf("doc", "docx").contains(it.extension.lowercase())) {
                val job = GlobalScope.launch {
                    println(it.name)
                    val attrs = Files.readAttributes<BasicFileAttributes>(Paths.get(it.path), BasicFileAttributes::class.java)

                    val content = tika.parseToString(it.absoluteFile)

                    //val doc = SolrInputDocument()
                    val doc = Document()

                    doc.add(StringField("id", it.path, Field.Store.YES))
                    doc.add(TextField("text", content, Field.Store.YES))
                    doc.add(StringField("name", it.name, Field.Store.YES))
                    it.path.split(File.separator).forEach {
                        doc.add(StringField("path", it, Field.Store.YES))
                    }
                    doc.add(StringField("extension", it.name, Field.Store.YES))

                    doc.add(StringField("created", toDateString(attrs.creationTime()), Field.Store.YES))
                    doc.add(StringField("accessed", toDateString(attrs.lastAccessTime()), Field.Store.YES))
                    doc.add(StringField("modified", toDateString(attrs.lastModifiedTime()), Field.Store.YES))

                    doc.add(LongPoint("created_point", attrs.creationTime().toMillis()))
                    doc.add(LongPoint("accessed_point", attrs.lastAccessTime().toMillis()))
                    doc.add(LongPoint("modified_point", attrs.lastModifiedTime().toMillis()))

                    val idTerm = Term("id", it.path)
                    indexWriter.updateDocument(idTerm, doc)
                }
                jobs.add(job)
            }
        }
        jobs.joinAll()
    }
    //client.commit("docs")

    println("Docs Indexed Successfully!")

    indexWriter.close()
}
package gr.serafeim.parser

import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.TextField
import org.apache.lucene.document.StringField
import org.apache.lucene.analysis.el.GreekAnalyzer
import org.apache.lucene.document.Field

import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule


val logger = LoggerFactory.getLogger("LuceneParser")

fun init(directory: String, interval: Int) {
    logger.info("Lucene parser init, directory: ${directory}, interval: ${interval} minutes")
    Timer("Parser").schedule(
        0, TimeUnit.MINUTES.toMillis(1)) {
        println("Parse START init....")
        parse()
    }
}
fun parse() {
    println("Parse START")
    //We open a File System directory as we want to store the index on our local file system.
    val directory: Directory = FSDirectory.open(Paths.get("lucene_index"))

    //The analyzer is used to perform analysis on text of documents and create the terms that will be added in the index.
    val analyzer: Analyzer = GreekAnalyzer()
    val indexWriterConfig = IndexWriterConfig(analyzer)
    val indexWriter = IndexWriter(directory, indexWriterConfig)

    //Now we create three documents for 3 movies. We have only one Field called title in each document.
    val movie1 = Document()
    movie1.add(TextField("title", "Harry Potter and the Prisoner of Azkaban", Field.Store.YES))

    val movie2 = Document()
    movie2.add(TextField("title", "Lord of the Rings: The fellowship of the ring.", Field.Store.YES))

    val movie3 = Document()
    movie3.add(TextField("title", "Toy Story 3", Field.Store.YES))

    println("Going to index 3 movies.")

    //Now we add the three documents to our index.

    //Now we add the three documents to our index.
    indexWriter.addDocument(movie1)
    indexWriter.addDocument(movie2)
    indexWriter.addDocument(movie3)
    println("Movies Indexed Successfully!")

    indexWriter.close()
}
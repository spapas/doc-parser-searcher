package gr.serafeim.parser

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
import org.mapdb.DBMaker.fileDB
import org.mapdb.HTreeMap
import org.mapdb.Serializer
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule


val logger = LoggerFactory.getLogger("LuceneParser")

fun init(directory: String, interval: Int) {
    logger.info("Lucene parser init, directory: ${directory}, interval: ${interval} minutes")
    Timer("Parser").schedule(
        0, TimeUnit.MINUTES.toMillis(interval.toLong())) {
        logger.debug("Parse START init....")
        parse(directory)
    }
}

fun toDateString(ft: FileTime): String {
    return DateTools.timeToString(ft.toMillis(), DateTools.Resolution.MINUTE)
}

fun configureTika(): Tika {
    var config = TikaConfig(object {}.javaClass.getResourceAsStream("/tika-config.xml"))
    val tika = Tika(config)
    // Allow tikato read unlimited characters
    tika.maxStringLength = -1
    logger.debug("Will read up to ${tika.maxStringLength} length")
    return tika
}

fun configureIndexWriter(dir: String): IndexWriter {
    //We open a File System directory as we want to store the index on our local file system.
    val directory: Directory = FSDirectory.open(Paths.get("lucene_index"))
    //The analyzer is used to perform analysis on text of documents and create the terms that will be added in the index.
    val analyzer: Analyzer = GreekAnalyzer()
    val indexWriterConfig = IndexWriterConfig(analyzer)
    // NOTE: IndexWriter instances are completely thread safe, meaning multiple threads can call any of its methods, concurrently. If your application requires external synchronization, you should not synchronize on the IndexWriter instance as this may cause deadlock; use your own (non-Lucene) objects instead.
    val indexWriter = IndexWriter(directory, indexWriterConfig)
    return indexWriter
}

fun parseDocument(it: File, indexWriter: IndexWriter, tika: Tika, map: HTreeMap<String, Long>) {
    println(it.name)
    val attrs = Files.readAttributes<BasicFileAttributes>(Paths.get(it.path), BasicFileAttributes::class.java)
    val modified = attrs.lastModifiedTime().toMillis()

    val existingModTime = map.get(it.path)
    logger.debug("Existing mod time is ${existingModTime} and current mod time is ${modified}")
    if(existingModTime==null || existingModTime < modified) {
        logger.debug("Need to parse and index ${it.name}")
        map.put(it.path, modified)
        val content = tika.parseToString(it.absoluteFile)
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
    } else {
        logger.debug("Skipping the file")
    }
}

fun parse(dir: String) {
    logger.info("Parse START")

    val db = fileDB("map.db").make()
    val map = db.hashMap("docs", Serializer.STRING, Serializer.LONG).createOrOpen()

    val tika = configureTika()
    val indexWriter = configureIndexWriter(dir)

    var uniquePaths = ConcurrentHashMap.newKeySet<String>()

    val dir = File(dir)
    runBlocking {
        val jobs = mutableListOf<Job>()

        dir.walk(direction = FileWalkDirection.TOP_DOWN).forEach {
            if (listOf("doc", "docx").contains(it.extension.lowercase())) {
                uniquePaths.add(it.path)
                val job = GlobalScope.launch {
                    parseDocument(it, indexWriter, tika, map)
                }
                jobs.add(job)
            }
        }
        jobs.joinAll()
    }

    clearDeleted(map, uniquePaths, indexWriter)

    db.commit()
    db.close()

    println("Docs Indexed Successfully!")
    indexWriter.close()
}

private fun clearDeleted(
    map: HTreeMap<String, Long>,
    uniquePaths: ConcurrentHashMap.KeySetView<String, Boolean>,
    indexWriter: IndexWriter
) {
    val existingPathsSet = map.map { it.key }.toSet()
    val uniquePathsSet = uniquePaths.toSet()
    val remaining = existingPathsSet.minus(uniquePathsSet)
    println("Remaining ${remaining}")
    remaining.forEach {
        map.remove(it)
        val idTerm = Term("id", it)
        indexWriter.deleteDocuments(idTerm)
    }
}
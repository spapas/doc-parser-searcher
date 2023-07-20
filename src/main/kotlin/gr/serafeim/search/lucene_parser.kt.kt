package gr.serafeim.search

import gr.serafeim.conf.ConfigHolder
import gr.serafeim.DBHolder
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.codecs.PostingsFormat
import org.apache.lucene.document.*
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import org.apache.tika.Tika
import org.apache.tika.config.TikaConfig
import org.mapdb.HTreeMap
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
import kotlin.io.path.absolutePathString


val logger = LoggerFactory.getLogger("LuceneParser")

fun init(directory: String, interval: Int) {
    val x =  PostingsFormat.availablePostingsFormats()
    if(!x.contains("Lucene90")) {
        throw Exception("Lucene90 Not found!")
    }

    logger.info("Lucene parser init, directory: $directory, interval: $interval minutes")
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

    var config = if (ConfigHolder.config.parser.externalTikaConfig == null) {
        TikaConfig(object {}.javaClass.getResourceAsStream("/tika-config.xml"))
    } else {
        TikaConfig(ConfigHolder.config.parser.externalTikaConfig)
    }

    val tika = Tika(config)
    // Allow tika to read unlimited characters
    tika.maxStringLength = -1
    logger.debug("Will read up to ${tika.maxStringLength} length")
    return tika
}

fun configureIndexWriter(): IndexWriter {
    //We open a File System directory as we want to store the index on our local file system.
    val directory: Directory = getLuceneDir()

    //The analyzer is used to perform analysis on text of documents and create the terms that will be added in the index.
    val analyzer: Analyzer = ConfigHolder.getAnalyzerInstance()
    val indexWriterConfig = IndexWriterConfig(analyzer)

    // NOTE: IndexWriter instances are completely thread safe, meaning multiple threads can call any of its methods, concurrently. If your application requires external synchronization, you should not synchronize on the IndexWriter instance as this may cause deadlock; use your own (non-Lucene) objects instead.
    val indexWriter = IndexWriter(directory, indexWriterConfig)

    return indexWriter
}

fun parseDocument(it: File, indexWriter: IndexWriter, tika: Tika, map: HTreeMap<String, Any>) {
    logger.debug(it.name)
    val attrs = Files.readAttributes(Paths.get(it.path), BasicFileAttributes::class.java)
    val modified = attrs.lastModifiedTime().toMillis()

    val existingModTime: Pair<Boolean, Long>? = (map[it.path] as Pair<Boolean, Long>?)
    logger.debug("Existing mod time is $existingModTime and current mod time is $modified")
    if(existingModTime==null || existingModTime.second < modified) {
        logger.debug("Need to parse and index ${it.name}")
        var content: String? = null;
        try {
            content = tika.parseToString(it.absoluteFile)
            map[it.path] = Pair(true, modified)
        } catch (e: Exception) {
            e.printStackTrace()
            logger.info("File ${it.path} cannot be parsed, skipping")
            map[it.path] = Pair(false, modified)
        }

        if (content!= null) {
            val doc = Document()

            doc.add(StringField("id", it.path, Field.Store.YES))
            doc.add(TextField("text", content, Field.Store.YES))
            doc.add(StringField("name", it.name, Field.Store.YES))
            doc.add(TextField("name_t", it.name, Field.Store.NO))
            it.path.split(File.separator).forEach {
                doc.add(StringField("path", it, Field.Store.YES))
            }
            doc.add(StringField("extension", it.extension, Field.Store.YES))

            doc.add(StringField("created", toDateString(attrs.creationTime()), Field.Store.YES))
            doc.add(StringField("accessed", toDateString(attrs.lastAccessTime()), Field.Store.YES))
            doc.add(StringField("modified", toDateString(attrs.lastModifiedTime()), Field.Store.YES))

            doc.add(LongPoint("created_point", attrs.creationTime().toMillis()))
            doc.add(LongPoint("modified_point", attrs.lastModifiedTime().toMillis()))
            doc.add(LongPoint("accessed_point", attrs.lastAccessTime().toMillis()))

            val idTerm = Term("id", it.path)
            indexWriter.updateDocument(idTerm, doc)
        }
    } else {
        logger.debug("Skipping the file, not changed since we last saw it")
    }
}

fun parse(sdir: String) {
    logger.info("Parse START, extensions are ${ConfigHolder.config.parser.parseExtensions}")
    logger.info("Parse directory is ${Paths.get(sdir).absolutePathString()}")

    val tika = configureTika()
    val indexWriter = configureIndexWriter()

    var uniquePaths = ConcurrentHashMap.newKeySet<String>()

    val dir = File(sdir)
    val requestSemaphore = Semaphore(4)
    runBlocking {
        val jobs = mutableListOf<Job>()
        var totJobs = 0;
        //logger.info("Run the blocking")
        dir.walk(direction = FileWalkDirection.TOP_DOWN).forEach {
            //logger.info("Waqlking .. ${it.name}")
            if (!it.name.startsWith("~$")) {
                if (ConfigHolder.config.parser.parseExtensions.contains(it.extension.lowercase())) {
                    //logger.info("Parsing ${it.path}")
                    uniquePaths.add(it.path)

                    val job = GlobalScope.launch {
                        requestSemaphore.withPermit {
                            totJobs += 1
                            logger.debug("Start job, $totJobs")
                            parseDocument(it, indexWriter, tika, DBHolder.map)
                            totJobs -= 1
                            logger.debug("End job, $totJobs")
                        }
                    }
                    jobs.add(job)
                }
            }
        }
        jobs.joinAll()
    }

    clearDeleted(DBHolder.map, uniquePaths, indexWriter)

    DBHolder.db.commit()

    logger.info("Docs Indexed Successfully!")
    indexWriter.close()
}

private fun clearDeleted(
    map: HTreeMap<String, Any>,
    uniquePaths: ConcurrentHashMap.KeySetView<String, Boolean>,
    indexWriter: IndexWriter
) {
    val existingPathsSet = map.map { it.key }.toSet()
    val uniquePathsSet = uniquePaths.toSet()
    val remaining = existingPathsSet.minus(uniquePathsSet)
    logger.debug("Clear deleted, remaining ${remaining}")
    remaining.forEach {
        map.remove(it)
        val idTerm = Term("id", it)
        indexWriter.deleteDocuments(idTerm)
    }
}
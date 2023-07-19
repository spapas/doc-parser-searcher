package gr.serafeim

import gr.serafeim.conf.ConfigHolder
import mu.KotlinLogging
import org.mapdb.DBMaker
import org.mapdb.Serializer
import org.slf4j.LoggerFactory
import java.nio.file.Paths

object DBHolder {
    val logger = KotlinLogging.logger {}
    val db = DBMaker.fileDB(
        Paths.get(ConfigHolder.config.parser.dataDirectory, "map.db").toFile()
    ).transactionEnable().make()
    
    val map = db.hashMap("docs").keySerializer(Serializer.STRING).valueSerializer(Serializer.JAVA).createOrOpen()
    init {
        logger.info("DB Singleton class invoked.")

    }

}
package gr.serafeim

import gr.serafeim.conf.ConfigHolder
import org.mapdb.DBMaker
import org.mapdb.Serializer
import org.slf4j.LoggerFactory
import java.nio.file.Paths


fun getFileDB(): DBMaker.Maker {
    return DBMaker.fileDB(
        Paths.get(ConfigHolder.config.parser.dataDirectory, "map.db").toFile()
    )
}

object DBHolder {
    val logger = LoggerFactory.getLogger("DBHolder")
    val db = getFileDB().transactionEnable().make()
    val map = db.hashMap("docs").keySerializer(Serializer.STRING).valueSerializer(Serializer.JAVA).createOrOpen()

    init {
        logger.info("DB Singleton class invoked.")
    }
}
package gr.serafeim

import org.mapdb.DBMaker
import org.mapdb.Serializer
import org.slf4j.LoggerFactory

object DBHolder {
    val logger = LoggerFactory.getLogger("DB")
    val db = DBMaker.fileDB("map.db").transactionEnable().make()
    val map = db.hashMap("docs").keySerializer(Serializer.STRING).valueSerializer(Serializer.JAVA).createOrOpen()
    init {
        logger.info("DB Singleton class invoked.")

    }

}
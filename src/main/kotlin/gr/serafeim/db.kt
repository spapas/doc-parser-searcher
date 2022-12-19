package gr.serafeim

import org.mapdb.DBMaker
import org.mapdb.Serializer
import org.slf4j.LoggerFactory

object DBHolder {
    val logger = LoggerFactory.getLogger("DB")
    val db = DBMaker.fileDB("map.db").transactionEnable().make()
    val map = db.hashMap("docs", Serializer.STRING, Serializer.LONG).createOrOpen()
    init {
        logger.info("DB Singleton class invoked.")

    }

}
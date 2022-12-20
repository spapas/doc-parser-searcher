package gr.serafeim

import org.apache.lucene.analysis.Analyzer
import org.slf4j.LoggerFactory

object GlobalsHolder {
    val logger = LoggerFactory.getLogger("Globals")
    private lateinit var analyzerClazz: Class<*>

    init {
        logger.info("Globals Singleton class invoked.")
    }

    fun setAnalyzerClass(s: String): Unit {
        analyzerClazz = Class.forName(s)
    }

    fun getAnalyzerInstance(): Analyzer {
        val t = analyzerClazz.getDeclaredConstructor().newInstance()
        if(t is Analyzer) {
            return t
        } else{
            throw Exception("$analyzerClazz is not an analyzer")
        }
    }

}
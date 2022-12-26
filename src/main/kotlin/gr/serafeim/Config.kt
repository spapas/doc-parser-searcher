package gr.serafeim

import com.sksamuel.hoplite.*
import com.sksamuel.hoplite.decoder.Decoder
import com.sksamuel.hoplite.decoder.toValidated
import com.sksamuel.hoplite.fp.invalid
import io.ktor.util.reflect.*
import org.apache.lucene.analysis.Analyzer
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KTypeProjection.Companion.STAR
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf


data class Parser(val directory: String, val interval: Int, val pageSize: Int, val parseExtensions: List<String>, val analyzerClazzString: String)
data class Server(val host: String, val port: Int, val userUsername: String, val userPassword: String, val adminUsername: String, val adminPassword: String)
data class Config(val env: String, val parser: Parser, val server: Server)


object ConfigHolder {
    val logger = LoggerFactory.getLogger("Config")
    private lateinit var analyzerClazz: Class<*>

    val config = ConfigLoaderBuilder.default()
        .addResourceSource("/application.props")

        .build()
        .loadConfigOrThrow<Config>()
    init {
        logger.info("Config ok!")
        logger.info(config.toString())
        analyzerClazz = Class.forName(config.parser.analyzerClazzString)

    }

    fun getAnalyzerInstance(): Analyzer {
        val t = ConfigHolder.analyzerClazz.getDeclaredConstructor().newInstance()
        if(t is Analyzer) {
            return t
        } else{
            throw Exception("${ConfigHolder.analyzerClazz} is not an analyzer")
        }
    }
}
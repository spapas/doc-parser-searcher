package gr.serafeim.conf

import com.sksamuel.hoplite.*
import org.apache.lucene.analysis.Analyzer
import org.slf4j.LoggerFactory
import java.io.File


data class Parser(val parseDirectory: String,
                  val dataDirectory: String,
                  val interval: Int,
                  val pageSize: Int,
                  val parseExtensions: List<String>,
                  val analyzerClazzString: String,
                  val externalTikaConfig: String?)
data class Server(val host: String, val port: Int, val userUsername: String, val userPassword: String, val adminUsername: String, val adminPassword: String)
data class Config(val parser: Parser, val server: Server)


object ConfigHolder {
    val logger = LoggerFactory.getLogger("ConfigHolder")
    private lateinit var analyzerClazz: Class<*>
    lateinit var config: Config

    fun init(f: File?) {

        val configLB = ConfigLoaderBuilder.default()

        if(f!=null) {
            // logger.info("Config with $f")
            configLB.addFileSource(f, optional = false)
        }

        config = configLB
            .addResourceSource("/application.props")
            .build()
            .loadConfigOrThrow<Config>()
        analyzerClazz = Class.forName(config.parser.analyzerClazzString)

    }

    fun getAnalyzerInstance(): Analyzer {
        val t = analyzerClazz.getDeclaredConstructor().newInstance()
        if(t is Analyzer) {
            return t
        } else {
            throw Exception("$analyzerClazz is not an analyzer")
        }
    }
}
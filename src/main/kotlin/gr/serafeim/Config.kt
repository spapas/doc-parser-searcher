package gr.serafeim

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import org.slf4j.LoggerFactory

data class Parser(val directory: String, val interval: Int, val pageSize: Int, val analyzerClazz: String, val parseExtensions: List<String>)
data class Server(val host: String, val port: Int, val userUsername: String, val userPassword: String, val adminUsername: String, val adminPassword: String)
data class Config(val env: String, val parser: Parser, val server: Server)



object ConfigHolder {
    val logger = LoggerFactory.getLogger("Config")

    val config = ConfigLoaderBuilder.default()
        .addResourceSource("/application.props")
        //.addDecoder()
        .build()
        .loadConfigOrThrow<Config>()
    init {
        logger.info("Config ok!")
        logger.info(config.toString())
    }
}
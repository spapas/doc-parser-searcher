package gr.serafeim

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource

data class Parser(val directory: String, val interval: Int, val pageSize: Int, val analyzerClazz: String, val parseExtensions: String)
data class Server(val port: Int, val userUsername: String, val userPassword: String, val adminUsername: String, val adminPassword: String)
data class Config(val env: String, val parser: Parser, val server: Server)


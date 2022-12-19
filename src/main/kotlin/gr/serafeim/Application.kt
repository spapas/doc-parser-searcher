package gr.serafeim

import com.mitchellbosecke.pebble.loader.ClasspathLoader
import gr.serafeim.web.*
import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.routing.*
import org.slf4j.Logger


import org.slf4j.LoggerFactory

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

val logger: Logger = LoggerFactory.getLogger(Application::class.java)

fun Application.module() {

    val directory = environment.config.propertyOrNull("parser.directory")?.getString() ?: "."
    val interval = environment.config.propertyOrNull("parser.interval")?.getString()?.toInt() ?: 60
    val pageSize = environment.config.propertyOrNull("ktor.pageSize")?.getString()?.toInt() ?: 10

    gr.serafeim.parser.init(directory, interval)

    install(Pebble) {
        loader(ClasspathLoader().apply {
            prefix = "templates"
        })
    }

    routing {
        listKeysRoute()
        downloadFile()
        index(pageSize)
    }
}

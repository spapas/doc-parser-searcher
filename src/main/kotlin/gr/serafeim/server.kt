package gr.serafeim

import gr.serafeim.conf.ConfigHolder
import gr.serafeim.web.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import io.ktor.server.pebble.*
import io.ktor.server.routing.*
import io.pebbletemplates.pebble.loader.ClasspathLoader
import org.slf4j.LoggerFactory


object StateHolder {
    var parsing = false
}

fun server() {
    val logger = LoggerFactory.getLogger("server")
    logger.info("Starting server...")
    val config = ConfigHolder.config

    val userUsername = config.server.userUsername
    val userPassword = config.server.userPassword
    val adminUsername = config.server.adminUsername
    val adminPassword = config.server.adminPassword

    gr.serafeim.search.init(config.parser.parseDirectory, config.parser.interval)

    embeddedServer(Jetty, port = config.server.port, host = config.server.host, watchPaths = listOf("classes", "resources")) {
        install(Pebble) {
            loader(ClasspathLoader().apply {
                prefix = "templates"
            })
        }

        install(Authentication) {
            basic("auth-basic-user") {
                realm = "User access"
                validate { credentials ->
                    if (credentials.name == userUsername && credentials.password == userPassword) {
                        UserIdPrincipal(credentials.name)
                    } else {
                        null
                    }
                }
            }

            basic("auth-basic-admin") {
                realm = "Admin access"
                validate { credentials ->
                    if (credentials.name == adminUsername && credentials.password == adminPassword) {
                        UserIdPrincipal(credentials.name)
                    } else {
                        null
                    }
                }
            }
        }

        routing {

            if (userUsername != "" && userPassword != "") {
                authenticate("auth-basic-user") {
                    index(config.parser.pageSize)
                    aboutRoute()
                    downloadFile()
                }
            } else {
                index(config.parser.pageSize)
                aboutRoute()
                downloadFile()
            }
            if (adminUsername != "" && adminPassword != "") {
                authenticate("auth-basic-admin") {
                    listKeysRoute()
                    statusRoute()
                }
            } else {
                listKeysRoute()
                statusRoute()
            }
        }
    }.start(wait = true)

}

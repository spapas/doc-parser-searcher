package gr.serafeim

import com.mitchellbosecke.pebble.loader.ClasspathLoader
import gr.serafeim.conf.ConfigHolder
import gr.serafeim.web.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import io.ktor.server.pebble.*
import io.ktor.server.routing.*
import mu.KotlinLogging

fun server() {
    val logger = KotlinLogging.logger {}

    val config = ConfigHolder.config

    val userUsername = config.server.userUsername
    val userPassword = config.server.userPassword
    val adminUsername = config.server.adminUsername
    val adminPassword = config.server.adminPassword

    gr.serafeim.parser.init(config.parser.directory, config.parser.interval)

    embeddedServer(Jetty, port = config.server.port, host = config.server.host) {
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
                    downloadFile()
                }
            } else {
                index(config.parser.pageSize)
                downloadFile()
            }
            if (adminUsername != "" && adminPassword != "") {
                authenticate("auth-basic-admin") {
                    listKeysRoute()
                }
            } else {
                listKeysRoute()
            }
        }
    }.start(wait = true)

}

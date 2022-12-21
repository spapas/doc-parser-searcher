package gr.serafeim

import com.mitchellbosecke.pebble.loader.ClasspathLoader
import gr.serafeim.web.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
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

    val analyzerClazz = environment.config.propertyOrNull("parser.analyzer_clazz")?.getString()?: "org.apache.lucene.analysis.el.GreekAnalyzer"
    val parseExtensions = environment.config.propertyOrNull("parser.extensions")?.getString()?: "doc,docx,xls,xlsx"

    val userUsername = environment.config.propertyOrNull("ktor.auth.user_username")?.getString() ?: "."
    val userPassword = environment.config.propertyOrNull("ktor.auth.user_password")?.getString() ?: "."
    val adminUsername = environment.config.propertyOrNull("ktor.auth.admin_username")?.getString() ?: "."
    val adminPassword = environment.config.propertyOrNull("ktor.auth.admin_password")?.getString() ?: "."

    GlobalsHolder.setAnalyzerClass(analyzerClazz)
    GlobalsHolder.setExtensions(parseExtensions)

    gr.serafeim.parser.init(directory, interval)

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

        if(userUsername != "" && userPassword != "") {
            authenticate("auth-basic-user") {
                index(pageSize)
                downloadFile()
            }
        } else {
            index(pageSize)
            downloadFile()
        }
        if(adminUsername != "" && adminPassword != "") {
            authenticate("auth-basic-admin") {
                listKeysRoute()
            }
        } else {
            listKeysRoute()
        }
    }
}

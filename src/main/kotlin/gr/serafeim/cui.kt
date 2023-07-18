package gr.serafeim

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import gr.serafeim.conf.ConfigHolder
import gr.serafeim.parser.logger
import gr.serafeim.web.SearchParams

class Main: CliktCommand() {
    val configFile by option("-c", "--config", help="Config file").file()
    val operation by argument("operation", help="What to run").choice("server", "clear", "index", "search")
    //val search by argument(help="What to search for").optional()

    override fun run() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "TRACE")
        ConfigHolder.init(configFile)
        println(ConfigHolder.config)
        if(operation == "server") {
            server()
        } else if(operation == "clear") {
            println("clear")
        } else if(operation == "search") {
            println("Search")
            val sh = SearchHolder.search(SearchParams("do", 10, 1))
            for(r: Result in sh.results) {
                println(r.path)
            }

        } else if(operation == "index") {
            println("Index")
        }
    }
}

fun main(args: Array<String>) = Main().main(args)
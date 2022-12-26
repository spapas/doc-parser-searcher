package gr.serafeim

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import gr.serafeim.conf.ConfigHolder

class Hello: CliktCommand() {
    val configFile by option("-c", "--config", help="Config file").file()
    val operation by argument("operation", help="What to run").choice("server", "clear", "index", "search")
    //val search by argument(help="What to search for").optional()

    override fun run() {
        ConfigHolder.init(configFile)
        println(ConfigHolder.config)
        if(operation == "server") {
            server()
        } else if(operation == "clear") {
            println("clear")
        } else if(operation == "search") {
            println("Search")
        } else if(operation == "index") {
            println("Index")
        }
    }
}
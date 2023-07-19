package gr.serafeim

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import gr.serafeim.conf.ConfigHolder
import gr.serafeim.search.Result
import gr.serafeim.search.SearchHolder
import gr.serafeim.search.parse
import gr.serafeim.web.SearchParams

class Server(): CliktCommand() {
    override fun run() {
        server()
    }
}

class Search(): CliktCommand() {
    val search by argument(help="What to search for")
    override fun run() {
        println("Search")
        val sh = SearchHolder.search(SearchParams(search, 10, 1))
        if(sh.total == 0) {
            println("Empty results")
        }
        for(r: Result in sh.results) {
            println(r.path)

        }
    }
}

class Info(): CliktCommand() {
    override fun run() {
        println("Info")
        println(ConfigHolder.config)

    }
}

class Main: CliktCommand() {
    val configFile by option("-c", "--config", help="Config file").file()
    val loglevel by option("--loglevel", help="Log level (default=INFO)").choice("INFO", "DEBUG", "ERROR", "WARNING")
    override fun run() {
        ConfigHolder.init(configFile)
        println("Main $loglevel")
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", loglevel?:"INFO")

    }
}

class Parse(): CliktCommand() {
    override fun run() {
        val dir = ConfigHolder.config.parser.parseDirectory
        println("Parsing, from directory $dir")
        parse(dir)
    }
}


fun main(args: Array<String>) = Main().subcommands(
    Server(), Search(), Parse(), Info()
).main(args)
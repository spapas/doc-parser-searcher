package gr.serafeim

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import gr.serafeim.conf.ConfigHolder
import gr.serafeim.search.Result
import gr.serafeim.search.SearchHolder
import gr.serafeim.search.getLuceneDirName
import gr.serafeim.search.parse
import gr.serafeim.web.SearchParams
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

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
        println("- Config")
        println(ConfigHolder.config)

        println("- Number of docs on map ${DBHolder.map.keys.size}")
        try {
            val sh = SearchHolder.getTotalDocs()
            println("- Number of docs on index $sh")
        } catch (e: Throwable) {
            println("- No lucene index")
        }

    }
}

class Main: CliktCommand() {
    val configFile by option("-c", "--config", help="Config file").file()

    override fun run() {
        ConfigHolder.init(configFile)

    }
}

class Parse(): CliktCommand() {
    override fun run() {
        val dir = ConfigHolder.config.parser.parseDirectory
        println("Parsing, from directory $dir")
        parse(dir)
    }
}

class Clear(): CliktCommand() {
    override fun run() {
        val dir = ConfigHolder.config.parser.dataDirectory
        println("Clearing data from directory $dir")
        DBHolder.map.clear()
        DBHolder.db.commit()
        val luceneDirName = getLuceneDirName()
        Files.walk(luceneDirName)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }
}

fun main(args: Array<String>) = Main().subcommands(
    Server(), Search(), Parse(), Info(), Clear()
).main(args)
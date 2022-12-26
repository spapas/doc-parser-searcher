package gr.serafeim

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import gr.serafeim.conf.ConfigHolder

class Hello: CliktCommand() {
    val configFile by option(help="Config file").file()
    override fun run() {
        echo("Hello World!")
        ConfigHolder.init(configFile)
        println(ConfigHolder.config)
    }
}
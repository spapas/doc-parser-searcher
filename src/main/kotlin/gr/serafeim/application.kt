package gr.serafeim

import mu.KotlinLogging

fun main(args: Array<String>) {

    val logger = KotlinLogging.logger {}
    logger.info("DB ok, has ${DBHolder.map.keys.size} keys!")
    Hello().main(args)
}

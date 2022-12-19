import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    id("io.ktor.plugin") version "2.2.1"

}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

group = "gr.hcg"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
}

val ktor_version = "2.2.1"
val tika_version = "2.6.0"
val lucene_version = "9.4.2"

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.apache.tika:tika-core:$tika_version")
    implementation("org.apache.tika:tika-parsers:$tika_version")
    implementation("org.apache.tika:tika-parsers-standard-package:$tika_version")

    implementation("org.apache.logging.log4j:log4j-core:2.19.0")
    implementation("org.slf4j:slf4j-api:2.0.5")
    implementation("org.slf4j:slf4j-simple:2.0.5")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-pebble:$ktor_version")

    implementation("org.apache.lucene:lucene-core:$lucene_version")
    implementation("org.apache.lucene:lucene-queryparser:$lucene_version")
    implementation("org.apache.lucene:lucene-highlighter:$lucene_version")
    implementation("org.apache.lucene:lucene-analyzers-common:8.11.2")
    //implementation("org.apache.lucene:lucene-analyzers-common:$lucene_version")

    implementation("org.mapdb:mapdb:3.0.9")


}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}


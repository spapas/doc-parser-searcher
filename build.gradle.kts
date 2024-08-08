import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar


plugins {
    application
    kotlin("jvm") version "2.0.10"
    id("io.ktor.plugin") version "2.3.12"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
    mainClass.set("gr.serafeim.CuiKt")

}

group = "gr.hcg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktor_version = "2.3.12"
val tika_version = "2.9.2"
val lucene_version = "9.11.1"

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.apache.tika:tika-core:$tika_version")
    implementation("org.apache.tika:tika-parsers:$tika_version")
    implementation("org.apache.tika:tika-parsers-standard-package:$tika_version")

    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    implementation("org.slf4j:slf4j-api:2.0.14")
    implementation("org.slf4j:slf4j-log4j12:2.0.14")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-jetty:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-pebble:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-ldap:$ktor_version")

    implementation("org.apache.lucene:lucene-core:$lucene_version")
    implementation("org.apache.lucene:lucene-codecs:$lucene_version")
    implementation("org.apache.lucene:lucene-queryparser:$lucene_version")
    implementation("org.apache.lucene:lucene-highlighter:$lucene_version")
    implementation("org.apache.lucene:lucene-backward-codecs:$lucene_version")
    implementation("org.apache.lucene:lucene-analysis-common:$lucene_version")
    implementation("org.apache.lucene:lucene-memory:$lucene_version")

    implementation("com.github.ajalt.clikt:clikt:4.4.0")

    implementation("com.sksamuel.hoplite:hoplite-core:2.7.5")
    implementation("org.mapdb:mapdb:3.1.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "18"
}


tasks {
    named<ShadowJar>("shadowJar") {
        mergeServiceFiles()
        archiveBaseName.set("shadow")
    }

}
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar


plugins {
    application
    kotlin("jvm") version "1.9.0"
    id("io.ktor.plugin") version "2.3.2"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

application {
    mainClass.set("gr.serafeim.CuiKt")

}

group = "gr.hcg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktor_version = "2.3.7"
val tika_version = "2.9.1"
val lucene_version = "9.9.1"

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.apache.tika:tika-core:$tika_version")
    implementation("org.apache.tika:tika-parsers:$tika_version")
    implementation("org.apache.tika:tika-parsers-standard-package:$tika_version")

    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    //implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    //implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.20.0")

    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("org.slf4j:slf4j-log4j12:2.0.7")


    // implementation("org.slf4j:slf4j-simple:2.0.7")


    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")

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

    implementation("com.github.ajalt.clikt:clikt:4.2.1")

    implementation("com.sksamuel.hoplite:hoplite-core:2.7.5")
    implementation("org.mapdb:mapdb:3.0.10")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "14"
}


tasks {
    named<ShadowJar>("shadowJar") {
        mergeServiceFiles()
        archiveBaseName.set("shadow")
    }

}
plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("io.github.goooler.shadow") version "8.1.8"
    application
}

group = "com.arkhamdb.mcp"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // MCP SDK
    implementation("io.modelcontextprotocol:kotlin-sdk:0.8.4")
    implementation("io.modelcontextprotocol:kotlin-sdk-server:0.8.4")

    // Kotlinx IO for STDIO transport
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.6.0")

    // Ktor client with CIO engine
    implementation("io.ktor:ktor-client-cio:3.1.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.1")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("org.slf4j:slf4j-api:2.0.16")

    // PDF parsing
    implementation("org.apache.pdfbox:pdfbox:3.0.3")

    // Testing
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.arkhamdb.mcp.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveBaseName.set("arkhamdb-mcp-server")
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = "com.arkhamdb.mcp.MainKt"
    }
}

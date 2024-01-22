plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    id(libs.plugins.spring.kotlin.get().pluginId) version libs.versions.kotlin.get()
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.jackson)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.arrow.suspendapp)
    implementation(libs.arrow.fx.coroutines)
    annotationProcessor(libs.spring.boot.configuration.processor)
}

application {
    mainClass.set("org.example.Main.ExampleApp")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
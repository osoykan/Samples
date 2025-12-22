plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.kotlin)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spotless)
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring WebFlux
    implementation(libs.spring.boot.starter.webflux)
    annotationProcessor(libs.spring.boot.configuration.processor)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)

    // Testing
    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.extensions.spring)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.reactor.test)
    testImplementation(libs.turbine)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(
                mapOf(
                    "max_line_length" to "140",
                    "ktlint_standard_no-wildcard-imports" to "disabled",
                    "ktlint_standard_package-name" to "disabled",
                    "ktlint_standard_function-naming" to "disabled",
                    "ktlint_standard_filename" to "disabled",
                ),
            )
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
    }
}

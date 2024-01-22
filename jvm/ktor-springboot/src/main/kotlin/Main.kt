package org.example

import arrow.continuations.SuspendApp
import arrow.fx.coroutines.resourceScope
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.awaitCancellation
import org.springframework.boot.*
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.stereotype.Component


@SpringBootApplication
@ComponentScan
class ExampleApp

fun main(args: Array<String>) = SuspendApp {
    resourceScope {
        run(args)
        awaitCancellation()
    }

}

private fun run(
    args: Array<String>,
    configure: SpringApplication.() -> Unit = {}
) = runApplication<ExampleApp>(*args) {
    webApplicationType = WebApplicationType.NONE
    configure()
}

@Component
class HelloWorldService {
    fun helloWorld(): String = "Hello World"
}

@Component
class KtorApplicationRunner(private val ctx: ApplicationContext) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        embeddedServer(Netty, port = 8080) {
            register(ctx)
            routing {
                get("/") {
                    val helloWorldService: HelloWorldService = call.application.inject()
                    call.respondText(helloWorldService.helloWorld())
                }
            }
        }.start(wait = false)
    }
}
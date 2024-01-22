package org.example

import io.ktor.server.application.*
import io.ktor.util.*
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.registerBean

/**
 * An [AttributeKey] for accessing the Spring [ApplicationContext] through the Ktor [Application].
 */
internal val SPRING_KEY = AttributeKey<ApplicationContext>("Spring")

/**
 * Register the given [ApplicationContext] to the given Ktor [Application].
 */
fun Application.register(context: ApplicationContext) {
    attributes.put(SPRING_KEY, context)
}

/**
 * Return the Spring [ApplicationContext] that is associated with this application.
 */
val Application.applicationContext: ApplicationContext get() = attributes[SPRING_KEY]

/**
 * Inject a bean of the given type into the application.
 */
inline fun <reified T : Any> Application.inject(): T = applicationContext.getBean()

/**
 * Inject a bean of the name and type into the application.
 *
 * @param name The name of the bean.
 */
inline fun <reified T : Any> Application.inject(name: String): T = applicationContext.getBean(name, T::class.java)

/**
 * Inject all beans of a given type into the application.
 */
inline fun <reified T : Any> Application.injectAll(): List<T> =
    applicationContext.getBeansOfType(T::class.java).values.toList()

/**
 * Configure the Spring [GenericApplicationContext] for the given Ktor [Application].
 */
fun GenericApplicationContext.configure(application: Application) {
    // Add Ktor configuration to Spring property sources
    environment.conversionService.addConverter(KtorApplicationConfigValueListConverter)
    environment.conversionService.addConverter(KtorApplicationConfigValueStringConverter)
    environment.propertySources.addFirst(KtorPropertySource("ktor", application.environment.config))

    // Register the Ktor application to the container
    registerBean { application }

    // Associate the context with this application
    application.register(this)
}

/**
 * Bootstrap the given Ktor module inside a Spring DI container.
 *
 * @param application The application to run the module in.
 * @param module The module to bootstrap inside the container.
 */
fun GenericApplicationContext.bootstrap(application: Application, module: Application.() -> Unit = {}) {
    refresh()
    module(application)
}
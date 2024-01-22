package org.example

import io.ktor.server.application.*
import io.ktor.server.config.*
import org.springframework.beans.factory.FactoryBean
import org.springframework.core.convert.converter.Converter
import org.springframework.core.env.PropertySource

/**
 * A [PropertySource] for a Spring Container for Ktor applications.
 *
 * @param name The name of the property source.
 * @param source The [ApplicationConfig] to use for the configuration.
 */
class KtorPropertySource(name: String, source: ApplicationConfig) : PropertySource<ApplicationConfig>(name, source) {
    override fun getProperty(path: String): Any? {
        try {
            return source.config(path)
        } catch (e: Exception) {}
        try {
            return source.configList(path)
        } catch (e: Exception) {}
        try {
            return source.property(path)
        } catch (e: Exception) {}
        return null
    }
}

/**
 * A [Converter] that converts an instance of [ApplicationConfigValue] to a string.
 */
object KtorApplicationConfigValueStringConverter : Converter<ApplicationConfigValue, String> {
    override fun convert(source: ApplicationConfigValue): String? = source.getString()
}

/**
 * A [Converter] that converts an instance of [ApplicationConfigValue] to a list of strings.
 */
object KtorApplicationConfigValueListConverter : Converter<ApplicationConfigValue, List<String>> {
    override fun convert(source: ApplicationConfigValue): List<String> = source.getList()
}

/**
 * A [FactoryBean] for accessing an [ApplicationConfig] object of the running [Application].
 *
 * @property application The Ktor [Application] to read the configuration from.
 * @property path The path to the object.
 */
class KtorApplicationConfigFactoryBean(
    private val application: Application,
    private val path: String? = null
) : FactoryBean<ApplicationConfig> {

    override fun getObject(): ApplicationConfig = application.environment.config.let {
        if (path != null)
            it.config(path)
        else
            it
    }

    override fun getObjectType(): Class<*> = ApplicationConfig::class.java
}
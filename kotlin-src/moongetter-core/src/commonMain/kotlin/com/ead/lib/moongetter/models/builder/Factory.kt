@file:Suppress("UNUSED")

package com.ead.lib.moongetter.models.builder

import com.ead.lib.moongetter.MoonGetter
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.ExperimentalFeature
import com.ead.lib.moongetter.core.MoonCoreFactory
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.UserAgent

/**
 * Represents a factory configuration used to discover streaming resources from various URLs.
 * Built around a user-agent header, timeout configs, and scraping engines.
 */
class Factory(
    val client: MoonClient?,
    val headers: Map<String, String>,
    val timeout: Configuration.Data,
    val engine: Engine
) {

    /**
     * Builder for constructing [Factory] instances. Contains logic for resolving servers and scraping.
     */
    class Builder {

        var headers: HashMap<String, String> = hashMapOf("User-Agent" to UserAgent.value)
        var client: MoonClient? = null
        var configData: Configuration.Data = Configuration.Data()
        var engine: Engine = Engine.Builder().build()

        /**
         * Sets custom headers for the request.
         *
         * @param headers Custom HTTP headers.
         * @return Self reference for chaining.
         */
        fun setHeaders(headers: Map<String, String>) = apply {
            this.headers = HashMap(headers)
        }

        /**
         * Returns an identifier string for a given URL.
         *
         * @throws InvalidServerException If the URL is empty or engine is not configured.
         */
        fun identifier(url: String): String? = run {
            if (MoonGetter.instance == null)
                throw InvalidServerException(Resources.MOON_GETTER_NOT_INITIALIZED, Error.CONFIG_NOT_INITIALIZED)

            MoonCoreFactory.identifier(
                url = url.ifEmpty {
                    throw InvalidServerException(Resources.NO_PARAMETERS_TO_WORK, Error.NO_PARAMETERS_TO_WORK)
                },
                serversFactory = engine.servers.ifEmpty {
                    throw InvalidServerException(Resources.ENGINES_NOT_PROVIDED, Error.INVALID_BUILDER_PARAMETERS)
                }
            )
        }

        /**
         * Returns a list of identifier strings for multiple URLs.
         * Useful for batching logging or validation.
         */
        fun identifier(urls: List<String>): List<String> = run {
            if (MoonGetter.instance == null)
                throw InvalidServerException(Resources.MOON_GETTER_NOT_INITIALIZED, Error.CONFIG_NOT_INITIALIZED)

            MoonCoreFactory.identifierList(
                urls = urls.ifEmpty {
                    throw InvalidServerException(Resources.NO_PARAMETERS_TO_WORK, Error.NO_PARAMETERS_TO_WORK)
                },
                serversFactory = engine.servers.ifEmpty {
                    throw InvalidServerException(Resources.ENGINES_NOT_PROVIDED, Error.INVALID_BUILDER_PARAMETERS)
                }
            )
        }

        /**
         * Attempts to get a valid [Server] from a single URL.
         * Returns null if the process fails.
         */
        suspend fun get(url: String): Server = run {
            if (MoonGetter.instance == null)
                throw InvalidServerException(Resources.MOON_GETTER_NOT_INITIALIZED, Error.CONFIG_NOT_INITIALIZED)

            MoonCoreFactory.create(
                url = url.ifEmpty {
                    throw InvalidServerException(Resources.NO_PARAMETERS_TO_WORK, Error.NO_PARAMETERS_TO_WORK)
                },
                serversFactory = engine.servers.ifEmpty {
                    throw InvalidServerException(Resources.ENGINES_NOT_PROVIDED, Error.INVALID_BUILDER_PARAMETERS)
                },
                robot = engine.robot,
                headers = headers,
                configData = configData,
                client = client ?: throw InvalidServerException(Resources.CLIENT_NOT_PROVIDED, Error.INVALID_BUILDER_PARAMETERS)
            )
        }

        /**
         * Returns a list of available [Server]s from the provided list of URLs.
         */
        @ExperimentalFeature
        suspend fun get(urls: List<String>): List<Server> = run {
            if (MoonGetter.instance == null)
                throw InvalidServerException(Resources.MOON_GETTER_NOT_INITIALIZED, Error.CONFIG_NOT_INITIALIZED)

            MoonCoreFactory.creates(
                urls = urls.ifEmpty {
                    throw InvalidServerException(Resources.NO_PARAMETERS_TO_WORK, Error.NO_PARAMETERS_TO_WORK)
                },
                serversFactory = engine.servers.ifEmpty {
                    throw InvalidServerException(Resources.ENGINES_NOT_PROVIDED, Error.INVALID_BUILDER_PARAMETERS)
                },
                robot = engine.robot,
                headers = headers,
                configData = configData,
                client = client ?: throw InvalidServerException(Resources.CLIENT_NOT_PROVIDED, Error.INVALID_BUILDER_PARAMETERS)
            )
        }

        /**
         * Searches the provided URLs and returns the first valid [Server].
         * If none work, it returns null.
         */
        suspend fun getUntilFindResource(urls: List<String>): Server = run {
            if (MoonGetter.instance == null)
                throw InvalidServerException(Resources.MOON_GETTER_NOT_INITIALIZED, Error.CONFIG_NOT_INITIALIZED)

            MoonCoreFactory.createUntilFindResource(
                urls = urls.ifEmpty {
                    throw InvalidServerException(Resources.NO_PARAMETERS_TO_WORK, Error.NO_PARAMETERS_TO_WORK)
                },
                serversFactory = engine.servers.ifEmpty {
                    throw InvalidServerException(Resources.ENGINES_NOT_PROVIDED, Error.INVALID_BUILDER_PARAMETERS)
                },
                robot = engine.robot,
                headers = headers,
                configData = configData,
                client = client ?: throw InvalidServerException(Resources.CLIENT_NOT_PROVIDED, Error.INVALID_BUILDER_PARAMETERS)
            )
        }

        /**
         * Copies an existing [Factory] into a new builder instance.
         */
        constructor(factory: Factory) {
            this.headers = HashMap(factory.headers)
            this.configData = factory.timeout
            this.engine = factory.engine
        }

        /** Default constructor. */
        constructor()
    }
}

package com.ead.lib.moongetter


import com.ead.lib.moongetter.core.ExperimentalFeature
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.builder.Factory
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException

/**
 * MoonGetter is the central access point for retrieving `Server` instances.
 *
 * It works as a singleton façade that delegates server creation and resource
 * resolution to an internal `Factory.Builder`. Before calling any of the
 * retrieval methods, the library must be initialized using [start].
 *
 * If the instance is not initialized, calls to any `get` methods will throw
 * an [InvalidServerException] with the error type [Error.CONFIG_NOT_INITIALIZED].
 */
object MoonGetter {

    /**
     * The internal [Factory.Builder] instance responsible for creating
     * and resolving `Server` objects.
     *
     * This property is `null` until [start] is called, ensuring that
     * initialization is performed explicitly by the user.
     */
    internal var instance: Factory.Builder? = null

    /**
     * Initializes the [MoonGetter] singleton with a [Factory.Builder] implementation.
     *
     * This must be called before invoking any of the `get` methods.
     * Typically, the provided factory is configured with the available
     * server implementations and resource resolution strategies.
     *
     * @param factory The [Factory.Builder] responsible for server creation.
     */
    fun start(factory: Factory.Builder) {
        instance = factory
    }

    /**
     * Attempts to resolve the identifier of a server from the given [url].
     *
     * The identifier typically represents the unique name, key, or internal code
     * associated with the server implementation that matches the provided URL.
     *
     * @param url The URL to analyze in order to determine the corresponding server identifier.
     * @return A [String] representing the identifier of the matching server,
     *         or `null` if no server can be resolved for the given URL.
     *
     * @throws InvalidServerException If the factory has not been initialized via [start].
     */
    fun identifier(url: String): String? {
        return(instance ?: throw InvalidServerException(Resources.MOON_GETTER_NOT_INITIALIZED, Error.CONFIG_NOT_INITIALIZED))
            .identifier(url = url)
    }

    /**
     * Attempts to resolve identifiers for a list of candidate [urls].
     *
     * For each URL in the list, the factory will attempt to determine the
     * corresponding server identifier. The result will contain a list of
     * identifiers in the same order as the input URLs.
     *
     * Unlike [identifier] for a single URL, this method will throw an exception
     * if the factory has not been initialized before use.
     *
     * @param urls A list of URLs to analyze in order to determine their server identifiers.
     * @return A [List] of identifiers, one for each input URL that could be resolved.
     *
     * @throws InvalidServerException If the factory has not been initialized via [start],
     *                                preventing identifiers from being resolved.
     */
    fun identifier(urls: List<String>): List<String> {
        return (instance ?: throw InvalidServerException(Resources.MOON_GETTER_NOT_INITIALIZED, Error.CONFIG_NOT_INITIALIZED))
            .identifier(urls = urls)
    }

    /**
     * Attempts to resolve a [Server] for the given [url].
     *
     * @param url The URL used to determine which server implementation to return.
     * @return A [Server] instance corresponding to the given URL.
     *
     * @throws InvalidServerException If the factory has not been initialized via [start],
     *                                or if no valid server can be created for the given URL.
     * @throws RuntimeException For any unexpected runtime errors during resolution.
     */
    @Throws(InvalidServerException::class, RuntimeException::class)
    suspend fun get(url: String): Server {
        return (instance ?: throw InvalidServerException(Resources.MOON_GETTER_NOT_INITIALIZED, Error.CONFIG_NOT_INITIALIZED))
            .get(url = url)
    }

    /**
     * Attempts to resolve a [Server] for the given [url], returning `null`
     * instead of throwing when resolution fails.
     *
     * @param url The URL used to determine which server implementation to return.
     * @return A [Server] instance if successful, or `null` if no server can be resolved.
     *
     * @throws InvalidServerException If the factory has not been initialized via [start].
     *                                Unlike other cases, this exception is re-thrown
     *                                to signal improper usage of the API.
     */
    @Throws(InvalidServerException::class)
    suspend fun getOrNull(url: String): Server? {
        return try {
            instance?.get(url = url)
        } catch (e: InvalidServerException) {
            if (e.error == Error.CONFIG_NOT_INITIALIZED)
                throw InvalidServerException(Resources.MOON_GETTER_NOT_INITIALIZED, Error.CONFIG_NOT_INITIALIZED)
            else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Attempts to resolve a list [Server] for the given [urls].
     *
     * @param urls The URL used to determine which server implementation to return.
     * @return A [Server] instance corresponding to the given URL.
     *
     * @throws InvalidServerException If the factory has not been initialized via [start],
     *                                or if no valid server can be created for the given URL.
     * @throws RuntimeException For any unexpected runtime errors during resolution.
     */
    @Throws(InvalidServerException::class, RuntimeException::class)
    @ExperimentalFeature
    suspend fun get(urls: List<String>) : List<Server> {
        return (instance ?: throw InvalidServerException(Resources.MOON_GETTER_NOT_INITIALIZED, Error.CONFIG_NOT_INITIALIZED))
            .get(urls = urls)
    }

    /**
     * Attempts to resolve a [Server] by checking a list of candidate [urls].
     * The factory will iterate through the list until a valid resource is found.
     *
     * @param urls A list of potential URLs to resolve against available servers.
     * @return The first successfully resolved [Server].
     *
     * @throws InvalidServerException If the factory has not been initialized via [start],
     *                                or if no valid server can be found among the provided URLs.
     * @throws RuntimeException For any unexpected runtime errors during resolution.
     */
    @Throws(InvalidServerException::class, RuntimeException::class)
    suspend fun getUntilFindResource(urls: List<String>): Server {
        return (instance ?: throw InvalidServerException(Resources.MOON_GETTER_NOT_INITIALIZED, Error.CONFIG_NOT_INITIALIZED))
            .getUntilFindResource(urls = urls)
    }

    /**
     * Attempts to resolve a [Server] by checking a list of candidate [urls].
     * Returns `null` if no valid server is found, instead of throwing.
     *
     * @param urls A list of potential URLs to resolve against available servers.
     * @return The first successfully resolved [Server], or `null` if none could be found.
     *
     * @throws InvalidServerException If the factory has not been initialized via [start].
     *                                This exception is re-thrown to signal improper usage
     *                                of the API configuration.
     */
    @Throws(InvalidServerException::class)
    suspend fun getUntilFindResourceOrNull(urls: List<String>): Server? {
        return try {
            instance?.getUntilFindResource(urls = urls)
        } catch (e: InvalidServerException) {
            if (e.error == Error.CONFIG_NOT_INITIALIZED)
                throw InvalidServerException(Resources.MOON_GETTER_NOT_INITIALIZED, Error.CONFIG_NOT_INITIALIZED)
            else null
        } catch (_: Exception) {
            null
        }
    }
}
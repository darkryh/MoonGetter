package com.ead.lib.moongetter.core

import kotlin.coroutines.cancellation.CancellationException
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.MoonCoreFactory.create
import com.ead.lib.moongetter.core.MoonCoreFactory.identifier
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.resolution.ReplayingMoonClient
import com.ead.lib.moongetter.resolution.Resolution
import com.ead.lib.moongetter.resolution.ResolutionConfig
import com.ead.lib.moongetter.resolution.ResolutionEvent
import com.ead.lib.moongetter.resolution.ResolutionStage
import com.ead.lib.moongetter.resolution.ServerResolver
import com.ead.lib.moongetter.resolution.UnresolvedReason
import com.ead.lib.moongetter.utils.Values.DEBUG_ERROR
import com.ead.lib.moongetter.utils.Values.restoreValues

/**
 * Server factory object that handle the creation of servers
 * and the identification of the servers from a url
 */
internal object MoonCoreFactory {

    private var _moonClientInstance : MoonClient?= null

    /**
     * Identify the server from a url
     * return the server identifier name  or null if not found
     */
    fun identifier(
        /**
         * @param url that's provided in the MoonGetter Builder
         */
        url: String,
        /**
         * @param serversFactory that's provided in the MoonGetter Builder
         */
        serversFactory : Array<Server.Factory>,
        /**
         * @param resolution learned hosts are consulted, the network fallback never runs here
         */
        resolution: ResolutionConfig = ResolutionConfig(),
        /**
         * return a nullable string name of the server
         */
    ) : String? {
        /**
         * known hosts and learned hosts only, without network requests
         */
        val resolved = ServerResolver(serversFactory.toList(), resolution).resolveStatic(url)
        return (resolved as? Resolution.Resolved)?.primary?.factory?.serverName
    }



    /**
     * Identify the servers from a list of urls
     * if none servers was found return InvalidServerException
     */
    fun identifierList(
        /**
         * @param urls that's provided in the MoonGetter Builder
         */
        urls: List<String>,
        /**
         * @param serversFactory that's provided in the MoonGetter Builder
         */
        serversFactory : Array<Server.Factory>,
        /**
         * @param resolution learned hosts are consulted, the network fallback never runs here
         */
        resolution: ResolutionConfig = ResolutionConfig(),
        /**
         * return a list of servers
         */
    ) : List<String> {
        /**
         * create and instance of mutable list
         * and set also scope level
         * to handle urls
         */
        return mutableListOf<String>().also { list ->
            /**
             * cycle the injected [urls]
             */
            urls.forEach { url ->
                /**
                 * identified the server using the [identifier] function
                 */
                val identifiedServerName = identifier(url, serversFactory, resolution)
                /**
                 * if servers is founded add to list
                 */
                if (identifiedServerName != null) list.add(identifiedServerName)
            }
            /**
             * if the list is empty throw [InvalidServerException]
             */
            list.ifEmpty {
                throw InvalidServerException(Resources.NOT_SERVERS_FOUND, Error.NO_PARAMETERS_TO_WORK)
            }
        }
    }



    /**
     * Create a server from a url
     * using regex to identified the supported server
     * return the server or null if not found
     */
    suspend fun create(
        /**
         * @param url that's provided in the MoonGetter Builder
         */
        url : String,
        /**
         * @param serversFactory that's provided in the MoonGetter Builder
         */
        serversFactory : Array<Server.Factory>,
        /**
         * @param headers that's provided in the MoonGetter Builder
         */
        headers : HashMap<String, String>,
        /**
         * @param configData that's get configurations set in the MoonGetter Builder
         */
        configData: Configuration.Data,
        /**
         * @param client that's provided in the MoonGetter Builder
         */
        client: MoonClient,
        /**
         * @param resolution how urls of unknown hosts are handled
         */
        resolution: ResolutionConfig = ResolutionConfig(),
        /**
         * return a nullable server
         */
    ) : Server {
        try {
            val moonClient = getSingleton(client)

            /**
             * Identify the server from the url: known hosts first,
             * then the optional network fallback
             */
            val result = resolve(url, serversFactory, headers, moonClient, resolution)



            /**
             * If no server was identified don't do the extract process
             */
            if (result is Resolution.Unresolved) {
                resolution.listener?.onEvent(ResolutionEvent.Unresolved(url, result.reason, result.evidence))
                throw result.toException()
            }



            /**
             * Extract with the identified server, a fallback answer
             * is only accepted once its extractor returns videos
             */
            return extract(result as Resolution.Resolved, headers, configData, moonClient, resolution)
        } finally {
            restoreValues()
        }
    }



    /**
     * Resolve which server handles the url without extracting
     */
    suspend fun resolve(
        url: String,
        serversFactory: Array<Server.Factory>,
        headers: Map<String, String>,
        client: MoonClient,
        resolution: ResolutionConfig = ResolutionConfig(),
    ): Resolution = ServerResolver(serversFactory.toList(), resolution).resolve(url, getSingleton(client), headers)



    /**
     * Extract with the resolution candidates in order,
     * the first candidate that returns videos wins
     */
    private suspend fun extract(
        resolved: Resolution.Resolved,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient,
        resolution: ResolutionConfig,
    ): Server {
        var firstFailure: Exception? = null
        for (candidate in resolved.candidates) {
            val candidateClient = resolved.page
                ?.takeIf { resolution.reuseFetchedPage }
                ?.let { ReplayingMoonClient(client, it) }
                ?: client
            /**
             * every candidate receives its own headers
             * because servers add their own Referer and Origin
             */
            val server = candidate.factory.create(candidate.url, HashMap(headers), configData, candidateClient)

            /**
             * If the server is deprecated return it without extracting
             */
            if (server.isDeprecated) return server

            /**
             * Known hosts keep the historical behaviour:
             * extraction errors propagate unchanged
             */
            if (resolved.stage == ResolutionStage.KNOWN_HOST) {
                server.setVideos(server.onExtract())
                return server
            }

            try {
                val videos = server.onExtract()
                if (videos.isEmpty()) throw InvalidServerException(Resources.expectedResponseNotFound(server.name), Error.EXPECTED_RESPONSE_NOT_FOUND)
                server.setVideos(videos)
                confirm(resolved, candidate.factory.serverName, resolution)
                return server
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (resolved.stage == ResolutionStage.LEARNED_HOST) forgetLearnedHost(resolved, candidate.factory.serverName, resolution)
                firstFailure = firstFailure ?: error
            }
        }
        throw firstFailure ?: InvalidServerException(Resources.NOT_SERVERS_FOUND, Error.NOT_SERVERS_FOUND)
    }



    /**
     * Remember a confirmed fallback host and report it
     */
    private fun confirm(resolved: Resolution.Resolved, serverName: String, resolution: ResolutionConfig) {
        if (resolved.stage == ResolutionStage.LEARNED_HOST) return
        val host = resolved.url.toHost() ?: return
        resolved.learnableHost?.let { resolution.learnedHosts?.remember(it, serverName) }
        resolution.listener?.onEvent(
            ResolutionEvent.UnknownHostConfirmed(resolved.url, host, serverName, resolved.stage, resolved.evidence)
        )
    }



    /**
     * A learned host that no longer extracts is forgotten,
     * the next url on it goes through the fallback again
     */
    private fun forgetLearnedHost(resolved: Resolution.Resolved, serverName: String, resolution: ResolutionConfig) {
        val host = resolved.url.toHost() ?: return
        resolution.learnedHosts?.forget(host)
        resolution.listener?.onEvent(ResolutionEvent.LearnedHostRejected(resolved.url, host, serverName))
    }

    private fun String.toHost(): String? = com.ead.lib.moongetter.resolution.EmbedUrl.parse(this)?.host

    private fun Resolution.Unresolved.toException(): InvalidServerException = when (reason) {
        UnresolvedReason.CONFLICTING_SERVERS, UnresolvedReason.AMBIGUOUS ->
            InvalidServerException("${Resources.AMBIGUOUS_URL_ENTRY}: ${suspects.joinToString()}", Error.AMBIGUOUS_URL_ENTRY)
        UnresolvedReason.NO_KNOWN_HOST, UnresolvedReason.INVALID_URL ->
            InvalidServerException(Resources.UNKNOWN_URL_ENTRY, Error.UNKNOWN_URL_ENTRY)
        else -> InvalidServerException("${Resources.UNKNOWN_URL_ENTRY}: ${reason.name.lowercase()}", Error.UNKNOWN_URL_ENTRY)
    }



    /**
     * Create a server from a list of urls
     * using regex to identified the supported server
     * return the servers or throw [InvalidServerException] if not found
     */
    suspend fun creates(
        /**
         * @param urls that's provided in the MoonGetter Builder
         */
        urls : List<String>,
        /**
         * @param serversFactory that's provided in the MoonGetter Builder
         */
        serversFactory : Array<Server.Factory>,
        /**
         * @param headers that's provided in the MoonGetter Builder
         */
        headers : HashMap<String, String>,
        /**
         * @param configData that's get configurations set in the MoonGetter Builder
         */
        configData: Configuration.Data,
        /**
         * @param client that's provided in the MoonGetter Builder
         */
        client: MoonClient,
        /**
         * @param resolution how urls of unknown hosts are handled
         */
        resolution: ResolutionConfig = ResolutionConfig(),
        /**
         * return a list of servers
         */
    ) : List<Server> {
        /**
         * create and instance of mutable list
         * and set also scope level
         * to handle servers
         */
        return mutableListOf<Server>().also { servers ->
            /**
             * cycle the injected [urls]
             * and set try catch
             * to keep extracting servers
             */
            urls.forEach { url ->
                try {

                    /**
                     * create the server using [create]
                     */
                    val server = create(url, serversFactory, headers, configData, getSingleton(client), resolution)

                    /**
                     * In case of null return to next cycle
                     */
                    servers.add(server)

                }
                /**
                 * catching expected exceptions
                 * [InvalidServerException], [RuntimeException],
                 */
                catch (e : InvalidServerException) {
                    e.printStackTrace()
                    println("$DEBUG_ERROR ${e.message ?: Resources.UNKNOWN_ERROR}")
                }
                catch (cancelled: CancellationException) {
                    throw cancelled
                }
                catch (e : RuntimeException) {
                    e.printStackTrace()
                    println("$DEBUG_ERROR ${e.message ?: Resources.UNKNOWN_ERROR}")
                }
                catch (e : Exception) {
                    e.printStackTrace()
                    println("$DEBUG_ERROR ${e.message ?: Resources.UNKNOWN_ERROR}")
                }
            }
        }
    }



    /**
     * Create a server until a server is found
     * using regex to identified the supported server
     * in this case if the first server was found stop the searching process
     * return the server or null if not found
     */
    suspend fun createUntilFindResource(
        /**
         * @param urls that's provided in the MoonGetter Builder
         */
        urls : List<String>,

        serversFactory : Array<Server.Factory>,
        /**
         * @param headers that's provided in the MoonGetter Builder
         */
        headers : HashMap<String, String>,
        /**
         * @param configData that's get configurations set in the MoonGetter Builder
         */
        configData: Configuration.Data,
        /**
         * @param client that's provided in the MoonGetter Builder
         */
        client: MoonClient,
        /**
         * @param resolution how urls of unknown hosts are handled
         */
        resolution: ResolutionConfig = ResolutionConfig(),
        /**
         * return a nullable server
         */
    ) : Server {
        /**
         * cycle the injected [urls]
         * and set try catch
         * to keep extracting servers
         */
        urls.forEach { url ->
            try {

                /**
                 * create the server using [create]
                 */
                val server = create(url, serversFactory, headers, configData, getSingleton(client), resolution)
                /**
                 * if resource is founded return the server
                 */
                if (server.isResourceFounded) return server

            }
            /**
             * catching expected exceptions
             * [InvalidServerException], [RuntimeException]
             */
            catch (e : InvalidServerException) {
                e.printStackTrace()
                println("$DEBUG_ERROR ${e.message ?: Resources.UNKNOWN_ERROR}")
            }
            catch (cancelled: CancellationException) {
                    throw cancelled
                }
                catch (e : RuntimeException) {
                e.printStackTrace()
                println("$DEBUG_ERROR ${e.message ?: Resources.UNKNOWN_ERROR}")
            }
            catch (e : Exception) {
                e.printStackTrace()
                println("$DEBUG_ERROR ${e.message ?: Resources.UNKNOWN_ERROR}")
            }
        }

        /**
         * return null if none of the servers injected are founded
         */
        throw InvalidServerException(Resources.NOT_SERVERS_FOUND, Error.NOT_SERVERS_FOUND)
    }


    /**
     * Keeps the configured client; a client from a later [com.ead.lib.moongetter.MoonGetter.start]
     * replaces the previous one instead of being ignored
     */
    private fun getSingleton(client: MoonClient) : MoonClient {
        return _moonClientInstance?.takeIf { it === client } ?: client.also { _moonClientInstance = it }
    }
}
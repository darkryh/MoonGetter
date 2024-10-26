package com.ead.lib.moongetter.core

import android.content.Context
import android.util.Log
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.Values.DEBUG_ERROR
import java.io.IOException

/**
 * Server factory object that handle the creation of servers
 * and the identification of the servers from a url
 */
internal object MoonFactory {



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
         * return a nullable string name of the server
         */
    ) : String? {
        /**
         * from the servers injected in the builder
         * and passed in the function
         */
        return serversFactory
            /**
             * get or null server that matches the pattern
             */
            .singleOrNull { serverFactory ->
                PatternManager
                    .match(serverFactory.pattern, url)
            }
            /**
             * get the class
             */
            ?.belongedClass
            /**
             * and return the simple name
             */
            ?.simpleName
    }



    /**
     * Identify the servers from a list of urls
     * if none servers was found return InvalidServerException
     */
    fun identifierList(
        context: Context,
        /**
         * @param urls that's provided in the MoonGetter Builder
         */
        urls: List<String>,
        /**
         * @param serversFactory that's provided in the MoonGetter Builder
         */
        serversFactory : Array<Server.Factory>,
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
                val identifiedServerName = identifier(url,serversFactory)
                /**
                 * if servers is founded add to list
                 */
                if (identifiedServerName != null) list.add(identifiedServerName)
            }
            /**
             * if the list is empty throw [InvalidServerException]
             */
            list.ifEmpty { throw InvalidServerException(context.getString(R.string.not_servers_found)) }
        }
    }



    /**
     * Create a server from a url
     * using regex to identified the supported server
     * return the server or null if not found
     */
    suspend fun create(
        /**
         * @param context that's provided in the MoonGetter Builder
         */
        context: Context,
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
        configData: Configuration.Data
        /**
         * return a nullable server
         */
    ) : Server? {



        /**
         * Identify the server from the url
         * and the engines provided by
         * the builder process
         */
        val serverResult = serversFactory.onFactory(
            context = context,
            url = url,
            headers = headers,
            configData = configData
        )



        /**
         * If the server is nullable return
         * and don't to extract process
         */
        val server = serverResult ?: return null



        /**
         * If the server is pending return null
         */
        if (server.isPending) return null



        /**
         * If the server is deprecated return null
         */
        if (!server.isDeprecated) {



            /**
             * Extract the videos from the server
             * and set them to the server
             */
            server.setVideos(server.onExtract())
        }



        /**
         * Return the server
         */
        return serverResult
    }



    /**
     * Create a server from a list of urls
     * using regex to identified the supported server
     * return the servers or throw [InvalidServerException] if not found
     */
    suspend fun creates(
        /**
         * @param context that's provided in the MoonGetter Builder
         */
        context: Context,
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
        configData: Configuration.Data
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
                    val server = create(context, url, serversFactory, headers, configData)

                    /**
                     * In case of null return to next cycle
                     */
                    servers.add(server ?: return@forEach)

                }
                /**
                 * catching expected exceptions
                 * [InvalidServerException], [RuntimeException], [IOException]
                 */
                catch (e : InvalidServerException) { e.printStackTrace() }
                catch (e : RuntimeException) { e.printStackTrace() }
                catch (e : IOException) { e.printStackTrace() }
            }

            /**
             * if the servers is empty throw [InvalidServerException]
             */
            servers.ifEmpty { throw InvalidServerException(context.getString(R.string.not_servers_found)) }
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
         * @param context that's provided in the MoonGetter Builder
         */
        context: Context,
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
        configData: Configuration.Data
        /**
         * return a nullable server
         */
    ) : Server? {
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
                val server = create(context, url, serversFactory, headers, configData)
                /**
                 * if resource is founded return the server
                 */
                if (server?.isResourceFounded == true) return server

            }
            /**
             * catching expected exceptions
             * [InvalidServerException], [RuntimeException], [IOException]
             */
            catch (e : InvalidServerException) {
                e.printStackTrace()
                Log.e(DEBUG_ERROR,e.message ?: context.getString(R.string.unknown_error))
            }
            catch (e : RuntimeException) {
                e.printStackTrace()
                Log.e(DEBUG_ERROR,e.message ?: context.getString(R.string.unknown_error))
            }
            catch (e : IOException) {
                e.printStackTrace()
                Log.e(DEBUG_ERROR,e.message ?: context.getString(R.string.unknown_error))
            }
        }

        /**
         * return null if none of the servers injected are founded
         */
        return null
    }
}
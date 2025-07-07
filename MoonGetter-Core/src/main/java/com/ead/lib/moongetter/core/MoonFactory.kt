package com.ead.lib.moongetter.core

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Robot
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.Values.DEBUG_ERROR
import com.ead.lib.moongetter.utils.Values.restoreValues
import java.io.IOException

/**
 * Server factory object that handle the creation of servers
 * and the identification of the servers from a url
 */
internal object MoonFactory {

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
        robot: Robot?,
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
         * return a nullable server
         */
    ) : Server? {



        /**
         * Identify the server from the url
         * and the engines provided by
         * the builder process
         */
        val serverResult = serversFactory.onFactory(
            url = url,
            headers = headers,
            configData = configData,
            client = getSingleton(client),
            robot = robot
        )



        /**
         * If the server is nullable return
         * and don't to extract process
         */
        val server = serverResult ?: return null.also { restoreValues() }



        /**
         * If the server is pending return null
         */
        if (server.isPending) return null.also { restoreValues() }



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
        return serverResult.also { restoreValues() }
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
        robot: Robot?,
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
                    val server = create(url, serversFactory, robot, headers, configData, getSingleton(client))

                    /**
                     * In case of null return to next cycle
                     */
                    servers.add(server ?: return@forEach)

                }
                /**
                 * catching expected exceptions
                 * [InvalidServerException], [RuntimeException], [IOException]
                 */
                catch (e : InvalidServerException) {
                    e.printStackTrace()
                    println("$DEBUG_ERROR ${e.message ?: Resources.UNKNOWN_ERROR}")
                }
                catch (e : RuntimeException) {
                    e.printStackTrace()
                    println("$DEBUG_ERROR ${e.message ?: Resources.UNKNOWN_ERROR}")
                }
                catch (e : IOException) {
                    e.printStackTrace()
                    println("$DEBUG_ERROR ${e.message ?: Resources.UNKNOWN_ERROR}")
                }
                catch (e : Exception) {
                    e.printStackTrace()
                    println("$DEBUG_ERROR ${e.message ?: Resources.UNKNOWN_ERROR}")
                }
            }

            /**
             * if the servers is empty throw [InvalidServerException]
             */
            servers.ifEmpty {
                throw InvalidServerException(Resources.NOT_SERVERS_FOUND, Error.NO_PARAMETERS_TO_WORK)
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
        robot: Robot?,
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
                val server = create(url, serversFactory, robot, headers, configData, getSingleton(client))
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
                println("$DEBUG_ERROR ${e.message ?: Resources.UNKNOWN_ERROR}")
            }
            catch (e : RuntimeException) {
                e.printStackTrace()
                println("$DEBUG_ERROR ${e.message ?: Resources.UNKNOWN_ERROR}")
            }
            catch (e : IOException) {
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
        return null
    }


    private fun getSingleton(client: MoonClient) : MoonClient {
        return _moonClientInstance ?: client.also { _moonClientInstance = it }
    }
}
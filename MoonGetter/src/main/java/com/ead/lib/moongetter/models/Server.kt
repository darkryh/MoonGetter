package com.ead.lib.moongetter.models

import android.content.Context
import com.ead.lib.moongetter.core.Pending
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import okio.IOException
import java.lang.RuntimeException

open class Server(
    /**
     * @param @context the context of the application
     */
    protected open val context: Context,
    /**
     * @param @url the url of the server
     */
    protected open var url : String,
    /**
     * @param @isDeprecated true if the server is deprecated
     */
    open val isDeprecated : Boolean = false,
) {

    /**
     * @return true if server has videos so it means the resource was found
     */
    internal val isResourceFounded get() = videos.isNotEmpty()



    /**
     * @return true if the server is pending
     */
    internal val isPending = this::class.annotations.any { it::class == Pending::class }



    /**
     * @return list of videos direct link
     */
    val videos : List<Video> get() = _videos
    private val _videos : MutableList<Video> = mutableListOf()







    /**
     * onExtract function when the solving or scraping process is getting handle
     *
     * example:
     *
     * fun onExtract() {
     *
     *   do extraction process
     *
     *   ..
     *
     *   ..
     *
     *   when finally resource is found
     *
     *   ..
     *
     *   ..
     *
     *   you can use addDefault()
     *
     *   function to add default video
     *
     *   save to the server the videos founded
     *
     *   the url has to be managed the direct  url
     *
     *   example :
     *
     *   url = "mi direct url"
     *
     *   or you can use
     *
     *   add(Video(title: String, url: String))
     *
     *   ..
     *
     *   ..
     *
     *   at final you can have a list of videos
     *
     * }
     */
    @Throws(InvalidServerException::class,IOException::class, RuntimeException::class)
    open suspend fun onExtract() { /*to do in child*/}




    /**
     * add video to the list of videos
     */
    protected fun add(video: Video) { _videos.add(video) }



    /**
     * add default video to the list of videos
     * in this case the url is the direct url
     */
    protected fun addDefault() { _videos.add(Video(quality = null, url = url)) }


    companion object {
        /**
         * representation of request results
         */
        const val unauthorized = 401
        const val forbidden = 403
        const val notFound = 404
        val clientError = 400..499
        val serverError = 500..599
    }
}
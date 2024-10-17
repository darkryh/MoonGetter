package com.ead.lib.moongetter.core

import android.content.Context
import android.util.Log
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.ServerIntegration
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.server_sites.Anonfiles
import com.ead.lib.moongetter.server_sites.Bayfiles
import com.ead.lib.moongetter.server_sites.Doodstream
import com.ead.lib.moongetter.server_sites.Facebook
import com.ead.lib.moongetter.server_sites.Fembed
import com.ead.lib.moongetter.server_sites.Vihide
import com.ead.lib.moongetter.server_sites.Filemoon
import com.ead.lib.moongetter.server_sites.Fireload
import com.ead.lib.moongetter.server_sites.Gofile
import com.ead.lib.moongetter.server_sites.GoodStream
import com.ead.lib.moongetter.server_sites.GoogleDrive
import com.ead.lib.moongetter.server_sites.Hexload
import com.ead.lib.moongetter.server_sites.Lulustream
import com.ead.lib.moongetter.server_sites.Mediafire
import com.ead.lib.moongetter.server_sites.Mixdrop
import com.ead.lib.moongetter.server_sites.Mp4Upload
import com.ead.lib.moongetter.server_sites.Okru
import com.ead.lib.moongetter.server_sites.OneCloudFile
import com.ead.lib.moongetter.server_sites.Onefichier
import com.ead.lib.moongetter.server_sites.PixelDrain
import com.ead.lib.moongetter.server_sites.Senvid
import com.ead.lib.moongetter.server_sites.StreamSb
import com.ead.lib.moongetter.server_sites.StreamWish
import com.ead.lib.moongetter.server_sites.Streamtape
import com.ead.lib.moongetter.server_sites.Uqload
import com.ead.lib.moongetter.server_sites.Vidguard
import com.ead.lib.moongetter.server_sites.Voe
import com.ead.lib.moongetter.server_sites.XTwitter
import com.ead.lib.moongetter.server_sites.YourUpload
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.Values.DEBUG_ERROR
import java.io.IOException

/**
 * Server factory object that handle the creation of servers
 * and the identification of the servers from a url
 */
internal object ServerFactory {

    /**
     * Identify the server from a url
     * return the server identifier name  or null if not found
     */
    fun identifier(
        url: String,
        serverIntegrations: List<ServerIntegration>
    ) : String? {
        val identifier = serverIntegrations.singleOrNull {
            PatternManager.match(it.pattern, url)
        }?.serverClass?.simpleName ?: when {
            PatternManager.match(Properties.Anonfiles, url) -> Properties.AnonfilesIdentifier
            PatternManager.match(Properties.Bayfiles, url) -> Properties.BayfilesIdentifier
            PatternManager.match(Properties.DoodStream, url) -> Properties.DoodStreamIdentifier
            PatternManager.match(Properties.Facebook, url) -> Properties.FacebookIdentifier
            PatternManager.match(Properties.Fembed, url) -> Properties.FembedIdentifier
            PatternManager.match(Properties.Filemoon, url) -> Properties.FilemoonIdentifier
            PatternManager.match(Properties.Fireload, url) -> Properties.FireloadIdentifier
            PatternManager.match(Properties.Gofile, url) -> Properties.GofileIdentifier
            PatternManager.match(Properties.GoodStream, url) -> Properties.GoodStreamIdentifier
            PatternManager.match(Properties.GoogleDrive, url) -> Properties.GoogleDriveIdentifier
            PatternManager.match(Properties.Hexload, url) -> Properties.HexloadIdentifier
            PatternManager.match(Properties.Lulustream, url) -> Properties.LulustreamIdentifier
            PatternManager.match(Properties.Mediafire, url) -> Properties.MediafireIdentifier
            PatternManager.match(Properties.Mixdrop, url) -> Properties.MixdropIdentifier
            PatternManager.match(Properties.Mp4upload, url) -> Properties.Mp4UploadIdentifier
            PatternManager.match(Properties.Okru, url) -> Properties.OkruIdentifier
            PatternManager.match(Properties.OneFichier, url) -> Properties.OneFichierIdentifier
            PatternManager.match(Properties.OneCloudFile, url) -> Properties.OneCloudFileIdentifier
            PatternManager.match(Properties.PixelDrain, url) -> Properties.PixelDrainIdentifier
            PatternManager.match(Properties.Sendvid, url) -> Properties.SendvidIdentifier
            PatternManager.match(Properties.StreamSb, url) -> Properties.StreamSbIdentifier
            PatternManager.match(Properties.Streamtape, url) -> Properties.StreamtapeIdentifier
            PatternManager.match(Properties.StreamWish, url) -> Properties.StreamWishIdentifier
            PatternManager.match(Properties.Uqload, url) -> Properties.UqloadIdentifier
            PatternManager.match(Properties.Voe, url) -> Properties.VoeIdentifier
            PatternManager.match(Properties.Vidguard, url) -> Properties.VidguardIdentifier
            PatternManager.match(Properties.Vihide, url) -> Properties.VihideIdentifier
            PatternManager.match(Properties.YourUpload, url) -> Properties.YourUploadIdentifier
            PatternManager.match(Properties.XTwitter, url) -> Properties.XTwitterIdentifier
            else -> null
        }


        return identifier
    }

    /**
     * Identify the servers from a list of urls
     * if none servers was found return InvalidServerException
     */
    fun identifierList(
        urls: List<String>,
        serverIntegrations: List<ServerIntegration>
    ) : List<String> {
        val identifiers : MutableList<String> = mutableListOf()

        urls.forEach { url ->
            val identifier = identifier(url,serverIntegrations)
            if (identifier != null) identifiers.add(identifier)
        }

        return identifiers
    }


    /**
     * Create a server from a url
     * using regex to identified the supported server
     * return the server or null if not found
     */
    suspend fun create(
        context: Context,
        url : String,
        oneFichierToken : String?,
        serverIntegrations : List<ServerIntegration>
    ) : Server? {

        /**
         * Identify the server from the url
         * if servers integrations are applied
         * search them in case of not found
         * get server from the library
         */
        val serverResult = serverIntegrations.getServerInstance(
            context = context,
            url = url,
        ) ?:
        when {
            PatternManager.match(Properties.Anonfiles, url) -> Anonfiles(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Bayfiles, url) -> Bayfiles(
                context = context,
                url = url
            )
            PatternManager.match(Properties.DoodStream, url) -> Doodstream(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Facebook, url) -> Facebook(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Fembed, url) -> Fembed(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Filemoon, url) -> Filemoon(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Fireload, url) -> Fireload(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Gofile, url) -> Gofile(
                context = context,
                url = url
            )
            PatternManager.match(Properties.GoodStream, url) -> GoodStream(
                context = context,
                url = url
            )
            PatternManager.match(Properties.GoogleDrive, url) -> GoogleDrive(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Hexload, url) -> Hexload(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Lulustream, url) -> Lulustream(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Mediafire, url) -> Mediafire(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Mixdrop, url) -> Mixdrop(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Mp4upload, url) -> Mp4Upload(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Okru, url) -> Okru(
                context = context,
                url = url
            )
            PatternManager.match(Properties.OneFichier, url) -> Onefichier(
                context = context,
                url = url,
                token = oneFichierToken
            )
            PatternManager.match(Properties.OneCloudFile, url) -> OneCloudFile(
                context = context,
                url = url
            )
            PatternManager.match(Properties.PixelDrain, url) -> PixelDrain(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Sendvid, url) -> Senvid(
                context = context,
                url = url
            )
            PatternManager.match(Properties.StreamSb, url) -> StreamSb(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Streamtape, url) -> Streamtape(
                context = context,
                url = url
            )
            PatternManager.match(Properties.StreamWish, url) -> StreamWish(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Uqload, url) -> Uqload(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Vidguard, url) -> Vidguard(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Vihide, url) -> Vihide(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Voe, url) -> Voe(
                context = context,
                url = url
            )
            PatternManager.match(Properties.YourUpload, url) -> YourUpload(
                context = context,
                url = url
            )
            PatternManager.match(Properties.XTwitter, url) -> XTwitter(
                context = context,
                url = url
            )
            else -> null
        }

        /**
         * If the server is nullable return
         * and don't to extract process
         */
        val server = serverResult ?: return null


        /**
         * If the server is pending return
         * null
         */
        if (server.isPending) return null


        /**
         * If the server is deprecated return
         * null
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
     * return the servers or throw InvalidServerException if not found
     */
    suspend fun creates(
        context: Context,
        urls : List<String>,
        oneFichierToken : String?,
        serverIntegrations : List<ServerIntegration>
    ) : List<Server> {
        val servers : MutableList<Server> = mutableListOf()

        urls.forEach { url ->
            try {

                val server = create(context,url,oneFichierToken,serverIntegrations)
                if (server?.isDeprecated == false) servers.add(server)

            }
            catch (e : InvalidServerException) { e.printStackTrace() }
            catch (e : IOException) { e.printStackTrace() }
        }
        return servers.ifEmpty { throw InvalidServerException(context.getString(R.string.not_servers_found)) }
    }

    /**
     * Create a server until a server is found
     * using regex to identified the supported server
     * in this case if the first server was found stop the searching process
     * return the server or null if not found
     */
    suspend fun createUntilFindResource(
        context: Context,
        urls : List<String>,
        oneFichierToken : String?,
        serverIntegrations : List<ServerIntegration>
    ) : Server? {

        urls.forEach { url ->
            try {

                val server = create(context,url,oneFichierToken,serverIntegrations)
                if (server?.isResourceFounded == true) return server

            }
            catch (e : InvalidServerException) {
                e.printStackTrace()
                Log.e(DEBUG_ERROR, "error: ${e.message}")
            }
            catch (e : RuntimeException) {
                e.printStackTrace()
                Log.e(DEBUG_ERROR, "error: ${e.message}")
            }
            catch (e : IOException) {
                e.printStackTrace()
                Log.e(DEBUG_ERROR, "error: ${e.message}")
            }
        }

        return null
    }
}
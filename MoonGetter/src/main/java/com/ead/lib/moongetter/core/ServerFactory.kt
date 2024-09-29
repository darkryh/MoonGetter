package com.ead.lib.moongetter.core

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.ServerIntegration
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.server_sites.Anonfiles
import com.ead.lib.moongetter.server_sites.Bayfiles
import com.ead.lib.moongetter.server_sites.Fembed
import com.ead.lib.moongetter.server_sites.Filelions
import com.ead.lib.moongetter.server_sites.Filemoon
import com.ead.lib.moongetter.server_sites.Fireload
import com.ead.lib.moongetter.server_sites.Gofile
import com.ead.lib.moongetter.server_sites.GoodStream
import com.ead.lib.moongetter.server_sites.GoogleDrive
import com.ead.lib.moongetter.server_sites.Luluvdo
import com.ead.lib.moongetter.server_sites.Mediafire
import com.ead.lib.moongetter.server_sites.Okru
import com.ead.lib.moongetter.server_sites.OneCloudFile
import com.ead.lib.moongetter.server_sites.Onefichier
import com.ead.lib.moongetter.server_sites.PixelDrain
import com.ead.lib.moongetter.server_sites.Senvid
import com.ead.lib.moongetter.server_sites.StreamSb
import com.ead.lib.moongetter.server_sites.StreamWish
import com.ead.lib.moongetter.server_sites.Streamtape
import com.ead.lib.moongetter.server_sites.Vidguard
import com.ead.lib.moongetter.server_sites.Voe
import com.ead.lib.moongetter.utils.PatternManager
import java.io.IOException

/**
 * Server factory object that handle the creation of servers
 * and the identification of the servers from a url
 */
object ServerFactory {

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
            PatternManager.match(Properties.Fembed, url) -> Properties.FembedIdentifier
            PatternManager.match(Properties.Filelions, url) -> Properties.FilelionsIdentifier
            PatternManager.match(Properties.Filemoon, url) -> Properties.FilemoonIdentifier
            PatternManager.match(Properties.Fireload, url) -> Properties.FireloadIdentifier
            PatternManager.match(Properties.Gofile, url) -> Properties.GofileIdentifier
            PatternManager.match(Properties.GoodStream, url) -> Properties.GoodStreamIdentifier
            PatternManager.match(Properties.GoogleDrive, url) -> Properties.GoogleDriveIdentifier
            PatternManager.match(Properties.Luluvdo, url) -> Properties.LuluvdoIdentifier
            PatternManager.match(Properties.Mediafire, url) -> Properties.MediafireIdentifier
            PatternManager.match(Properties.Okru, url) -> Properties.OkruIdentifier
            PatternManager.match(Properties.OneFichier, url) -> Properties.OneFichierIdentifier
            PatternManager.match(Properties.OneCloudFile, url) -> Properties.OneCloudFileIdentifier
            PatternManager.match(Properties.PixelDrain, url) -> Properties.PixelDrainIdentifier
            PatternManager.match(Properties.Sendvid, url) -> Properties.SendvidIdentifier
            PatternManager.match(Properties.StreamSb, url) -> Properties.StreamSbIdentifier
            PatternManager.match(Properties.Streamtape, url) -> Properties.StreamtapeIdentifier
            PatternManager.match(Properties.StreamWish, url) -> Properties.StreamWishIdentifier
            PatternManager.match(Properties.Voe, url) -> Properties.VoeIdentifier
            PatternManager.match(Properties.Vidguard, url) -> Properties.VidguardIdentifier
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

        val server = serverIntegrations.getServerInstance(
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
            PatternManager.match(Properties.Fembed, url) -> Fembed(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Filelions, url) -> Filelions(
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
            PatternManager.match(Properties.Luluvdo, url) -> Luluvdo(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Mediafire, url) -> Mediafire(
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
            PatternManager.match(Properties.Voe, url) -> Voe(
                context = context,
                url = url
            )
            PatternManager.match(Properties.Vidguard, url) -> Vidguard(
                context = context,
                url = url
            )
            else -> null
        }

        val serverResult = server ?: return null

        if (serverResult.javaClass.annotations.any { it == Pending() }) {
            print("The server ${serverResult.javaClass.simpleName} is pending")
        }

        if (!serverResult.isDeprecated) server.onExtract()

        return server
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
            catch (e : InvalidServerException) { e.printStackTrace() }
            catch (e : IOException) { e.printStackTrace() }
        }

        return null
    }
}
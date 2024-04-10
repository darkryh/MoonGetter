package com.ead.lib.moongetter.core

import android.content.Context
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.ServerView
import com.ead.lib.moongetter.server_sites.Anonfiles
import com.ead.lib.moongetter.server_sites.Bayfiles
import com.ead.lib.moongetter.server_sites.Fembed
import com.ead.lib.moongetter.server_sites.Fireload
import com.ead.lib.moongetter.server_sites.Gofile
import com.ead.lib.moongetter.server_sites.GoogleDrive
import com.ead.lib.moongetter.server_sites.Mediafire
import com.ead.lib.moongetter.server_sites.Okru
import com.ead.lib.moongetter.server_sites.Onefichier
import com.ead.lib.moongetter.server_sites.PixelDrain
import com.ead.lib.moongetter.server_sites.Senvid
import com.ead.lib.moongetter.server_sites.StreamSb
import com.ead.lib.moongetter.server_sites.StreamWish
import com.ead.lib.moongetter.server_sites.Streamtape
import com.ead.lib.moongetter.server_sites.util.UnknownServer
import com.ead.lib.moongetter.utils.PatternManager

object ServerFactory {

    suspend fun create(
        context: Context,
        url : String,
        oneFichierToken : String?
    ) : Server {
        val server = if (PatternManager.match(Properties.Anonfiles,url)) {
            Anonfiles(
                context = context,
                url = url
            )
        }
        else if (PatternManager.match(Properties.Bayfiles,url)) {
            Bayfiles(
                context = context,
                url = url
            )
        }
        else if (PatternManager.match(Properties.Fembed,url)) {
            Fembed(
                context = context,
                url = url
            )
        }
        else if (PatternManager.match(Properties.Fireload,url)) {
            Fireload(
                context = context,
                url = url
            )
        }
        else if (PatternManager.match(Properties.Gofile,url)) {
            Gofile(
                context = context,
                url = url
            )
        }
        else if (PatternManager.match(Properties.GoogleDrive,url)) {
            GoogleDrive(
                context = context,
                url = url
            )
        }
        else if (PatternManager.match(Properties.Mediafire,url)) {
            Mediafire(
                context = context,
                url = url
            )
        }
        else if (PatternManager.match(Properties.Okru,url)) {
            Okru(
                context = context,
                url = url
            )
        }
        else if (PatternManager.match(Properties.OneFichier,url)) {
            Onefichier(
                context = context,
                url = url,
                token = oneFichierToken
            )
        }
        else if (PatternManager.match(Properties.PixelDrain,url)) {
            PixelDrain(
                context = context,
                url = url
            )
        }
        else if (PatternManager.match(Properties.Sendvid,url)) {
            Senvid(
                context = context,
                url = url
            )
        }
        else if (PatternManager.match(Properties.StreamSb,url)) {
            StreamSb(
                context = context,
                url = url
            )
        }
        else if (PatternManager.match(Properties.Streamtape,url)) {
            Streamtape(
                context = context,
                url = url
            )
        }
        else if (PatternManager.match(Properties.StreamWish,url)) {
            StreamWish(
                context = context,
                url = url
            )
        }
        else {
            UnknownServer(
                context = context,
                url = url
            )
        }

        server.onExtract()

        return server
    }

    suspend fun creates() : List<Server> {
        return emptyList()
    }
}
package com.ead.lib.moongetter.core

object Properties {

    const val api1FichierDownloadRequest = "https://api.1fichier.com/v1/download/get_token.cgi"

    const val okruUserAgent = "Mozilla/5.0 (Linux; Android 4.1.1; Galaxy Nexus Build/JRO03C) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19"

    const val AnonfilesIdentifier = "Anonfiles"
    const val BayfilesIdentifier  = "Bayfiles"
    const val FembedIdentifier = "Fembed"
    const val FilelionsIdentifier = "Filelions"
    const val FilemoonIdentifier = "Filemoon"
    const val FireloadIdentifier = "Fireload"
    const val GofileIdentifier = "Gofile"
    const val GoogleDriveIdentifier = "GoogleDrive"
    const val MediafireIdentifier = "Mediafire"
    const val OkruIdentifier = "Okru"
    const val OneFichierIdentifier = "OneFichier"
    const val PixelDrainIdentifier = "PixelDrain"
    const val SendvidIdentifier = "Sendvid"
    const val StreamSbIdentifier = "StreamSb"
    const val StreamtapeIdentifier = "Streamtape"
    const val StreamWishIdentifier = "StreamWish"
    const val VoeIdentifier = "Voe"
    const val VidguardIdentifier = "Vidguard"


    const val Anonfiles  = "https?:\\/\\/(www\\.)?(anonfile)\\.com\\/\\?.+"
    const val Bayfiles  = "https?:\\/\\/(www\\.)?(bayfiles)\\.com\\/\\?.+"
    const val Fembed = "https?:\\/\\/(www\\.)?(fembed|vcdn)\\.[^\\/,^\\.]{2,}\\/(v|f)\\/.+"
    const val Filelions = """https?://(?:www\.)?(filelions\.site)/v/[a-zA-Z0-9]+"""
    const val Filemoon = "https?://filemoon\\.sx/[^/]+"
    const val Fireload = "https?:\\/\\/(www\\.)?(fireload)\\.com\\/.+"
    const val Gofile = """https://gofile\.io/d/(\w+)"""
    const val GoogleDrive = "https?:\\/\\/(www\\.)?drive\\.google\\.com\\/(?:file\\/d\\/|open\\?id=|uc\\?id=)([\\w-]+)"
    const val Mediafire = """https?:\/\/(?:www\.)?mediafire\.com\/(?:file\/|\?.+)"""
    const val Okru = "https?:\\/\\/(www\\.)?ok\\.ru\\/videoembed\\/.+"
    const val OneFichier = "https?:\\/\\/(www\\.)?(1fichier)\\.com\\/\\?.+"
    const val PixelDrain = "https?:\\/\\/pixeldrain\\.com\\/u\\/\\w+(?:\\?.*)?"
    const val Sendvid = "https?:\\/\\/sendvid\\.com\\/\\w+"
    const val StreamSb = "https?://(?:sblanh|lvturbo|sbface|sbbrisk|sbchill|sblongvu|sbanh|playersb|embedsb|sbspeed|tubesb|sbrity)\\.com/(?:e|d)/\\w+\\.html"
    const val Streamtape = "https?://(?:streamtape|gettapeads)\\.com/e/\\w+"
    const val Voe = "https?://(?:voe|markstyleall|shannonpersonalcost|cindyeyefinal)\\.com/(?:e|d)/\\w+"
    const val StreamWish = "https?://(?:embedwish|streamwish|cdnwish|jodwish|swhoi)\\.(?:com|to)/e/\\w+"
    const val Vidguard = "https:\\/\\/listeamed\\.net\\/e\\/[a-zA-Z0-9]+"
}
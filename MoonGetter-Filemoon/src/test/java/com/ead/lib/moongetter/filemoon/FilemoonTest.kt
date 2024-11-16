package com.ead.lib.moongetter.filemoon

import android.content.Context
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.Values
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class FilemoonTest {

    private lateinit var context: Context
    private val client = OkHttpClient()
    private lateinit var server : MockWebServer

    private val hashMap = hashMapOf<String,String>()
    private val configData = Configuration.Data()

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        server = MockWebServer()

        server.start()
    }

    @Test
    fun `onExtract should add videos when response is successful always returns sd link at first`() = runBlocking {

        //given
        val url = server.url("successful test").toString()
        val url2 = server.url("successful test2").toString()

        var mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                    <iframe src="$url2" frameborder="0" marginwidth="0" marginheight="0" scrolling="no" width="100%" height="100%" allowfullscreen></iframe>
                    """.trimIndent())

        server.enqueue(mockResponse)

        mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                eval(function(p,a,c,k,e,d){while(c--)if(k[c])p=p.replace(new RegExp('\\b'+c.toString(a)+'\\b','g'),k[c]);return p}('l d=r("1c");d.9e({9d:[{9c:"2s://9b.9a.99.98.97/96/95/94/93/92.91?t=90&s=47&e=8z&f=48&8y=49&8x=8w&8v=8u&p="}],8t:"2s://8s.8r/19.8q",2k:"1z%",2j:"1z%",8p:"8o",8n:\'8m\',8l:"8k",8j:"8i",o:[],"8h":{"8g":"8f","8e":"8d","8c":1b},\'8b\':{"8a":"89"},88:{2l:"/87/1y/4p-4o.1y?v=3.0.6",1p:"4p-4o"},86:"85",84:"2s://83.82",81:{},80:1b,7z:[0.25,0.50,0.75,1,1.25,1.5,2]});l 2p,2r,7y;l 7x=0,7w=0;l d=r("1c");l 4n=0,7v=0,7u=0,1m=0;l 2h=\'19\';${'$'}.7t({7s:{\'7r-7q\':\'3z-7p\'}});d.k(\'37\',8(x){9(5>0&&x.1d>=5&&2r!=1){2r=1;$\{'$'}(\'y.7o\').7n(\'7m\')}9(x.1d>=1m+5||x.1d<1m){1m=x.1d;2d.7l(\'19\',2f.7k(1m),{7j:60*60*24})}});d.k(\'1i\',8(x){4n=x.1d});d.k(\'1s\',8(x){2q(x);$\{'$'}(\'.7-18-3u\').2g()});d.k(\'7i\',8(){$\{'$'}(\'y.4l\').7h();2d.2g(\'19\')});$\{'$'}(7g).k(\'w\',8(17){16 1l=17.7f-$\{'$'}(\'#1c\').4m().41;16 1k=17.7e-$\{'$'}(\'#1c\').4m().2i;2q(1l,1k)});8 2q(1l,1k){$\{'$'}(\'y.4l\').38();9(2p)1o;2p=1;16 20=0;9(2m.7d===1b){20=1}16 13="21";9(/7c|4j/i.j(g.h)){13=/7b|7a|79/i.j(g.h)?"4k/78":"4k/4j"}q 9(/4i/i.j(g.h)){13="21/4i"}q 9(/4h/i.j(g.h)){13="21/4h"}q 9(/77|76 74 73/i.j(g.h)){13="21/72"}q 9(/4g/i.j(g.h)){13="4f/4g"}q 9(/4e/i.j(g.h)){13="4f/4e"}q 9(/71|70|6z|6y/i.j(g.h)){13="6x"}16 z="6w";9(/4d/i.j(g.h)){z="4d"}q 9(/2o/i.j(g.h)&&!/2n|4a/i.j(g.h)){z="2o"}q 9(/4c/i.j(g.h)&&!/2o/i.j(g.h)){z="4c"}q 9(/4b/i.j(g.h)){z="4b"}q 9(/2n/i.j(g.h)){z="2n"}q 9(/4a/i.j(g.h)){z="6v"}q 9(/6u|6t/i.j(g.h)){z="6s 6r"}9(g.6q){z+=" (6p)"}16 45=2m.6o;16 44=2m.6n;16 2l=\'/6m?b=6l&2h=19&6k=48-6j-6i-47-6h&6g=1&6f=&6e=&20=\'+20+\'&1l=\'+1l+\'&1k=\'+1k+\'&6d=\'+46(13)+\'&6c=\'+46(z)+\'&6b=\'+45+\'&6a=\'+44;$\{'$'}.3d(2l,8(43){$\{'$'}(\'#69\').68(43)});$\{'$'}(\'.7-c-67-66:65("64")\').w(8(e){42();r().63(0);r().62(1b)});8 42(){l $\{'$'}1x=$\{'$'}("<y />").1y({1d:"61",2k:"1z%",2j:"1z%",41:0,2i:0,40:5z,5y:"5x(10%, 10%, 10%, 0.4)","2b-5w":"5v"});$\{'$'}("<5u />").1y({2k:"60%",2j:"60%",40:5t,"5s-2i":"5r"}).5q({\'5p\':\'/?b=5o&2h=19\',\'5n\':\'0\',\'5m\':\'3z\'}).3y($\{'$'}1x);$\{'$'}1x.w(8(){$\{'$'}(5l).2g();r().1s()});$\{'$'}1x.3y($\{'$'}(\'#1c\'))}r().1i(0)}8 3v(2e){l 3w=2f.3x(2e/60);l 1j=2f.3x(2e%60);9(1j<10){1j="0"+1j}1o 3w+":"+1j}8 3c(a){a=3v(a);$\{'$'}(\'#1c\').36(`<y 1w="7-18-3u"><y 1w="7-18-2b">5k 5j $\{a}</y><1v 1w="7-18-12">5i</1v><1v 1w="7-18-3b">5h</1v></y>`)}8 5g(){l o=d.23(3t);3s.3r(o);9(o.1g>1){2v(i=0;i<o.1g;i++){9(o[i].1p==3t){3s.3r(\'!!=\'+i);d.2t(i)}}}}d.k(\'5f\',8(){r().29(\'<1h 3q="3p://3o.3n.3m/3l/1h" 3k:3j="3i" 3h=" 0 0 1 1" 3g="1.1"/>\',"5e 10 3f",8(){r().1i(r().3e()+10)},"1r");$\{'$'}("y[12=1r]").3a().39(\'.7-u-2c\');r().29(\'<1h 3q="3p://3o.3n.3m/3l/1h" 3k:3j="3i" 3h=" 0 0 1 1" 3g="1.1"/>\',"5d 10 3f",8(){l 1u=r().3e()-10;9(1u<0)1u=0;r().1i(1u)},"1q");l 1t=2d.3d(\'19\');9(1t!==5c){3c(1t);$\{'$'}(\'1f\').k(\'w\',\'.7-18-12\',8(){d.1s();2y(8(){d.1i(1t)},5b)});$\{'$'}(\'1f\').k(\'w\',\'.7-18-3b\',8(){r().1s()})}$\{'$'}("y[12=1q]").3a().39(\'.7-u-2c\');$\{'$'}("y.7-u-2c").38();$\{'$'}(\'.7-5a-37\').36($\{'$'}(\'.7-2b-59\'));$\{'$'}(\'1f\').k(\'w\',\'.7-35-u-34 .7-u[12="1r"]\',8(){$\{'$'}(\'.7-28 .7-u[12="1r"]\').33(\'w\')});$\{'$'}(\'1f\').k(\'w\',\'.7-35-u-34 .7-u[12="1q"]\',8(){$\{'$'}(\'.7-28 .7-u[12="1q"]\').33(\'w\')})});8 2a(){}d.k(\'58\',8(){2a()});d.k(\'57\',8(){2a()});d.k("m",8(17){l o=d.23();9(o.1g<2)1o;$\{'$'}(\'.7-c-56-55\').54(8(){$\{'$'}(\'#7-c-n-m\').1n(\'7-c-n-1e\');$\{'$'}(\'.7-n-m\').15(\'11-14\',\'1a\')});d.29("/53/52.1h","30 2z",8(e){$\{'$'}(\'.7-32\').51(\'7-c-31\');9($\{'$'}(\'.7-32\').4z(\'7-c-31\')){$\{'$'}(\'.7-c-m\').15(\'11-14\',\'1b\');$\{'$'}(\'.7-c-n-m \').15(\'11-14\',\'1b\');$\{'$'}(\'.7-c-n-m \').4y(\'7-c-n-1e\')}q{$\{'$'}(\'.7-c-m\').15(\'11-14\',\'1a\');$\{'$'}(\'.7-c-n-m \').15(\'11-14\',\'1a\');$\{'$'}(\'.7-c-n-m \').1n(\'7-c-n-1e\')}$\{'$'}(\'.7-28 .7-u:4x([11-4w="30 2z"])\').k(\'w\',8(){$\{'$'}(\'.7-c-m\').15(\'11-14\',\'1a\');$\{'$'}(\'.7-c-n-m \').15(\'11-14\',\'1a\');$\{'$'}(\'.7-c-n-m \').1n(\'7-c-n-1e\')})},"4v");d.k("4u",8(17){27.4t(\'26\',17.o[17.4s].1p)});9(27.2x(\'26\')){2y("2w(27.2x(\'26\'));",4r)}});l 22;8 2w(2u){l o=d.23();9(o.1g>1){2v(i=0;i<o.1g;i++){9(o[i].1p==2u){9(i==22){1o}22=i;d.2t(i)}}}}$\{'$'}(\'1f\').k(\'w\',\'.7-u-c\',8(){$\{'$'}(\'.7-c-n-m \').1n(\'7-c-n-1e\');$\{'$'}(\'.7-12-4q.7-c-m\').15(\'11-14\',\'1a\')});',36,339,'|||||||jw|function|if|||settings|videop|||navigator|userAgent||test|on|var|audioTracks|submenu|tracks||else|jwplayer|||icon||click||div|browserName||aria|button|deviceType|expanded|attr|let|event|resume|cvnd9zqj2i9w|false|true|vplayer|position|active|body|length|svg|seek|remainingSeconds|cy|cx|lastt|removeClass|return|name|ff00|ff11|play|savedTime|tt|span|class|dd|css|100|adb|Desktop|current_audio|getAudioTracks|||default_audio|localStorage|controlbar|addButton|callMeMaybe|text|rewind|ls|seconds|Math|remove|file_code|top|height|width|url|window|Edge|Chrome|vvplay|doPlay|vvad|https|setCurrentAudioTrack|audio_name|for|audio_set|getItem|setTimeout|Track|Audio|open|controls|trigger|container|display|append|time|hide|insertAfter|detach|reset|addResume|get|getPosition|sec|version|viewBox|preserve|space|xml|2000|org|w3|www|http|xmlns|log|console|track_name|box|formatTime|minutes|floor|appendTo|no|zIndex|left|showCCform|data|windowHeight|windowWidth|encodeURIComponent|1731717439|35240312||OPR|Firefox|Safari|Brave|PlayStation|Console|Xbox|Linux|Windows|Android|Mobile|video_ad|offset|prevt|theme|jw8|color|300|currentTrack|setItem|audioTrackChanged|dualSound|label|not|addClass|hasClass||toggleClass|dualy|images|mousedown|buttons|topbar|playAttemptFailed|beforePlay|countdown|slider|1500|null|Rewind|Forward|ready|set_audio_track|No|Yes|at|Resume|this|scrolling|frameborder|upload_srt|src|prop|50px|margin|1000001|iframe|center|align|rgba|background|1000000||absolute|pause|setCurrentCaptions|Upload|contains|item|content|html|fviews|wh|ww|browser|device|referer|prem|embed|d460d93956bc965757395ddeb3d1e20e|199|181|hash|view|dl|innerHeight|innerWidth|Selenium|webdriver|Explorer|Internet|Trident|MSIE|Opera|Unknown|TV|NetCast|Web0S|Tizen|SmartTV|MacOS|X|OS||Mac|Macintosh|iOS|iPod|iPad|iPhone|Mobi|ZorDon|pageY|pageX|document|show|complete|ttl|round|set|slow|fadeIn|video_ad_fadein|cache|Cache|Content|headers|ajaxSetup|v2done|tott|vastdone2|vastdone1|vvbefore|playbackRates|playbackRateControls|cast|sx|filemoon|aboutlink|FileMoon|abouttext|assets|skin|720p|1231|qualityLabels|preloadAds|insecure|vpaidmode|vast|client|advertising|start|startparam|none|fullscreenOrientationLock|metadata|preload|uniform|stretching|jpg|me|videothumbs|image|4000|sp|27947|asn|srv|10800|T_lO4_kfxwAD7fnnYMkRp3QmBiNeFNNLmadWEwgfCO0|m3u8|master|cvnd9zqj2i9w_h|07048|01|hls2|com|cdn112|waw04|rcr72|be6721|file|sources|setup'.split('|')))
                """.trimIndent())

        server.enqueue(mockResponse)

        //when
        val filemoon = Filemoon(context, url, client , hashMap, configData)
        Values.targetUrl = url

        val videos = filemoon.onExtract()

        //then
        assert(videos.firstOrNull()?.request?.url == """https://be6721.rcr72.waw04.cdn112.com/hls2/01/07048/cvnd9zqj2i9w_h/master.m3u8?t=T_lO4_kfxwAD7fnnYMkRp3QmBiNeFNNLmadWEwgfCO0&s=1731717439&e=10800&f=35240312&srv=49&asn=27947&sp=4000&p=""".trimIndent())
        server.shutdown()
    }



    @Test(expected = InvalidServerException::class)
    fun `onExtract should add videos when response is unsuccessful`() = runBlocking {

        //given
        val url = server.url("response is unsuccessful").toString()

        val mockResponse = MockResponse()
            .setResponseCode(400)

        server.enqueue(mockResponse)

        //when
        val filemoon = Filemoon(context, url, client ,hashMap, configData)
        Values.targetUrl = url
        filemoon.onExtract()

        //then
        server.shutdown()
    }


    @Test(expected = InvalidServerException::class)
    fun `onExtract should add videos when response is successful but body isn't expected, throws InvalidServerException`() = runBlocking {

        //given
        val url = server.url("response is successful but body isn't expected").toString()

        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""This is random body""".trimIndent())

        server.enqueue(mockResponse)

        //when
        val filemoon = Filemoon(context, url, client ,hashMap, configData)
        Values.targetUrl = url
        filemoon.onExtract()

        //then
        server.shutdown()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }
}
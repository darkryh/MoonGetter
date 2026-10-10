package com.ead.lib.moongetter.voe

import com.ead.lib.moongetter.voe.factory.VoeFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VoeSourceTest {
    @Test
    fun decodesCurrentConfigurationAndIgnoresPlaceholder() {
        val html = """<script>var source='https://test-videos.co.uk/placeholder.mp4';</script>
            <SCRIPT type="application/json">
            ["DROH^^nJkp^^J117^^KSqy^^rJf1^^MQqW^^sJH4^^GHMZ^^BHkJ^^KJ48^^AIkc^^JHcy^^p11j^^GTc6^^oRk9^^GSqD^^CSOk^^GJ97^^FzM3^^FHcb^^omEU^^MmWd^^I2q9^^Z3OC^^sSyX^^M31W^^JzI5^^KSgq^^r1kK^^MKye^^AJD3^^FK1y^^BQk8^^KUqZ^^sJM2^^nmIC^^omun^^MUR4^^Jy15^^ZmIy^^oGuJ^^KKMS^^J2Ie^^n1gq^^rQqX^^KJ08^^sR85^^HRgz^^Z1IY^^MTkR^^oSWf^^JQIp^^sSx2^^MK1A^^sTt="]
            </SCRIPT>"""
        assertEquals("https://cdn.example.com/engine/hls2-c/yo1xkhom61ev/master.m3u8?t=example&s=123", VoeSource.extract(html))
    }

    @Test
    fun malformedConfigurationDoesNotReturnPlaceholder() {
        assertNull(VoeSource.extract("""<SCRIPT type="application/json">
            ["invalid"]</script>
            <script>var source='https://test-videos.co.uk/placeholder.mp4';</script>"""))
    }

    @Test
    fun unrelatedJsonDoesNotHideValidLegacySource() {
        assertEquals("https://cdn.example.com/master.m3u8", VoeSource.extract(
            """<script type='application/json'>{"metadata": true}</script>
                var sources = {'hls': 'aHR0cHM6Ly9jZG4uZXhhbXBsZS5jb20vbWFzdGVyLm0zdTg='};"""))
    }

    @Test
    fun recognizesNewMirror() {
        assertTrue(Regex(VoeFactory.pattern).matches("https://teresapoliticallearn.com/e/yo1xkhom61ev"))
    }
}

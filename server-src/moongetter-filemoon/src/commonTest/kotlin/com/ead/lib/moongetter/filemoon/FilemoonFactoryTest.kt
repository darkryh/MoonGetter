package com.ead.lib.moongetter.filemoon

import com.ead.lib.moongetter.filemoon.factory.FilemoonFactory
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FilemoonFactoryTest {
    @Test fun matchesExistingAndNewMirrorUrls() {
        val regex = Regex(FilemoonFactory.pattern)
        assertTrue(regex.matches("https://bysekoze.com/e/y6kmotbkxiha/"))
        assertTrue(regex.matches("https://filemoon.sx/e/cvnd9zqj2i9w"))
        assertTrue(regex.matches("https://filemoon.sx/abcd1234"))
        assertFalse(regex.matches("https://bysekoze.com.evil.test/e/y6kmotbkxiha/"))
    }
}

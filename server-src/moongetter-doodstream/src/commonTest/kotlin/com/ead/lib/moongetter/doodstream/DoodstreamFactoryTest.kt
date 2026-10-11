package com.ead.lib.moongetter.doodstream

import com.ead.lib.moongetter.doodstream.factory.DoodstreamFactory
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DoodstreamFactoryTest {
    private val pattern = Regex(DoodstreamFactory.pattern)

    @Test fun acceptsDsvplayAndExistingDomainFamily() {
        listOf(
            "https://dsvplay.com/e/j0jlmfjfalnj",
            "https://playmogo.com/e/j0jlmfjfalnj",
            "https://www.dsvplay.com/e/j0jlmfjfalnj/?autoplay=1#play",
            "https://doodstream.com/e/abcdef1234",
            "https://dood.wf/e/abcd1234",
            "https://myvidplay.com/e/abcd1234",
            "https://d-s.io/e/abcd1234"
        ).forEach { assertTrue(pattern.matches(it), it) }
    }

    @Test fun rejectsUnrelatedDomainsAndEmbeddedUrlText() {
        listOf(
            "https://dsvplay.com.evil.test/e/j0jlmfjfalnj",
            "https://evil.test/?url=https://dsvplay.com/e/j0jlmfjfalnj",
            "https://dsvplayXcom/e/j0jlmfjfalnj",
            "https://dsvplay.com/e/",
            "https://dsvplay.com/e/j0jlmfjfalnj/extra",
            "https://other.com/e/j0jlmfjfalnj",
            // Unlisted look-alike names are no longer guessed; the opt-in fallback identifies them instead.
            "https://do9streamxyz.io/e/abcd1234"
        ).forEach { assertFalse(pattern.containsMatchIn(it), it) }
    }
}

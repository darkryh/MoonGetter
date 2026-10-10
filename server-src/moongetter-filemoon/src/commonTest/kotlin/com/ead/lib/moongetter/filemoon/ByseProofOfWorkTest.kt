package com.ead.lib.moongetter.filemoon

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ByseProofOfWorkTest {
    @Test fun matchesPublicPlayerFixture() = runBlocking {
        assertEquals("169", ByseProofOfWork.solve("validation-fixture", 8, 170))
        assertNull(ByseProofOfWork.solve("validation-fixture", 8, 169))
    }

    @Test fun rejectsUnboundedDifficulty() = runBlocking {
        assertFailsWith<IllegalArgumentException> {
            ByseProofOfWork.solve("validation-fixture", 256)
        }
        Unit
    }
}

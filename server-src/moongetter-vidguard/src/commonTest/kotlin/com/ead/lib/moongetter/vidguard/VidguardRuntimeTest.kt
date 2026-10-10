package com.ead.lib.moongetter.vidguard

import com.ead.lib.moongetter.vidguard.factory.util.executeRunnableScript
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class VidguardRuntimeTest {
    @Test fun returnsSvgJsonInsteadOfLastExpression() = runBlocking {
        repeat(10) {
            assertEquals("{\"stream\":\"https://cdn.example/video.m3u8\"}",
                executeRunnableScript("window.svg = {stream: 'https://cdn.example/video.m3u8'}; 42;"))
        }
    }

    @Test fun javascriptErrorsAreReportedAndNextExecutionWorks() = runBlocking {
        assertFails { executeRunnableScript("throw new Error('test failure');") }
        assertEquals("{\"stream\":\"ok\"}", executeRunnableScript("var svg = {stream: 'ok'};"))
    }
}

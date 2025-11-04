package com.ead.lib.moongetter.utils

import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExtractionStrategyTest {

    @Test
    fun testWithRetry_successOnFirstAttempt() = runTest {
        var attempts = 0
        val result = ExtractionStrategy.withRetry(
            config = ExtractionStrategy.RetryConfig.NoRetry
        ) {
            attempts++
            "success"
        }
        
        assertEquals("success", result)
        assertEquals(1, attempts)
    }

    @Test
    fun testWithRetry_successOnSecondAttempt() = runTest {
        var attempts = 0
        val result = ExtractionStrategy.withRetry(
            config = ExtractionStrategy.RetryConfig(maxAttempts = 3, delayMillis = 10)
        ) {
            attempts++
            if (attempts < 2) {
                throw InvalidServerException("Temporary failure", Error.TIMEOUT_ERROR)
            }
            "success"
        }
        
        assertEquals("success", result)
        assertEquals(2, attempts)
    }

    @Test
    fun testWithRetry_failsAfterMaxAttempts() = runTest {
        var attempts = 0
        assertFailsWith<InvalidServerException> {
            ExtractionStrategy.withRetry(
                config = ExtractionStrategy.RetryConfig(maxAttempts = 3, delayMillis = 10)
            ) {
                attempts++
                throw InvalidServerException("Persistent failure", Error.EXPECTED_RESPONSE_NOT_FOUND)
            }
        }
        
        assertEquals(3, attempts)
    }

    @Test
    fun testWithRetry_defaultConfig() = runTest {
        var attempts = 0
        assertFailsWith<InvalidServerException> {
            ExtractionStrategy.withRetry() {
                attempts++
                throw InvalidServerException("Failure", Error.EXPECTED_RESPONSE_NOT_FOUND)
            }
        }
        
        // Default is 3 attempts
        assertEquals(3, attempts)
    }

    @Test
    fun testTryStrategies_firstSucceeds() = runTest {
        val strategies = listOf<suspend () -> String>(
            { "first" },
            { "second" },
            { "third" }
        )
        
        val result = ExtractionStrategy.tryStrategies(strategies)
        
        assertEquals("first", result)
    }

    @Test
    fun testTryStrategies_secondSucceeds() = runTest {
        val strategies = listOf<suspend () -> String>(
            { throw Exception("First failed") },
            { "second" },
            { "third" }
        )
        
        val result = ExtractionStrategy.tryStrategies(strategies)
        
        assertEquals("second", result)
    }

    @Test
    fun testTryStrategies_allFail() = runTest {
        val strategies = listOf<suspend () -> String>(
            { throw Exception("First failed") },
            { throw Exception("Second failed") },
            { throw Exception("Third failed") }
        )
        
        assertFailsWith<InvalidServerException> {
            ExtractionStrategy.tryStrategies(strategies)
        }
    }

    @Test
    fun testWithFallback_primarySucceeds() = runTest {
        val result = ExtractionStrategy.withFallback(
            primary = { "primary" },
            fallback = { "fallback" }
        )
        
        assertEquals("primary", result)
    }

    @Test
    fun testWithFallback_primaryFailsFallbackSucceeds() = runTest {
        val result = ExtractionStrategy.withFallback(
            primary = { throw Exception("Primary failed") },
            fallback = { "fallback" }
        )
        
        assertEquals("fallback", result)
    }

    @Test
    fun testWithValidation_validResult() = runTest {
        var attempts = 0
        val result = ExtractionStrategy.withValidation(
            config = ExtractionStrategy.RetryConfig(maxAttempts = 3, delayMillis = 10),
            validator = { it.startsWith("http") }
        ) {
            attempts++
            "https://example.com/video.mp4"
        }
        
        assertEquals("https://example.com/video.mp4", result)
        assertEquals(1, attempts)
    }

    @Test
    fun testWithValidation_invalidResultRetries() = runTest {
        var attempts = 0
        assertFailsWith<InvalidServerException> {
            ExtractionStrategy.withValidation(
                config = ExtractionStrategy.RetryConfig(maxAttempts = 3, delayMillis = 10),
                validator = { it.startsWith("https") }
            ) {
                attempts++
                "http://example.com/video.mp4" // Always fails validation
            }
        }
        
        assertEquals(3, attempts)
    }

    @Test
    fun testWithValidation_becomesValidOnSecondAttempt() = runTest {
        var attempts = 0
        val result = ExtractionStrategy.withValidation(
            config = ExtractionStrategy.RetryConfig(maxAttempts = 3, delayMillis = 10),
            validator = { it.contains("video") }
        ) {
            attempts++
            if (attempts < 2) "test.mp4" else "video.mp4"
        }
        
        assertEquals("video.mp4", result)
        assertEquals(2, attempts)
    }

    @Test
    fun testRetryConfig_default() {
        val config = ExtractionStrategy.RetryConfig.Default
        
        assertEquals(3, config.maxAttempts)
        assertEquals(1000L, config.delayMillis)
        assertEquals(2.0, config.backoffMultiplier)
    }

    @Test
    fun testRetryConfig_noRetry() {
        val config = ExtractionStrategy.RetryConfig.NoRetry
        
        assertEquals(1, config.maxAttempts)
    }

    @Test
    fun testRetryConfig_aggressive() {
        val config = ExtractionStrategy.RetryConfig.Aggressive
        
        assertEquals(5, config.maxAttempts)
        assertEquals(500L, config.delayMillis)
    }

    @Test
    fun testRetryConfig_custom() {
        val config = ExtractionStrategy.RetryConfig(
            maxAttempts = 10,
            delayMillis = 100,
            backoffMultiplier = 1.5
        )
        
        assertEquals(10, config.maxAttempts)
        assertEquals(100L, config.delayMillis)
        assertEquals(1.5, config.backoffMultiplier)
    }
}

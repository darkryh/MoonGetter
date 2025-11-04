package com.ead.lib.moongetter.utils

import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.delay

/**
 * Defines different strategies for content extraction with retry and fallback mechanisms.
 */
object ExtractionStrategy {

    /**
     * Configuration for retry behavior during extraction.
     *
     * @property maxAttempts Maximum number of retry attempts (default: 3)
     * @property delayMillis Delay between retries in milliseconds (default: 1000ms)
     * @property backoffMultiplier Multiplier for exponential backoff (default: 2.0)
     */
    data class RetryConfig(
        val maxAttempts: Int = 3,
        val delayMillis: Long = 1000L,
        val backoffMultiplier: Double = 2.0
    ) {
        companion object {
            val Default = RetryConfig()
            val NoRetry = RetryConfig(maxAttempts = 1)
            val Aggressive = RetryConfig(maxAttempts = 5, delayMillis = 500L)
        }
    }

    /**
     * Executes an extraction operation with retry logic and exponential backoff.
     *
     * @param config Retry configuration to use.
     * @param operation The extraction operation to execute.
     * @return The result of the successful operation.
     * @throws InvalidServerException if all retry attempts fail.
     */
    suspend fun <T> withRetry(
        config: RetryConfig = RetryConfig.Default,
        operation: suspend () -> T
    ): T {
        var lastException: Exception? = null
        var currentDelay = config.delayMillis

        repeat(config.maxAttempts) { attempt ->
            try {
                return operation()
            } catch (e: InvalidServerException) {
                lastException = e
                if (attempt < config.maxAttempts - 1) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * config.backoffMultiplier).toLong()
                }
            }
        }

        throw lastException ?: InvalidServerException(
            "Extraction failed after ${config.maxAttempts} attempts",
            com.ead.lib.moongetter.models.error.Error.EXPECTED_RESPONSE_NOT_FOUND
        )
    }

    /**
     * Tries multiple extraction strategies in sequence until one succeeds.
     *
     * @param strategies List of extraction functions to try.
     * @return The result from the first successful strategy.
     * @throws InvalidServerException if all strategies fail.
     */
    suspend fun <T> tryStrategies(
        strategies: List<suspend () -> T>
    ): T {
        val exceptions = mutableListOf<Exception>()

        for ((index, strategy) in strategies.withIndex()) {
            try {
                return strategy()
            } catch (e: Exception) {
                exceptions.add(e)
            }
        }

        throw InvalidServerException(
            "All ${strategies.size} extraction strategies failed",
            com.ead.lib.moongetter.models.error.Error.EXPECTED_RESPONSE_NOT_FOUND
        )
    }

    /**
     * Executes an extraction with a fallback strategy if the primary fails.
     *
     * @param primary The primary extraction operation.
     * @param fallback The fallback operation to try if primary fails.
     * @return The result from either primary or fallback operation.
     */
    suspend fun <T> withFallback(
        primary: suspend () -> T,
        fallback: suspend () -> T
    ): T {
        return try {
            primary()
        } catch (e: Exception) {
            fallback()
        }
    }

    /**
     * Validates the extracted result and retries if validation fails.
     *
     * @param config Retry configuration to use.
     * @param validator Function to validate the extracted result.
     * @param operation The extraction operation to execute.
     * @return The validated result.
     * @throws InvalidServerException if validation fails after all retries.
     */
    suspend fun <T> withValidation(
        config: RetryConfig = RetryConfig.Default,
        validator: (T) -> Boolean,
        operation: suspend () -> T
    ): T {
        return withRetry(config) {
            val result = operation()
            if (!validator(result)) {
                throw InvalidServerException(
                    "Validation failed for extracted content",
                    com.ead.lib.moongetter.models.error.Error.EXPECTED_RESPONSE_NOT_FOUND
                )
            }
            result
        }
    }
}

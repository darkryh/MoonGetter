package com.ead.lib.moongetter.vidguard.util

import kotlinx.coroutines.asCoroutineDispatcher

/**
 * Provides coroutine dispatchers for VidGuard.
 *
 * Encapsulates low-level thread management behind a CoroutineDispatcher
 * so that VidGuard tasks can run within Kotlin coroutines seamlessly.
 */
object Dispatchers {
    /**
     * A CoroutineDispatcher backed by the single-thread executor service.
     *
     * All coroutine work dispatched here will execute on the dedicated
     * VidguardWorkerThread with an enlarged stack size.
     */
    val Vidguard = threadService.asCoroutineDispatcher()
}
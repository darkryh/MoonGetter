package com.ead.lib.moongetter.utils

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

actual suspend fun <T> runWithStackSize(size: Int, block: () -> T): T = suspendCancellableCoroutine { continuation ->
    val thread = Thread(
        null,
        {
            try {
                val result = block()
                continuation.resume(result) { cause, _, _ ->cause.printStackTrace() }
            } catch (e: Throwable) { continuation.resumeWithException(e) }
        },
        "RunWithStackSizeThread",
        size.toLong()
    )
    thread.start()
    continuation.invokeOnCancellation { thread.interrupt() }
}
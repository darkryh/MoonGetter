package com.ead.lib.moongetter.utils

import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
import platform.posix.pthread_attr_destroy
import platform.posix.pthread_attr_init
import platform.posix.pthread_attr_setstacksize
import platform.posix.pthread_attr_t
import platform.posix.pthread_create
import platform.posix.pthread_join
import platform.posix.pthread_tVar
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
private fun runWithStackSizeThreadEntry(arg: COpaquePointer?): COpaquePointer? {
    val pairRef = arg!!.asStableRef<Pair<() -> Any?, kotlin.coroutines.Continuation<Any?>>>()
    val (block, cont) = pairRef.get()

    try {
        val result = block()
        cont.resume(result)
    } catch (e: Throwable) {
        cont.resumeWithException(e)
    } finally {
        pairRef.dispose()
    }

    return null
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun <T> runWithStackSize(size: Int, block: () -> T): T = suspendCoroutine { continuation ->
    throw InvalidServerException("not implemented", Error.EXPECTED_RESPONSE_NOT_FOUND)
    memScoped {
        val attr = alloc<pthread_attr_t>()
        check(pthread_attr_init(attr.ptr) == 0)
        check(pthread_attr_setstacksize(attr.ptr, size.toULong()) == 0)

        val thread = alloc<pthread_tVar>()
        val stableRef = StableRef.create(block to continuation)

        val result = pthread_create(
            thread.ptr,
            attr.ptr,
            staticCFunction(::runWithStackSizeThreadEntry),
            stableRef.asCPointer()
        )

        pthread_attr_destroy(attr.ptr)

        if (result != 0) {
            stableRef.dispose()
            continuation.resumeWithException(RuntimeException("Failed to create pthread: $result"))
        } else {
            pthread_join(thread.value, null)
        }
    }
}
package com.ead.lib.moongetter.vidguard.factory.util

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.free
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.JavaScriptCore.JSEvaluateScript
import platform.JavaScriptCore.JSGlobalContextCreate
import platform.JavaScriptCore.JSGlobalContextRelease
import platform.JavaScriptCore.JSStringCreateWithUTF8CString
import platform.JavaScriptCore.JSStringGetMaximumUTF8CStringSize
import platform.JavaScriptCore.JSStringGetUTF8CString
import platform.JavaScriptCore.JSStringRef
import platform.JavaScriptCore.JSStringRelease
import platform.JavaScriptCore.JSValueRefVar
import platform.JavaScriptCore.JSValueToStringCopy
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.pthread_attr_destroy
import platform.posix.pthread_attr_init
import platform.posix.pthread_attr_setstacksize
import platform.posix.pthread_attr_t
import platform.posix.pthread_create
import platform.posix.pthread_tVar
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class)
actual suspend fun executeRunnableScript(scriptCode: String): String = suspendCoroutine { cont ->
    memScoped {
        val thread = alloc<pthread_tVar>()
        val attr = alloc<pthread_attr_t>()
        val STACK_SIZE = (8 * 1024 * 1024).toULong()

        pthread_attr_init(attr.ptr)
        pthread_attr_setstacksize(attr.ptr, STACK_SIZE)

        val stableRef = StableRef.create<Pair<String, Continuation<String>>>(
            Pair(scriptCode, cont) // O cont.freeze() si usás memory manager legacy
        )

        val result = pthread_create(
            thread.ptr,
            attr.ptr,
            staticCFunction(::jsThreadEntry),
            stableRef.asCPointer()
        )

        pthread_attr_destroy(attr.ptr)

        if (result != 0) {
            stableRef.dispose()
            cont.resumeWithException(RuntimeException("Failed to create pthread: $result"))
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun jsStringToKString(jsString: JSStringRef?): String {
    if (jsString == null) return ""
    val maxSize = JSStringGetMaximumUTF8CStringSize(jsString)
    val buffer = nativeHeap.allocArray<ByteVar>(maxSize.toInt())
    JSStringGetUTF8CString(jsString, buffer, maxSize)
    val result = buffer.toKString()
    nativeHeap.free(buffer)
    JSStringRelease(jsString)
    return result
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("jsThreadEntry")
fun jsThreadEntry(rawData: COpaquePointer?): COpaquePointer? {
    val ref = rawData!!.asStableRef<Pair<String, Continuation<String>>>()
    val (script, continuation) = ref.get()

    try {
        val context = JSGlobalContextCreate(null)

        val scriptRef = JSStringCreateWithUTF8CString(script)
        val sourceURLRef = JSStringCreateWithUTF8CString("script.js")

        val exception = nativeHeap.alloc<JSValueRefVar>()
        exception.value = null

        val result = JSEvaluateScript(context, scriptRef, null, sourceURLRef, 1, exception.ptr)

        JSStringRelease(scriptRef)
        JSStringRelease(sourceURLRef)

        if (exception.value != null) {
            val errorStrRef = JSValueToStringCopy(context, exception.value, null)
            val error = jsStringToKString(errorStrRef)
            JSStringRelease(errorStrRef)

            dispatch_async(dispatch_get_main_queue()) {
                continuation.resumeWithException(RuntimeException(error))
            }
        } else {
            val resultStrRef = JSValueToStringCopy(context, result, null)
            val output = jsStringToKString(resultStrRef)
            JSStringRelease(resultStrRef)

            dispatch_async(dispatch_get_main_queue()) {
                continuation.resume(output)
            }
        }

        JSGlobalContextRelease(context)
    } catch (e: Throwable) {
        dispatch_async(dispatch_get_main_queue()) {
            continuation.resumeWithException(e)
        }
    } finally {
        ref.dispose()
    }

    return null
}
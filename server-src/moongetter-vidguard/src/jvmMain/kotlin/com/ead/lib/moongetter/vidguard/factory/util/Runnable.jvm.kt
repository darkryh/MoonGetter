package com.ead.lib.moongetter.vidguard.factory.util

import com.ead.lib.moongetter.vidguard.util.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeJSON
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.Scriptable

actual suspend fun executeRunnableScript(scriptCode: String): String = withContext(Dispatchers.Vidguard) {
    val result: String
    val rhinoContext = Context.enter()

    try {

        rhinoContext.initSafeStandardObjects()
        rhinoContext.isInterpretedMode = true

        val executorScriptable: Scriptable = rhinoContext.initSafeStandardObjects()
        executorScriptable.put("window", executorScriptable, executorScriptable)

        rhinoContext.evaluateString(
            executorScriptable,
            scriptCode,
            "JavaScript",
            1,
            null
        )

        val svgObject = executorScriptable.get("svg", executorScriptable)

        result = if (svgObject is NativeObject) {
            NativeJSON.stringify(Context.getCurrentContext(), executorScriptable, svgObject, null, null).toString()
        } else {
            Context.toString(svgObject)
        }
    } finally {
        Context.exit()
    }

    result
}
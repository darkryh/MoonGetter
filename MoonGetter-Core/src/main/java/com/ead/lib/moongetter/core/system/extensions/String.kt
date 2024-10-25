package com.ead.lib.moongetter.core.system.extensions

fun String.delete(string: String) : String =
    this.replace(string,"")

/*
fun String.toHtml() : String =
    replace("\\u003C","<")
        .lineSeparator()
        .replace("\\\"","\"")
        .takeIf { it.isNotEmpty() } ?: "null"
        .trim().removeSurrounding("\"")*/

package com.ead.lib.moongetter.core.system.extensions

fun String.delete(string: String) : String =
    this.replace(string,"")
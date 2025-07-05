@file:Suppress("unused")

package com.ead.lib.moongetter.core.system.extensions

import com.ead.lib.moongetter.utils.PatternManager

fun String.delete(string: String) : String =
    this.replace(string,"")

fun String.replaceDomainWith(domain: String) : String? {
    val domainToReplace = PatternManager.singleMatch(
        string = this,
        regex = """https?://([a-zA-Z0-9\-]+\.[a-zA-Z]{2,})(/[^\s]*)?"""
    )

    return domainToReplace?.let {
        this.replace(domainToReplace, domain)
    }
}

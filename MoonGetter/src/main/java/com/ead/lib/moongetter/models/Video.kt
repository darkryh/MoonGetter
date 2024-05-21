package com.ead.lib.moongetter.models

data class Video(
    val quality : String?,
    val url : String,
    val cookies : Map<String, String> ?= null,
    val headers : Map<String, String> ?= null
)
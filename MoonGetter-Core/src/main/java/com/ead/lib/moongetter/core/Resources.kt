package com.ead.lib.moongetter.core

object Resources {

    const val NO_PARAMETERS_TO_WORK: String = "No parameters to work"

    const val ENGINES_NOT_PROVIDED: String = "Engines hans´t provided"

    const val NOT_SERVERS_FOUND: String = "Not server's found"

    const val UNSUCCESSFUL_RESPONSE: String = "Unsuccessful response"

    const val UNKNOWN_ERROR = "Unknown error"

    fun invalidProcessInExpectedUrlEntry(name : String): String {
        return "Invalid process in expected url entry: $name"
    }


    fun emptyOrNullResponse(name : String): String {
        return "Empty or null response: $name"
    }

    fun expectedResponseNotFound(name : String): String {
        return "Expected response not found: $name"
    }

    fun expectedPackedResponseNotFound(name : String): String {
        return "Expected packed response not found: $name"
    }


    fun unsuccessfulResponse(name : String): String {
        return "Unsuccessful response: $name"
    }

    fun resourceTakenDown(name : String): String {
        return "Resource taken down: $name"
    }
}
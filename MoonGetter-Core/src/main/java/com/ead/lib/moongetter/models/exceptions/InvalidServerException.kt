package com.ead.lib.moongetter.models.exceptions

import com.ead.lib.moongetter.models.Error

class InvalidServerException (
    message: String,
    val error: Error,
    val code: Int? = null,
) : Exception(message)
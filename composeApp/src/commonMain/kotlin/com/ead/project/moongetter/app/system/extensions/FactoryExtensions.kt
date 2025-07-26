package com.ead.project.moongetter.app.system.extensions

import com.ead.lib.moongetter.core.ExperimentalFeature
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.builder.Factory
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.project.moongetter.app.network.util.MoonGetterError
import com.ead.project.moongetter.app.network.util.Result
import kotlinx.coroutines.delay

const val DelayValue = 500L

suspend inline fun <reified T> Factory.Builder.onGetResult(url : String) : Result<T,MoonGetterError> {
    return try {
        val  result = get(url) ?: return Result.Error(MoonGetterError.NOT_RECOGNIZED_URL).also { delay(DelayValue) }
        Result.Success(
            result as T
        )
    }
    catch (e : InvalidServerException) {
        return when(e.error) {
            Error.INVALID_PROCESS_IN_EXPECTED_URL_ENTRY -> {
                Result.Error(MoonGetterError.UNKNOWN)
            }
            Error.INVALID_BUILDER_PARAMETERS -> {
                Result.Error(MoonGetterError.INVALID_PARAMETERS_IN_BUILDER)
            }
            Error.NO_PARAMETERS_TO_WORK -> {
                Result.Error(MoonGetterError.NO_PARAMETERS_TO_WORK).also { delay(DelayValue) }
            }
            Error.EMPTY_OR_NULL_RESPONSE -> {
                Result.Error(MoonGetterError.SERVERS_RESPONSE_WENT_WRONG)
            }
            Error.EXPECTED_RESPONSE_NOT_FOUND -> {
                Result.Error(MoonGetterError.RESOURCE_TAKEN_DOWN)
            }
            Error.EXPECTED_PACKED_RESPONSE_NOT_FOUND -> {
                Result.Error(MoonGetterError.RESOURCE_TAKEN_DOWN)
            }
            Error.TIMEOUT_ERROR -> {
                Result.Error(MoonGetterError.REQUEST_TIMEOUT)
            }
            Error.UNSUCCESSFUL_RESPONSE -> {
                return when (e.code ?: return Result.Error(MoonGetterError.UNKNOWN)) {
                    401 -> Result.Error(MoonGetterError.UNAUTHORIZED)
                    408 -> Result.Error(MoonGetterError.REQUEST_TIMEOUT)
                    409 -> Result.Error(MoonGetterError.CONFLICT)
                    413 -> Result.Error(MoonGetterError.PAYLOAD_TOO_LARGE)
                    429 -> Result.Error(MoonGetterError.TOO_MANY_REQUESTS)
                    in 500..599 -> Result.Error(MoonGetterError.SERVER_ERROR)
                    else -> Result.Error(MoonGetterError.UNKNOWN)
                }
            }
            Error.RESOURCE_TAKEN_DOWN -> {
                Result.Error(MoonGetterError.RESOURCE_TAKEN_DOWN)
            }
        }
    }
    catch (_ : IllegalArgumentException) {
        Result.Error(MoonGetterError.UNKNOWN)
    }
}

suspend inline fun <reified T> Factory.Builder.onGetUntilFindResult(urls : List<String>) : Result<T,MoonGetterError> {
    return try {
        val  result = getUntilFindResource(urls) ?: return Result.Error(MoonGetterError.NOT_RECOGNIZED_URL).also { delay(DelayValue) }
        Result.Success(
            result as T
        )
    }
    catch (e : InvalidServerException) {
        return when(e.error) {
            Error.INVALID_PROCESS_IN_EXPECTED_URL_ENTRY -> {
                Result.Error(MoonGetterError.UNKNOWN)
            }
            Error.INVALID_BUILDER_PARAMETERS -> {
                Result.Error(MoonGetterError.INVALID_PARAMETERS_IN_BUILDER)
            }
            Error.NO_PARAMETERS_TO_WORK -> {
                Result.Error(MoonGetterError.NO_PARAMETERS_TO_WORK).also { delay(DelayValue) }
            }
            Error.EMPTY_OR_NULL_RESPONSE -> {
                Result.Error(MoonGetterError.SERVERS_RESPONSE_WENT_WRONG)
            }
            Error.EXPECTED_RESPONSE_NOT_FOUND -> {
                Result.Error(MoonGetterError.RESOURCE_TAKEN_DOWN)
            }
            Error.EXPECTED_PACKED_RESPONSE_NOT_FOUND -> {
                Result.Error(MoonGetterError.RESOURCE_TAKEN_DOWN)
            }
            Error.TIMEOUT_ERROR -> {
                Result.Error(MoonGetterError.REQUEST_TIMEOUT)
            }
            Error.UNSUCCESSFUL_RESPONSE -> {
                return when (e.code ?: return Result.Error(MoonGetterError.UNKNOWN)) {
                    401 -> Result.Error(MoonGetterError.UNAUTHORIZED)
                    408 -> Result.Error(MoonGetterError.REQUEST_TIMEOUT)
                    409 -> Result.Error(MoonGetterError.CONFLICT)
                    413 -> Result.Error(MoonGetterError.PAYLOAD_TOO_LARGE)
                    429 -> Result.Error(MoonGetterError.TOO_MANY_REQUESTS)
                    in 500..599 -> Result.Error(MoonGetterError.SERVER_ERROR)
                    else -> Result.Error(MoonGetterError.UNKNOWN)
                }
            }
            Error.RESOURCE_TAKEN_DOWN -> {
                Result.Error(MoonGetterError.RESOURCE_TAKEN_DOWN)
            }
        }
    }
    catch (_ : IllegalArgumentException) {
        Result.Error(MoonGetterError.UNKNOWN)
    }
}


@ExperimentalFeature
suspend inline fun <reified T> Factory.Builder.onGetResults(urls : List<String>) : Result<T,MoonGetterError> {
    return try {
        val  result = get(urls).ifEmpty { return Result.Error(MoonGetterError.NOT_RECOGNIZED_URL).also { delay(DelayValue) } }
        Result.Success(
            result as T
        )
    }
    catch (e : InvalidServerException) {
        return when(e.error) {
            Error.INVALID_PROCESS_IN_EXPECTED_URL_ENTRY -> {
                Result.Error(MoonGetterError.UNKNOWN)
            }
            Error.INVALID_BUILDER_PARAMETERS -> {
                Result.Error(MoonGetterError.INVALID_PARAMETERS_IN_BUILDER)
            }
            Error.NO_PARAMETERS_TO_WORK -> {
                Result.Error(MoonGetterError.NO_PARAMETERS_TO_WORK).also { delay(DelayValue) }
            }
            Error.EMPTY_OR_NULL_RESPONSE -> {
                Result.Error(MoonGetterError.SERVERS_RESPONSE_WENT_WRONG)
            }
            Error.EXPECTED_RESPONSE_NOT_FOUND -> {
                Result.Error(MoonGetterError.RESOURCE_TAKEN_DOWN)
            }
            Error.EXPECTED_PACKED_RESPONSE_NOT_FOUND -> {
                Result.Error(MoonGetterError.RESOURCE_TAKEN_DOWN)
            }
            Error.TIMEOUT_ERROR -> {
                Result.Error(MoonGetterError.REQUEST_TIMEOUT)
            }
            Error.UNSUCCESSFUL_RESPONSE -> {
                return when (e.code ?: return Result.Error(MoonGetterError.UNKNOWN)) {
                    401 -> Result.Error(MoonGetterError.UNAUTHORIZED)
                    408 -> Result.Error(MoonGetterError.REQUEST_TIMEOUT)
                    409 -> Result.Error(MoonGetterError.CONFLICT)
                    413 -> Result.Error(MoonGetterError.PAYLOAD_TOO_LARGE)
                    429 -> Result.Error(MoonGetterError.TOO_MANY_REQUESTS)
                    in 500..599 -> Result.Error(MoonGetterError.SERVER_ERROR)
                    else -> Result.Error(MoonGetterError.UNKNOWN)
                }
            }
            Error.RESOURCE_TAKEN_DOWN -> {
                Result.Error(MoonGetterError.RESOURCE_TAKEN_DOWN)
            }
        }
    }
    catch (_ : IllegalArgumentException) {
        Result.Error(MoonGetterError.UNKNOWN)
    }
}
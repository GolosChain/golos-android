package io.golos.golos.notifications

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.convertValue
import io.golos.golos.utils.JsonRpcError
import io.golos.golos.utils.mapper
import io.golos.golos.utils.rpcErrorFromCode

private data class AuthSuccess(@JsonProperty("status") val status: String)

fun GolosServicesResponse.isAuthSuccessMessage(): Boolean {
    try {
        val succes = mapper.convertValue<AuthSuccess>(result)
        return succes.status.equals("ok", true)
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

fun GolosServicesResponse.isPushSubscribeSuccesMessage(): Boolean {
    try {
        val succes = mapper.convertValue<String>(result)
        return succes.equals("ok", true)
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

fun GolosServicesErrorMessage.getErrorType(): JsonRpcError? {
    return rpcErrorFromCode(this.error.code)
}

fun GolosServicesException.getErrorType(): JsonRpcError? = rpcErrorFromCode(this.golosServicesError.code)
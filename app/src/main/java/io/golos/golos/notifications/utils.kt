package io.golos.golos.notifications

import io.golos.golos.utils.JsonRpcError
import io.golos.golos.utils.rpcErrorFromCode

fun GolosServicesResponse.isAuthSuccessMessage() = result.firstOrNull()?.equals("Passed") == true
fun GolosServicesResponse.isPushSubscribeSuccesMessage() = result.firstOrNull()?.equals("ok", true) == true

fun GolosServicesErrorMessage.getErrorType(): JsonRpcError? {
    return rpcErrorFromCode(this.error.code)
}

fun GolosServicesException.getErrorType(): JsonRpcError? = rpcErrorFromCode(this.golosServicesError.code)
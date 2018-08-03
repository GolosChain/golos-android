package io.golos.golos.utils

enum class JsonRpcError {
    PARSE_ERROR, INVALID_REQUEST, METHOD_NOT_FOUND, INVALID_PARAMS, INTERNAL_ERROR, SERVER_ERROR,
    AUTH_ERROR, NOT_AUTHORIZED;


}

fun rpcErrorFromCode(code: Int): JsonRpcError? {
    return when (code) {
        -32700 -> JsonRpcError.PARSE_ERROR
        -32600 -> JsonRpcError.INVALID_REQUEST
        -32601 -> JsonRpcError.METHOD_NOT_FOUND
        -32602 -> JsonRpcError.INVALID_PARAMS
        -32603 -> JsonRpcError.INTERNAL_ERROR
        403 -> JsonRpcError.AUTH_ERROR
        406 -> JsonRpcError.NOT_AUTHORIZED
        else -> {
            if (code in -32000 downTo -32099) JsonRpcError.SERVER_ERROR
            else null
        }
    }
}
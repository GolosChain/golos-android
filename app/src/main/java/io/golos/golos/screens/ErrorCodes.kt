package io.golos.golos.screens

/**
 * Created by yuri on 02.11.17.
 */
enum class ErrorCodes(val code: Int) {
    ERROR_NO_CONNECTION(1),
    ERROR_SLOW_CONNECTION(2),
    ERROR_INTERNAL_SERVER(3),
    ERROR_AUTH(4),
    ERROR_WRONG_ARGUMENTS(5),
    ERROR_ALREADY_ACTIVATED(6),
    NO_ERROR(7),
    UNKNOWN(8)
}
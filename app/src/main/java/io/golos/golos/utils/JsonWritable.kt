package io.golos.golos.utils

interface JsonConvertable {
    fun jsonRepresentation() = mapper.writeValueAsString(this)
}
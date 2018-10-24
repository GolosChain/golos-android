package io.golos.golos.utils

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

/**
 * Created by yuri on 14.11.17.
 */
data class GolosError(@JsonProperty("error") val errorCode: ErrorCode,
                      @JsonProperty("nativeMessage") val nativeMessage: String?,
                      @JsonProperty("localizedMessage") val localizedMessage: Int?,
                      @JsonProperty("id") val id: String = UUID.randomUUID().toString())
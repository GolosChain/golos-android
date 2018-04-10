package io.golos.golos.repository.model

import io.golos.golos.repository.persistence.model.GolosUserAccountInfo
import io.golos.golos.utils.GolosError

/**
 * Created by yuri on 09.11.17.
 */
data class UserAuthResponse(val isKeyValid: Boolean,
                            val postingAuth: Pair<String, String?>? = null,
                            val activeAuth: Pair<String, String?>? = null,
                            val error: GolosError? = null,
                            val accountInfo: GolosUserAccountInfo)


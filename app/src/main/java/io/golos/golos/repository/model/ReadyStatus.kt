package io.golos.golos.repository.model

import io.golos.golos.utils.GolosError

/**
 * Created by yuri on 26.01.18.
 */
data class ReadyStatus(val isAppReady: Boolean,
                       val error:GolosError?)

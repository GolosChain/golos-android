package io.golos.golos.repository.model

import io.golos.golos.utils.GolosError
import io.golos.golos.utils.UpdatingState

/**
 * Created by yuri yurivladdurain@gmail.com on 17/10/2018.
 */
data class RepostingState(val postId: Long,
                          val postPermlink: String,
                          val updatingState: UpdatingState,
                          val error: GolosError?)
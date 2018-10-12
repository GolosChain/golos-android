package io.golos.golos.repository.model

import io.golos.golos.utils.GolosError
import io.golos.golos.utils.UpdatingState

/**
 * Created by yuri yurivladdurain@gmail.com on 10/10/2018.
 */
data class GolosDiscussionItemVotingState(val storyId: Long,
                                          val votingStrength: Short,
                                          val state: UpdatingState,
                                          val error:GolosError? = null)
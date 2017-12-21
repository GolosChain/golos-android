package io.golos.golos.utils

/**
 * Created by yuri on 21.12.17.
 */
data class SubscriptionState(val canUserMakeActions: Boolean ,
                             val isUserSubscribed: Boolean = false,
                             val subscriptionProgressState: UpdatingState = UpdatingState.DONE)
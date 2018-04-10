package io.golos.golos.notifications

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import com.google.firebase.messaging.FirebaseMessaging
import timber.log.Timber

/**
 * Created by yuri on 05.03.18.
 * frGUvdd3Hpg:APA91bF6gz5U_cWY6V0uWSIe_GLI5pHFnfMy8gPEbDm3eoGekoDtMuxjbBJl_Ps55CllWF1nE5QZm70nxftxURzwowZ2q_0_oTRJDw9cgcZBpaZgp4Fuos4KdAxHNG-GlOPS_kUOCmRF
 */
class MyInstanceIdService : FirebaseInstanceIdService() {
    override fun onTokenRefresh() {
        super.onTokenRefresh()
        val token = FirebaseInstanceId.getInstance().token
        Timber.e("token is $token")
        FirebaseMessaging.getInstance().subscribeToTopic("all")
        FirebaseMessaging.getInstance().subscribeToTopic("all_android")
    }
}
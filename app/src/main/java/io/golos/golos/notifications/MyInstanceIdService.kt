package io.golos.golos.notifications

import android.preference.PreferenceManager
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import com.google.firebase.messaging.FirebaseMessaging
import timber.log.Timber

/**
 * Created by yuri on 05.03.18.

 */
class MyInstanceIdService : FirebaseInstanceIdService() {

    override fun onTokenRefresh() {
        super.onTokenRefresh()
        val token = FirebaseInstanceId.getInstance().token
        FirebaseMessaging.getInstance().subscribeToTopic("all")
        FirebaseMessaging.getInstance().subscribeToTopic("all_android")
        PreferenceManager.getDefaultSharedPreferences(baseContext).edit().putString("token", token).commit()
    }
}
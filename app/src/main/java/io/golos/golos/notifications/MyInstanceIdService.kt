package io.golos.golos.notifications

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import com.google.firebase.messaging.FirebaseMessaging
import io.golos.golos.App


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
        FCMTokenProviderImpl.setFcmToken(token ?: return)
    }
}

interface FCMTokenProvider {
    val onTokenChange: LiveData<String>
}

object FCMTokenProviderImpl : FCMTokenProvider {
    private val mLiveData = MutableLiveData<String>()

    init {
        PreferenceManager
                .getDefaultSharedPreferences(App.context)
                .getString("token", null)
                ?.let { mLiveData.value = it }
    }

    internal fun setFcmToken(token: String) {
        Handler(Looper.getMainLooper()).post {
            mLiveData.value = token
        }
    }


    override val onTokenChange = mLiveData
}
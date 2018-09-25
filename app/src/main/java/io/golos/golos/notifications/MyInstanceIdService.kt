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
import timber.log.Timber


/**
 * Created by yuri on 05.03.18.

 */
class MyInstanceIdService : FirebaseInstanceIdService() {

    override fun onTokenRefresh() {
        super.onTokenRefresh()
        val token = FirebaseInstanceId.getInstance().token
        Timber.i("onTokenRefresh $token")
        FirebaseMessaging.getInstance().subscribeToTopic("all")
        FirebaseMessaging.getInstance().subscribeToTopic("all_android")

        val oldToken = getOldToken()

        PreferenceManager.getDefaultSharedPreferences(baseContext).edit().putString("old_token", oldToken).commit()
        PreferenceManager.getDefaultSharedPreferences(baseContext).edit().putString("token", token).commit()

        FCMTokenProviderImpl.setFcmToken(FCMTokens(oldToken, token ?: return))
    }

    private fun getOldToken(): String? =
            PreferenceManager.getDefaultSharedPreferences(baseContext).getString("token", null)
}

interface FCMTokenProvider {
    val tokenLiveData: LiveData<FCMTokens>
}

data class FCMTokens(val oldToken: String?, val newToken: String)

object FCMTokenProviderImpl : FCMTokenProvider {
    private val mLiveData = MutableLiveData<FCMTokens>()

    init {
        val token = PreferenceManager.getDefaultSharedPreferences(App.context).getString("token", null)
        token?.let {
            setFcmToken(FCMTokens(null, token))
        }
    }

    internal fun setFcmToken(token: FCMTokens) {
        Timber.i("setFcmToken $token")
        Handler(Looper.getMainLooper()).post {
            mLiveData.value = token
        }
    }


    override val tokenLiveData = mLiveData
}
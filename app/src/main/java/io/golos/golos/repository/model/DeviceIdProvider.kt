package io.golos.golos.repository.model

import android.provider.Settings
import io.golos.golos.App
import java.util.*

/**
 * Created by yuri yurivladdurain@gmail.com on 16/11/2018.
 */
interface DeviceIdProvider {
    fun getDeviceId(): String
}

object DeviceIdProviderImpl : DeviceIdProvider {


    override fun getDeviceId(): String {
        return Settings.Secure.getString(App.context.getContentResolver(),
                Settings.Secure.ANDROID_ID) ?: UUID.randomUUID().toString()
    }
}
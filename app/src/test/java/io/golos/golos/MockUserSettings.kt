package io.golos.golos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.content.Context
import io.golos.golos.repository.UserSettingsRepository
import io.golos.golos.repository.model.GolosAppSettings
import io.golos.golos.repository.model.NotificationsDisplaySetting

/**
 * Created by yuri on 05.03.18.
 */
object MockUserSettings : UserSettingsRepository {
    override fun setStoriesCompactMode(isCompact: Boolean) {

    }

    override fun isStoriesCompactMode(): LiveData<Boolean> {
        return MutableLiveData<Boolean>()
    }

    override fun isNSFWShow(): LiveData<Boolean> {
        return MutableLiveData<Boolean>()
    }

    override fun setBountDisplay(display: GolosAppSettings.GolosBountyDisplay) {

    }

    override fun getBountDisplay(): LiveData<GolosAppSettings.GolosBountyDisplay> {
        val v = MutableLiveData<GolosAppSettings.GolosBountyDisplay>()
        v.value = GolosAppSettings.GolosBountyDisplay.THREE_PLACES
        return v
    }

    override fun setIsNSFWShown(isShown: Boolean) {

    }

    override fun getNotificationsSettings(): LiveData<NotificationsDisplaySetting> {
       return MutableLiveData<NotificationsDisplaySetting>()
    }

    override fun setNotificationSettings(newSettings: NotificationsDisplaySetting) {

    }

    override fun setUserVotedForApp(isVotedForApp: Boolean) {

    }

    override fun isUserVotedForApp(): Boolean {
        return true
    }

    override fun setVoteQuestionMade(isMade: Boolean) {

    }

    override fun isVoteQuestionMade(): Boolean {
        return true
    }

    override fun setShowImages(isShown: Boolean) {

    }

    override fun isImagesShown(): LiveData<Boolean> {
        return MutableLiveData<Boolean>()
    }

    override fun setNightMode(isNight: Boolean) {

    }

    override fun isNightMode(): Boolean {
        return false
    }

    override fun setCurrency(currency: GolosAppSettings.GolosCurrency) {

    }

    override fun getCurrency(): LiveData<GolosAppSettings.GolosCurrency> {
        val out = MutableLiveData<GolosAppSettings.GolosCurrency>()
        out.value = GolosAppSettings.GolosCurrency.USD
        return out
    }

    override fun setUp(ctx: Context) {

    }
}
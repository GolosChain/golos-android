package io.golos.golos

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import io.golos.golos.repository.UserSettingsRepository
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

    override fun setBountDisplay(display: UserSettingsRepository.GolosBountyDisplay) {

    }

    override fun getBountDisplay(): LiveData<UserSettingsRepository.GolosBountyDisplay> {
        val v = MutableLiveData<UserSettingsRepository.GolosBountyDisplay>()
        v.value = UserSettingsRepository.GolosBountyDisplay.THREE_PLACES
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

    override fun setCurrency(currency: UserSettingsRepository.GolosCurrency) {

    }

    override fun getCurrency(): LiveData<UserSettingsRepository.GolosCurrency> {
        val out = MutableLiveData<UserSettingsRepository.GolosCurrency>()
        out.value = UserSettingsRepository.GolosCurrency.USD
        return out
    }

    override fun setUp(ctx: Context) {

    }
}
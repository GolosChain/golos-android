package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import io.golos.golos.App
import io.golos.golos.repository.model.NotificationsDisplaySetting
import io.golos.golos.utils.mapper


internal class UserSettingsImpl : UserSettingsRepository {
    private val sharedPrefName = "UserSettings"
    private val mCompactLiveData = MutableLiveData<Boolean>()
    private val mShowImageLiveData = MutableLiveData<Boolean>()
    private val mShowNSFWLiveData = MutableLiveData<Boolean>()
    private val mCurrencyLiveData = MutableLiveData<UserSettingsRepository.GolosCurrency>()
    private val mBountyDisplay = MutableLiveData<UserSettingsRepository.GolosBountyDisplay>()
    private val mNotificationsSettings = MutableLiveData<NotificationsDisplaySetting>()

    override fun setUp(ctx: Context) {
        mCompactLiveData.value = ctx.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getBoolean("isCompact", false)
        mShowImageLiveData.value = ctx.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getBoolean("isShown", true)
        mShowNSFWLiveData.value = ctx.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getBoolean("showNsfw", false)

        val notificainsSerialized: String? = ctx.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getString("notifications_settings", null)
        mNotificationsSettings.value = if (notificainsSerialized != null) mapper.readValue(notificainsSerialized, NotificationsDisplaySetting::class.java)
        else NotificationsDisplaySetting(false,
                false,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true)

        val currencyString = ctx.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getString("goloscurrency", null)

        if (currencyString != null) {
            when (currencyString) {
                "DOLL" -> mCurrencyLiveData.value = UserSettingsRepository.GolosCurrency.USD
                else -> mCurrencyLiveData.value = UserSettingsRepository.GolosCurrency.valueOf(currencyString)
            }

        } else {
            mCurrencyLiveData.value = UserSettingsRepository.GolosCurrency.USD
        }

        val bountyDisplayString = ctx.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getString("bountyDisplayString", null)

        if (bountyDisplayString != null) {

            mBountyDisplay.value = UserSettingsRepository.GolosBountyDisplay.valueOf(bountyDisplayString)
        } else {
            mBountyDisplay.value = UserSettingsRepository.GolosBountyDisplay.THREE_PLACES
        }

    }

    override fun setCurrency(currency: UserSettingsRepository.GolosCurrency) {
        if (currency != mCurrencyLiveData.value) {
            mCurrencyLiveData.value = currency
        }
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putString("goloscurrency", currency.name).apply()
    }

    override fun getCurrency(): LiveData<UserSettingsRepository.GolosCurrency> {
        return mCurrencyLiveData
    }

    override fun setBountDisplay(display: UserSettingsRepository.GolosBountyDisplay) {
        if (display != mBountyDisplay.value) {
            mBountyDisplay.value = display
        }
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putString("bountyDisplayString", display.name).apply()
    }

    override fun getBountDisplay(): LiveData<UserSettingsRepository.GolosBountyDisplay> {
        return mBountyDisplay
    }

    override fun setStoriesCompactMode(isCompact: Boolean) {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putBoolean("isCompact", isCompact).apply()
        mCompactLiveData.value = isCompact
    }

    override fun isStoriesCompactMode(): LiveData<Boolean> {
        return mCompactLiveData
    }

    override fun isNSFWShow(): LiveData<Boolean> {
        return mShowNSFWLiveData
    }

    override fun setIsNSFWShown(isShown: Boolean) {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putBoolean("showNsfw", isShown).apply()
        mShowNSFWLiveData.value = isShown
    }

    override fun setUserVotedForApp(isVotedForApp: Boolean) {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putBoolean("isVotedForApp", isVotedForApp).apply()
    }

    override fun isUserVotedForApp(): Boolean {
        return App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getBoolean("isVotedForApp", false)
    }

    override fun setVoteQuestionMade(isMade: Boolean) {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putBoolean("setVoteQuestionMade", isMade).apply()
    }

    override fun isVoteQuestionMade(): Boolean {
        return App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getBoolean("setVoteQuestionMade", false)
    }

    override fun setShowImages(isShown: Boolean) {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putBoolean("isShown", isShown).apply()
        mShowImageLiveData.value = isShown
    }

    override fun isImagesShown(): LiveData<Boolean> {
        return mShowImageLiveData
    }


    override fun setNightMode(isNight: Boolean) {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putBoolean("isNight", isNight).apply()
    }

    override fun isNightMode(): Boolean {
        return App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getBoolean("isNight", false)
    }

    override fun getNotificationsSettings(): LiveData<NotificationsDisplaySetting> {
        return mNotificationsSettings
    }

    override fun setNotificationSettings(newSettings: NotificationsDisplaySetting) {
        if (newSettings != mNotificationsSettings.value) {
            mNotificationsSettings.value = newSettings
            App.context.getSharedPreferences(sharedPrefName,
                    Context.MODE_PRIVATE).edit().putString("notifications_settings", mapper.writeValueAsString(newSettings)).apply()
        }
    }
}
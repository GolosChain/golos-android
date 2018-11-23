package io.golos.golos.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fasterxml.jackson.module.kotlin.readValue
import io.golos.golos.repository.model.*
import io.golos.golos.repository.services.GolosSettingsService
import io.golos.golos.utils.mapper


internal class SettingsRepositoryImpl(private val mSettingGolosServices: GolosSettingsService,
                                      private val mAppDataProvider: UserDataProvider,
                                      private val mDeviceIdProvider: DeviceIdProvider = DeviceIdProviderImpl) : UserSettingRepository {

    private val defaultAppSettings = GolosAppSettings(true, GolosAppSettings.FeedMode.FULL, GolosAppSettings.NSFWMode.DISPLAY_NOT,
            GolosAppSettings.DisplayImagesMode.DISPLAY, false, GolosAppSettings.GolosCurrency.USD, GolosAppSettings.GolosBountyDisplay.THREE_PLACES)
    private var defaultNotificationSettings = GolosNotificationSettings(true,
            true, true, true, true,
            true, true, true, true,
            true, true, true, true)


    private val sharedPrefName = "u_settings"
    private val mAppSettingsLiveData = MutableLiveData<GolosAppSettings>()
    private val mNotificationsSettings = MutableLiveData<GolosNotificationSettings>()
    private var mContext: Context? = null
    private var isAppSettingInited = false

    override val appSettings: LiveData<GolosAppSettings>
        get() = mAppSettingsLiveData
    override val notificationSettings: LiveData<GolosNotificationSettings>
        get() = mNotificationsSettings

    fun AppSettings.toGolosSettings() = GolosAppSettings(this.loudNotification
            ?: defaultAppSettings.loudNotification,
            this.feedMode ?: defaultAppSettings.feedMode,
            this.nsfwMode ?: defaultAppSettings.nsfwMode,
            this.displayImagesMode ?: defaultAppSettings.displayImagesMode,
            this.nighModeEnable ?: defaultAppSettings.nighModeEnable,
            this.chosenCurrency ?: defaultAppSettings.chosenCurrency,
            this.bountyDisplay ?: defaultAppSettings.bountyDisplay)

    fun GolosAppSettings.toAppSettings() = AppSettings(this.loudNotification, this.feedMode, this.nsfwMode,
            this.displayImagesMode, this.nighModeEnable, this.chosenCurrency, this.bountyDisplay)

    fun NotificationSettings.toGolosNotificationSettings() = GolosNotificationSettings(this.vote
            ?: defaultNotificationSettings.showUpvoteNotifs,
            this.flag ?: defaultNotificationSettings.showFlagNotifs,
            this.reply ?: defaultNotificationSettings.showNewCommentNotifs,
            this.transfer ?: defaultNotificationSettings.showTransferNotifs,
            this.subscribe ?: defaultNotificationSettings.showSubscribeNotifs,
            this.unsubscribe ?: defaultNotificationSettings.showUnSubscribeNotifs,
            this.mention ?: defaultNotificationSettings.showMentions,
            this.repost ?: defaultNotificationSettings.showReblog,
            this.message ?: defaultNotificationSettings.showMessageNotifs,
            this.witnessVote ?: defaultNotificationSettings.showWitnessVote,
            this.witnessCancelVote ?: defaultNotificationSettings.showWitnessCancelVote,
            this.award ?: defaultNotificationSettings.showAward,
            this.curatorAward ?: defaultNotificationSettings.showCurationAward)

    fun GolosNotificationSettings.toNotificationSettings() = NotificationSettings(this.showUpvoteNotifs,
            this.showFlagNotifs, this.showNewCommentNotifs, this.showTransferNotifs, this.showSubscribeNotifs, this.showUnSubscribeNotifs,
            this.showMentions, this.showReblog, this.showMessageNotifs, this.showWitnessVote, this.showWitnessCancelVote, this.showAward, this.showCurationAward)

    fun setUp(ctx: Context) {
        val appSettingsString = ctx.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getString("app_settings_1", null)
        val notificationSettingsString = ctx.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getString("notifs_settings_1", null)

        mAppSettingsLiveData.value = if (appSettingsString != null) mapper.readValue<AppSettings>(appSettingsString).toGolosSettings() else defaultAppSettings
        mNotificationsSettings.value = if (notificationSettingsString != null) mapper.readValue<NotificationSettings>(notificationSettingsString).toGolosNotificationSettings() else defaultNotificationSettings
        mContext = ctx
        mAppDataProvider.appUserData.observeForever { appUser ->
            if (appUser == null || !appUser.isLogged) {
                mAppSettingsLiveData.value = defaultAppSettings
                isAppSettingInited = false
            } else {
                if (mSettingGolosServices.loadingState.value == PreparingState.DONE
                        && appUser.isLogged && !isAppSettingInited) {
                    onUserLoggedIn()
                    isAppSettingInited = true
                }
            }
        }
        mSettingGolosServices.loadingState.observeForever {
            if (it == PreparingState.DONE && mAppDataProvider.appUserData.value?.isLogged == true && !isAppSettingInited) {
                onUserLoggedIn()
                isAppSettingInited = true
            }
        }
        mSettingGolosServices.notificationSettings.observeForever {
            setNotificationSettings(it?.toGolosNotificationSettings()
                    ?: defaultNotificationSettings)
        }
        mSettingGolosServices.appSettings.observeForever {
            setAppSettings(it?.toGolosSettings() ?: defaultAppSettings)
        }
    }

    private fun onUserLoggedIn() {
        mSettingGolosServices.requestNotificationSettingsUpdate(mDeviceIdProvider.getDeviceId())
        mSettingGolosServices.requestAppSettingsUpdate(mDeviceIdProvider.getDeviceId())
    }

    override fun setAppSettings(newSettings: GolosAppSettings) {
        if (mSettingGolosServices.loadingState.value != PreparingState.DONE) return
        if (newSettings == mAppSettingsLiveData.value) return
        mAppSettingsLiveData.value = newSettings
        mSettingGolosServices.setAppSettings(mDeviceIdProvider.getDeviceId(), newSettings.toAppSettings())
        mContext?.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)?.edit()?.putString("app_settings_1", mapper.writeValueAsString(newSettings.toAppSettings()))?.apply()
    }

    override fun setNotificationSettings(newSettings: GolosNotificationSettings) {
        if (mSettingGolosServices.loadingState.value != PreparingState.DONE) return
        if (newSettings == mNotificationsSettings.value) return
        mNotificationsSettings.value = newSettings
        mSettingGolosServices.setNotificationSettings(mDeviceIdProvider.getDeviceId(), newSettings.toNotificationSettings())
        mContext?.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)?.edit()?.putString("notifs_settings_1", mapper.writeValueAsString(newSettings.toNotificationSettings()))?.apply()
    }
}
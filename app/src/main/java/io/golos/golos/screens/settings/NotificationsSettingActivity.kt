package io.golos.golos.screens.settings

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.NotificationsDisplaySetting
import io.golos.golos.screens.GolosActivity


class NotificationsSettingActivity : GolosActivity(), Observer<NotificationsDisplaySetting> {

    override fun onChanged(t: NotificationsDisplaySetting?) {
        if (t != null && mRecycler.adapter == null) {
            mRecycler.adapter = SettingsAdapter.new()
                    .setTitle(titleId = R.string.sound)
                    .setSwitch(NotificationSettings.PLAY_SOUND, R.string.play_sound, t.playSoundWhenAppStopped)
                    .setSpace(R.dimen.margin_material_small)
                    .setDelimeter()
                    .setTitle(titleId = R.string.mention)
                    .setSwitch(NotificationSettings.SHOW_UPVOTE, R.string.upvote, t.showUpvoteNotifs)
                    .setSwitch(NotificationSettings.SHOW_TRANSFER, R.string.transfer, t.showTransferNotifs)
                    .setSwitch(NotificationSettings.SHOW_NEW_COMMENT, R.string.new_comment, t.showNewCommentNotifs)
                    .build({ id, oldValue, newValue ->
                        val settings = Repository.get.userSettingsRepository.getNotificationsSettings().value
                                ?: t
                        val notifSettingsRepo = Repository.get.userSettingsRepository
                        (id as? NotificationSettings).let {
                            when (it) {
                                NotificationSettings.PLAY_SOUND -> notifSettingsRepo.setNotificationSettings(settings.setPLaySound(newValue))
                                NotificationSettings.SHOW_UPVOTE -> notifSettingsRepo.setNotificationSettings(settings.setShowUpvoteNotifs(newValue))
                                NotificationSettings.SHOW_NEW_COMMENT -> notifSettingsRepo.setNotificationSettings(settings.setShowNewCommentNotifs(newValue))
                                NotificationSettings.SHOW_TRANSFER -> notifSettingsRepo.setNotificationSettings(settings.setShowTransferNotifss(newValue))
                            }
                        }
                    })
        }
    }

    enum class NotificationSettings {
        SHOW_UPVOTE, SHOW_TRANSFER, SHOW_NEW_COMMENT, PLAY_SOUND
    }


    private lateinit var mRecycler: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_notification_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }
        Repository.get.userSettingsRepository.getNotificationsSettings().observe(this, this)

        mRecycler = findViewById(R.id.recycler)

    }
}
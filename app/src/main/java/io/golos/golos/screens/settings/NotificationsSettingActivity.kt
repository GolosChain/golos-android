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
                    .setSwitch(NotificationSettings.SHOW_FLAG, R.string.flag, t.showFlagNotifs)
                    .setSwitch(NotificationSettings.SHOW_NEW_COMMENT, R.string.new_comment, t.showNewCommentNotifs)
                    .setSwitch(NotificationSettings.SUBSCRIBE_ON_BLOG, R.string.subscribe_on_blog, t.showSubscribeNotifs)
                    .setSwitch(NotificationSettings.UNSUBSCRIBE_FROM_BLOG, R.string.unsubscribe_from_blog, t.showUnSubscribeNotifs)
                    .setSwitch(NotificationSettings.MENTIONS, R.string.mention, t.showMentions)
                    .setSwitch(NotificationSettings.REBLOG, R.string.reblog, t.showReblog)
                    .setSwitch(NotificationSettings.WITNESS_VOTE, R.string.witness_vote, t.showWitnessVote)
                    .setSwitch(NotificationSettings.WITNESS_CANCEL, R.string.witness_cancel_vote, t.showWitnessCancelVote)
                    .setSwitch(NotificationSettings.POST_REWARD, R.string.post_reward, t.showAward)
                    .setSwitch(NotificationSettings.CURATION_REWARD, R.string.curation_reward, t.showCurationAward)

                    .build { id, oldValue, newValue ->
                        val settings = Repository.get.userSettingsRepository.getNotificationsSettings().value
                                ?: t
                        val notifSettingsRepo = Repository.get.userSettingsRepository
                        (id as? NotificationSettings).let {
                            when (it) {
                                NotificationSettings.PLAY_SOUND -> notifSettingsRepo.setNotificationSettings(settings.setPLaySound(newValue))
                                NotificationSettings.SHOW_UPVOTE -> notifSettingsRepo.setNotificationSettings(settings.setShowUpvoteNotifs(newValue))
                                NotificationSettings.SHOW_NEW_COMMENT -> notifSettingsRepo.setNotificationSettings(settings.setShowNewCommentNotifs(newValue))
                                NotificationSettings.SHOW_TRANSFER -> notifSettingsRepo.setNotificationSettings(settings.setShowTransferNotifs(newValue))
                                NotificationSettings.SHOW_FLAG -> notifSettingsRepo.setNotificationSettings(settings.setShowFlags(newValue))
                                NotificationSettings.SUBSCRIBE_ON_BLOG -> notifSettingsRepo.setNotificationSettings(settings.setShowSubscribeNotifs(newValue))
                                NotificationSettings.UNSUBSCRIBE_FROM_BLOG -> notifSettingsRepo.setNotificationSettings(settings.setShowUnSubscribeNotifs(newValue))
                                NotificationSettings.MENTIONS -> notifSettingsRepo.setNotificationSettings(settings.setShowMentionsNotifs(newValue))
                                NotificationSettings.REBLOG -> notifSettingsRepo.setNotificationSettings(settings.setShowReblogNotifs(newValue))
                                NotificationSettings.WITNESS_VOTE -> notifSettingsRepo.setNotificationSettings(settings.setShowWitnessVote(newValue))
                                NotificationSettings.WITNESS_CANCEL -> notifSettingsRepo.setNotificationSettings(settings.setShowWitnessCancelVote(newValue))
                                NotificationSettings.POST_REWARD -> notifSettingsRepo.setNotificationSettings(settings.setShowAward(newValue))
                                NotificationSettings.CURATION_REWARD -> notifSettingsRepo.setNotificationSettings(settings.setShowCurationAward(newValue))
                            }
                        }
                    }
        }
    }

    enum class NotificationSettings {
        SHOW_UPVOTE, SHOW_FLAG, SHOW_TRANSFER, SHOW_NEW_COMMENT, PLAY_SOUND, SUBSCRIBE_ON_BLOG,
        UNSUBSCRIBE_FROM_BLOG, MENTIONS, REBLOG, WITNESS_VOTE, WITNESS_CANCEL, POST_REWARD, CURATION_REWARD
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
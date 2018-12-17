package io.golos.golos.screens.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.GolosNotificationSettings
import io.golos.golos.screens.GolosActivity
import java.util.*


class NotificationsSettingActivity : GolosActivity() {


    private val allOn = GolosNotificationSettings(true, true, true, true, true, true
            , true, true, true, true, true, true, true)

    private val allOff = GolosNotificationSettings(false, false, false, false, false, false, false, false, false, false, false, false, false)


    fun onChanged() {
        if (mRecycler.adapter == null) {
            mRecycler.adapter = SettingsAdapter(createRowsForAdapter()) { id, oldValue, newValue ->
                val notificationSettings = Repository.get.notificationsSettings.value
                        ?: return@SettingsAdapter
                val appSettings = Repository.get.appSettings.value ?: return@SettingsAdapter
                val repo = Repository.get
                (id as? NotificationSettingsTypes).let {
                    when (it) {
                        NotificationSettingsTypes.PLAY_SOUND -> repo.setAppSettings(appSettings.copy(loudNotification = newValue))
                        NotificationSettingsTypes.SHOW_UPVOTE -> repo.setNotificationSettings(notificationSettings.copy(showUpvoteNotifs = newValue))
                        NotificationSettingsTypes.SHOW_NEW_COMMENT -> repo.setNotificationSettings(notificationSettings.copy(showNewCommentNotifs = newValue))
                        NotificationSettingsTypes.SHOW_TRANSFER -> repo.setNotificationSettings(notificationSettings.copy(showTransferNotifs = newValue))
                        NotificationSettingsTypes.SHOW_FLAG -> repo.setNotificationSettings(notificationSettings.copy(showFlagNotifs = newValue))
                        NotificationSettingsTypes.SUBSCRIBE_ON_BLOG -> repo.setNotificationSettings(notificationSettings.copy(showSubscribeNotifs = newValue))
                        NotificationSettingsTypes.UNSUBSCRIBE_FROM_BLOG -> repo.setNotificationSettings(notificationSettings.copy(showUnSubscribeNotifs = newValue))
                        NotificationSettingsTypes.MENTIONS -> repo.setNotificationSettings(notificationSettings.copy(showMentions = newValue))
                        NotificationSettingsTypes.REBLOG -> repo.setNotificationSettings(notificationSettings.copy(showReblog = newValue))
                        NotificationSettingsTypes.WITNESS_VOTE -> repo.setNotificationSettings(notificationSettings.copy(showWitnessVote = newValue))
                        NotificationSettingsTypes.WITNESS_CANCEL -> repo.setNotificationSettings(notificationSettings.copy(showWitnessCancelVote = newValue))
                        NotificationSettingsTypes.POST_REWARD -> repo.setNotificationSettings(notificationSettings.copy(showAward = newValue))
                        NotificationSettingsTypes.CURATION_REWARD -> repo.setNotificationSettings(notificationSettings.copy(showCurationAward = newValue))
                        NotificationSettingsTypes.CHECK_ALL -> {
                            repo.setNotificationSettings(if (newValue) allOn else allOff)
                        }
                    }
                }
            }
        } else {
            (mRecycler.adapter as? SettingsAdapter)?.rows = createRowsForAdapter()
        }
    }

    private enum class NotificationSettingsTypes {
        SHOW_UPVOTE, SHOW_FLAG, SHOW_TRANSFER, SHOW_NEW_COMMENT, PLAY_SOUND, SUBSCRIBE_ON_BLOG,
        UNSUBSCRIBE_FROM_BLOG, MENTIONS, REBLOG, WITNESS_VOTE, WITNESS_CANCEL, POST_REWARD, CURATION_REWARD, CHECK_ALL
    }


    private lateinit var mRecycler: androidx.recyclerview.widget.RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_notification_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }
        Repository.get.notificationsSettings.observe(this, Observer { onChanged() })
        Repository.get.appSettings.observe(this, Observer { onChanged() })
        mRecycler = findViewById(R.id.recycler)
        mRecycler.itemAnimator = null
    }

    private fun createRowsForAdapter(): List<SettingRow> {
        val notificationSettings = Repository.get.notificationsSettings.value

        val appSettings = Repository.get.appSettings.value
        val rows = ArrayList<SettingRow>(20)
        rows.apply {
            add(SwitchRow(NotificationSettingsTypes.PLAY_SOUND, R.string.play_sound, appSettings?.loudNotification
                    ?: false))
            add(SwitchRow(NotificationSettingsTypes.CHECK_ALL,
                    R.string.turn_on_pushes,
                    notificationSettings?.isAllOn() == true))
            add(SpaceRow(R.dimen.margin_material_small, "space_1"))
            add(DelimeterRow(id = "del_row_1"))
            add(TitleRow(titleId = R.string.mention, id = "mentions"))
            add(CheckRow(NotificationSettingsTypes.SHOW_UPVOTE, R.string.like, notificationSettings?.showUpvoteNotifs
                    ?: false, R.drawable.ic_like_20dp))
            add(CheckRow(NotificationSettingsTypes.SHOW_FLAG, R.string.dizlike, notificationSettings?.showFlagNotifs
                    ?: false, R.drawable.ic_dizlike_20dp))
            add(CheckRow(NotificationSettingsTypes.SHOW_TRANSFER, R.string.transfer, notificationSettings?.showTransferNotifs
                    ?: false, R.drawable.ic_thank_20dp))
            add(CheckRow(NotificationSettingsTypes.SHOW_NEW_COMMENT, R.string.new_comment, notificationSettings?.showNewCommentNotifs
                    ?: false, R.drawable.ic_reply_gr_20dp))
            add(CheckRow(NotificationSettingsTypes.SUBSCRIBE_ON_BLOG, R.string.subscribe_on_blog, notificationSettings?.showSubscribeNotifs
                    ?: false, R.drawable.ic_subscribe_settgins_18dp))
            add(CheckRow(NotificationSettingsTypes.UNSUBSCRIBE_FROM_BLOG, R.string.unsubscribe_from_blog, notificationSettings?.showUnSubscribeNotifs
                    ?: false, R.drawable.ic_not_subscribe_gr_settings_18dp))
            add(CheckRow(NotificationSettingsTypes.MENTIONS, R.string.mention_with_at, notificationSettings?.showMentions
                    ?: false, R.drawable.ic_at_gr_18dp))
            add(CheckRow(NotificationSettingsTypes.REBLOG, R.string.reblog, notificationSettings?.showReblog
                    ?: false, R.drawable.ic_repost_gr_20dp))
            add(CheckRow(NotificationSettingsTypes.WITNESS_VOTE, R.string.witness_vote, notificationSettings?.showWitnessVote
                    ?: false, R.drawable.ic_delegat_vote_gr_20dp))
            add(CheckRow(NotificationSettingsTypes.WITNESS_CANCEL, R.string.witness_cancel_vote, notificationSettings?.showWitnessCancelVote
                    ?: false, R.drawable.ic_no_delegate_vote_cancel_gr_20dp))
            add(CheckRow(NotificationSettingsTypes.POST_REWARD, R.string.post_reward, notificationSettings?.showAward
                    ?: false, R.drawable.ic_user_award_20dp))
            add(CheckRow(NotificationSettingsTypes.CURATION_REWARD, R.string.curation_reward, notificationSettings?.showCurationAward
                    ?: false, R.drawable.ic_curation_award_20dp))
        }
        return rows
    }


    fun GolosNotificationSettings.isAllOn(): Boolean {
        return this == allOn
    }

    companion object {
        private const val TYPE_TAG = "type_tag"
        fun start(context: Context) {
            context.startActivity(Intent().apply { putExtra(TYPE_TAG, type) })
        }
    }
}
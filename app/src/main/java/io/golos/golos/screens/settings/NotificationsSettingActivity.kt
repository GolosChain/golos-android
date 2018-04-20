package io.golos.golos.screens.settings

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import io.golos.golos.R
import io.golos.golos.screens.GolosActivity
import timber.log.Timber

class NotificationsSettingActivity : GolosActivity() {
    enum class NotificationSettings {
        SHOW_UPVOTE, SHOW_TRANSFER, SHOW_ANSWER_ON_POST, SHOW_ANSWER_ON_COMMENT, PLAY_SOUND
    }


    private lateinit var mRecycler: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_notification_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        mRecycler = findViewById(R.id.recycler)
        mRecycler.adapter = SettingsAdapter.new()
                .setTitle(titleId = R.string.sound)
                .setSwitch(NotificationSettings.PLAY_SOUND, R.string.play_sound, false)
                .setSpace(R.dimen.margin_material_small)
                .setDelimeter()
                .setTitle(titleId = R.string.mention)
                .setSwitch(NotificationSettings.SHOW_UPVOTE, R.string.upvote, false)
                .setSwitch(NotificationSettings.SHOW_TRANSFER, R.string.transfer, false)
                .setSwitch(NotificationSettings.SHOW_ANSWER_ON_POST, R.string.answer_on_post, false)
                .setSwitch(NotificationSettings.SHOW_ANSWER_ON_COMMENT, R.string.reply_on_comment, false)
                .build()
        mRecycler.onFlingListener = object : RecyclerView.OnFlingListener() {
            override fun onFling(velocityX: Int, velocityY: Int): Boolean {
                Timber.e(" x = $velocityX y = $velocityY")
                return false
            }
        }

    }
}
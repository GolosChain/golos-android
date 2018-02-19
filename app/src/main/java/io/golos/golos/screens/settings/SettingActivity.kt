package io.golos.golos.screens.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.SwitchCompat
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import io.golos.golos.BuildConfig
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.StoryActivity
import io.golos.golos.utils.asIntentToShowUrl

/**
 * Created by yuri on 12.12.17.
 */
class SettingActivity : GolosActivity() {
    private var starter: Intent? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.a_settings)
        findViewById<View>(R.id.golos_wiki_tv).setOnClickListener({
            startActivity("https://wiki.golos.io/".asIntentToShowUrl())
        })
        findViewById<View>(R.id.about_golos_tv).setOnClickListener({
            StoryActivity.start(this,
                    "golos", "ru--golos",
                    "golos-russkoyazychnaya-socialno-mediinaya-blokchein-platforma",
                    FeedType.UNCLASSIFIED,
                    null)
        })
        findViewById<View>(R.id.privacy_policy_tv).setOnClickListener({
            StoryActivity.start(this,
                    "golos", "ru--konfidenczialxnostx",
                    "politika-konfidencialnosti",
                    FeedType.UNCLASSIFIED,
                    null)
        })
        findViewById<View>(R.id.exit).setOnClickListener({
            Repository.get.deleteUserdata()
            Toast.makeText(this, R.string.user_made_logout, Toast.LENGTH_SHORT).show()
        })
        val versionTv = findViewById<TextView>(R.id.version_tv)
        versionTv.text = getString(R.string.golos_android_v, BuildConfig.VERSION_NAME)

        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener { onBackPressed() }
        setUpNighMode()
        setUpCompactMode()
        setUpNoImagesMode()
        setUpNSFWMode()

    }

    private fun setUpNoImagesMode() {
        val switch = findViewById<SwitchCompat>(R.id.show_images_switch)
        if (UserSettings.isImagesShown().value == true) switch.isChecked = true
        switch.setOnClickListener {
            UserSettings.setShowImages(switch.isChecked)
        }
    }

    private fun setUpNighMode() {
        val spinner = findViewById<Spinner>(R.id.mode_spinner)
        spinner.adapter = DayNightSpinnerAdapter(this)
        spinner.setSelection(if (UserSettings.isNightMode()) 1 else 0)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val currentMode = if (UserSettings.isNightMode()) 1 else 0
                if (currentMode != p2) {
                    UserSettings.setNightMode(p2 == 1)
                    val i = Intent(this@SettingActivity, SettingActivity::class.java)
                    setResult(Activity.RESULT_OK)
                    finish()
                    startActivity(i)
                }
            }
        }
    }

    private fun setUpNSFWMode(){
        val switch = findViewById<SwitchCompat>(R.id.show_nsfw_images_switch)
        if (UserSettings.isNSFWShow().value == true) switch.isChecked = true
        switch.setOnClickListener {
            UserSettings.setIsNSFWShown(switch.isChecked)
        }
    }

    private fun setUpCompactMode() {
        val switch = findViewById<SwitchCompat>(R.id.compact_mode_switch)
        if (UserSettings.isStoriesCompactMode().value == true) switch.isChecked = true
        switch.setOnClickListener {
            UserSettings.setStoriesCompactMode(switch.isChecked)
        }
    }
}
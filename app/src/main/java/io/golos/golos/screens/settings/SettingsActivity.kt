package io.golos.golos.screens.settings

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import io.golos.golos.BuildConfig
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.UserSettingsRepository
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.profile.UserProfileActivity
import io.golos.golos.utils.asIntentToShowUrl

/**
 * Created by yuri on 12.12.17.
 * добавить ссылку -https://golos.io/about#team, текст гиппер ссылки "О Команде",
ссылку на "Частые вопросы" https://golos.io/faq
"Новости Команды" https://golos.io/@golosio
Ссылку на Политику конфиденциальности -вставить сылкой не постом, пост убрать
https://golos.io/ru--konfidenczialxnostx/@golos/politika-konfidencialnosti
 */
class SettingsActivity : GolosActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.a_settings)
        findViewById<View>(R.id.golos_wiki_tv).setOnClickListener {
            startActivity("https://wiki.golos.io/".asIntentToShowUrl())
        }
        findViewById<View>(R.id.about_golos_tv).setOnClickListener {
            startActivity("https://golos.io/welcome".asIntentToShowUrl())
        }
        findViewById<View>(R.id.privacy_policy_tv).setOnClickListener {
            val url = Uri.parse("https://golos.io/ru--konfidenczialxnostx/@golos/politika-konfidencialnosti")
            val intentList = ArrayList<Intent>(2)
            val browserIntent = Intent(Intent.ACTION_VIEW, url)
            val resInfos = packageManager.queryIntentActivities(browserIntent, 0)
            if (!resInfos.isEmpty()) {
                for (resInfo in resInfos) {
                    val packageName = resInfo.activityInfo.packageName
                    if (!packageName.toLowerCase().contains(BuildConfig.APPLICATION_ID)) {
                        Intent().apply {
                            component = ComponentName(packageName, resInfo.activityInfo.name);
                            action = Intent.ACTION_VIEW
                            data = url
                            setPackage(packageName)
                            intentList.add(this)
                        }
                    }
                }
                if (!intentList.isEmpty()) {
                    val chooserIntent = Intent.createChooser(intentList.removeAt(0), "Choose Browser");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentList.toArray())
                    startActivity(chooserIntent)
                } else
                    Log.e("Error", "No Apps can perform your task")
            }
        }
        findViewById<View>(R.id.about_team_tv).setOnClickListener {
            startActivity("https://golos.io/about#team".asIntentToShowUrl())
        }
        findViewById<View>(R.id.faq).setOnClickListener {
            startActivity("https://golos.io/faq".asIntentToShowUrl())
        }
        findViewById<View>(R.id.team_news_tv).setOnClickListener {
            UserProfileActivity.start(this, "golosio")
        }
        findViewById<View>(R.id.exit).setOnClickListener {
            Repository.get.deleteUserdata()
            Toast.makeText(this, R.string.user_made_logout, Toast.LENGTH_SHORT).show()
        }
        val versionTv = findViewById<TextView>(R.id.version_tv)
        versionTv.text = getString(R.string.golos_android_v, BuildConfig.VERSION_NAME)

        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener { onBackPressed() }
        setUpNighMode()
        setUpCompactMode()
        setUpNoImagesMode()
        setUpNSFWMode()
        setUpCurrency()
        setUpBountyDisplay()
        setUpNotifications()

    }

    private fun setUpNotifications() {
        findViewById<View>(R.id.notifications_tv).setOnClickListener {
            startActivity(Intent(this, NotificationsSettingActivity::class.java))
        }
    }

    private fun setUpNoImagesMode() {
        val switch = findViewById<SwitchCompat>(R.id.show_images_switch)
        if (Repository.get.userSettingsRepository.isImagesShown().value == true) switch.isChecked = true
        switch.setOnClickListener {
            Repository.get.userSettingsRepository.setShowImages(switch.isChecked)
        }
    }

    private fun setUpNighMode() {
        val spinner = findViewById<Spinner>(R.id.mode_spinner)
        spinner.adapter = SettingsSpinnerAdapter(this, R.array.daynight)
        spinner.setSelection(if (Repository.get.userSettingsRepository.isNightMode()) 1 else 0)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val currentMode = if (Repository.get.userSettingsRepository.isNightMode()) 1 else 0
                if (currentMode != p2) {
                    Repository.get.userSettingsRepository.setNightMode(p2 == 1)
                    val i = Intent(this@SettingsActivity, SettingsActivity::class.java)
                    setResult(Activity.RESULT_OK)
                    finish()
                    startActivity(i)
                }
            }
        }
    }

    private fun setUpCurrency() {
        val spinner = findViewById<Spinner>(R.id.currency_spinner)
        spinner.adapter = SettingsSpinnerAdapter(this, R.array.currency)
        spinner.setSelection(when (Repository.get.userSettingsRepository.getCurrency().value) {
            UserSettingsRepository.GolosCurrency.USD -> 1
            UserSettingsRepository.GolosCurrency.RUB -> 0
            UserSettingsRepository.GolosCurrency.GBG -> 2
            else -> 0
        })
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val currency = when (p2) {
                    0 -> UserSettingsRepository.GolosCurrency.RUB
                    1 -> UserSettingsRepository.GolosCurrency.USD
                    2 -> UserSettingsRepository.GolosCurrency.GBG
                    else -> UserSettingsRepository.GolosCurrency.USD
                }
                Repository.get.userSettingsRepository.setCurrency(currency)
            }
        }
    }

    private fun setUpBountyDisplay() {
        val spinner = findViewById<Spinner>(R.id.precision_spinner)
        spinner.adapter = SettingsSpinnerAdapter(this, R.array.precision)
        spinner.setSelection(when (Repository.get.userSettingsRepository.getBountDisplay().value) {
            UserSettingsRepository.GolosBountyDisplay.INTEGER -> 0
            UserSettingsRepository.GolosBountyDisplay.ONE_PLACE -> 1
            UserSettingsRepository.GolosBountyDisplay.TWO_PLACES -> 2
            UserSettingsRepository.GolosBountyDisplay.THREE_PLACES -> 3
            else -> 0
        })
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val bountyDisplay = when (p2) {
                    0 -> UserSettingsRepository.GolosBountyDisplay.INTEGER
                    1 -> UserSettingsRepository.GolosBountyDisplay.ONE_PLACE
                    2 -> UserSettingsRepository.GolosBountyDisplay.TWO_PLACES
                    3 -> UserSettingsRepository.GolosBountyDisplay.THREE_PLACES
                    else -> UserSettingsRepository.GolosBountyDisplay.THREE_PLACES
                }
                Repository.get.userSettingsRepository.setBountDisplay(bountyDisplay)
            }
        }
    }

    private fun setUpNSFWMode() {
        val switch = findViewById<SwitchCompat>(R.id.show_nsfw_images_switch)
        if (Repository.get.userSettingsRepository.isNSFWShow().value == true) switch.isChecked = true
        switch.setOnClickListener {
            Repository.get.userSettingsRepository.setIsNSFWShown(switch.isChecked)
        }
    }

    private fun setUpCompactMode() {
        val switch = findViewById<SwitchCompat>(R.id.compact_mode_switch)
        if (Repository.get.userSettingsRepository.isStoriesCompactMode().value == true) switch.isChecked = true
        switch.setOnClickListener {
            Repository.get.userSettingsRepository.setStoriesCompactMode(switch.isChecked)
        }
    }
}
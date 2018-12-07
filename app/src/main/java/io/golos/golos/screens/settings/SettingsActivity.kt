package io.golos.golos.screens.settings

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
import androidx.lifecycle.Observer
import io.golos.golos.BuildConfig
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.GolosAppSettings
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.profile.UserProfileActivity
import io.golos.golos.utils.asIntentToShowUrl
import timber.log.Timber

/**
 * Created by yuri on 12.12.17.
 */
class SettingsActivity : GolosActivity(), Observer<GolosAppSettings> {
    private lateinit var mCurrencySpinner: Spinner
    private lateinit var mNightModeSpinner: Spinner
    private lateinit var mCompactModeSwitch: SwitchCompat
    private lateinit var mNoImagesSwitch: SwitchCompat
    private lateinit var mNSFWSwitch: SwitchCompat
    private lateinit var mBountySpinner: Spinner
    private lateinit var mLangSettingsSpinner: Spinner
    private lateinit var mGolosPowerSpinner: Spinner
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
            finish()
        }
        val versionTv = findViewById<TextView>(R.id.version_tv)
        versionTv.text = getString(R.string.golos_android_v, BuildConfig.VERSION_NAME)

        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener { onBackPressed() }
        setUpNotifications()

        setUpNighMode()
        setUpCompactMode()
        setUpNoImagesMode()
        setUpNSFWMode()
        setUpCurrency()
        setUpLanguage()
        setUpBountyDisplay()
        setUpVotePower()
        Repository.get.appSettings.observe(this, this)
    }

    private fun setUpLanguage() {
        mLangSettingsSpinner = findViewById<Spinner>(R.id.lang_spinner)
        mLangSettingsSpinner.adapter = SpinnerAdapterWithDownChevron(this, R.array.lang)
        mLangSettingsSpinner.setSelection(when (Repository.get.appSettings.value?.language
                ?: return) {
            GolosAppSettings.GolosLanguage.RU -> 0
            GolosAppSettings.GolosLanguage.EN -> 1
        })
        mLangSettingsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                Timber.e("onItemSelected")
                val lang = when (p2) {
                    0 -> GolosAppSettings.GolosLanguage.RU
                    1 -> GolosAppSettings.GolosLanguage.EN
                    else -> GolosAppSettings.GolosLanguage.EN
                }
                val currentValue = Repository.get.appSettings.value ?: return
                Repository.get.setAppSettings(currentValue.copy(language = lang))
            }
        }
    }

    override fun onChanged(appSettings: GolosAppSettings?) {
        appSettings ?: return
        mCurrencySpinner.setSelection(when (appSettings.chosenCurrency) {
            GolosAppSettings.GolosCurrency.USD -> 1
            GolosAppSettings.GolosCurrency.RUB -> 0
            GolosAppSettings.GolosCurrency.GBG -> 2
        })
        mNightModeSpinner.setSelection(if (appSettings.nighModeEnable) 1 else 0)
        mCompactModeSwitch.isChecked = appSettings.feedMode == GolosAppSettings.FeedMode.COMPACT
        mNoImagesSwitch.isChecked = appSettings.displayImagesMode == GolosAppSettings.DisplayImagesMode.DISPLAY
        mNSFWSwitch.isChecked = appSettings.nsfwMode == GolosAppSettings.NSFWMode.DISPLAY
        mBountySpinner.setSelection(when (appSettings.bountyDisplay) {
            GolosAppSettings.GolosBountyDisplay.INTEGER -> 0
            GolosAppSettings.GolosBountyDisplay.ONE_PLACE -> 1
            GolosAppSettings.GolosBountyDisplay.TWO_PLACES -> 2
            GolosAppSettings.GolosBountyDisplay.THREE_PLACES -> 3
        })
        mLangSettingsSpinner.setSelection(when (appSettings.language) {
            GolosAppSettings.GolosLanguage.RU -> 0
            GolosAppSettings.GolosLanguage.EN -> 1
        })

        mGolosPowerSpinner.setSelection(when (appSettings.defaultUpvotePower) {
            in 0..10 -> 1
            in 11..20 -> 2
            in 21..30 -> 3
            in 31..40 -> 4
            in 41..50 -> 5
            in 51..60 -> 6
            in 61..70 -> 7
            in 71..80 -> 8
            in 81..90 -> 9
            in 91..100 -> 10
            else -> 0
        })
    }

    private fun setUpNotifications() {
        findViewById<View>(R.id.notifications_tv).setOnClickListener {
            startActivity(Intent(this, NotificationsSettingActivity::class.java))
        }
    }

    private fun setUpNoImagesMode() {
        mNoImagesSwitch = findViewById<SwitchCompat>(R.id.show_images_switch)

        mNoImagesSwitch.setOnClickListener {
            val currentValue = Repository.get.appSettings.value ?: return@setOnClickListener
            Repository.get.setAppSettings(currentValue.copy(displayImagesMode =
            if (currentValue.displayImagesMode == GolosAppSettings.DisplayImagesMode.DISPLAY) GolosAppSettings.DisplayImagesMode.DISPLAY_NOT else GolosAppSettings.DisplayImagesMode.DISPLAY))
        }
    }

    private fun setUpNighMode() {
        mNightModeSpinner = findViewById<Spinner>(R.id.mode_spinner)
        mNightModeSpinner.adapter = SpinnerAdapterWithDownChevron(this, R.array.daynight)
        mNightModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val currentValue = Repository.get.appSettings.value ?: return
                val currentMode = if (currentValue.nighModeEnable) 1 else 0
                if (currentMode != p2) {
                    Repository.get.setAppSettings(currentValue.copy(nighModeEnable = p2 == 1))
                }
            }
        }
    }

    private fun setUpCurrency() {
        mCurrencySpinner = findViewById<Spinner>(R.id.currency_spinner)
        mCurrencySpinner.adapter = SpinnerAdapterWithDownChevron(this, R.array.currency)
        mCurrencySpinner.setSelection(when (Repository.get.appSettings.value?.chosenCurrency) {
            GolosAppSettings.GolosCurrency.USD -> 1
            GolosAppSettings.GolosCurrency.RUB -> 0
            GolosAppSettings.GolosCurrency.GBG -> 2
            else -> 0
        })
        mCurrencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val currency = when (p2) {
                    0 -> GolosAppSettings.GolosCurrency.RUB
                    1 -> GolosAppSettings.GolosCurrency.USD
                    2 -> GolosAppSettings.GolosCurrency.GBG
                    else -> GolosAppSettings.GolosCurrency.USD
                }
                val currentValue = Repository.get.appSettings.value ?: return
                Repository.get.setAppSettings(currentValue.copy(chosenCurrency = currency))
            }
        }
    }

    private fun setUpVotePower() {
        mGolosPowerSpinner = findViewById<Spinner>(R.id.golos_power_spinner)
        mGolosPowerSpinner.adapter = SpinnerAdapterWithDownChevron(this, R.array.vote_power)

        mGolosPowerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val currentValue = Repository.get.appSettings.value ?: return
                val golosPower = when (p2) {
                    0 -> Byte.MIN_VALUE
                    1 -> (10).toByte()
                    2 -> (20).toByte()
                    3 -> (30).toByte()
                    4 -> (40).toByte()
                    5 -> (50).toByte()
                    6 -> (60).toByte()
                    7 -> (70).toByte()
                    8 -> (80).toByte()
                    9 -> (90).toByte()
                    10 -> (100).toByte()
                    else -> Byte.MIN_VALUE
                }
                Repository.get.setAppSettings(currentValue.copy(defaultUpvotePower = golosPower))
            }
        }
    }

    private fun setUpBountyDisplay() {
        mBountySpinner = findViewById<Spinner>(R.id.precision_spinner)
        mBountySpinner.adapter = SpinnerAdapterWithDownChevron(this, R.array.precision)

        mBountySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val currentValue = Repository.get.appSettings.value ?: return
                val bountyDisplay = when (p2) {
                    0 -> GolosAppSettings.GolosBountyDisplay.INTEGER
                    1 -> GolosAppSettings.GolosBountyDisplay.ONE_PLACE
                    2 -> GolosAppSettings.GolosBountyDisplay.TWO_PLACES
                    3 -> GolosAppSettings.GolosBountyDisplay.THREE_PLACES
                    else -> GolosAppSettings.GolosBountyDisplay.THREE_PLACES
                }
                Repository.get.setAppSettings(currentValue.copy(bountyDisplay = bountyDisplay))
            }
        }
    }

    private fun setUpNSFWMode() {
        mNSFWSwitch = findViewById<SwitchCompat>(R.id.show_nsfw_images_switch)
        mNSFWSwitch.setOnClickListener {
            val currentValue = Repository.get.appSettings.value ?: return@setOnClickListener
            Repository.get.setAppSettings(currentValue.copy(nsfwMode = if (mNSFWSwitch.isChecked) GolosAppSettings.NSFWMode.DISPLAY else GolosAppSettings.NSFWMode.DISPLAY_NOT))
        }
    }

    private fun setUpCompactMode() {
        mCompactModeSwitch = findViewById<SwitchCompat>(R.id.compact_mode_switch)
        mCompactModeSwitch.setOnClickListener {
            val currentValue = Repository.get.appSettings.value ?: return@setOnClickListener
            Repository.get.setAppSettings(currentValue.copy(feedMode = if (mCompactModeSwitch.isChecked) GolosAppSettings.FeedMode.COMPACT else GolosAppSettings.FeedMode.FULL))
        }
    }
}
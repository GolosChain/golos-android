package io.golos.golos.screens

import android.app.Activity
import android.app.UiModeManager
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.text.Html
import android.widget.Toast
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.utils.ErrorCode
import io.golos.golos.utils.OneShotLiveData
import io.golos.golos.utils.nextInt
import io.golos.golos.utils.restart
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Created by yuri yurivladdurain@gmail.com
 *
 */
abstract class GolosActivity : AppCompatActivity() {
    private var isResumed = false
    private val showVoteDialog: MutableLiveData<Boolean> = OneShotLiveData()


    override fun onCreate(savedInstanceState: Bundle?) {
        if (Repository.get.userSettingsRepository.isNightMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            ((getSystemService(UI_MODE_SERVICE) as UiModeManager).setNightMode(UiModeManager.MODE_NIGHT_YES))
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            ((getSystemService(UI_MODE_SERVICE) as UiModeManager).setNightMode(UiModeManager.MODE_NIGHT_NO))
        }
        super.onCreate(savedInstanceState)
        if (needToShowVoteDialog()) {
            Handler().postDelayed({
                if (needToShowVoteDialog())
                    showVoteDialog.value = true
            }, TimeUnit.SECONDS.toMillis(120))
        }
        showVoteDialog.observe(this, Observer {
            if (it == true) VoteForAppDialog.getInstance().show(supportFragmentManager, null)
        })
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
    }

    private fun needToShowVoteDialog(): Boolean {
        return !Repository.get.userSettingsRepository.isUserVotedForApp()
                && !Repository.get.userSettingsRepository.isVoteQuestionMade()
    }

    override fun onStop() {
        super.onStop()
        isResumed = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val fragments = supportFragmentManager.fragments
        fragments.forEach { it.onActivityResult(requestCode, resultCode, data) }

        if (requestCode == CHANGE_THEME && resultCode == Activity.RESULT_OK) {
            restart()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val fragments = supportFragmentManager.fragments
        fragments.forEach { it.onRequestPermissionsResult(requestCode, permissions, grantResults) }
    }

    override fun finish() {
        super.finish()
        // overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun startActivity(intent: Intent?) {
        super.startActivity(intent)
        //overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    open fun showErrorMessage(code: ErrorCode) {
        val message = when (code) {
            ErrorCode.ERROR_SLOW_CONNECTION -> R.string.slow_internet_connection
            ErrorCode.ERROR_NO_CONNECTION -> R.string.slow_internet_connection
            else -> R.string.unknown_error
        }
        Snackbar.make(findViewById(android.R.id.content),
                Html.fromHtml("<font color=\"#ffffff\">${resources.getString(message)}</font>"),
                Toast.LENGTH_SHORT).show()
    }

    companion object {
        val CHANGE_THEME: Int = nextInt()
    }
}



package io.golos.golos.screens

import android.app.UiModeManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Html
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import io.golos.golos.BuildConfig
import io.golos.golos.EActivity
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.utils.*
import java.util.concurrent.TimeUnit

/**
 * Created by yuri yurivladdurain@gmail.com
 *
 */
abstract class GolosActivity : AppCompatActivity() {
    private var isResumed = false
    private val showVoteDialog: MutableLiveData<Boolean> = OneShotLiveData()
    private val mRepostObserver = RepostObserver(supportFragmentManager)
    private var mLastNightModeEnbledFlag = false


    override fun onCreate(savedInstanceState: Bundle?) {
        if (Repository.get.appSettings.value?.nighModeEnable == true) {
            mLastNightModeEnbledFlag = true
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            ((getSystemService(UI_MODE_SERVICE) as UiModeManager).setNightMode(UiModeManager.MODE_NIGHT_YES))
        } else {
            mLastNightModeEnbledFlag = false
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            ((getSystemService(UI_MODE_SERVICE) as UiModeManager).setNightMode(UiModeManager.MODE_NIGHT_NO))
        }
        super.onCreate(savedInstanceState)
        if (needToShowVoteDialog()) {
            Handler().postDelayed({
                if (needToShowVoteDialog())
                    showVoteDialog.value = true
            }, TimeUnit.MINUTES.toMillis(3))
        }
        showVoteDialog.observe(this, Observer {
            if (it == true) VoteForAppDialog.getInstance().show(supportFragmentManager, null)
        })
        Repository.get.lastRepost.observe(this, mRepostObserver)
        Repository.get.appSettings.observe(this, Observer { appSettings ->
            appSettings ?: return@Observer
            if (appSettings.nighModeEnable != mLastNightModeEnbledFlag) {
                restart()
            }
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (BuildConfig.DEBUG) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    startActivity(Intent(this, EActivity::class.java))
                    return true
                }
            }
        }


        return super.onKeyDown(keyCode, event)
    }


    override fun onResume() {
        super.onResume()
        isResumed = true

    }


    private fun needToShowVoteDialog(): Boolean {
        return !userVotedForApp
                && !isVoteQuestionMade
    }

    override fun onStop() {
        super.onStop()
        isResumed = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val fragments = supportFragmentManager.fragments
        fragments.forEach { it.onActivityResult(requestCode, resultCode, data) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val fragments = supportFragmentManager.fragments
        fragments.forEach { it.onRequestPermissionsResult(requestCode, permissions, grantResults) }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun startActivity(intent: Intent?) {
        super.startActivity(intent)
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
                Snackbar.LENGTH_SHORT).show()
    }

    companion object {
        val CHANGE_THEME: Int = nextInt()
    }
}



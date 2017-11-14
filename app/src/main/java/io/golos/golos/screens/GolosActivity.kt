package io.golos.golos.screens

import android.content.Intent
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.widget.Toast
import io.golos.golos.R
import io.golos.golos.utils.ErrorCode

/**
 * Created by yuri yurivladdurain@gmail.com
 *
 */
abstract class GolosActivity : AppCompatActivity() {
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
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right)
    }

    override fun startActivity(intent: Intent?) {
        super.startActivity(intent)
        overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left)
    }

    open fun showErrorMessage(code: ErrorCode) {
        var message = R.string.unknown_error

        when (code) {
            ErrorCode.ERROR_SLOW_CONNECTION -> message = R.string.slow_internet_connection
            ErrorCode.ERROR_NO_CONNECTION -> message = R.string.slow_internet_connection
            else -> message = R.string.unknown_error
        }
        Snackbar.make(findViewById(android.R.id.content),
                Html.fromHtml("<font color=\"#ffffff\">${resources.getString(message)}</font>"),
                Toast.LENGTH_SHORT).show()
    }
}



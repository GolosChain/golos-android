package io.golos.golos.screens

import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import fr.castorflex.android.circularprogressbar.CircularProgressBar
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.ReadyStatus
import io.golos.golos.screens.main_activity.MainActivity
import io.golos.golos.utils.setViewGone
import io.golos.golos.utils.setViewVisible

/**
 * Created by yuri on 27.12.17.
 */
class SplashActivity : GolosActivity(), Observer<ReadyStatus> {
    val repo = Repository.get
    private lateinit var mProgress: CircularProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_splash)
        mProgress = findViewById(R.id.progress)
        repo.getAppReadyStatus().observe(this, this)
    }

    override fun onChanged(t: ReadyStatus?) {
        t?.let {
            mProgress.setViewGone()
            if (it.error == null) {
                val i = Intent(this, MainActivity::class.java)
                finish()
                startActivity(i)
            } else {
                if (this.isFinishing || this.isDestroyed) return
                AlertDialog.Builder(this)
                        .setMessage(it.error.localizedMessage ?: R.string.unknown_error)

                        .setPositiveButton(R.string.retry, { _, _ ->
                            mProgress.setViewVisible()
                            repo.requestInitRetry()
                        })
                        .setNegativeButton(R.string.cancel, { _, _ ->
                            finish()
                        })
                        .create()
                        .show()
            }
        }
    }
}
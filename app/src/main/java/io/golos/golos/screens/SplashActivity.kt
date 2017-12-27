package io.golos.golos.screens

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.View
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.StoryFilter
import io.golos.golos.screens.stories.model.FeedType

/**
 * Created by yuri on 27.12.17.
 */
class SplashActivity : GolosActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_splash)
        startPreloading()
    }

    private fun startPreloading() {
        val progress = findViewById<View>(R.id.progress)
        val isUserLoggedIn = Repository.get.isUserLoggedIn()
        Repository.get.requestStoriesListUpdate(20,
                if (isUserLoggedIn) FeedType.PERSONAL_FEED else FeedType.POPULAR,
                filter = if (isUserLoggedIn) StoryFilter(userNameFilter = Repository.get.getCurrentUserDataAsLiveData().value?.userName ?: "") else null,
                complitionHandler = { _, e ->
                    progress.visibility = View.GONE
                    if (e == null) {
                        val i = Intent(this, MainActivity::class.java)
                        startActivity(i)
                        finish()
                    } else {
                        AlertDialog.Builder(this)
                                .setMessage(e.localizedMessage ?: R.string.unknown_error)

                                .setPositiveButton(R.string.retry, { _, _ ->
                                    progress.visibility = View.VISIBLE
                                    startPreloading()
                                })
                                .setNegativeButton(R.string.cancel, { _, _ ->
                                    finish()
                                })
                                .create()
                                .show()
                    }
                })
    }
}
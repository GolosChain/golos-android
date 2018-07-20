package io.golos.golos

import android.content.Intent
import android.os.Bundle
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.SplashActivity
import io.golos.golos.screens.main_activity.MainActivity
import io.golos.golos.screens.profile.UserProfileActivity
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.StoryActivity
import io.golos.golos.utils.FeedMatch
import io.golos.golos.utils.GolosLinkMatcher
import io.golos.golos.utils.StoryLinkMatch
import io.golos.golos.utils.UserLinkMatch
import timber.log.Timber

public class RouteActivity : GolosActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(android.R.style.Theme_Translucent_NoTitleBar)
        super.onCreate(savedInstanceState)
        Timber.e("start activity data = ${intent.data}")

        intent?.data?.let {
            val link = it.toString()
            val match = GolosLinkMatcher.match(link)
            Timber.e("$match")
            val startMainActivityIntent = MainActivity.getIntent(this)
            when (match) {
                is StoryLinkMatch -> {
                    startActivities(arrayOf(startMainActivityIntent,
                            StoryActivity.getStartIntent(this, match.author, match.blog, match.permlink, FeedType.UNCLASSIFIED, null)))
                }
                is UserLinkMatch -> {
                    startActivities(arrayOf(startMainActivityIntent, UserProfileActivity.getStartIntent(this, match.user)))
                }
                is FeedMatch -> {
                    MainActivity.start(this, match.feedType)
                }
                else -> {
                    startActivity(Intent(this, SplashActivity::class.java))
                }

            }
        }
        finish()

    }
}
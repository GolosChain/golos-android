package io.golos.golos.screens.profile

import android.arch.lifecycle.LiveData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.golos.golos.R
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.utils.OneShotLiveData

/**
 * Created by yuri on 15.12.17.
 */
class UserProfileActivity : GolosActivity(), ProfileSectionPreselector {
    private var fragment: UserProfileFragment? = null
    private val mPreselectLiveData = OneShotLiveData<FeedType>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_profile)
        if (intent.hasExtra(USERNAME_TAG)) {
            fragment = UserProfileFragment.getInstance(intent.getStringExtra(USERNAME_TAG))
            supportFragmentManager.beginTransaction()
                    .replace(R.id.root_lo, fragment).commit()
        }
        checkPreselection(intent)
    }

    fun checkPreselection(intent: Intent?) {
        ((intent?.getSerializableExtra(FEED_TYPE) as? FeedType)
                ?: FeedType.BLOG).let { mPreselectLiveData.value = it }
    }

    override fun onNewIntent(intent: Intent?) {
        checkPreselection(intent)
    }

    override fun getPreselect() = mPreselectLiveData

    override fun onResume() {
        super.onResume()
        fragment?.userVisibleHint = true
    }

    companion object {
        private val USERNAME_TAG = "USERNAME_TAG"
        private val FEED_TYPE = "FEED_TYPE"
        fun start(ctx: Context,
                  username: String) {
            ctx.startActivity(getStartIntent(ctx, username))
        }

        fun getStartIntent(ctx: Context,
                           username: String,
                           feedType: FeedType? = null): Intent {
            val i = Intent(ctx, UserProfileActivity::class.java)
            i.putExtra(USERNAME_TAG, username)
            if (feedType != null) i.putExtra(FEED_TYPE, feedType)

            return i
        }
    }
}

interface ProfileSectionPreselector {
    fun getPreselect(): LiveData<FeedType>
}
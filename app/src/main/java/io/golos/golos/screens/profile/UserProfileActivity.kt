package io.golos.golos.screens.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.golos.golos.R
import io.golos.golos.screens.GolosActivity

/**
 * Created by yuri on 15.12.17.
 */
class UserProfileActivity : GolosActivity() {
    private var fragment: UserProfileFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_profile)
        if (intent.hasExtra(USERNAME_TAG)) {
            fragment = UserProfileFragment.getInstance(intent.getStringExtra(USERNAME_TAG))
            supportFragmentManager.beginTransaction()
                    .replace(R.id.root_lo, fragment).commit()
        }
    }

    override fun onResume() {
        super.onResume()
        fragment?.userVisibleHint = true
    }

    companion object {
        private val USERNAME_TAG = "USERNAME_TAG"
        fun start(ctx: Context,
                  username: String) {
            val i = Intent(ctx, UserProfileActivity::class.java)
            i.putExtra(USERNAME_TAG, username)
            ctx.startActivity(i)
        }
    }
}
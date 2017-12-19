package io.golos.golos.screens.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.golos.golos.R
import io.golos.golos.screens.GolosActivity

/**
 * Created by yuri on 15.12.17.
 */
class ProfileActivity : GolosActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_profile)
        if (intent.hasExtra(USERNAME_TAG)) {
            val fragment = UserProfileFragment.getInstance(intent.getStringExtra(USERNAME_TAG), false)
            supportFragmentManager.beginTransaction()
                    .replace(R.id.root_lo, fragment).commit()
        }
    }

    companion object {
        private val USERNAME_TAG = "USERNAME_TAG"
        fun start(ctx: Context,
                  username: String) {
            val i = Intent(ctx, ProfileActivity::class.java)
            i.putExtra(USERNAME_TAG, username)
            ctx.startActivity(i)
        }
    }
}
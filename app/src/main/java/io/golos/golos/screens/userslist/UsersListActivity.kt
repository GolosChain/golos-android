package io.golos.golos.screens.userslist

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.screens.GolosActivity
import io.golos.golos.utils.showSnackbar

/**
 * Created by yuri on 22.12.17.
 */
class UsersListActivity : GolosActivity(), Observer<UserListViewState> {
    private lateinit var mTitle: TextView
    private lateinit var mRecycler: RecyclerView
    private lateinit var mProgress: ProgressBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_users_list_to_follow)
        mTitle = findViewById(R.id.title_text)
        mRecycler = findViewById(R.id.recycler)
        mProgress = findViewById(R.id.progress)
        val viewmodel = ViewModelProviders.of(this).get(UserListViewModel::class.java)
        viewmodel.getLiveData().observe(this, this)
        if (intent.hasExtra(USERNAME_TAG)) {
            val isSubscribers = intent.getBooleanExtra(SUBSCRIBERS_OR_SUBSCRIPTIONS, false)
            viewmodel.onCreate(intent.getStringExtra(USERNAME_TAG),
                    intent.getBooleanExtra(SUBSCRIBERS_OR_SUBSCRIPTIONS, false))

            mTitle.setText(if (isSubscribers) R.string.subscribers else R.string.subscriptions)
        }
        mRecycler.visibility = View.GONE
        mProgress.visibility = View.VISIBLE
        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener { onBackPressed() }
        mRecycler.layoutManager = LinearLayoutManager(this)
        (mRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        mRecycler.adapter = UserListAdapter({
            viewmodel.onUserClick(this, it)
        },
                { viewmodel.onSubscribeClick(this, it) })
    }

    override fun onChanged(t: UserListViewState?) {
        if (t == null) return
        mProgress.visibility = if (t.users.isEmpty()) View.VISIBLE else View.GONE
        mRecycler.visibility = if (t.users.isNotEmpty()) View.VISIBLE else View.GONE
        (mRecycler.adapter as UserListAdapter).listItems = t.users
        t.error?.let {
            mRecycler.showSnackbar(it.localizedMessage ?: 0)
        }
    }

    companion object {
        private val USERNAME_TAG = "USERNAME_TAG"
        private val SUBSCRIBERS_OR_SUBSCRIPTIONS = "SUBSCRIBERS_OR_SUBSCRIPTIONS"
        fun start(ctx: Context,
                  userNameFor: String,
                  isSubscribersOrSubscriptions: Boolean) {
            val i = Intent(ctx, UsersListActivity::class.java)
            i.putExtra(USERNAME_TAG, userNameFor)
            i.putExtra(SUBSCRIBERS_OR_SUBSCRIPTIONS, isSubscribersOrSubscriptions)
            ctx.startActivity(i)
        }
    }
}
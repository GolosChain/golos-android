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
import io.golos.golos.App
import io.golos.golos.R
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.userslist.model.ListType
import io.golos.golos.utils.InternetStatusNotifier
import io.golos.golos.utils.StringSupplier
import io.golos.golos.utils.showSnackbar
import timber.log.Timber

/**
 * Created by yuri on 22.12.17.
 */
class UsersListActivity : GolosActivity(), Observer<UserListViewState> {
    private lateinit var mTitle: TextView
    private lateinit var mRecycler: RecyclerView
    private lateinit var mProgress: ProgressBar
    private lateinit var mViewModel:UserListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_users_list_to_follow)
        mTitle = findViewById(R.id.title_text)
        mRecycler = findViewById(R.id.recycler)
        mProgress = findViewById(R.id.progress)
        mViewModel = ViewModelProviders.of(this).get(UserListViewModel::class.java)
        mViewModel.getLiveData().observe(this, this)

        if (!intent.hasExtra(TYPE_TAG)) {
            Timber.e("start activity from factory methods")
            return
        }

        val type = intent.getSerializableExtra(TYPE_TAG) as ListType

        mViewModel.onCreate(intent.getStringExtra(USERNAME_TAG),
                if (intent.hasExtra(STORY_ID_TAG)) intent.getLongExtra(STORY_ID_TAG, 0L) else null,
                type,
                object : StringSupplier {
                    override fun get(id: Int): String {
                        return getString(id)
                    }
                },
                object : InternetStatusNotifier {
                    override fun isAppOnline(): Boolean {
                        return App.isAppOnline()
                    }
                })

        mRecycler.visibility = View.GONE
        mProgress.visibility = View.VISIBLE
        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener { onBackPressed() }
        mRecycler.layoutManager = LinearLayoutManager(this)
        (mRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        mRecycler.adapter = UserListAdapter({
            mViewModel.onUserClick(this, it)
        },
                { mViewModel.onSubscribeClick(it) })
    }

    override fun onChanged(t: UserListViewState?) {
        if (t == null) return
        mProgress.visibility = if (t.users.isEmpty()) View.VISIBLE else View.GONE
        mRecycler.visibility = if (t.users.isNotEmpty()) View.VISIBLE else View.GONE
        (mRecycler.adapter as UserListAdapter).listItems = t.users
        t.error?.let {
            mRecycler.showSnackbar(it.localizedMessage ?: 0)
        }
        mTitle.text = t.title
    }

    override fun onDestroy() {
        super.onDestroy()
        mViewModel.onDestroy()
    }

    companion object {
        private val USERNAME_TAG = "USERNAME_TAG"
        private val TYPE_TAG = "TYPE_TAG"
        private val STORY_ID_TAG = "STORY_ID_TAG"
        fun startForSubscribersOrSubscriptions(ctx: Context,
                                               userNameFor: String,
                                               listType: ListType) {
            val i = Intent(ctx, UsersListActivity::class.java)
            if (listType != ListType.SUBSCRIBERS && listType != ListType.SUBSCRIPTIONS) {
                Timber.e("cal this with ListType.SUBSCRIBERS or ListType.SUBSCRIPTIONS")
                return
            }
            i.putExtra(USERNAME_TAG, userNameFor)
            i.putExtra(TYPE_TAG, listType)
            ctx.startActivity(i)
        }

        fun startToShowVoters(ctx: Context,
                              storyId: Long) {
            val i = Intent(ctx, UsersListActivity::class.java)
            i.putExtra(TYPE_TAG, ListType.VOTERS)
            i.putExtra(STORY_ID_TAG, storyId)
            ctx.startActivity(i)
        }
    }
}
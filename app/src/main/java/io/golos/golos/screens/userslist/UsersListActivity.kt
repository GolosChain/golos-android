package io.golos.golos.screens.userslist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.golos.golos.App
import io.golos.golos.R
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.userslist.model.ListType
import io.golos.golos.screens.widgets.dialogs.UnSubscribeConfirmalDialog
import io.golos.golos.utils.*
import timber.log.Timber

/**
 * Created by yuri on 22.12.17.
 */
class UsersListActivity : GolosActivity(), Observer<UserListViewState>, UnSubscribeConfirmalDialog.OnUserUnsubscriptionConfirm {
    private lateinit var mTitle: TextView
    private lateinit var mRecycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var mProgress: ProgressBar
    private lateinit var mViewModel: UserListViewModel
    private lateinit var mNothigHereLabel: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_users_list_to_follow)
        mTitle = findViewById(R.id.title_text)
        mRecycler = findViewById(R.id.recycler)
        mProgress = findViewById(R.id.progress)
        mNothigHereLabel = findViewById(R.id.nothing_tv)
        mViewModel = ViewModelProviders.of(this).get(UserListViewModel::class.java)
        mViewModel.getLiveData().observe(this, this)
        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            Timber.e("onClicl")
            onBackPressed()
        }

        if (!intent.hasExtra(TYPE_TAG)) {
            Timber.e("you must start this activity from static  factory methods")
            return
        }

        val type = intent.getSerializableExtra(TYPE_TAG) as ListType

        mViewModel.onCreate(intent.getStringExtra(USERNAME_TAG),
                if (intent.hasExtra(STORY_ID_TAG)) intent.getLongExtra(STORY_ID_TAG, 0L) else null,
                type,
                object : StringProvider {
                    override fun get(resId: Int, args: String?): String {
                        return getString(resId, args)
                    }
                },
                object : InternetStatusNotifier {
                    override fun isAppOnline(): Boolean {
                        return App.isAppOnline()
                    }
                })
        mProgress.setViewVisible()
        mRecycler.setViewGone()

        mRecycler.layoutManager = MyLinearLayoutManager(this)
        (mRecycler.itemAnimator as androidx.recyclerview.widget.SimpleItemAnimator).supportsChangeAnimations = false
        mRecycler.adapter = UserListAdapter({
            mViewModel.onUserClick(this, it)
        },
                { mViewModel.onSubscribeClick(it) })
        mViewModel.onUnsubscribeConfirmal.observe(this, Observer {
            it ?: return@Observer
            UnSubscribeConfirmalDialog.getInstance(it).show(supportFragmentManager, null)
        })
    }

    override fun onUnsubscriptionConfirmed(userName: String) {
        mViewModel.unsubscribe(userName)
    }

    override fun onChanged(t: UserListViewState?) {

        if (t == null) {
            mProgress.setViewVisible()
            mRecycler.setViewGone()
            mNothigHereLabel.setViewGone()
            return
        }

        mProgress.setViewGone()
        if (t.users.isNotEmpty()) {
            mRecycler.setViewVisible()
            mNothigHereLabel.setViewGone()
        } else {
            mRecycler.setViewGone()
            mNothigHereLabel.setViewVisible()
        }
        (mRecycler.adapter as UserListAdapter).listItems = t.users
        t.error?.let {
            mRecycler.showSnackbar(it.localizedMessage ?: 0)
        }
        mTitle.text = t.title
    }

    override fun onStart() {
        super.onStart()
        mViewModel.onStart()
    }

    override fun onStop() {
        super.onStop()
        mViewModel.onStop()
    }

    companion object {
        private const val USERNAME_TAG = "USERNAME_TAG"
        private const val TYPE_TAG = "TYPE_TAG"
        private const val STORY_ID_TAG = "STORY_ID_TAG"
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

        fun startToShowUpVoters(ctx: Context,
                                storyId: Long) {
            val i = Intent(ctx, UsersListActivity::class.java)
            i.putExtra(TYPE_TAG, ListType.UP_VOTERS)
            i.putExtra(STORY_ID_TAG, storyId)
            ctx.startActivity(i)
        }

        fun startToShowDownVoters(ctx: Context,
                                  storyId: Long) {
            val i = Intent(ctx, UsersListActivity::class.java)
            i.putExtra(TYPE_TAG, ListType.DOWN_VOTERS)
            i.putExtra(STORY_ID_TAG, storyId)
            ctx.startActivity(i)
        }

        fun startToShowAllVoters(ctx: Context,
                                 storyId: Long) {
            val i = Intent(ctx, UsersListActivity::class.java)
            i.putExtra(TYPE_TAG, ListType.ALL_VOTERS)
            i.putExtra(STORY_ID_TAG, storyId)
            ctx.startActivity(i)
        }
    }
}
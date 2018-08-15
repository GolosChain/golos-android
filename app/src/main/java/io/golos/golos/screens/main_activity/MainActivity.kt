package io.golos.golos.screens.main_activity

import android.animation.Animator
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.BottomNavigationView
import android.support.v4.view.ViewPager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.notifications.GolosNotifications
import io.golos.golos.notifications.NOTIFICATION_KEY
import io.golos.golos.notifications.PostLinkable
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.CreatePostResult
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.editor.EditorActivity
import io.golos.golos.screens.main_activity.adapters.DissmissTouchHelper
import io.golos.golos.screens.main_activity.adapters.MainPagerAdapter
import io.golos.golos.screens.main_activity.adapters.NotificationsAdapter
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.StoryActivity
import io.golos.golos.utils.*
import timber.log.Timber
import java.util.*

class MainActivity : GolosActivity(), Observer<CreatePostResult>, FeedTypePreselect {
    private var lastTimeTapped: Long = Date().time
    private var mDoubleBack = false
    private lateinit var mNotificationsIndicator: TextView
    private lateinit var mButtonContainer: ViewGroup
    private lateinit var mNotificationsContainer: ViewGroup

    private lateinit var mNotificationsRecycler: RecyclerView
    private val mHandler = Handler()
    private val mSelectLiveData = OneShotLiveData<FeedType>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_tabs)

        findViewById<ViewGroup>(R.id.main_a_frame).setFullAnimationToViewGroup()

        mNotificationsRecycler = findViewById(R.id.notification_recycler)
        mButtonContainer = findViewById(R.id.button_container)
        mNotificationsIndicator = findViewById(R.id.notifications_count_tv)
        mNotificationsContainer = findViewById(R.id.notifications_container)

        val pager: ViewPager = findViewById(R.id.content_pager)
        pager.adapter = MainPagerAdapter(supportFragmentManager)

        pager.offscreenPageLimit = 4

        val bottomNavView: BottomNavigationView = findViewById(R.id.bottom_nav_view)
        bottomNavView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.stories -> {
                    pager.currentItem = STORIES_FRAGMENT_POSITION
                    true
                }
                R.id.groups -> {
                    pager.currentItem = FILTERED_BY_TAG_STORIES
                    true
                }
                R.id.notifications -> {
                    bottomNavView.showSnackbar(R.string.unaval_in_current_version)
                    false
                }
                R.id.add -> {
                    if (!Repository.get.isUserLoggedIn()) {
                        bottomNavView.showSnackbar(R.string.must_be_logged_in_for_this_action)
                        false
                    } else {
                        EditorActivity.startPostCreator(this, "")
                        false
                    }
                }
                R.id.profile -> {
                    pager.currentItem = PROFILE_FRAGMENT_POSITION
                    true
                }
                else -> true
            }
        }
        Repository.get.lastCreatedPost().observe(this, this)

        checkStartArgsForNotification(intent)

        mNotificationsRecycler.adapter = NotificationsAdapter(listOf(),
                {
                    if (it is PostLinkable) {
                        it.getLink()?.let {
                            StoryActivity.start(this, it.author, it.blog, it.permlink, FeedType.UNCLASSIFIED, null)
                        }
                        mHandler.postDelayed({ Repository.get.notificationsRepository.dismissNotification(it) }, 1000)
                    } else {
                        mHandler.post { Repository.get.notificationsRepository.dismissNotification(it) }
                    }

                },
                { Repository.get.notificationsRepository.dismissNotification(it) },
                true)
        mNotificationsRecycler.overScrollMode = View.OVER_SCROLL_NEVER

        val adapter = mNotificationsRecycler.adapter as? NotificationsAdapter
        adapter?.let {
            DissmissTouchHelper(it).attachToRecyclerView(mNotificationsRecycler)
        }

        Repository.get.notificationsRepository.notifications.observe(this, Observer<GolosNotifications> {

            mButtonContainer.animate().cancel()
            (mNotificationsRecycler.adapter as? NotificationsAdapter)?.notification = it?.notifications ?: listOf()
            if (it?.notifications?.isEmpty() != false) {
                mNotificationsContainer.setViewGone()
                mButtonContainer.setViewGone()
                mButtonContainer.animate().alpha(0f)

            } else {
                if (it.notifications.size == 1) {
                    mNotificationsContainer.setViewVisible()
                    if (mButtonContainer.alpha > 0f) {

                        mButtonContainer.animate().alpha(0f).setDuration(200L).setListener(object : EndAnimationListener() {
                            override fun onAnimationEnd(p0: Animator?) {

                                mButtonContainer.setViewGone()
                            }
                        })
                    } else {
                        mButtonContainer.setViewGone()
                    }

                } else {
                    mNotificationsContainer.setViewVisible()
                    if (mButtonContainer.alpha == 0f) {

                        mButtonContainer.animate().alpha(1f).setDuration(200).setListener(object : EndAnimationListener() {
                            override fun onAnimationEnd(p0: Animator?) {

                                mButtonContainer.setViewVisible()
                            }
                        })
                    } else {
                        mButtonContainer.setViewVisible()
                    }
                    mNotificationsIndicator.text = getString(R.string.show_more_notifications,
                            resources.getQuantityString(R.plurals.notifications, it.notifications.count() - 1,
                                    (it.notifications.count() - 1).toString()))
                }
            }
        })
        mNotificationsIndicator.setOnClickListener {
            NotificationsDialog().show(supportFragmentManager, "NotificationsDialog")
        }
        checkpreSelect(intent)


    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkStartArgsForNotification(intent)
        checkpreSelect(intent)
    }

    override fun onChanged(it: CreatePostResult?) {
        if (it?.isPost == true) {
            StoryActivity.start(this,
                    it.author, it.blog, it.permlink, FeedType.UNCLASSIFIED, null)
        }
    }

    override fun getSelection() = mSelectLiveData

    private fun checkpreSelect(intent: Intent?) {
        val feedType = intent?.getSerializableExtra(PRESELECT_FEED_TYPE) as? FeedType
        mSelectLiveData.value = feedType
    }

    private fun checkStartArgsForNotification(intent: Intent?) {
        Timber.e("checkStartArgsForNotification")
        if (intent?.getIntExtra(STARTED_FROM_NOTIFICATION, 0) != 0) {
            Timber.e("checkStartArgsForNotification, value = ${intent?.getIntExtra(STARTED_FROM_NOTIFICATION, 0)
                    ?: 0}")
            val intentHashCode = intent?.getIntExtra(STARTED_FROM_NOTIFICATION, 0) ?: 0
            val broadcasterIntent = Intent(NOTIFICATION_KEY)
            broadcasterIntent.putExtra(NOTIFICATION_KEY, intentHashCode)
            sendBroadcast(broadcasterIntent)
        }
    }

    override fun onBackPressed() {
        if ((Date().time - lastTimeTapped) > 3000) {
            mDoubleBack = false
            lastTimeTapped = Date().time
        }
        if (!mDoubleBack) {
            mDoubleBack = true
            findViewById<View>(R.id.content_pager)?.let {
                it.showSnackbar(R.string.tab_back_double_to_enter)
            }
        } else {
            super.onBackPressed()
        }
    }


    companion object {
        const val STORIES_FRAGMENT_POSITION = 0
        const val FILTERED_BY_TAG_STORIES = 1
        const val PROFILE_FRAGMENT_POSITION = 3
        const val STARTED_FROM_NOTIFICATION = "STARTED_FROM_NOTIFICATION"
        const val PRESELECT_FEED_TYPE = "SHOW_FEED_TYPE"

        fun getIntent(context: Context, feedType: FeedType? = null): Intent {
            val intent = Intent(context, MainActivity::class.java)
            if (feedType != null) intent.putExtra(PRESELECT_FEED_TYPE, feedType)
            return intent
        }

        fun start(context: Context, feedType: FeedType? = null) {
            context.startActivity(getIntent(context, feedType))
        }
    }
}

interface FeedTypePreselect {
    fun getSelection(): LiveData<FeedType>
}


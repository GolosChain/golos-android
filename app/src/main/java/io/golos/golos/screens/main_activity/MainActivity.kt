package io.golos.golos.screens.main_activity

import android.animation.Animator
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.viewpager.widget.ViewPager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.notifications.GolosNotifications
import io.golos.golos.notifications.NOTIFICATION_KEY
import io.golos.golos.notifications.NotificationsBroadCastReceiver
import io.golos.golos.notifications.PostLinkable
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.CreatePostResult
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.editor.EditorActivity
import io.golos.golos.screens.main_activity.adapters.DissmissTouchHelper
import io.golos.golos.screens.main_activity.adapters.MainPagerAdapter
import io.golos.golos.screens.main_activity.adapters.NotificationsAdapter
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.DiscussionActivity
import io.golos.golos.screens.widgets.GolosBottomNavView
import io.golos.golos.utils.*
import java.util.*

class MainActivity : GolosActivity(), Observer<CreatePostResult>, FeedTypePreselect {
    private var lastTimeTapped: Long = Date().time
    private var mDoubleBack = false
    private lateinit var mNotificationsIndicator: TextView
    private lateinit var mButtonContainer: ViewGroup
    private lateinit var mNotificationsContainer: ViewGroup

    private lateinit var mNotificationsRecycler: androidx.recyclerview.widget.RecyclerView
    private val mHandler = Handler()
    private val mSelectLiveData = OneShotLiveData<FeedType>()
    private lateinit var mPager: ViewPager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_tabs)

        findViewById<ViewGroup>(R.id.main_a_frame).setFullAnimationToViewGroup()

        mNotificationsRecycler = findViewById(R.id.notification_recycler)
        mButtonContainer = findViewById(R.id.button_container)
        mNotificationsIndicator = findViewById(R.id.notifications_count_tv)
        mNotificationsContainer = findViewById(R.id.notifications_container)

        mPager = findViewById(R.id.content_pager)
        mPager.adapter = MainPagerAdapter(supportFragmentManager)

        mPager.offscreenPageLimit = 1

        mPager.setPageTransformer(false) { page, pos ->
            val posAbs = Math.abs(pos)
            if (pos > 0.99) page.alpha = 0.0f
            else page.alpha = 1 - posAbs
        }

        val bottomNavView: GolosBottomNavView = findViewById(R.id.bottom_nav_view)
        bottomNavView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.stories -> {
                    mPager.currentItem = STORIES_FRAGMENT_POSITION
                    true
                }
                R.id.groups -> {
                    mPager.currentItem = FILTERED_BY_TAG_STORIES
                    true
                }
                R.id.notifications -> {
                    if (Repository.get.isUserLoggedIn()) {
                        mPager.currentItem = NOTIFICATIONS_HISTORY
                        true
                    } else {
                        bottomNavView.showSnackbar(R.string.must_be_logged_in_for_this_action)
                        false
                    }
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
                    mPager.currentItem = PROFILE_FRAGMENT_POSITION
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

                            DiscussionActivity.start(this, it.author, it.blog, it.permlink, FeedType.UNCLASSIFIED, null)
                        }
                        mHandler.post({ Repository.get.notificationsRepository.dismissNotification(it) })
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
                    if (mButtonContainer.alpha < 1f) {

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
        Repository.get.getUnreadEventsCount().observe(this, Observer {

            bottomNavView.setCounterAt(3, it ?: 0)
        })
        Repository.get.notificationsRepository.notifications.observe(this, object : Observer<GolosNotifications> {
            var lastNotifsCount = 0
            override fun onChanged(t: GolosNotifications?) {
                t ?: return
                if (lastNotifsCount < t.notifications.size) {
                    Repository.get.requestEventsUpdate(null, limit = 15, markAsRead = false)
                }
                lastNotifsCount = t.notifications.size
            }
        })

        checkpreSelect(intent)

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkStartArgsForNotification(intent)
        checkpreSelect(intent)
    }

    override fun onChanged(it: CreatePostResult?) {
        if (it?.isPost == true) {
            DiscussionActivity.start(this,
                    it.author, it.blog, it.permlink, FeedType.UNCLASSIFIED, null)
        }
    }

    override fun getSelection() = mSelectLiveData

    private fun checkpreSelect(intent: Intent?) {
        val feedType = intent?.getSerializableExtra(PRESELECT_FEED_TYPE) as? FeedType
        mSelectLiveData.value = feedType
    }

    private fun checkStartArgsForNotification(intent: Intent?) {

        if (intent?.getIntExtra(STARTED_FROM_NOTIFICATION, 0) != 0) {

            val intentHashCode = intent?.getIntExtra(STARTED_FROM_NOTIFICATION, 0) ?: 0
            val broadcasterIntent = Intent(this, NotificationsBroadCastReceiver::class.java)
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
        const val NOTIFICATIONS_HISTORY = 2
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


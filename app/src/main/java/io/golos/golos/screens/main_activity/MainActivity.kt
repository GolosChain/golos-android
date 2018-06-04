package io.golos.golos.screens.main_activity

import android.animation.Animator
import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.BottomNavigationView
import android.support.v4.view.ViewPager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.model.Notification
import io.golos.golos.notifications.NOTIFICATION_KEY
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.CreatePostResult
import io.golos.golos.repository.model.GolosNotifications
import io.golos.golos.repository.model.PostLinkable
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.editor.EditorActivity
import io.golos.golos.screens.main_activity.adapters.DissmissTouchHelper
import io.golos.golos.screens.main_activity.adapters.MainPagerAdapter
import io.golos.golos.screens.main_activity.adapters.NotificationsAdapter
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.StoryActivity
import io.golos.golos.utils.*
import java.util.*

class MainActivity : GolosActivity(), Observer<CreatePostResult> {
    private var lastTimeTapped: Long = Date().time
    private var mDoubleBack = false
    private lateinit var mNotificationsIndicator: TextView
    private lateinit var mButtonContainer: ViewGroup
    private lateinit var mNotificationsContainer: ViewGroup

    private lateinit var mNotificationsRecycler: RecyclerView
    private val mHandler = Handler()


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

        findViewById<Button>(R.id.add_btn).setOnClickListener {
            val listType = mapper.typeFactory.constructCollectionType(List::class.java, Notification::class.java)
            val notifications = mapper.readValue<List<Notification>>(ntfns, listType)
            Repository.get.notificationsRepository.onReceiveNotifications(notifications)
        }

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
            (mNotificationsRecycler.adapter as? NotificationsAdapter)?.notification = it?.notifications ?: listOf()
            if (it?.notifications?.isEmpty() != false) mNotificationsContainer.animate().alpha(0f).setDuration(200).setListener(object : EndAnimationListener() {
                override fun onAnimationEnd(p0: Animator?) {
                    mNotificationsContainer.setViewGone()
                    mButtonContainer.setViewGone()
                }
            })
            else {
                if (it.notifications.size == 1) {
                    mNotificationsContainer.setViewVisible()
                    if (mButtonContainer.visibility == View.VISIBLE) {
                        mButtonContainer.animate().alpha(0f).setDuration(200).setListener(object : EndAnimationListener() {
                            override fun onAnimationEnd(p0: Animator?) {
                                mButtonContainer.setViewGone()
                            }
                        })
                    }

                } else {
                    mNotificationsContainer.setViewVisible()
                    if (mButtonContainer.visibility != View.VISIBLE) {
                        mButtonContainer.animate().alpha(1f).setDuration(200).setListener(object : EndAnimationListener() {
                            override fun onAnimationEnd(p0: Animator?) {
                                mButtonContainer.setViewVisible()
                            }
                        })
                    }
                    mNotificationsIndicator.text = getString(R.string.show_more_notifications,
                            (it.notifications.count() - 1).toString(),
                            resources.getQuantityString(R.plurals.notifications, it.notifications.count()))
                }
            }
        })
        mNotificationsIndicator.setOnClickListener {
            NotificationsDialog().show(supportFragmentManager, "NotificationsDialog")
        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        checkStartArgsForNotification(intent)
    }

    override fun onChanged(it: CreatePostResult?) {
        if (it?.isPost == true) {
            StoryActivity.start(this,
                    it.author, it.blog, it.permlink, FeedType.UNCLASSIFIED, null)
        }
    }

    private fun checkStartArgsForNotification(intent: Intent?) {
        if (intent?.getIntExtra(STARTED_FROM_NOTIFICATION, 0) != 0) {
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
    }
}

val ntfns = "[\n" +
        "  {\n" +
        "    \"parent_author\": \"yuri-vlad-second\",\n" +
        "    \"parent_permlink\": \"73f6a95e-b346-49d3-b411-8556b9b8f906\",\n" +
        "    \"parent_url\": \"\\/test\\/@yuri-vlad-second\\/73f6a95e-b346-49d3-b411-8556b9b8f906\",\n" +
        "    \"count\": 57,\n" +
        "    \"voter\": {\n" +
        "      \"account\": \"yuri-vlad\"\n" +
        "    },\n" +
        "    \"parent_depth\": 0,\n" +
        "    \"parent_title\": \"235235235\",\n" +
        "    \"type\": \"upvote\",\n" +
        "    \"parent_body\": \"edited\\n\\n\"\n" +
        "  },\n" +
        "  {\n" +
        "    \"parent_author\": \"yuri-vlad-second\",\n" +
        "    \"parent_permlink\": \"asf\",\n" +
        "    \"parent_url\": \"\\/test\\/@yuri-vlad-second\\/asf\",\n" +
        "    \"count\": 1,\n" +
        "    \"voter\": {\n" +
        "      \"account\": \"yuri-vlad\"\n" +
        "    },\n" +
        "    \"parent_depth\": 0,\n" +
        "    \"parent_title\": \"b7f27653-8324-4daa-a46a-752bdf352146\",\n" +
        "    \"type\": \"upvote\",\n" +
        "    \"parent_body\": \"556de2f2-3196-4b5f-81e4-eac7a4ca94ec\"\n" +
        "  },\n" +
        "  {\n" +
        "    \"amount\": \"0.001 GOLOS\",\n" +
        "    \"_to\": \"yuri-vlad-second\",\n" +
        "    \"memo\": \"блабла\",\n" +
        "    \"type\": \"transfer\",\n" +
        "    \"_from\": {\n" +
        "      \"profile_image\": \"https:\\/\\/s10.postimg.org\\/izwpd7gt5\\/9168810_original.jpg\",\n" +
        "      \"account\": \"sualex\"\n" +
        "    }\n" +
        "  },\n" +
        "  {\n" +
        "    \"parent_author\": \"yuri-vlad-second\",\n" +
        "    \"parent_permlink\": \"sdgsdgsdg234234\",\n" +
        "    \"parent_url\": \"\\/test\\/@yuri-vlad-second\\/sdgsdgsdg234234\",\n" +
        "    \"author\": {\n" +
        "      \"account\": \"yuri-vlad\"\n" +
        "    },\n" +
        "    \"count\": 1,\n" +
        "    \"comment_url\": \"\\/test\\/@yuri-vlad-second\\/sdgsdgsdg234234#@yuri-vlad\\/re-yuri-vlad-second-sdgsdgsdg234234-20180418t093157785z\",\n" +
        "    \"parent_depth\": 0,\n" +
        "    \"parent_title\": \"sdgsdgsdg234234\",\n" +
        "    \"type\": \"comment\",\n" +
        "    \"parent_body\": \"sgsdgsdgsdgsdgsdfgsdgsdg2\",\n" +
        "    \"permlink\": \"re-yuri-vlad-second-sdgsdgsdg234234-20180418t093157785z\"\n" +
        "  },\n" +
        "  {\n" +
        "    \"parent_author\": \"yuri-vlad-second\",\n" +
        "    \"parent_permlink\": \"sdgsdgsdg234234\",\n" +
        "    \"parent_url\": \"\\/test\\/@yuri-vlad-second\\/sdgsdgsdg234234\",\n" +
        "    \"author\": {\n" +
        "      \"account\": \"yuri-vlad\"\n" +
        "    },\n" +
        "    \"count\": 1,\n" +
        "    \"comment_url\": \"\\/test\\/@yuri-vlad-second\\/sdgsdgsdg234234#@yuri-vlad\\/re-yuri-vlad-second-sdgsdgsdg234234-20180418t093157785z\",\n" +
        "    \"parent_depth\": 1,\n" +
        "    \"parent_title\": \"sdgsdgsdg234234\",\n" +
        "    \"type\": \"comment\",\n" +
        "    \"parent_body\": \"sgsdgsdgsdgsdgsdfgsdgsdg2\",\n" +
        "    \"permlink\": \"re-yuri-vlad-second-sdgsdgsdg234234-20180418t093157785z\"\n" +
        "  }," +
        "  {\n" +
        "    \"parent_author\": \"yuri-vlad-second\",\n" +
        "    \"parent_permlink\": \"73f6a95e-b346-49d3-b411-8556b9b8f906\",\n" +
        "    \"parent_url\": \"\\/test\\/@yuri-vlad-second\\/73f6a95e-b346-49d3-b411-8556b9b8f906\",\n" +
        "    \"count\": 1,\n" +
        "    \"voter\": {\n" +
        "      \"account\": \"yuri-vlad\"\n" +
        "    },\n" +
        "    \"parent_depth\": 0,\n" +
        "    \"parent_title\": \"235235235\",\n" +
        "    \"type\": \"upvote\",\n" +
        "    \"parent_body\": \"edited\"\n" +
        "  },\n" +
        "  {\n" +
        "    \"parent_author\": \"yuri-vlad-second\",\n" +
        "    \"parent_permlink\": \"sdgsdgsdg234234\",\n" +
        "    \"parent_url\": \"\\/test\\/@yuri-vlad-second\\/sdgsdgsdg234234\",\n" +
        "    \"count\": 1,\n" +
        "    \"weight\": -10000,\n" +
        "    \"voter\": {\n" +
        "      \"account\": \"yuri-vlad\"\n" +
        "    },\n" +
        "    \"parent_depth\": 0,\n" +
        "    \"parent_title\": \"sdgsdgsdg234234\",\n" +
        "    \"type\": \"downvote\",\n" +
        "    \"parent_body\": \"sgsdgsdgsdgsdgsdfgsdgsdg2\"\n" +
        "  },\n" +
        "  {\n" +
        "    \"parent_author\": \"yuri-vlad-second\",\n" +
        "    \"parent_permlink\": \"sdgsdgsdg234234\",\n" +
        "    \"parent_url\": \"\\/test\\/@yuri-vlad-second\\/sdgsdgsdg234234\",\n" +
        "    \"count\": 7,\n" +
        "    \"weight\": -10000,\n" +
        "    \"voter\": {\n" +
        "      \"account\": \"yuri-vlad\"\n" +
        "    },\n" +
        "    \"parent_depth\": 0,\n" +
        "    \"parent_title\": \"sdgsdgsdg234234\",\n" +
        "    \"type\": \"downvote\",\n" +
        "    \"parent_body\": \"sgsdgsdgsdgsdgsdfgsdgsdg2\"\n" +
        "  }\n" +
        "]"
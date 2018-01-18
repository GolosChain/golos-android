package io.golos.golos.screens.userslist

import android.arch.core.executor.testing.InstantTaskExecutorRule
import io.golos.golos.MockPersister
import io.golos.golos.repository.Repository
import io.golos.golos.repository.RepositoryImpl
import io.golos.golos.repository.api.ApiImpl
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.SubscribeStatus
import io.golos.golos.screens.userslist.model.ListType
import io.golos.golos.screens.userslist.model.UserListRowData
import io.golos.golos.utils.InternetStatusNotifier
import io.golos.golos.utils.StringSupplier
import io.golos.golos.utils.UpdatingState
import junit.framework.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executor

/**
 * Created by yuri on 18.01.18.
 */
class UserListViewModelTest {

    @Rule
    @JvmField
    public val rule = InstantTaskExecutorRule()
    val executor: Executor
        get() {
            return Executor { it.run() }
        }
    private lateinit var repo: RepositoryImpl
    private lateinit var mViewModel: UserListViewModel
    @Before
    fun before() {
        repo = RepositoryImpl(
                executor,
                executor,
                MockPersister, ApiImpl(), null
        )
        Repository.setSingletoneInstance(repo)
        mViewModel = UserListViewModel()
        repo.authWithPostingWif("yuri-vlad-second", "5JeKh4taphREBdqfKzfapu6ar3gCNPPKgG5QbUzEwmuasSAQFs3", {})
    }

    @Test
    fun testUserSubscriptions() {


        //current user subscriptions list
        repo.subscribeOnUserBlog("med", { _, _ -> })
        var status: UserListViewState? = null
        mViewModel.getLiveData().observeForever {
            status = it
        }
        Assert.assertNull(status)
        mViewModel.onCreate("yuri-vlad-second", null, ListType.SUBSCRIPTIONS, object : StringSupplier {
            override fun get(id: Int): String {
                return "stub"
            }
        }, object : InternetStatusNotifier {
            override fun isAppOnline(): Boolean {
                return true
            }
        })
        Assert.assertNotNull(status)
        Assert.assertTrue(status!!.users.size > 4)
        val currentSubscriptionsNumber = status!!.users.size
        Assert.assertTrue(status!!.users.find { !it.subscribeStatus.isCurrentUserSubscribed } == null)

        mViewModel.onSubscribeClick(UserListRowData("med", "", "", SubscribeStatus(true, UpdatingState.DONE)))
        Assert.assertEquals("we unsubscribed, so size must decrease ", currentSubscriptionsNumber - 1, status!!.users.size)


        //other user subscriptions
        mViewModel.onCreate("med", null, ListType.SUBSCRIPTIONS, object : StringSupplier {
            override fun get(id: Int): String {
                return "stub"
            }
        }, object : InternetStatusNotifier {
            override fun isAppOnline(): Boolean {
                return true
            }
        })
        Assert.assertTrue(status!!.users.size > 4)
        Assert.assertFalse(status!!.users[3].subscribeStatus.isCurrentUserSubscribed)

        mViewModel.onSubscribeClick(UserListRowData(status!!.users[3].name, "", "", SubscribeStatus(false, UpdatingState.DONE)))
        Assert.assertTrue(status!!.users[3].subscribeStatus.isCurrentUserSubscribed)
        repo.unSubscribeOnUserBlog(status!!.users[3].name, { _, _ -> })

    }

    @Test
    fun getVoters() {
        repo.requestStoriesListUpdate(1, FeedType.ACTUAL, null, null)
        repo.unSubscribeOnUserBlog("pavel.didkovsky", { _, _ -> })
        val story = repo.getStories(FeedType.ACTUAL, null).value!!.items.first().rootStory()!!
        var status: UserListViewState? = null
        mViewModel.getLiveData().observeForever {
            status = it
        }
        Assert.assertNull(status)
        mViewModel.onCreate(null, story.id, ListType.VOTERS, object : StringSupplier {
            override fun get(id: Int): String {
                return "stub"
            }
        }, object : InternetStatusNotifier {
            override fun isAppOnline(): Boolean {
                return true
            }
        })
        Assert.assertNotNull(status)
        Assert.assertTrue(status!!.users.size > 1)

        Assert.assertFalse(status!!.users.first().subscribeStatus.isCurrentUserSubscribed)

        mViewModel.onSubscribeClick(UserListRowData(status!!.users.first().name, "", "", SubscribeStatus(false, UpdatingState.DONE)))

        Assert.assertTrue(status!!.users.first().subscribeStatus.isCurrentUserSubscribed)

        mViewModel.onSubscribeClick(UserListRowData(status!!.users.first().name, "", "", SubscribeStatus(true, UpdatingState.DONE)))

        Assert.assertFalse(status!!.users.first().subscribeStatus.isCurrentUserSubscribed)
    }

}
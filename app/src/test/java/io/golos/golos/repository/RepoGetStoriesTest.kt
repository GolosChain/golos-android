package io.golos.golos.repository

import android.arch.core.executor.testing.InstantTaskExecutorRule
import eu.bittrade.libs.steemj.Golos4J
import eu.bittrade.libs.steemj.base.models.AccountName
import io.golos.golos.MainThreadExecutor
import io.golos.golos.MockPersister
import io.golos.golos.repository.api.ApiImpl
import io.golos.golos.repository.persistence.model.AccountInfo
import io.golos.golos.screens.stories.model.FeedType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executor

class RepoGetStoriesTest {
    val userName = "yuri-vlad-second"
    val publicActive = "GLS7feysP2A87x4uNwn68q13rF3bchD6AKhJWf4CmPWjqQF8vCb5G"
    val privateActive = "5K7YbhJZqGnw3hYzsmH5HbDixWP5ByCBdnJxM5uoe9LuMX5rcZV"
    val publicPosting = "GLS75PefYywHJtG1cDtd4JoF7NRMT7tJig69zn1YytFdbsDHatLju"
    val privatePosting = "5JeKh4taphREBdqfKzfapu6ar3gCNPPKgG5QbUzEwmuasSAQFs3"
    @Rule
    @JvmField
    public val rule = InstantTaskExecutorRule()
    val executor: Executor
        get() {
            return Executor { it.run() }
        }
    private lateinit var repo: RepositoryImpl
    @Before
    fun before() {
        repo = RepositoryImpl(
                MainThreadExecutor,
                MainThreadExecutor,
                MockPersister,
                ApiImpl()
        )
    }

    @Test
    fun getStoriesNotAuthedTest() {
        var stories = repo.getStories(FeedType.NEW)
        repo.requestStoriesListUpdate(20, FeedType.NEW)
        Assert.assertEquals(20, stories.value!!.items.size)
        var size = stories.value!!.items.size
        repo.requestStoriesListUpdate(20,
                FeedType.NEW,
                null,
                stories.value!!.items.last().rootStory()!!.author,
                stories.value!!.items.last().rootStory()!!.permlink)
        Assert.assertTrue(stories.value!!.items.size > size)

        stories = repo.getStories(FeedType.ACTUAL)
        repo.requestStoriesListUpdate(20, FeedType.ACTUAL)
        Assert.assertEquals(20, stories.value!!.items.size)
        size = stories.value!!.items.size
        repo.requestStoriesListUpdate(20,
                FeedType.ACTUAL,
                null,
                stories.value!!.items.last().rootStory()!!.author,
                stories.value!!.items.last().rootStory()!!.permlink)
        Assert.assertTrue(stories.value!!.items.size > size)

        stories = repo.getStories(FeedType.POPULAR)
        repo.requestStoriesListUpdate(20, FeedType.POPULAR)
        Assert.assertEquals(20, stories.value!!.items.size)
        size = stories.value!!.items.size
        repo.requestStoriesListUpdate(20,
                FeedType.POPULAR,
                null,
                stories.value!!.items.last().rootStory()!!.author,
                stories.value!!.items.last().rootStory()!!.permlink)
        Assert.assertTrue(stories.value!!.items.size > size)
    }

    @Test
    fun testGetAuthedStoiries() {
        repo.authWithActiveWif(userName, privateActive, { _ -> })
        var stories = repo.getStories(FeedType.BLOG)
        repo.requestStoriesListUpdate(5, FeedType.BLOG)
        Assert.assertTrue(stories.value!!.items.size > 4)
        var size = stories.value!!.items.size
        repo.requestStoriesListUpdate(20,
                FeedType.BLOG,
                null,
                stories.value!!.items.last().rootStory()!!.author,
                stories.value!!.items.last().rootStory()!!.permlink)
        Assert.assertTrue(stories.value!!.items.size > size)

        stories = repo.getStories(FeedType.PERSONAL_FEED)
        repo.requestStoriesListUpdate(10, FeedType.PERSONAL_FEED)
        Assert.assertTrue(stories.value!!.items.size > 9)
        size = stories.value!!.items.size
        repo.requestStoriesListUpdate(20,
                FeedType.PERSONAL_FEED,
                null,
                stories.value!!.items.last().rootStory()!!.author,
                stories.value!!.items.last().rootStory()!!.permlink)
        Assert.assertTrue(stories.value!!.items.size > size)


        stories = repo.getStories(FeedType.COMMENTS)
        repo.requestStoriesListUpdate(10, FeedType.COMMENTS)
        Assert.assertEquals(10, stories.value!!.items.size)
        size = stories.value!!.items.size
        repo.requestStoriesListUpdate(20,
                FeedType.COMMENTS,
                null,
                stories.value!!.items.last().rootStory()!!.author,
                stories.value!!.items.last().rootStory()!!.permlink)
        Assert.assertTrue(stories.value!!.items.size > size)

    }

    @Test
    fun testGetComments() {
        repo.authWithPostingWif(userName, privatePosting, { _ -> })
        val items = repo.getStories(FeedType.COMMENTS)
        repo.requestStoriesListUpdate(20, FeedType.COMMENTS)
        println(items)
    }

    @Test
    fun testGetNewStories() {
        val items = repo.getStories(FeedType.PROMO)
        repo.requestStoriesListUpdate(20, FeedType.PROMO)
        (0 until 10).forEach {
            repo.requestStoriesListUpdate(20, FeedType.PROMO)
        }
        println(items)
    }

    @Test
    fun testStoryNoBlog() {
        val items = repo.getStories(FeedType.UNCLASSIFIED)
        Assert.assertNull(items.value)
        repo.requestStoryUpdate("sinte",
                "o-socialnykh-psikhopatakh-chast-3-o-tikhonyakh-mechtatelyakh-stesnitelnykh",
                null, FeedType.UNCLASSIFIED)
        Assert.assertNotNull(items.value)
        Assert.assertEquals(1, items.value?.items?.size)
        Assert.assertTrue(items.value?.items!![0].comments().isNotEmpty())
    }

    @Test
    fun loadAvatartest() {
        val items = repo.getStories(FeedType.POPULAR)
        Assert.assertNull(items.value)
        repo.requestStoriesListUpdate(20, FeedType.POPULAR)
        Assert.assertNotNull(items.value)

    }

    @Test
    fun laodPersonalizedTest() {
        var items = repo.getStories(FeedType.COMMENTS, StoryFilter(null, "yuri-vlad-second"))
        Assert.assertNull(items.value)
        repo.requestStoriesListUpdate(5, FeedType.COMMENTS, StoryFilter(null, "yuri-vlad-second"))
        Assert.assertNotNull(items.value)

        items = repo.getStories(FeedType.BLOG, StoryFilter(null, "yuri-vlad-second"))
        Assert.assertNull(items.value)
        repo.requestStoriesListUpdate(5, FeedType.BLOG, StoryFilter(null, "yuri-vlad-second"))
        Assert.assertNotNull(items.value)

        items = repo.getStories(FeedType.PERSONAL_FEED, StoryFilter(null, "yuri-vlad-second"))
        Assert.assertNull(items.value)
        repo.requestStoriesListUpdate(5, FeedType.PERSONAL_FEED, StoryFilter(null, "yuri-vlad-second"))
        Assert.assertNotNull(items.value)

    }

    @Test
    fun testloadComments() {
        var items = repo.getStories(FeedType.COMMENTS, StoryFilter(null, "yuri-vlad"))
        Assert.assertNull(items.value)
        repo.requestStoriesListUpdate(20, FeedType.COMMENTS, StoryFilter(null, "yuri-vlad"))
        Assert.assertNotNull(items.value)
    }

    @Test
    fun testLoadPostsWithFilter() {
        var items = repo.getStories(FeedType.ACTUAL, StoryFilter("psk", null))
        Assert.assertNull(items.value)
        repo.requestStoriesListUpdate(20, FeedType.ACTUAL, StoryFilter("psk", null))
        Assert.assertNotNull(items.value)
    }

    @Test
    fun getAccountInfo() {
        var items = repo.getUserInfo("yuri-vlad")
        Assert.assertNull(items.value)
        var userInfo: AccountInfo? = null
        repo.requestUserInfoUpdate("yuri-vlad", { a, _ ->
            userInfo = a
        })
        Assert.assertNotNull(items.value)
        Assert.assertNotNull(userInfo)

        Assert.assertEquals("yuri-vlad", userInfo!!.userName)
        Assert.assertEquals(null, userInfo!!.userMotto)
        Assert.assertTrue(userInfo!!.activePublicKey.isNotEmpty())
        Assert.assertTrue(userInfo!!.avatarPath == null)
        Assert.assertTrue(userInfo!!.postingPublicKey.isNotEmpty())
        Assert.assertTrue(userInfo!!.accountWorth > 0.0)
        Assert.assertTrue(userInfo!!.gbgAmount > 0)
        Assert.assertTrue(userInfo!!.golosAmount > 0)
        Assert.assertTrue(userInfo!!.golosPower > 0)
        Assert.assertTrue(userInfo!!.postsCount == 2L)
        Assert.assertTrue(userInfo!!.safeGbg > 0)
        Assert.assertTrue(userInfo!!.subscibesCount == 6L)
        Assert.assertTrue(userInfo!!.subscribersCount == 7L)

        repo.authWithPostingWif(userName, postingWif = privatePosting, listener = {})
        items = repo.getUserInfo(userName)
        items.observeForever { userInfo = it }
        Assert.assertNotNull(items.value)
        repo.requestUserInfoUpdate(userName, { a, _ ->
            userInfo = a
        })
        Assert.assertNotNull(items.value)
        Assert.assertNotNull(userInfo)

        Assert.assertEquals(userName, userInfo!!.userName)
        Assert.assertNotNull(userInfo!!.userMotto)
        Assert.assertTrue(userInfo!!.activePublicKey.isNotEmpty())
        Assert.assertNotNull(userInfo!!.avatarPath)
        Assert.assertTrue(userInfo!!.postingPublicKey.isNotEmpty())
        Assert.assertTrue(userInfo!!.accountWorth > 0.0)
        Assert.assertTrue(userInfo!!.gbgAmount > 0)
        Assert.assertTrue(userInfo!!.golosAmount > 0)
        Assert.assertTrue(userInfo!!.golosPower > 0)
        Assert.assertTrue(userInfo!!.postsCount > 10)
        Assert.assertTrue(userInfo!!.safeGbg == 0.0)
        Assert.assertTrue(userInfo!!.subscibesCount == 3L)
        Assert.assertTrue(userInfo!!.subscribersCount == 1L)

        Assert.assertTrue(repo.isUserLoggedIn())
    }

    @Test
    fun testGetSubscribersAndSubscriptions() {
        val wokrkingAcc = "vredinka2345"
        var subs = repo.getSubscribers(wokrkingAcc)
        Assert.assertNull(subs.value)
        val countObject = Golos4J.getInstance().followApiMethods.getFollowCount(AccountName(wokrkingAcc))
        repo.requestSubscribersUpdate(wokrkingAcc, { _, _ -> })
        Assert.assertNotNull(subs.value)
        Assert.assertEquals(countObject.followerCount, subs.value!!.size)
        Assert.assertTrue(subs.value!!.find { it.subscribeStatus.isCurrentUserSubscribed } == null)

        subs = repo.getSubscriptions(wokrkingAcc)
        Assert.assertNull(subs.value)
        repo.requestSubscriptionUpdate(wokrkingAcc, { _, _ -> })
        Assert.assertNotNull(subs.value)
        Assert.assertEquals(countObject.followingCount, subs.value!!.size)
        Assert.assertTrue(subs.value!!.find { it.subscribeStatus.isCurrentUserSubscribed } == null)
    }
}
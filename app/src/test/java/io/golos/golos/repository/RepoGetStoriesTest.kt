package io.golos.golos.repository

import android.arch.core.executor.testing.InstantTaskExecutorRule
import io.golos.golos.repository.api.ApiImpl
import io.golos.golos.repository.persistence.Persister
import io.golos.golos.repository.persistence.model.UserData
import io.golos.golos.screens.stories.model.FeedType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*
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
                executor,
                executor,
                object : Persister() {
                    var users = HashMap<String, String>()
                    var userData: UserData? = null
                    var name: String? = null
                    override fun saveAvatarPathForUser(userName: String, avatarPath: String, updatedDate: Long) {
                        users.put(userName, avatarPath + "__" + updatedDate)

                    }

                    override fun getAvatarForUser(userName: String): Pair<String, Long>? {
                        return null
                    }

                    override fun getActiveUserData(): UserData? = userData

                    override fun saveUserData(userData: UserData) {
                        this.userData = userData
                    }

                    override fun deleteUserData() {
                        userData = null
                    }

                    override fun getCurrentUserName() = userData?.userName

                    override fun saveCurrentUserName(name: String?) {
                        this.name = name
                    }
                }, ApiImpl()
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

        stories = repo.getStories(FeedType.PROMO)
        repo.requestStoriesListUpdate(10, FeedType.PROMO)

        Assert.assertTrue(stories.value!!.items.size > 9)
        size = stories.value!!.items.size
        repo.requestStoriesListUpdate(20,
                FeedType.PROMO,
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
}
package io.golos.golos.repository

import android.arch.core.executor.testing.InstantTaskExecutorRule
import eu.bittrade.libs.steemj.enums.PrivateKeyType
import io.golos.golos.Utils
import io.golos.golos.repository.api.ApiImpl
import io.golos.golos.repository.persistence.Persister
import io.golos.golos.screens.editor.EditorImagePart
import io.golos.golos.screens.editor.EditorTextPart
import io.golos.golos.screens.stories.model.FeedType
import junit.framework.Assert.*
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*
import java.util.concurrent.Executor

/**
 * Created by yuri on 23.11.17.
 */
class RepositoryImplTest {
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
                    var keys = HashMap<PrivateKeyType, String?>()
                    var name: String? = null
                    override fun saveAvatarPathForUser(userName: String, avatarPath: String, updatedDate: Long) {
                        users.put(userName, avatarPath + "__" + updatedDate)

                    }

                    override fun getAvatarForUser(userName: String): Pair<String, Long>? {
                        if (!users.containsKey(userName)) return null
                        val path = users.get(userName)!!.split("__")
                        return Pair(path[0].replace("__", ""),
                                path[1].replace("__", "").toLong())
                    }

                    override fun saveKeys(keys: Map<PrivateKeyType, String?>) {
                        this.keys = HashMap(keys)
                    }

                    override fun getKeys(types: Set<PrivateKeyType>): Map<PrivateKeyType, String?> {
                        val out = HashMap<PrivateKeyType, String?>()
                        keys.forEach { t, u ->
                            if (types.contains(t)) out[t] = u
                        }
                        return out
                    }

                    override fun getCurrentUserName() = name

                    override fun saveCurrentUserName(name: String?) {
                        this.name = name
                    }
                }, ApiImpl()
        )
    }

    @Test
    fun createPost() {
        repo.authWithActiveWif("yuri-vlad-second", privateActive)
        val image = EditorImagePart(imageName = "image", imageUrl = Utils.getFileFromResources("back_rect.png").absolutePath,
                pointerPosition = null)
        repo.createPost(UUID.randomUUID().toString(), listOf(EditorTextPart("sdg", "test content", pointerPosition = null),
                image),
                listOf("ase", "гаврик"), { a, b -> print(b) })

    }

    @Test
    fun testAuth() {
        val authData = repo.getCurrentUserDataAsLiveData()
        Assert.assertNull(authData.value)
        repo.setActiveUserAccount(userName, privateActive, privatePosting)
        assertNotNull(authData.value)
        assertEquals(userName, authData.value!!.userName)
        assertEquals(privateActive, authData.value!!.privateActiveWif)
        assertEquals(privatePosting, authData.value!!.privatePostingWif)
        Assert.assertNull(authData.value!!.avatarPath)
    }

    @Test
    fun testAvatarUpdate() {
        val popular = repo.getStories(FeedType.ACTUAL, null)
        assertNull(popular.value)
        repo.requestStoriesListUpdate(20, FeedType.ACTUAL, null, null, null)
        assertNotNull(popular.value)
        assertTrue(popular.value!!.items.any { it.rootStory()!!.avatarPath != null })
    }

    @Test
    fun createCommentTest() {
        val authData = repo.getCurrentUserDataAsLiveData()
        repo.setActiveUserAccount(userName, privateActive, privatePosting)
        assertNotNull(authData.value)
        val newItems = repo.getStories(FeedType.NEW, null)
        repo.requestStoriesListUpdate(20, FeedType.NEW, null, null, null)
        assertNotNull(newItems.value)
        assertEquals(20, newItems.value!!.items.size)
        var notCommentedItem = newItems.value!!.items.first()
        repo.requestStoryUpdate(notCommentedItem)
        notCommentedItem = newItems.value!!.items.first()
        val image = EditorImagePart(imageName = "image", imageUrl = Utils.getFileFromResources("back_rect.png").absolutePath,
                pointerPosition = null)
        val text = EditorTextPart("sdg", "test content ${UUID.randomUUID()}", pointerPosition = null)
        repo.createComment(notCommentedItem, notCommentedItem.rootStory()!!, listOf(image, text), { _, _ -> })
        assertEquals(notCommentedItem.comments().size + 1, newItems.value!!.items.first().comments().size)
    }

    @Test
    fun createSecondLevelComment() {
        val authData = repo.getCurrentUserDataAsLiveData()
        repo.setActiveUserAccount(userName, privateActive, privatePosting)
        assertNotNull(authData.value)
        val newItems = repo.getStories(FeedType.POPULAR, null)
        repo.requestStoriesListUpdate(20, FeedType.POPULAR, null, null, null)
        assertNotNull(newItems.value)
        assertEquals(20, newItems.value!!.items.size)
        var notCommentedItem = newItems.value!!.items.first()
        repo.requestStoryUpdate(notCommentedItem)

        notCommentedItem = newItems.value!!.items.first()
        var lastComment = newItems.value!!.items.first().comments().last()

        val image = EditorImagePart(imageName = "image", imageUrl = Utils.getFileFromResources("back_rect.png").absolutePath,
                pointerPosition = null)
        val text = EditorTextPart("sdg", "test content ${UUID.randomUUID()}", pointerPosition = null)
        repo.createComment(notCommentedItem, lastComment, listOf(image, text), { _, _ -> })

        assertEquals(notCommentedItem.rootStory()!!.commentsCount + 1, newItems.value!!.items.first().rootStory()!!.commentsCount)
        assertEquals(lastComment.commentsCount + 1, newItems.value!!.items.first().comments().last().commentsCount)

    }

    @Test
    fun testAuthWithPosting() {
        val resp = repo.authWithPostingWif(userName, privatePosting)
        assertNotNull(resp)
        assertNotNull(resp.postingAuth?.second)
        assertNull(resp.activeAuth?.second)
        assertEquals(userName, resp.userName)
        assertNull(resp.avatarPath)
    }

    @Test
    fun testVoting() {
        val popular = repo.getStories(FeedType.POPULAR, null)
        repo.setActiveUserAccount(userName, privateActive, privatePosting)
        assertNull(popular.value)
        repo.requestStoriesListUpdate(20, FeedType.POPULAR, null, null, null)
        assertNotNull(popular.value)
        var votingItem = popular.value?.items?.get(1)!!

        assert(!votingItem.rootStory()!!.isUserUpvotedOnThis)
        repo.requestStoryUpdate(votingItem)

        repo.upVote(votingItem.rootStory()!!, 100)

        votingItem = popular.value?.items?.find { it.rootStory()?.id == votingItem.rootStory()?.id }!!
        assert(votingItem.rootStory()!!.isUserUpvotedOnThis)
        assert(!votingItem.comments().first().isUserUpvotedOnThis)

        repo.cancelVote(votingItem.rootStory()!!)

        votingItem = popular.value?.items?.find { it.rootStory()?.id == votingItem.rootStory()?.id }!!
        assert(!votingItem.rootStory()!!.isUserUpvotedOnThis)
        assert(!votingItem.comments().first().isUserUpvotedOnThis)
    }

    @Test
    fun testVoting2() {
        val popular = repo.getStories(FeedType.POPULAR, null)
        repo.setActiveUserAccount(userName, privateActive, privatePosting)
        assertNull(popular.value)
        repo.requestStoriesListUpdate(20, FeedType.POPULAR, null, null, null)
        assertNotNull(popular.value)
        var votingItem = popular.value?.items?.get(1)!!
        assert(!votingItem.rootStory()!!.isUserUpvotedOnThis)

        repo.upVote(votingItem.rootStory()!!, 100)

        repo.requestStoryUpdate(votingItem)
        votingItem = popular.value?.items?.find { it.rootStory()?.id == votingItem.rootStory()?.id }!!
        assert(votingItem.rootStory()!!.isUserUpvotedOnThis)
        assert(!votingItem.comments().first().isUserUpvotedOnThis)
        Thread.sleep(4000L)

        var actualStoriesWithFilter = repo.getStories(FeedType.POPULAR, StoryFilter(votingItem.rootStory()!!.categoryName))
        assertNotNull(actualStoriesWithFilter)
        assertNull(actualStoriesWithFilter.value)
        repo.requestStoriesListUpdate(20, FeedType.POPULAR, StoryFilter(votingItem.rootStory()!!.categoryName))
        Assert.assertEquals(20, actualStoriesWithFilter.value!!.items.size)
        Assert.assertTrue(actualStoriesWithFilter.value!!.items.find { it.rootStory()!!.id == votingItem.rootStory()!!.id }!!.rootStory()!!.isUserUpvotedOnThis)

        repo.cancelVote(votingItem.rootStory()!!)

        Assert.assertFalse(actualStoriesWithFilter.value!!.items.find { it.rootStory()!!.id == votingItem.rootStory()!!.id }!!.rootStory()!!.isUserUpvotedOnThis)
        Assert.assertFalse(popular.value!!.items.find { it.rootStory()!!.id == votingItem.rootStory()!!.id }!!.rootStory()!!.isUserUpvotedOnThis)

    }

    @Test
    fun testVotingStatus() {
        val popular = repo.getStories(FeedType.ACTUAL, null)
        repo.setActiveUserAccount(userName, privateActive, privatePosting)
        assertNull(popular.value)
        repo.requestStoriesListUpdate(20, FeedType.ACTUAL, null, null, null)
        assertNotNull(popular.value)
        var votingItem = popular.value?.items?.get(5)!!
        repo.upVote(votingItem.rootStory()!!, 100)
        Assert.assertTrue(popular.value!!.items.find { it.rootStory()!!.id == votingItem.rootStory()!!.id }!!.rootStory()!!.isUserUpvotedOnThis)

        repo.requestStoriesListUpdate(20, FeedType.ACTUAL, null, null, null)
        Assert.assertTrue(popular.value!!.items.find { it.rootStory()!!.id == votingItem.rootStory()!!.id }!!.rootStory()!!.isUserUpvotedOnThis)
    }

    @Test
    fun getNewStories() {
        val newStories = repo.getStories(FeedType.NEW, null)
        repo.requestStoriesListUpdate(20, FeedType.NEW, null, null, null)
        assertTrue(newStories.value!!.items.size > 5)
    }

    @Test
    fun getStoriesFilteredByTagTest() {
        val filteredStories = repo.getStories(FeedType.ACTUAL, StoryFilter("psk"))
        assertNotNull(filteredStories)
        assertNull(filteredStories.value)
        repo.requestStoriesListUpdate(20, FeedType.ACTUAL, StoryFilter("psk"))
        assertNotNull(filteredStories.value)
        Assert.assertEquals(20, filteredStories.value!!.items.size)
        Assert.assertFalse(filteredStories.value!!.items.any { !it.rootStory()!!.tags.contains("psk") })

        repo.requestStoriesListUpdate(20, FeedType.ACTUAL, StoryFilter("psk"),
                filteredStories.value!!.items.last().rootStory()!!.author,
                filteredStories.value!!.items.last().rootStory()!!.permlink)

        Assert.assertEquals(39, filteredStories.value!!.items.size)
        Assert.assertFalse(filteredStories.value!!.items.any { !it.rootStory()!!.tags.contains("psk") })
    }

    @Test
    fun testRequestStoryUpdate() {
        val actualStories = repo.getStories(FeedType.POPULAR, null)
        assertNotNull(actualStories)
        assertNull(actualStories.value)
        repo.requestStoriesListUpdate(20, FeedType.POPULAR, null)
        assertNotNull(actualStories.value)
        Assert.assertEquals(20, actualStories.value!!.items.size)

        Assert.assertTrue(actualStories.value!!.items.first().comments().isEmpty())
        repo.requestStoryUpdate(actualStories.value!!.items.first().rootStory()!!.id, FeedType.POPULAR)
        Assert.assertTrue(actualStories.value!!.items.first().comments().isNotEmpty())

        var updatingSoty = actualStories.value!!.items[1].rootStory()!!
        var actualStoriesWithFilter = repo.getStories(FeedType.POPULAR, StoryFilter(updatingSoty.categoryName))
        assertNotNull(actualStoriesWithFilter)
        assertNull(actualStoriesWithFilter.value)
        repo.requestStoriesListUpdate(20, FeedType.POPULAR, StoryFilter(updatingSoty.categoryName))
        Assert.assertEquals(20, actualStoriesWithFilter.value!!.items.size)

        Assert.assertTrue(actualStories.value!!.items[1].comments().isEmpty())
        Assert.assertTrue(actualStoriesWithFilter.value!!.items.find { it.rootStory()!!.id == updatingSoty.id }!!.comments().isEmpty())

        repo.requestStoryUpdate(updatingSoty.id, FeedType.POPULAR)
        Assert.assertTrue(actualStories.value!!.items[1].comments().isNotEmpty())
        Assert.assertTrue(actualStoriesWithFilter.value!!.items.find { it.rootStory()!!.id == updatingSoty.id }!!.comments().isNotEmpty())

        actualStoriesWithFilter = repo.getStories(FeedType.POPULAR, StoryFilter("psk"))
        assertNotNull(actualStoriesWithFilter)
        assertNull(actualStoriesWithFilter.value)
        repo.requestStoriesListUpdate(20, FeedType.POPULAR, StoryFilter("psk"))


        updatingSoty = actualStoriesWithFilter.value!!.items[1].rootStory()!!
        Assert.assertTrue(actualStoriesWithFilter.value!!.items[1].comments().isEmpty())
        repo.requestStoryUpdate(updatingSoty.id, FeedType.POPULAR)
        Assert.assertTrue(actualStoriesWithFilter.value!!.items[1].comments().isNotEmpty())
    }
}
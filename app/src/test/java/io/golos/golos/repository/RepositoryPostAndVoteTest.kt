package io.golos.golos.repository

import android.arch.core.executor.testing.InstantTaskExecutorRule
import io.golos.golos.MockPersister
import io.golos.golos.Utils
import io.golos.golos.repository.api.ApiImpl
import io.golos.golos.repository.model.FollowUserObject
import io.golos.golos.repository.persistence.model.AccountInfo
import io.golos.golos.repository.persistence.model.UserData
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
class RepositoryPostAndVoteTest {
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
                MockPersister, ApiImpl(), null
        )
        repo.authWithActiveWif(userName, activeWif = privateActive, listener = { _ -> })
    }

    @Test
    fun testAuth() {
        val authData = repo.getCurrentUserDataAsLiveData()
        val resp = authData.value!!
        assertNotNull(resp)
        assertNotNull(resp.privateActiveWif)
        assertNull(resp.privatePostingWif)
        assertEquals(userName, resp.userName)
        assertNotNull(resp.avatarPath)
        assertNotNull(resp.getmMoto())
        assertTrue(resp.postsCount != 0L)
        assertTrue(resp.subscibesCount != 0L)
        assertTrue(resp.subscribersCount != 0L)
        assertTrue(resp.gbgAmount != 0.0)
        assertTrue(resp.golosAmount != 0.0)
        assertTrue(resp.golosPower != 0.0)
        assertTrue(resp.subscibesCount > resp.subscribersCount)
    }

    @Test
    fun createPost() {
        val image = EditorImagePart(imageName = "image", imageUrl = Utils.getFileFromResources("back_rect.png").absolutePath,
                pointerPosition = null)
        repo.createPost(UUID.randomUUID().toString(), listOf(EditorTextPart("sdg", "test content", pointerPosition = null),
                image),
                listOf("ase", "гаврик"), { a, b -> print(b) })

    }


    @Test
    fun testAvatarUpdate() {
        val popular = repo.getStories(FeedType.ACTUAL, null)
        assertNull(popular.value)
        repo.requestStoriesListUpdate(20, FeedType.ACTUAL, null, null, null, { _, _ -> })
        assertNotNull(popular.value)
        assertTrue(popular.value!!.items.any { it.rootStory()!!.avatarPath != null })
    }

    @Test
    fun createCommentTest() {
        val authData = repo.getCurrentUserDataAsLiveData()
        assertNotNull(authData.value)
        val newItems = repo.getStories(FeedType.ACTUAL, null)
        repo.requestStoriesListUpdate(20, FeedType.ACTUAL, null, null, null)
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

        assertNotNull(repo.lastCreatedPost().value)
        assertEquals(userName, repo.lastCreatedPost().value!!.author)
    }

    @Test
    fun createSecondLevelComment() {
        Thread.sleep(20000)
        val authData = repo.getCurrentUserDataAsLiveData()
        assertNotNull(authData.value)
        val newItems = repo.getStories(FeedType.POPULAR, null)
        repo.requestStoriesListUpdate(20, FeedType.POPULAR, null, null, null, { _, _ -> })
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
        repo.authWithPostingWif(userName, privatePosting, { resp ->
            assertNotNull(resp)
            assertNotNull(resp.postingAuth?.second)
            assertNull(resp.activeAuth?.second)
            assertEquals(userName, resp.accountInfo.userName)
            assertNotNull(resp.accountInfo.avatarPath)
            assertNotNull(resp.accountInfo.userMotto)
            assertTrue(resp.accountInfo.postsCount != 0L)
            assertTrue(resp.accountInfo.subscibesCount != 0L)
            assertTrue(resp.accountInfo.subscribersCount != 0L)
            assertTrue(resp.accountInfo.gbgAmount != 0.0)
            assertTrue(resp.accountInfo.golosAmount != 0.0)
            assertTrue(resp.accountInfo.golosPower != 0.0)
        })

    }

    @Test
    fun testVoting() {
        val popular = repo.getStories(FeedType.POPULAR, null)
        assertNull(popular.value)
        repo.requestStoriesListUpdate(20, FeedType.POPULAR, null, null, null)
        assertNotNull(popular.value)
        var votingItem = popular.value?.items?.get(2)!!

        assert(!votingItem.rootStory()!!.isUserUpvotedOnThis)
        repo.requestStoryUpdate(votingItem)

        repo.upVote(votingItem.rootStory()!!, 100)

        votingItem = popular.value?.items?.find { it.rootStory()?.id == votingItem.rootStory()?.id }!!
        assert(votingItem.rootStory()!!.isUserUpvotedOnThis)
        assert(!votingItem.comments().first().isUserUpvotedOnThis)

        Thread.sleep(3000)
        repo.cancelVote(votingItem.rootStory()!!)

        votingItem = popular.value?.items?.find { it.rootStory()?.id == votingItem.rootStory()?.id }!!
        assert(!votingItem.rootStory()!!.isUserUpvotedOnThis)
        assert(!votingItem.comments().first().isUserUpvotedOnThis)
    }


    @Test
    fun testVoting2() {
        val popular = repo.getStories(FeedType.POPULAR, null)
        assertNull(popular.value)
        repo.requestStoriesListUpdate(20, FeedType.POPULAR, null, null, null)
        assertNotNull(popular.value)
        var votingItem = popular.value?.items?.get(2)!!
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
        repo.requestStoriesListUpdate(20, FeedType.POPULAR, StoryFilter(votingItem.rootStory()!!.categoryName), complitionHandler = { _, _ -> })
        Assert.assertTrue(actualStoriesWithFilter.value!!.items.size > 2)
        Assert.assertTrue(actualStoriesWithFilter.value!!.items.find { it.rootStory()!!.id == votingItem.rootStory()!!.id }!!.rootStory()!!.isUserUpvotedOnThis)

        repo.cancelVote(votingItem.rootStory()!!)

        Assert.assertFalse(actualStoriesWithFilter.value!!.items.find { it.rootStory()!!.id == votingItem.rootStory()!!.id }!!.rootStory()!!.isUserUpvotedOnThis)
        Assert.assertFalse(popular.value!!.items.find { it.rootStory()!!.id == votingItem.rootStory()!!.id }!!.rootStory()!!.isUserUpvotedOnThis)
    }

    @Test
    fun testVoting3() {
        val feedItems = repo.getStories(FeedType.PERSONAL_FEED, StoryFilter(null, userName))
        assertNull(feedItems.value)
        repo.requestStoriesListUpdate(20, FeedType.PERSONAL_FEED,
                StoryFilter(null, userName),
                null,
                null)
        assertNotNull(feedItems.value)
        var votingItem = feedItems.value?.items?.get(2)!!

        assert(!votingItem.rootStory()!!.isUserUpvotedOnThis)

        repo.upVote(votingItem.rootStory()!!, 100)

        votingItem = feedItems.value?.items?.find { it.rootStory()?.id == votingItem.rootStory()?.id }!!
        assert(votingItem.rootStory()!!.isUserUpvotedOnThis)
    }

    @Test
    fun testVotingStatus() {
        val popular = repo.getStories(FeedType.ACTUAL, null)
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
        repo.requestStoriesListUpdate(20, FeedType.ACTUAL, StoryFilter("psk"), complitionHandler = { _, _ -> })
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
        repo.requestStoriesListUpdate(20, FeedType.POPULAR, null, complitionHandler = { _, _ -> })
        assertNotNull(actualStories.value)
        Assert.assertEquals(20, actualStories.value!!.items.size)

        Assert.assertTrue(actualStories.value!!.items.first().comments().isEmpty())

        val firstStory = actualStories.value!!.items.first().rootStory()!!
        repo.requestStoryUpdate(firstStory.author, firstStory.permlink, firstStory.categoryName, FeedType.POPULAR)
        Assert.assertTrue(actualStories.value!!.items.first().comments().isNotEmpty())

        var updatingSoty = actualStories.value!!.items[2].rootStory()!!
        var actualStoriesWithFilter = repo.getStories(FeedType.POPULAR, StoryFilter(updatingSoty.categoryName))
        assertNotNull(actualStoriesWithFilter)
        assertNull(actualStoriesWithFilter.value)
        repo.requestStoriesListUpdate(20, FeedType.POPULAR, StoryFilter(updatingSoty.categoryName), complitionHandler = { _, _ -> })
        Assert.assertTrue(actualStoriesWithFilter.value!!.items.size > 2)

        Assert.assertTrue(actualStories.value!!.items[1].comments().isEmpty())
        Assert.assertTrue(actualStoriesWithFilter.value!!.items.find { it.rootStory()!!.id == updatingSoty.id }!!.comments().isEmpty())

        repo.requestStoryUpdate(updatingSoty.author, updatingSoty.permlink, updatingSoty.categoryName, FeedType.POPULAR)
        Assert.assertTrue(actualStories.value!!.items[2].comments().isNotEmpty())
        Assert.assertTrue(actualStoriesWithFilter.value!!.items.find { it.rootStory()!!.id == updatingSoty.id }!!.comments().isNotEmpty())

        actualStoriesWithFilter = repo.getStories(FeedType.POPULAR, StoryFilter("psk"))
        assertNotNull(actualStoriesWithFilter)
        assertNull(actualStoriesWithFilter.value)
        repo.requestStoriesListUpdate(20, FeedType.POPULAR, StoryFilter("psk"), complitionHandler = { _, _ -> })


        updatingSoty = actualStoriesWithFilter.value!!.items[1].rootStory()!!
        Assert.assertTrue(actualStoriesWithFilter.value!!.items[1].comments().isEmpty())

        repo.requestStoryUpdate(updatingSoty.author, updatingSoty.permlink, updatingSoty.categoryName, FeedType.POPULAR)
        Assert.assertTrue(actualStoriesWithFilter.value!!.items[1].comments().isNotEmpty())
    }

    @Test
    fun testFollowAndUnfollow() {
        val workingAccount = "vredinka2345"
        var vredinkaAccountInfo: AccountInfo? = null
        var userAcc: UserData? = null
        var subscribesStatus: List<FollowUserObject>? = null

        repo.getSubscribersToUserBlog(workingAccount).observeForever {
            subscribesStatus = it
        }
        repo.getUserInfo(workingAccount).observeForever {
            vredinkaAccountInfo = it
        }
        repo.getCurrentUserDataAsLiveData().observeForever({
            userAcc = it
        })
        repo.follow("golosmedia", { _, _ -> })

        Assert.assertNull(vredinkaAccountInfo)
        Assert.assertNull(subscribesStatus)

        repo.requestUserInfoUpdate(workingAccount, { _, _ -> })
        repo.requestSubscribersUpdate(workingAccount, { _, _ -> })
        repo.unFollow(subscribesStatus!!.first().name, { _, _ -> })
        repo.unFollow(workingAccount, { _, _ -> })
        repo.requestSubscribersUpdate(workingAccount, { _, _ -> })

        Assert.assertNotNull(vredinkaAccountInfo)
        Assert.assertNotNull(userAcc)
        Assert.assertNotNull(subscribesStatus)


        Assert.assertEquals(false, vredinkaAccountInfo!!.isCurrentUserSubscribed)
        val subscibesCount = vredinkaAccountInfo!!.subscribersCount
        val userSubsCount = userAcc!!.subscibesCount

        repo.follow(workingAccount, { _, _ -> })
        Assert.assertEquals(true, vredinkaAccountInfo!!.isCurrentUserSubscribed)
        Assert.assertEquals(subscibesCount + 1, vredinkaAccountInfo!!.subscribersCount)
        Assert.assertEquals(userSubsCount + 1, userAcc!!.subscibesCount)
        Assert.assertTrue(subscribesStatus!!.find { it.name == userName } != null)

        val firstInSubscribersList = subscribesStatus!!.first()


        repo.follow(firstInSubscribersList.name, { _, _ -> })
        Assert.assertEquals(userSubsCount + 2, userAcc!!.subscibesCount)
        Assert.assertTrue(subscribesStatus!!.first().subscribeStatus.isCurrentUserSubscribed)
        repo.unFollow(firstInSubscribersList.name, { _, _ -> })
        Assert.assertEquals(userSubsCount + 1, userAcc!!.subscibesCount)

        repo.unFollow(workingAccount, { _, _ -> })

        Assert.assertEquals(false, vredinkaAccountInfo!!.isCurrentUserSubscribed)

        repo.requestSubscribersUpdate(workingAccount, { _, _ -> })
        Assert.assertEquals(subscibesCount, vredinkaAccountInfo!!.subscribersCount)

        Assert.assertEquals(userSubsCount, userAcc!!.subscibesCount)
        Assert.assertTrue(subscribesStatus!!.find { it.name == userName } == null)

        repo.follow("med", { _, _ -> })
        repo.unFollow("med", { _, _ -> })

        val feedStories = repo.getStories(FeedType.PERSONAL_FEED, StoryFilter(userNameFilter = userName))
        Assert.assertNull(feedStories.value)
        repo.requestStoriesListUpdate(20, FeedType.PERSONAL_FEED, StoryFilter(userNameFilter = userName), complitionHandler = { _, _ -> })
        Assert.assertNotNull(feedStories.value)

        Assert.assertTrue(feedStories.value!!.items.filter { it.rootStory()!!.author == "golosmedia" }.first().userSubscribeUpdatingStatus.isCurrentUserSubscribed)
        repo.unFollow("golosmedia", { _, _ -> })
        Assert.assertFalse(feedStories.value!!.items.filter { it.rootStory()!!.author == "golosmedia" }.first().userSubscribeUpdatingStatus.isCurrentUserSubscribed)

        val story = feedStories.value!!.items.filter { it.rootStory()!!.author == "golosmedia" }.first()
        repo.requestStoryUpdate(story)
        Assert.assertFalse(feedStories.value!!.items.filter { it.rootStory()!!.author == "golosmedia" }.first().userSubscribeUpdatingStatus.isCurrentUserSubscribed)
        repo.follow("golosmedia", { _, _ -> })
        Assert.assertTrue(feedStories.value!!.items.filter { it.rootStory()!!.author == "golosmedia" }.first().userSubscribeUpdatingStatus.isCurrentUserSubscribed)

    }
}
package io.golos.golos.screens.story

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.golos.golos.MockPersister
import io.golos.golos.MockUserSettings
import io.golos.golos.repository.Repository
import io.golos.golos.repository.RepositoryImpl
import io.golos.golos.repository.api.ApiImpl
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryViewState
import io.golos.golos.utils.GolosLinkMatcher
import io.golos.golos.utils.Htmlizer
import io.golos.golos.utils.InternetStatusNotifier
import io.golos.golos.utils.StoryLinkMatch
import junit.framework.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executor

/**
 * Created by yuri on 13.12.17.
 */
class DiscussionViewModelTest {
    @Rule
    @JvmField
    public val rule = InstantTaskExecutorRule()
    private lateinit var repo: RepositoryImpl
    private lateinit var discussionViewModel: DiscussionViewModel
    val executor: Executor
        get() {
            return Executor { it.run() }
        }

    @Before
    fun before() {
        repo = RepositoryImpl(
                executor,
                executor,
                executor,
                MockPersister,
                ApiImpl(),
                mLogger = null,
                mUserSettings = MockUserSettings,
                mHtmlizer = object : Htmlizer {
                    override fun toHtml(input: String): CharSequence {
                        return input
                    }
                }
        )
        Repository.setSingletoneInstance(repo)
        discussionViewModel = DiscussionViewModel()
        discussionViewModel.mRepository = repo
    }

    @Test
    fun onCreate() {
        var state: StoryViewState? = null
        discussionViewModel.storyLiveData.observeForever { t -> state = t }
        Assert.assertNull(state)
        val result = GolosLinkMatcher.match("https://goldvoice.club/@sinte/o-socialnykh-psikhopatakh-chast-3-o-tikhonyakh-mechtatelyakh-stesnitelnykh/") as StoryLinkMatch
        var story = repo.getStories(FeedType.UNCLASSIFIED, null)
        repo.requestStoryUpdate(result.author, result.permlink, result.blog,,, FeedType.UNCLASSIFIED) { _, e -> }
        discussionViewModel.onCreate(result.author, result.permlink, result.blog, FeedType.UNCLASSIFIED, null, object : InternetStatusNotifier {
            override fun isAppOnline(): Boolean {
                return true
            }
        })
        Assert.assertNotNull(state)
    }

    @Test
    fun requestPersonalPage() {
        val stories = repo.getStories(FeedType.PERSONAL_FEED, StoryFilter(userNameFilter = "yuri-vlad-second"))
        Assert.assertNull(stories.value)
        repo.requestStoriesListUpdate(20, FeedType.PERSONAL_FEED, StoryFilter(userNameFilter = "yuri-vlad-second"), completionHandler = { _, _ -> })
        Assert.assertNotNull(stories.value)
        var state: StoryViewState? = null
        discussionViewModel.storyLiveData.observeForever { t -> state = t }
        Assert.assertNull(state)
        val story = stories.value!!.items.first().rootStory()!!
        discussionViewModel.onCreate(story.author, story.permlink,
                story.categoryName, FeedType.PERSONAL_FEED, StoryFilter(userNameFilter = "yuri-vlad-second"), object : InternetStatusNotifier {
            override fun isAppOnline(): Boolean {
                return true
            }
        })
        Assert.assertNotNull(state!!.storyTree)
        Assert.assertTrue(state?.storyTree?.comments()?.size ?: 0 > 1)

    }

    @Test
    fun testSubscribe() {
        repo.authWithPostingWif("yuri-vlad-second", "5JeKh4taphREBdqfKzfapu6ar3gCNPPKgG5QbUzEwmuasSAQFs3", {})
        repo.unSubscribeOnUserBlog("sinte", { _, _ -> })

        var state: StoryViewState? = null
        discussionViewModel.storyLiveData.observeForever { t -> state = t }
        Assert.assertNull(state)

        val result = GolosLinkMatcher.match("https://goldvoice.club/@sinte/o-socialnykh-psikhopatakh-chast-3-o-tikhonyakh-mechtatelyakh-stesnitelnykh/") as StoryLinkMatch
        discussionViewModel.onCreate(result.author, result.permlink, result.blog, FeedType.UNCLASSIFIED, null, object : InternetStatusNotifier {
            override fun isAppOnline(): Boolean {
                return true
            }
        })

        Assert.assertNotNull("we must get parsed story", state)
        Assert.assertFalse(state!!.subscribeOnStoryAuthorStatus.isCurrentUserSubscribed)
        Assert.assertFalse(state!!.subscribeOnTagStatus.isCurrentUserSubscribed)

        discussionViewModel.onSubscribeToMainTagClick()
        discussionViewModel.onSubscribeToBlogButtonClick()

        Assert.assertTrue(state!!.subscribeOnStoryAuthorStatus.isCurrentUserSubscribed)
        Assert.assertTrue(state!!.subscribeOnTagStatus.isCurrentUserSubscribed)
    }

}
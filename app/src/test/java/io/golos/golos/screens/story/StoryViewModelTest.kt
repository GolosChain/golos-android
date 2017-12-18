package io.golos.golos.screens.story

import android.arch.core.executor.testing.InstantTaskExecutorRule
import io.golos.golos.MainThreadExecutor
import io.golos.golos.MockPersister
import io.golos.golos.repository.Repository
import io.golos.golos.repository.RepositoryImpl
import io.golos.golos.repository.StoryFilter
import io.golos.golos.repository.api.ApiImpl
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryViewState
import io.golos.golos.utils.GolosLinkMatcher
import io.golos.golos.utils.StoryLinkMatch
import junit.framework.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Created by yuri on 13.12.17.
 */
class StoryViewModelTest {
    @Rule
    @JvmField
    public val rule = InstantTaskExecutorRule()
    private lateinit var repo: RepositoryImpl
    private lateinit var storyViewModel: StoryViewModel
    @Before
    fun before() {
        repo = RepositoryImpl(
                MainThreadExecutor,
                MainThreadExecutor,
                MockPersister, ApiImpl()
        )
        Repository.setSingletoneInstance(repo)
        storyViewModel = StoryViewModel()
        storyViewModel.mRepository = repo
    }

    @Test
    fun onCreate() {
        var state: StoryViewState? = null
        storyViewModel.liveData.observeForever { t -> state = t }
        Assert.assertNull(state)
        val result = GolosLinkMatcher.match("https://goldvoice.club/@sinte/o-socialnykh-psikhopatakh-chast-3-o-tikhonyakh-mechtatelyakh-stesnitelnykh/") as StoryLinkMatch
        var story = repo.getStories(FeedType.UNCLASSIFIED, null)
        repo.requestStoryUpdate(result.author, result.permlink, result.blog, FeedType.UNCLASSIFIED)
        storyViewModel.onCreate(result.author, result.permlink, result.blog, FeedType.UNCLASSIFIED, null)
        Assert.assertNotNull(state)
    }

    @Test
    fun requestPersonalPage() {
        val stories = repo.getStories(FeedType.PERSONAL_FEED, StoryFilter(userNameFilter = "yuri-vlad-second"))
        Assert.assertNull(stories.value)
        repo.requestStoriesListUpdate(20, FeedType.PERSONAL_FEED, StoryFilter(userNameFilter = "yuri-vlad-second"))
        Assert.assertNotNull(stories.value)
        var state: StoryViewState? = null
        storyViewModel.liveData.observeForever { t -> state = t }
        Assert.assertNull(state)
        val story = stories.value!!.items.first().rootStory()!!
        storyViewModel.onCreate(story.author, story.permlink,
                story.categoryName, FeedType.PERSONAL_FEED, StoryFilter(userNameFilter = "yuri-vlad-second"))
        Assert.assertNotNull(state!!.storyTree)
        Assert.assertTrue(state?.storyTree?.comments()?.size ?: 0 > 1)

    }

}
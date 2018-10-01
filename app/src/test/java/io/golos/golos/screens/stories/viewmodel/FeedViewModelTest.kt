package io.golos.golos.screens.stories.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.golos.golos.MainThreadExecutor
import io.golos.golos.MockPersister
import io.golos.golos.repository.Repository
import io.golos.golos.repository.RepositoryImpl
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.repository.api.ApiImpl
import io.golos.golos.utils.InternetStatusNotifier
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Created by yuri on 15.12.17.
 */
class FeedViewModelTest {
    @Rule
    @JvmField
    public val rule = InstantTaskExecutorRule()
    private lateinit var repo: RepositoryImpl
    private lateinit var storyViewModel: FeedViewModel
    @Before
    fun before() {
        repo = RepositoryImpl(
                MainThreadExecutor,
                MainThreadExecutor ,
                MainThreadExecutor, MockPersister, ApiImpl(), mLogger = null
        )
        Repository.setSingletoneInstance(repo)
        storyViewModel = FeedViewModel()
    }

    @Test
    fun testGetComments() {
        val filter = StoryFilter(null, "yuri-vlad-second")
        var state: StoriesViewState? = null
        storyViewModel.storiesLiveData.observeForever { state = it }
        Assert.assertNull(state)
        storyViewModel.onCreate(object : InternetStatusNotifier {
            override fun isAppOnline(): Boolean {
                return true
            }
        }, filter)
        storyViewModel.onChangeVisibilityToUser(true)
        Assert.assertNotNull(state)
        Assert.assertEquals(false, state!!.isLoading)
        Assert.assertEquals(null, state!!.fullscreenMessage)
        Assert.assertEquals(null, state!!.popupMessage)
        Assert.assertEquals(20, state!!.items.size)

    }
}
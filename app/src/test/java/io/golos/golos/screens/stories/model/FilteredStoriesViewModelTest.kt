package io.golos.golos.screens.stories.model

import android.arch.core.executor.testing.InstantTaskExecutorRule
import io.golos.golos.MainThreadExecutor
import io.golos.golos.MockPersister
import io.golos.golos.repository.Repository
import io.golos.golos.repository.RepositoryImpl
import io.golos.golos.repository.api.ApiImpl
import junit.framework.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Created by yuri on 11.01.18.
 */
class FilteredStoriesViewModelTest {
    @Rule
    @JvmField
    public val rule = InstantTaskExecutorRule()
    private lateinit var repo: RepositoryImpl
    private lateinit var storyViewModel: FilteredStoriesViewModel
    @Before
    fun before() {
        repo = RepositoryImpl(
                MainThreadExecutor,
                MainThreadExecutor ,
                MainThreadExecutor, MockPersister, ApiImpl(), mLogger = null
        )
        Repository.setSingletoneInstance(repo)
        storyViewModel = FilteredStoriesViewModel()
    }

    @Test
    fun onCreate() {
        var mState: FilteredStoriesViewState? = null
        storyViewModel.getLiveData().observeForever {
            mState = it
        }
        Assert.assertNull(mState)
        storyViewModel.onCreate("psk")
        Assert.assertNotNull(mState)
        Assert.assertTrue(!mState!!.isUserSubscribedOnThisTag)
        Assert.assertTrue(mState!!.mainTag.tag.topPostsCount > 0)
        Assert.assertTrue(mState!!.postsCount ?: 0 > 0)
        Assert.assertTrue(mState!!.similarTags.isNotEmpty())

        storyViewModel.onMainTagSubscribe()

        Assert.assertTrue(mState!!.isUserSubscribedOnThisTag)
        Assert.assertTrue(mState!!.mainTag.tag.topPostsCount > 0)
        Assert.assertTrue(mState!!.postsCount ?: 0 > 0)
        Assert.assertTrue(mState!!.similarTags.isNotEmpty())

        storyViewModel.onTagUnsubscribe()
        Assert.assertTrue(!mState!!.isUserSubscribedOnThisTag)
        Assert.assertTrue(mState!!.mainTag.tag.topPostsCount > 0)
        Assert.assertTrue(mState!!.postsCount ?: 0 > 0)
        Assert.assertTrue(mState!!.similarTags.isNotEmpty())

    }

}
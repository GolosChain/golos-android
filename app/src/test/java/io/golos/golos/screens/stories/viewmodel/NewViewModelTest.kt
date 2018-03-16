package io.golos.golos.screens.stories.viewmodel

import android.arch.core.executor.testing.InstantTaskExecutorRule
import io.golos.golos.MainThreadExecutor
import io.golos.golos.MockPersister
import io.golos.golos.repository.Repository
import io.golos.golos.repository.RepositoryImpl
import io.golos.golos.repository.api.ApiImpl
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.utils.InternetStatusNotifier
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Created by yuri on 12.01.18.
 */
class NewViewModelTest {
    @Rule
    @JvmField
    public val rule = InstantTaskExecutorRule()
    private lateinit var repo: RepositoryImpl
    private lateinit var storyViewModel: NewViewModel
    @Before
    fun before() {
        repo = RepositoryImpl(
                MainThreadExecutor,
                MainThreadExecutor,
                MainThreadExecutor, MockPersister, ApiImpl(), mLogger = null
        )
        Repository.setSingletoneInstance(repo)
        storyViewModel = NewViewModel()
    }

    @Test
    fun testOnCreate() {
        storyViewModel.onCreate(object : InternetStatusNotifier {
            override fun isAppOnline(): Boolean {
                return true
            }
        }, StoryFilter(listOf("psk", "ru--foto")))
        var svm: StoriesViewState? = null
        storyViewModel.storiesLiveData.observeForever {
            svm = it
        }
        Assert.assertNull(svm)
        storyViewModel.onChangeVisibilityToUser(true)
        Assert.assertNotNull(svm)
        Assert.assertTrue(svm!!.items.size > 1)
    }
}
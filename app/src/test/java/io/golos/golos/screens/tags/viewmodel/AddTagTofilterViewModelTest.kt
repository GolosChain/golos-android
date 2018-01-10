package io.golos.golos.screens.tags.viewmodel

import android.arch.core.executor.testing.InstantTaskExecutorRule
import io.golos.golos.MainThreadExecutor
import io.golos.golos.MockPersister
import io.golos.golos.repository.Repository
import io.golos.golos.repository.RepositoryImpl
import io.golos.golos.repository.api.ApiImpl
import io.golos.golos.screens.tags.model.LocalizedTag
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Created by yuri on 09.01.18.
 */
class AddTagTofilterViewModelTest {
    @Rule
    @JvmField
    public val rule = InstantTaskExecutorRule()
    private lateinit var repo: RepositoryImpl
    private lateinit var storyViewModel: AddTagTofilterViewModel
    @Before
    fun before() {
        repo = RepositoryImpl(
                MainThreadExecutor,
                MainThreadExecutor,
                MockPersister, ApiImpl(), null
        )
        Repository.setSingletoneInstance(repo)
        storyViewModel = AddTagTofilterViewModel()
    }

    @Test
    fun onCreate() {
        var allTags: List<LocalizedTag> = ArrayList()
        var subscribedTags: List<LocalizedTag> = ArrayList()
        storyViewModel.getLiveData().observeForever {
            allTags = it?.shownTags ?: ArrayList()
            subscribedTags = it?.subscribedTags ?: ArrayList()
        }
        Assert.assertTrue(allTags.isEmpty())
        Assert.assertTrue(subscribedTags.isEmpty())

        storyViewModel.onCreate()

        Assert.assertTrue(allTags.isNotEmpty())
        Assert.assertTrue(subscribedTags.isEmpty())

        storyViewModel.onTagSubscribe(allTags.first())

        Assert.assertTrue(allTags.isNotEmpty())
        Assert.assertTrue(subscribedTags.size == 1)
        Assert.assertTrue(!allTags.contains(subscribedTags[0]))

        var tag = subscribedTags[0]

        storyViewModel.onTagUnSubscribe(tag)
        Assert.assertTrue(allTags.isNotEmpty())
        Assert.assertTrue(allTags.contains(tag))
        Assert.assertTrue(subscribedTags.isEmpty())

        storyViewModel.onTagSubscribe(allTags.first())
        storyViewModel.onSearchStart()
        Assert.assertTrue(allTags.isEmpty())
        storyViewModel.search(tag.tag.name.substring(0, 2))

        Assert.assertTrue(allTags.isNotEmpty())
        Assert.assertTrue(!allTags.contains(tag))

        storyViewModel.search("апвот")
        Assert.assertTrue(allTags.isNotEmpty())
        tag = allTags[0]

        Assert.assertTrue(allTags[0].tag.name == "ru--apvot50-50")
        storyViewModel.onTagSubscribe(tag)
        Assert.assertTrue(allTags[0].tag.name!= "ru--apvot50-50")

        storyViewModel.onTagUnSubscribe(tag)
        Assert.assertTrue(allTags[0].tag.name == "ru--apvot50-50")

    }
}
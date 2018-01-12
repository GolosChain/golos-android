package io.golos.golos.screens.tags.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import io.golos.golos.repository.Repository
import io.golos.golos.screens.stories.FilteredStoriesActivity
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.utils.GolosError

/**
 * Created by yuri on 08.01.18.
 */
data class FiltersScreenState(
        val isLoading: Boolean = false,
        val subscribedTags: List<LocalizedTag>,
        val shownTags: List<LocalizedTag>,
        val error: GolosError?
)

class AddTagTofilterViewModel : ViewModel() {
    private val mData = MediatorLiveData<FiltersScreenState>()
    private var mAllTags = ArrayList<LocalizedTag>()
    private val userSubscribedTags = HashSet<LocalizedTag>()
    private val mRepo = Repository.get
    private var lastLookupString: String? = null
    private var isSearchInProgress = false

    fun onCreate() {

        mData.addSource(mRepo.getUserSubscribedTags(), {
            userSubscribedTags.clear()
            userSubscribedTags.addAll(it?.map { LocalizedTag(it) } ?: HashSet())
            mData.value = FiltersScreenState(mData.value?.isLoading ?: false,
                    userSubscribedTags.toList().sorted(),
                    getFilteredTags(lookup = lastLookupString),
                    null)
        })
        mData.addSource(mRepo.getTrendingTags(), {
            mAllTags = ArrayList(it?.map { LocalizedTag(it) } ?: ArrayList())
            if (it?.size ?: 0 == 0) {
                mData.value = FiltersScreenState(true,
                        mData.value?.subscribedTags ?: ArrayList(),
                        ArrayList(),
                        null)
            } else {
                mData.value = FiltersScreenState(false,
                        mData.value?.subscribedTags ?: ArrayList(),
                        shownTags = getFilteredTags(lastLookupString),
                        error = null)
            }
        })
        mRepo.requestTrendingTagsUpdate({ l, e ->
            if (e != null) {
                mData.value = FiltersScreenState(false,
                        mData.value?.subscribedTags ?: ArrayList(),
                        mData.value?.shownTags ?: ArrayList(),
                        e)
            }
        })
    }

    fun search(lookupString: String) {
        this.lastLookupString = lookupString
        mData.value = FiltersScreenState(mData.value?.isLoading ?: false,
                mData.value?.subscribedTags ?: ArrayList(),
                getFilteredTags(lookupString),
                null)

    }

    fun onTagClick(context: Context?, tag: LocalizedTag) {
        FilteredStoriesActivity.start(context ?: return, FeedType.NEW, tag.tag.name)
    }

    fun onTagUnSubscribe(tag: LocalizedTag) {
        mRepo.unSubscribeOnTag(tag.tag)
    }

    fun onTagSubscribe(tag: LocalizedTag) {
        if (userSubscribedTags.contains(tag)) return
        mRepo.subscribeOnTag(tag.tag)
    }

    fun onSearchStart() {
        isSearchInProgress = true
        mData.value = FiltersScreenState(mData.value?.isLoading ?: false,
                mData.value?.subscribedTags ?: ArrayList(),
                ArrayList(),
                null)
    }

    fun onSearchEnd() {
        isSearchInProgress = false
        lastLookupString = null
        mData.value = FiltersScreenState(mData.value?.isLoading ?: false,
                mData.value?.subscribedTags ?: ArrayList(),
                getFilteredTags(null),
                null)
    }

    fun onDestroy() {
        mData.removeSource(mRepo.getTrendingTags())
        mData.removeSource(mRepo.getUserSubscribedTags())
    }

    private fun getFilteredTags(lookup: String?): List<LocalizedTag> {
        if (isSearchInProgress && lastLookupString?.isEmpty() != false) return ArrayList()
        return mAllTags
                .filter {
                    !userSubscribedTags.contains(it)
                }
                .filter {
                    it.getLocalizedName().startsWith(lookup ?: "")
                }
    }

    fun getLiveData(): LiveData<FiltersScreenState> = mData
}
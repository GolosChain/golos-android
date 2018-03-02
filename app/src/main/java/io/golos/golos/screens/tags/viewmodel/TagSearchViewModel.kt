package io.golos.golos.screens.tags.viewmodel

import android.app.Activity
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.content.Intent
import io.golos.golos.App
import io.golos.golos.repository.Repository
import io.golos.golos.screens.stories.FilteredStoriesActivity
import io.golos.golos.screens.tags.TagSearchActivity.Companion.TAG_TAG
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.utils.GolosError

/**
 * Created by yuri on 08.01.18.
 */
data class TagSearchViewModelScreenState(
        val isLoading: Boolean = false,
        val shownTags: List<LocalizedTag>,
        val error: GolosError?
)

class TagSearchViewModel : ViewModel(), Observer<List<LocalizedTag>> {
    private val mData = MutableLiveData<TagSearchViewModelScreenState>()
    private val mAllTags = ArrayList<LocalizedTag>()
    private val mRepo = Repository.get
    private var lastLookupString: String? = null
    private var isSearchInProgress = false

    fun onCreate() {
        mRepo.getLocalizedTags().observeForever(this)
        mRepo.requestTrendingTagsUpdate({ l, e ->
            if (e != null) {
                mData.value = TagSearchViewModelScreenState(false,
                        mData.value?.shownTags ?: ArrayList(),
                        e)
            }
        })
    }

    override fun onChanged(it: List<LocalizedTag>?) {

        if (it?.size ?: 0 == 0) {
            mData.value = TagSearchViewModelScreenState(true,
                    ArrayList(),
                    null)
        } else {
            mAllTags.clear()
            mAllTags.addAll(it ?: listOf())
            mData.value = TagSearchViewModelScreenState(false,
                    shownTags = getFilteredTags(lastLookupString),
                    error = null)
        }
    }

    fun search(lookupString: String) {
        this.lastLookupString = lookupString
        mData.value = TagSearchViewModelScreenState(mData.value?.isLoading ?: false,
                getFilteredTags(lookupString),
                null)

    }

    fun onTagClick(activity: Activity,
                   tag: LocalizedTag,
                   isStartedForResult: Boolean) {
        if (isStartedForResult) {
            val i = Intent()
            i.putExtra(TAG_TAG, tag)
            activity.setResult(Activity.RESULT_OK, i)
            activity.finish()
        } else {
            FilteredStoriesActivity.start(activity, tag.tag.name)
        }

    }

    fun onSearchStart() {
        isSearchInProgress = true
        mData.value = TagSearchViewModelScreenState(mData.value?.isLoading ?: false,
                ArrayList(),
                null)
    }

    fun onSearchEnd() {
        isSearchInProgress = false
        lastLookupString = null
        mData.value = TagSearchViewModelScreenState(mData.value?.isLoading ?: false,
                getFilteredTags(null),
                null)
    }

    fun onDestroy() {
        mRepo.getLocalizedTags().removeObserver(this)
    }

    private fun getFilteredTags(lookup: String?): List<LocalizedTag> {
        if (isSearchInProgress && lastLookupString?.isEmpty() != false) return ArrayList()
        return mAllTags
                .filter {
                    it.getLocalizedName().startsWith(lookup ?: "")
                }
    }

    fun getLiveData(): LiveData<TagSearchViewModelScreenState> = mData
}
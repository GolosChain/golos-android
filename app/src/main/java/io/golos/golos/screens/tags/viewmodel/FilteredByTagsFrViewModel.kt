package io.golos.golos.screens.tags.viewmodel

import android.app.Activity
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.content.Context
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.Tag
import io.golos.golos.screens.stories.FilteredStoriesActivity
import io.golos.golos.screens.tags.TagAndUsersSearchActivity
import io.golos.golos.screens.tags.TagsCloudActivity
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.utils.GolosError
import timber.log.Timber

/**
 * Created by yuri on 06.01.18.
 */
data class FilteredStoriesByTagViewModel(val tags: List<LocalizedTag>,
                                         val error: GolosError?)

class FilteredStoriesByTagFragmentViewModel : ViewModel(), Observer<Set<Tag>> {
    private val mTags = MutableLiveData<FilteredStoriesByTagViewModel>()

    fun onCreate() {
        Repository.get.getUserSubscribedTags().observeForever(this)
    }

    override fun onChanged(t: Set<Tag>?) {
        mTags.value = FilteredStoriesByTagViewModel((t?.map { LocalizedTag(it) } ?: ArrayList()).sorted(), null)
    }

    fun getTagsLiveData(): LiveData<FilteredStoriesByTagViewModel> = mTags

    fun onAddTagButtonClick(activity: Context?) {
        TagsCloudActivity.start(activity ?: return)
    }

    fun onTagClick(context: Context?, it: LocalizedTag) {
        FilteredStoriesActivity.start(context ?: return, it.tag.name)
    }

    fun onTagDeleteClick(activity: Context?, it: LocalizedTag) {

        Repository.get.unSubscribeOnTag(it.tag)
    }

    fun onTagSearchClick(context: Activity?) {
        TagAndUsersSearchActivity.start(context ?: return)
    }
}
package io.golos.golos.screens.stories.model

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.Tag
import io.golos.golos.screens.stories.FilteredStoriesActivity
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.utils.toArrayList

/**
 * Created by yuri on 11.01.18.
 */
data class FilteredStoriesViewState(val mainTag: LocalizedTag,
                                    val postsCount: Long?,
                                    val isUserSubscribedOnThisTag: Boolean,
                                    val similarTags: List<LocalizedTag>)

class FilteredStoriesViewModel : ViewModel() {
    private lateinit var mTagName: String
    private val mLiveData = MediatorLiveData<FilteredStoriesViewState>()
    private val mRepository = Repository.get
    private val mUserSubscribedTags = HashSet<Tag>()

    fun onCreate(tagName: String) {
        mTagName = tagName
        mUserSubscribedTags.addAll(mRepository.getUserSubscribedTags().value ?: HashSet())
        mLiveData.addSource(mRepository.getTrendingTags(), {
            val foundTag = it?.find { it.name == mTagName }

            if (foundTag != null) {
                var filter = ""
                if (foundTag.name.length < 5) filter = foundTag.name
                else {
                    if (foundTag.name.startsWith("ru--")) {
                        if (foundTag.name.length < 9) {
                            filter = foundTag.name
                        } else {
                            filter = foundTag.name.substring(0, 9)
                        }
                    } else {
                        filter = foundTag.name.substring(0, 5)
                    }
                }
                val filtered = it.filter { it.name.startsWith(filter) }.toArrayList()
                filtered.remove(foundTag)
                mLiveData.value = FilteredStoriesViewState(LocalizedTag(foundTag),
                        foundTag.topPostsCount,
                        mUserSubscribedTags.contains(foundTag),
                        filtered.map { LocalizedTag(it) }
                )
            }
        })
        mLiveData.addSource(mRepository.getUserSubscribedTags(), {
            mUserSubscribedTags.clear()
            mUserSubscribedTags.addAll(HashSet(it ?: HashSet()))
            val tag = mLiveData.value?.mainTag?.tag ?: Tag(tagName, 0.0, 0L, 0L)
            mLiveData.value = FilteredStoriesViewState(mLiveData.value?.mainTag ?: LocalizedTag(Tag(tagName, 0.0, 0L, 0L)),
                    mLiveData.value?.postsCount,
                    mUserSubscribedTags.contains(tag),
                    mLiveData.value?.similarTags ?: ArrayList())
        })
        if (mRepository.getTrendingTags().value?.size ?: 0 == 0) mRepository.requestTrendingTagsUpdate({ _, _ -> })
    }

    fun getLiveData(): LiveData<FilteredStoriesViewState> = mLiveData

    fun onTagUnsubscribe() {
        mRepository.unSubscribeOnTag(mLiveData.value?.mainTag?.tag ?: return)
    }

    fun onMainTagSubscribe() {
        mRepository.subscribeOnTag(mLiveData.value?.mainTag?.tag ?: return)
    }

    fun onTagClick(context: Context, localizedTag: LocalizedTag) {
        FilteredStoriesActivity.start(context, localizedTag.tag.name)
    }

    fun onDestroy() {
        mLiveData.removeSource(mRepository.getTrendingTags())
        mLiveData.removeSource(mRepository.getUserSubscribedTags())
    }
}
package io.golos.golos.screens.tags.viewmodel

import android.app.Activity
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.content.Intent
import io.golos.golos.repository.Repository
import io.golos.golos.repository.persistence.model.GolosUser
import io.golos.golos.screens.profile.UserProfileActivity
import io.golos.golos.screens.stories.FilteredStoriesActivity
import io.golos.golos.screens.tags.TagAndUsersSearchActivity
import io.golos.golos.screens.tags.TagAndUsersSearchActivity.Companion.TAG_TAG
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.utils.GolosError

/**
 * Created by yuri on 08.01.18.
 */
data class TagSearchViewModelScreenState(
        val isLoading: Boolean = false,
        val shownTags: List<LocalizedTag>,
        val shownUsers: List<GolosUser>,
        val error: GolosError?
)

class TagSearchViewModel : ViewModel(), Observer<List<LocalizedTag>> {
    private val mTagsData = MutableLiveData<TagSearchViewModelScreenState>()
    private val mAllTags = ArrayList<LocalizedTag>()
    private val mRepo = Repository.get
    private var lastLookupString: String? = null
    private var isSearchInProgress = false
    private val userRegexps = "([a-z][a-zA-Z.\\-0-9]{2,15})".toRegex()
    private var mUserObserver = Observer<List<GolosUser>> {
        mTagsData.value = TagSearchViewModelScreenState(false,
                mTagsData.value?.shownTags ?: listOf(),
                it ?: listOf(),
                null)
    }
    private var mLastUserLiveData: LiveData<List<GolosUser>> = MutableLiveData()

    fun onCreate() {
        mRepo.getLocalizedTags().observeForever(this)
        mRepo.requestTrendingTagsUpdate({ _, e ->
            if (e != null) {
                mTagsData.value = TagSearchViewModelScreenState(false,
                        mTagsData.value?.shownTags ?: listOf(),
                        listOf(),
                        e)
            }
        })
    }

    override fun onChanged(it: List<LocalizedTag>?) {

        if (it?.size ?: 0 == 0) {
            mTagsData.value = TagSearchViewModelScreenState(true,
                    listOf(),
                    listOf(),
                    null)
        } else {
            mAllTags.clear()
            mAllTags.addAll(it ?: listOf())
            mTagsData.value = TagSearchViewModelScreenState(false,
                    shownTags = getFilteredTags(lastLookupString),
                    shownUsers = mTagsData.value?.shownUsers ?: listOf(),
                    error = null)
        }
    }

    fun searchTag(lookupString: String) {
        this.lastLookupString = lookupString
        mTagsData.value = TagSearchViewModelScreenState(mTagsData.value?.isLoading ?: false,
                getFilteredTags(lookupString),
                mTagsData.value?.shownUsers ?: listOf(),
                null)

    }

    fun searchUser(nick: String) {
        mLastUserLiveData.removeObserver(mUserObserver)

        if (!nick.matches(userRegexps)) {
            mTagsData.value = TagSearchViewModelScreenState(false,
                    mTagsData.value?.shownTags ?: listOf(),
                    listOf(),
                    null)
        } else {
            mLastUserLiveData = mRepo.getGolosUsers(nick.toLowerCase())
            mLastUserLiveData.observeForever(mUserObserver)
        }
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

    fun onTagSearchStart() {
        isSearchInProgress = true
        mTagsData.value = TagSearchViewModelScreenState(mTagsData.value?.isLoading ?: false,
                listOf(),
                mTagsData.value?.shownUsers ?: listOf(),
                null)
    }

    fun onTagSearchEnd() {
        isSearchInProgress = false
        lastLookupString = null
        mTagsData.value = TagSearchViewModelScreenState(mTagsData.value?.isLoading ?: false,
                getFilteredTags(null),
                mTagsData.value?.shownUsers ?: listOf(),
                null)
    }

    fun onDestroy() {
        mRepo.getLocalizedTags().removeObserver(this)
    }

    private fun getFilteredTags(lookup: String?): List<LocalizedTag> {
        if (isSearchInProgress && lastLookupString?.isEmpty() != false) return ArrayList()
        return mAllTags
                .filter {
                    it.getLocalizedName().startsWith(lookup?.toLowerCase() ?: "")
                }
    }

    fun getLiveData(): LiveData<TagSearchViewModelScreenState> = mTagsData

    fun onUserClick(tagSearchActivity: TagAndUsersSearchActivity, user: GolosUser) {
        UserProfileActivity.start(tagSearchActivity, user.userName)
    }
}
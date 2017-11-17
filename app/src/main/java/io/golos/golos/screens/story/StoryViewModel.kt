package io.golos.golos.screens.story

import android.app.Activity
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.ViewModel
import android.widget.ImageView
import io.golos.golos.repository.Repository
import io.golos.golos.screens.main_stripes.model.FeedType
import io.golos.golos.screens.story.model.GolosDiscussionItem
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.screens.story.model.StoryViewState
import io.golos.golos.screens.widgets.PhotoActivity

/**
 * Created by yuri on 06.11.17.
 */
class StoryViewModel : ViewModel() {
    private val mLiveData = MediatorLiveData<StoryViewState>()
    private val mRepository = Repository.get
    private lateinit var mStory: StoryTree

    fun onCreate(story: StoryTree, feedType: FeedType) {
        mStory = story
        mLiveData.addSource(mRepository.getStories(feedType)) {
            val storyItems = it
            it?.
                    items?.
                    filter { it.rootStory()?.id == mStory.rootStory()?.id }?.
                    forEach {
                        mLiveData.value = StoryViewState(false,
                                it.rootStory()?.title ?: "",
                                mLiveData.value?.isStoryCommentButtonShown ?: false,
                                storyItems?.error,
                                it.rootStory()?.tags ?: ArrayList(),
                                it)
                    }
        }
        mLiveData.addSource(mRepository.getCurrentUserDataAsLiveData()) {
            mLiveData.value = StoryViewState(mLiveData.value?.isLoading ?: false,
                    mLiveData.value?.storyTitle ?: "",
                    it?.userName != null,
                    mLiveData.value?.errorCode,
                    mLiveData.value?.tags ?: ArrayList(),
                    mLiveData.value?.storyTree ?: StoryTree(null, ArrayList()))
        }
        mRepository.requestStoryUpdate(mStory)
    }


    val liveData: LiveData<StoryViewState>
        get() {
            return mLiveData
        }

    fun onMainStoryTextClick(activity: Activity, text: String) {

    }

    fun onMainStoryImageClick(activity: Activity, src: String, iv: ImageView?) {
        if (iv == null) PhotoActivity.startActivity(context = activity, imageSrc = src)
        else PhotoActivity.startActivityUsingTransition(activity, iv, src)
    }

    val showVoteDialog: Boolean
        get() {
            return mRepository.getCurrentUserData() != null && mRepository.getCurrentUserData()?.userName != null

        }
    val canUserVoteOnThis: Boolean
        get() {
            return mLiveData.value?.storyTree?.rootStory()?.isUserUpvotedOnThis == false
        }

    fun onStoryVote(percent: Short) {
        val story = mStory.rootStory() as? GolosDiscussionItem ?: return
        if (canUserVoteOnThis) mRepository.upVote(story, percent)
        else mRepository.cancelVote(story)
    }
}
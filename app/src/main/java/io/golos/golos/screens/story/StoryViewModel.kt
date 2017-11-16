package io.golos.golos.screens.story

import android.app.Activity
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.ViewModel
import android.widget.ImageView
import io.golos.golos.repository.Repository
import io.golos.golos.screens.story.model.Comment
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.screens.story.model.StoryViewState
import io.golos.golos.screens.widgets.PhotoActivity
import timber.log.Timber

/**
 * Created by yuri on 06.11.17.
 */
class StoryViewModel : ViewModel() {
    private val mLiveData = MediatorLiveData<StoryViewState>()
    private val mRepository = Repository.get

    fun onCreate(comment: Comment) {
        mLiveData.addSource(mRepository.getStory(comment)) {
            mLiveData.value = StoryViewState(false,
                    comment.title,
                    it?.error,
                    it?.treeState?.rootStory?.tags ?: ArrayList(),
                    it?.treeState ?: StoryTree(null, ArrayList()))
        }
        mRepository.requestStoryUpdate(comment)
    }

    val liveData: LiveData<StoryViewState>
        get() {
            return mLiveData
        }

    init {
        Timber.e("oncreate")
    }

    fun onMainStoryTextClick(activity: Activity, text: String) {

    }

    fun onMainStoryImageClick(activity: Activity, src: String, iv: ImageView?) {
        if (iv == null) PhotoActivity.startActivity(context = activity, imageSrc = src)
        else PhotoActivity.startActivityUsingTransition(activity, iv, src)
    }
}
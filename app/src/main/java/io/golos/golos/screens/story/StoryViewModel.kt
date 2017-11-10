package io.golos.golos.screens.story

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Handler
import android.os.Looper
import eu.bittrade.libs.steemj.exceptions.SteemCommunicationException
import eu.bittrade.libs.steemj.exceptions.SteemConnectionException
import eu.bittrade.libs.steemj.exceptions.SteemTimeoutException
import io.golos.golos.repository.Repository
import io.golos.golos.screens.story.model.StoryViewState
import io.golos.golos.utils.ErrorCodes
import timber.log.Timber
import java.security.InvalidParameterException

/**
 * Created by yuri on 06.11.17.
 */
class StoryViewModel : ViewModel() {
    val liveData: MutableLiveData<StoryViewState> = MutableLiveData<StoryViewState>()
    val repository = Repository.get
    private val executor = Repository.sharedExecutor
    private val mHandler = Handler(Looper.getMainLooper())

    fun onCreate(blogName: String, author: String, permlink: String) {
        Timber.e("on create $blogName $author $permlink")
        if (liveData.value?.storyTree?.rootStory?.body?.isEmpty() != false) {
            liveData.value = StoryViewState(isLoading = true)
            postWithCatch {
                val story = repository.getStory(blogName, author, permlink)
                story.rootStory?.avatarPath = repository.getUserAvatar(author, permlink, blogName)
                mHandler.post({
                    liveData.value = StoryViewState(false,
                            story.rootStory?.title ?: "",
                            null,
                            storyTree = story,
                            tags = story.rootStory?.tags ?: emptyList())
                })
            }
        }
    }

    private fun postWithCatch(action: () -> Unit) {
        executor.execute({
            try {
                action.invoke()
            } catch (e: SteemTimeoutException) {
                mHandler.post({
                    liveData.value = StoryViewState(false,
                            errorCode = ErrorCodes.ERROR_SLOW_CONNECTION)
                })
            } catch (e: SteemCommunicationException) {
                e.printStackTrace()
                mHandler.post({
                    liveData.value = StoryViewState(false,
                            errorCode = ErrorCodes.ERROR_NO_CONNECTION)
                })
            } catch (e: InvalidParameterException) {
                e.printStackTrace()
                mHandler.post({
                    liveData.value = StoryViewState(false,
                            errorCode = ErrorCodes.ERROR_WRONG_ARGUMENTS)
                })
            } catch (e: SteemConnectionException) {
                e.printStackTrace()
                mHandler.post({
                    liveData.value = StoryViewState(false,
                            errorCode = ErrorCodes.ERROR_NO_CONNECTION)
                })
            } catch (e: Exception) {
                e.printStackTrace()
                mHandler.post({
                    liveData.value = StoryViewState(false,
                            errorCode = ErrorCodes.UNKNOWN)
                })
            }
        })
    }
}
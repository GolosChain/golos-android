package io.golos.golos.screens.androidviewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.os.Handler
import android.os.Looper
import eu.bittrade.libs.steemj.exceptions.*
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.UpdatingState
import io.golos.golos.screens.main_stripes.viewmodel.StoriesViewModel
import io.golos.golos.screens.story.model.Comment
import io.golos.golos.utils.ErrorCode
import io.golos.golos.utils.GolosError
import io.golos.golos.utils.GolosErrorParser
import java.security.InvalidParameterException

data class CommentUpdatingStatus(val updatedComment: Comment,
                                 val status: UpdatingState,
                                 val id: Long)

data class VoteState(val updatedComment: CommentUpdatingStatus,
                     val error: GolosError? = null)

class VoteViewModel(app: Application) : AndroidViewModel(app) {
    private val mRepository = Repository.get
    val voteLiveData = MutableLiveData<VoteState>()
    private val mHandler = Handler(Looper.getMainLooper())

    fun upVote(comment: Comment, power: Short, id: Long) {
        if (mRepository.getCurrentUserData() == null) {
            StoriesViewModel.mHandler.post({
                voteLiveData.value = VoteState(CommentUpdatingStatus(comment, UpdatingState.FAILED, id),
                        error =
                        GolosError(ErrorCode.ERROR_AUTH, null, R.string.login_to_vote))
            })
            return
        }
        mHandler.post { voteLiveData.value = VoteState(CommentUpdatingStatus(comment, UpdatingState.UPDATING, id)) }
        postWithCatch(comment, id) {
            val story = mRepository.upVote(comment.author, comment.permlink, power)
            mHandler.post({
                voteLiveData.value = VoteState(CommentUpdatingStatus(story, UpdatingState.DONE, id))
            })
        }
    }

    fun cancelVote(comment: Comment, id: Long) {
        postWithCatch(comment, id) {
            mHandler.post { voteLiveData.value = VoteState(CommentUpdatingStatus(comment, UpdatingState.UPDATING, id)) }
            val story = mRepository.downVote(comment.author, comment.permlink)
            mHandler.post({
                voteLiveData.value = VoteState(CommentUpdatingStatus(story, UpdatingState.UPDATING, id))
            })
        }
    }

    protected fun postWithCatch(comment: Comment, id: Long, action: () -> Unit) {
        Repository.sharedExecutor.execute({
            try {
                action.invoke()
            } catch (e: SteemResponseError) {
                e.printStackTrace()
                mHandler.post({
                    voteLiveData.value = VoteState(CommentUpdatingStatus(comment, UpdatingState.FAILED, id),
                            GolosError(ErrorCode.ERROR_WRONG_ARGUMENTS, null, GolosErrorParser.getLocalizedError(e)))
                })
            } catch (e: SteemInvalidTransactionException) {
                e.printStackTrace()
                mHandler.post({
                    voteLiveData.value = VoteState(CommentUpdatingStatus(comment, UpdatingState.FAILED, id),
                            GolosError(ErrorCode.ERROR_WRONG_ARGUMENTS, e.message, null))
                })
            } catch (e: SteemTimeoutException) {
                mHandler.post({
                    voteLiveData.value = VoteState(CommentUpdatingStatus(comment, UpdatingState.FAILED, id),
                            GolosError(ErrorCode.ERROR_SLOW_CONNECTION, null, R.string.slow_internet_connection))
                })
            } catch (e: SteemCommunicationException) {
                e.printStackTrace()
                mHandler.post({
                    voteLiveData.value = VoteState(CommentUpdatingStatus(comment, UpdatingState.FAILED, id),
                            GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.no_internet_connection))
                })
            } catch (e: InvalidParameterException) {
                e.printStackTrace()
                mHandler.post({
                    voteLiveData.value = VoteState(CommentUpdatingStatus(comment, UpdatingState.FAILED, id),
                            GolosError(ErrorCode.ERROR_WRONG_ARGUMENTS, null, R.string.wrong_args))
                })
            } catch (e: SteemConnectionException) {
                e.printStackTrace()
                mHandler.post({
                    voteLiveData.value = VoteState(CommentUpdatingStatus(comment, UpdatingState.FAILED, id),
                            GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.no_internet_connection))
                })
            } catch (e: Exception) {
                e.printStackTrace()
                mHandler.post({
                    voteLiveData.value = VoteState(CommentUpdatingStatus(comment, UpdatingState.FAILED, id),
                            GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.unknown_error))
                })
            }
        })
    }
}
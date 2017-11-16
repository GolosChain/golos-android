package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.os.Handler
import android.os.Looper
import android.support.annotation.WorkerThread
import eu.bittrade.libs.steemj.Golos4J
import io.golos.golos.App
import io.golos.golos.repository.model.StoryItems
import io.golos.golos.repository.model.StoryTreeState
import io.golos.golos.repository.model.UserAuthResponse
import io.golos.golos.repository.persistence.Persister
import io.golos.golos.repository.persistence.model.AccountInfo
import io.golos.golos.repository.persistence.model.UserData
import io.golos.golos.screens.main_stripes.model.FeedType
import io.golos.golos.screens.main_stripes.viewmodel.ImageLoadRunnable
import io.golos.golos.screens.story.model.Comment
import io.golos.golos.screens.story.model.RootStory
import io.golos.golos.screens.story.model.StoryTree
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


abstract class Repository {

    companion object {
        private var instance: Repository? = null
        val get: Repository
            @Synchronized
            get() {
                if (App.isMocked) {
                    if (instance == null) instance = MockRepoImpl()
                    return instance!!
                } else {
                    if (instance == null) instance = RepositoryImpl(
                            sharedExecutor,
                            mMainThreadExecutor,
                            Persister.get,
                            Golos4J.getInstance())
                    return instance!!
                }
            }
        val sharedExecutor: ThreadPoolExecutor by lazy {
            val queu = PriorityBlockingQueue<Runnable>(15, Comparator<Runnable> { o1, o2 ->
                if (o1 is ImageLoadRunnable) Int.MAX_VALUE
                else if (o1 is Runnable) Int.MIN_VALUE
                else 0
            })
            ThreadPoolExecutor(1, 1,
                    Long.MAX_VALUE, TimeUnit.MILLISECONDS, queu)
        }
        private val mMainThreadExecutor: Executor
            get() {
                val handler = Handler(Looper.getMainLooper())
                return Executor { command -> handler.post(command) }
            }
    }

    @WorkerThread
    abstract fun getStripeItems(limit: Int, type: FeedType, truncateBody: Int,
                                startAuthor: String? = null, startPermlink: String? = null): List<RootStory>

    abstract fun getStories(type: FeedType): LiveData<StoryItems>

    abstract fun requestStoriesUpdate(limit: Int, type: FeedType,
                                      startAuthor: String? = null, startPermlink: String? = null)

    @WorkerThread
    abstract fun upVote(author: String, permlink: String, percents: Short): Comment

    abstract fun upVoteNoCallback(comment: Comment, percents: Short)

    @WorkerThread
    abstract fun getUserAvatar(username: String, permlink: String? = null, blog: String? = null): String?

    @WorkerThread
    abstract fun getStory(blog: String, author: String, permlink: String): StoryTree


    @WorkerThread
    abstract fun authWithMasterKey(userName: String, masterKey: String): UserAuthResponse

    @WorkerThread
    abstract fun authWithActiveWif(login: String, activeWif: String): UserAuthResponse

    @WorkerThread
    abstract fun authWithPostingWif(login: String, postingWif: String): UserAuthResponse

    @WorkerThread
    abstract fun getAccountData(of: String): AccountInfo

    abstract fun getCurrentUserData(): UserData?

    abstract fun deleteUserdata()
    abstract fun setUserAccount(userName: String, privateActiveWif: String?, privatePostingWif: String?)
    @WorkerThread
    abstract fun downVote(author: String, permlink: String): Comment

    abstract fun cancelVoteNoCallback(comment: Comment)


    abstract fun getUserFeed(userName: String, limit: Int, truncateBody: Int, startAuthor: String?, startPermlink: String?): List<RootStory>

    abstract fun requestStoryUpdate(comment: Comment)

    abstract fun getStory(comment: Comment): LiveData<StoryTreeState>
}



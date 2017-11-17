package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.os.Handler
import android.os.Looper
import android.support.annotation.WorkerThread
import eu.bittrade.libs.steemj.Golos4J
import io.golos.golos.App
import io.golos.golos.repository.model.StoryTreeItems
import io.golos.golos.repository.model.UserAuthResponse
import io.golos.golos.repository.persistence.Persister
import io.golos.golos.repository.persistence.model.AccountInfo
import io.golos.golos.repository.persistence.model.UserData
import io.golos.golos.screens.main_stripes.model.FeedType
import io.golos.golos.screens.main_stripes.viewmodel.ImageLoadRunnable
import io.golos.golos.screens.story.model.GolosDiscussionItem
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


    abstract fun getStories(type: FeedType): LiveData<StoryTreeItems>

    abstract fun requestStoriesListUpdate(limit: Int, type: FeedType,
                                          startAuthor: String? = null, startPermlink: String? = null)

    @WorkerThread
    abstract fun authWithMasterKey(userName: String, masterKey: String): UserAuthResponse

    @WorkerThread
    abstract fun authWithActiveWif(login: String, activeWif: String): UserAuthResponse

    @WorkerThread
    abstract fun authWithPostingWif(login: String, postingWif: String): UserAuthResponse

    @WorkerThread
    abstract fun getAccountData(of: String): AccountInfo

    abstract fun getCurrentUserData(): UserData?

    abstract fun getCurrentUserDataAsLiveData(): LiveData<UserData?>

    abstract fun deleteUserdata()

    abstract fun setUserAccount(userName: String, privateActiveWif: String?, privatePostingWif: String?)

    abstract fun upVote(comment: GolosDiscussionItem, percents: Short)

    abstract fun cancelVote(comment: GolosDiscussionItem)

    abstract fun requestStoryUpdate(story: StoryTree)

}



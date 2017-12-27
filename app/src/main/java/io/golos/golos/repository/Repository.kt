package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.os.Handler
import android.os.Looper
import com.fasterxml.jackson.annotation.JsonProperty
import io.golos.golos.repository.api.GolosApi
import io.golos.golos.repository.model.*
import io.golos.golos.repository.persistence.Persister
import io.golos.golos.repository.persistence.model.AccountInfo
import io.golos.golos.repository.persistence.model.UserData
import io.golos.golos.screens.editor.EditorPart
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.utils.GolosError
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

data class StoryFilter(
        @JsonProperty("tagFilter")
        val tagFilter: String? = null,
        @JsonProperty("userNameFilter")
        val userNameFilter: String? = null)

abstract class Repository {

    companion object {
        private var instance: Repository? = null
        val get: Repository
            @Synchronized
            get() {
                if (instance == null) instance = RepositoryImpl(
                        sharedExecutor,
                        mMainThreadExecutor,
                        Persister.get,
                        GolosApi.get)
                return instance!!
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

        fun setSingletoneInstance(repository: Repository) {
            instance = repository
        }
    }

    open fun onAppCreate() {}

    abstract fun getStories(type: FeedType, filter: StoryFilter? = null): LiveData<StoryTreeItems>

    abstract fun requestStoriesListUpdate(limit: Int,
                                          type: FeedType,
                                          filter: StoryFilter? = null,
                                          startAuthor: String? = null,
                                          startPermlink: String? = null,
                                          complitionHandler: (Unit, GolosError?) -> Unit = { _, _ -> })

    abstract fun authWithMasterKey(userName: String,
                                   masterKey: String,
                                   listener: (UserAuthResponse) -> Unit)

    abstract fun authWithActiveWif(login: String,
                                   activeWif: String,
                                   listener: (UserAuthResponse) -> Unit)

    abstract fun authWithPostingWif(login: String,
                                    postingWif: String,
                                    listener: (UserAuthResponse) -> Unit)

    abstract fun getCurrentUserDataAsLiveData(): LiveData<UserData>

    abstract fun requestActiveUserDataUpdate()

    abstract fun getUserInfo(userName: String): LiveData<AccountInfo>

    abstract fun requestUserInfoUpdate(userName: String, completionHandler: (AccountInfo, GolosError?) -> Unit)

    abstract fun deleteUserdata()

    abstract fun lastCreatedPost(): LiveData<CreatePostResult>

    abstract fun upVote(comment: GolosDiscussionItem, percents: Short)

    abstract fun cancelVote(comment: GolosDiscussionItem)

    abstract fun requestStoryUpdate(story: StoryTree)

    abstract fun requestStoryUpdate(author: String, permLink: String,
                                    blog: String?, feedType: FeedType)

    abstract fun createPost(title: String, content: List<EditorPart>, tags: List<String>,
                            resultListener: (CreatePostResult?, GolosError?) -> Unit)

    abstract fun createComment(rootStory: StoryTree, to: GolosDiscussionItem, content: List<EditorPart>,
                               resultListener: (CreatePostResult?, GolosError?) -> Unit)

    abstract fun isUserLoggedIn(): Boolean

    abstract fun follow(user: String, completionHandler: (Unit, GolosError?) -> Unit)

    abstract fun unFollow(user: String, completionHandler: (Unit, GolosError?) -> Unit)

    abstract fun getSubscribers(ofUser: String): LiveData<List<FollowUserObject>>

    abstract fun requestSubscribersUpdate(ofUser: String, completionHandler: (List<FollowUserObject>, GolosError?) -> Unit)

    abstract fun getSubscriptions(ofUser: String): LiveData<List<FollowUserObject>>

    abstract fun requestSubscriptionUpdate(ofUser: String, completionHandler: (List<FollowUserObject>, GolosError?) -> Unit)

    fun getShareStoryLink(item: GolosDiscussionItem): String {
        return "https://golos.io/${item.categoryName}/@${item.author}/${item.permlink}"
    }
}

interface ImageLoadRunnable : Runnable

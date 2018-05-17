package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.annotation.AnyThread
import android.support.annotation.MainThread
import com.crashlytics.android.Crashlytics
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.fabric.sdk.android.Fabric
import io.golos.golos.App
import io.golos.golos.repository.model.*
import io.golos.golos.repository.persistence.model.GolosUserAccountInfo
import io.golos.golos.repository.persistence.model.AppUserData
import io.golos.golos.repository.persistence.model.GolosUser
import io.golos.golos.screens.editor.EditorPart
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryWithComments
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.utils.ExceptionLogger
import io.golos.golos.utils.GolosError
import io.golos.golos.utils.Regexps
import java.util.*
import java.util.concurrent.*

abstract class Repository {

    companion object {
        private var instance: Repository? = null
        val get: Repository
            @Synchronized
            get() {
                if (instance == null) instance = RepositoryImpl(
                        networkExecutor,
                        Executors.newSingleThreadExecutor(ThreadFactoryBuilder().setNameFormat("worker executor thread -%d").build()),
                        mMainThreadExecutor,
                        mLogger = object : ExceptionLogger {
                            override fun log(t: Throwable) {
                                try {
                                    if (!Fabric.isInitialized()) {
                                        Fabric.with(App.context, Crashlytics())
                                    }
                                    Crashlytics.logException(t)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        })
                return instance!!
            }
        private val networkExecutor: ThreadPoolExecutor by lazy {
            val queu = PriorityBlockingQueue<Runnable>(15, Comparator<Runnable> { o1, o2 ->
                if (o1 is ImageLoadRunnable) Int.MAX_VALUE
                else if (o1 is Runnable) Int.MIN_VALUE
                else 0
            })

            ThreadPoolExecutor(1, 1,
                    Long.MAX_VALUE, TimeUnit.MILLISECONDS, queu, ThreadFactoryBuilder().setNameFormat("network executor thread -%d").build())
        }
        private val mMainThreadExecutor: Executor
            get() {

                val handler = Handler(Looper.getMainLooper())
                return Executor { command -> handler.post(command) }
            }

        fun setSingletoneInstance(repository: Repository) {
            instance = repository
        }

        @JvmStatic
        val blacklistTags = hashSetOf("test", "bm-open", "bm-ceh23", "bm-tasks", "bm-taskceh1", "хардфоркамынежде")

    }

    internal fun checkTagIsValid(tag: String): Boolean {
        return tag.length > 2 && !blacklistTags.contains(tag) && !Regexps.wrongTagRegexp.matches(tag)
    }

    @MainThread
    open fun onAppCreate(ctx: Context) {}
    @MainThread
    abstract fun getStories(type: FeedType, filter: StoryFilter? = null): LiveData<StoriesFeed>
    @MainThread
    abstract fun requestStoriesListUpdate(limit: Int,
                                          type: FeedType,
                                          filter: StoryFilter? = null,
                                          startAuthor: String? = null,
                                          startPermlink: String? = null,
                                          completionHandler: (Unit, GolosError?) -> Unit = { _, _ -> })
    @MainThread
    abstract fun authWithMasterKey(userName: String,
                                   masterKey: String,
                                   listener: (UserAuthResponse) -> Unit)
    @MainThread
    abstract fun authWithActiveWif(login: String,
                                   activeWif: String,
                                   listener: (UserAuthResponse) -> Unit)

    abstract fun authWithPostingWif(login: String,
                                    postingWif: String,
                                    listener: (UserAuthResponse) -> Unit)
    @MainThread
    abstract fun getCurrentUserDataAsLiveData(): LiveData<AppUserData>
    @MainThread
    abstract fun requestActiveUserDataUpdate()
    @MainThread
    abstract fun getUserInfo(userName: String): LiveData<GolosUserAccountInfo>
    @MainThread
    abstract fun requestUserInfoUpdate(userName: String, completionHandler: (GolosUserAccountInfo, GolosError?) -> Unit)
    @MainThread
    abstract fun deleteUserdata()
    @MainThread
    abstract fun lastCreatedPost(): LiveData<CreatePostResult>
    @MainThread
    abstract fun vote(comment: StoryWrapper, percents: Short)
    @MainThread
    abstract fun cancelVote(comment: StoryWrapper)
    @MainThread
    abstract fun requestStoryUpdate(story: StoryWithComments,
                                    completionListener: (Unit, GolosError?) -> Unit = { _, _ -> })
    @MainThread
    abstract fun requestStoryUpdate(author: String, permLink: String,
                                    blog: String?, feedType: FeedType,
                                    completionListener: (Unit, GolosError?) -> Unit = { _, _ -> })
    @MainThread
    abstract fun createPost(title: String, content: List<EditorPart>, tags: List<String>,
                            resultListener: (CreatePostResult?, GolosError?) -> Unit = { _, _ -> })
    @MainThread
    abstract fun editPost(title: String, content: List<EditorPart>, tags: List<String>,
                          originalPost: StoryWrapper,
                          resultListener: (CreatePostResult?, GolosError?) -> Unit = { _, _ -> })
    @MainThread
    abstract fun createComment(toItem: StoryWrapper, content: List<EditorPart>,
                               resultListener: (CreatePostResult?, GolosError?) -> Unit = { _, _ -> })
    @MainThread
    abstract fun editComment(originalComment: StoryWrapper,
                             content: List<EditorPart>,
                             resultListener: (CreatePostResult?, GolosError?) -> Unit = { _, _ -> })
    @AnyThread
    abstract fun isUserLoggedIn(): Boolean

    //subscription to blog
    @MainThread
    abstract fun getCurrentUserSubscriptions(): LiveData<List<UserBlogSubscription>>
    @MainThread
    abstract fun getSubscribersToBlog(ofUser: String): LiveData<List<UserObject>>
    @MainThread
    abstract fun getSubscriptionsToBlogs(ofUser: String): LiveData<List<UserObject>>
    @MainThread
    abstract fun requestSubscribersUpdate(ofUser: String, completionHandler: (List<UserObject>, GolosError?) -> Unit)
    @MainThread
    abstract fun requestSubscriptionUpdate(ofUser: String, completionHandler: (List<UserObject>, GolosError?) -> Unit)
    @MainThread
    abstract fun subscribeOnUserBlog(user: String, completionHandler: (Unit, GolosError?) -> Unit)
    @MainThread
    abstract fun unSubscribeOnUserBlog(user: String, completionHandler: (Unit, GolosError?) -> Unit)

    //tags
    @MainThread
    abstract fun getUserSubscribedTags(): LiveData<Set<Tag>>
    @MainThread
    abstract fun subscribeOnTag(tag: Tag)
    @MainThread
    abstract fun unSubscribeOnTag(tag: Tag)
    @MainThread
    abstract fun getTrendingTags(): LiveData<List<Tag>>
    @MainThread
    abstract fun getLocalizedTags(): LiveData<List<LocalizedTag>>
    @MainThread
    abstract fun requestTrendingTagsUpdate(completionHandler: (List<Tag>, GolosError?) -> Unit)
    @MainThread
    abstract fun getVotedUsersForDiscussion(id: Long): LiveData<List<VotedUserObject>>
    @MainThread
    abstract fun getAppReadyStatus(): LiveData<ReadyStatus>
    @MainThread
    abstract fun requestInitRetry()
    @MainThread
    open fun getExchangeLiveData(): LiveData<ExchangeValues> {
        val liveData = MutableLiveData<ExchangeValues>()
        liveData.value = ExchangeValues(00.04106528f, 00.04106528f * 56)
        return liveData
    }


    fun getShareStoryLink(item: GolosDiscussionItem): String {
        return "https://golos.io/${item.categoryName}/@${item.author}/${item.permlink}"
    }

    open fun onAppStop() {


    }

    open fun onAppDestroy() {


    }

    abstract val userSettingsRepository: UserSettingsRepository

    abstract val notificationsRepository: NotificationsRepository

    abstract fun getGolosUsers(nick: String): LiveData<List<GolosUser>>
}

interface ImageLoadRunnable : Runnable

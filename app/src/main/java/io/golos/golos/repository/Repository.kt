package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.support.annotation.AnyThread
import android.support.annotation.MainThread
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.golos.golos.BuildConfig
import io.golos.golos.notifications.PushNotificationsRepository
import io.golos.golos.repository.model.*
import io.golos.golos.repository.services.EventType
import io.golos.golos.repository.services.GolosEvent
import io.golos.golos.screens.editor.EditorPart
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryWithComments
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.utils.FabricExceptionLogger
import io.golos.golos.utils.GolosError
import io.golos.golos.utils.MainThreadExecutor
import io.golos.golos.utils.Regexps
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

interface UserDataProvider {
    val appUserData: LiveData<ApplicationUser>
}

interface EventsProvider {
    fun getEvents(type: List<EventType>?): LiveData<List<GolosEvent>>
    fun requestEventsUpdate(type: List<EventType>?,
                            fromId: String? = null,
                            limit: Int = 40,
                            completionHandler: (Unit, GolosError?) -> Unit = { _, _ -> })
}

abstract class Repository : UserDataProvider, EventsProvider, GolosUsersRepository {

    companion object {
        private var instance: Repository? = null
        val get: Repository
            @Synchronized
            get() {
                if (instance == null) instance = RepositoryImpl(
                        networkExecutor,
                        Executors.newSingleThreadExecutor(ThreadFactoryBuilder().setNameFormat("worker executor thread -%d").build()),
                        mMainThreadExecutor,
                        mLogger = FabricExceptionLogger)
                return instance!!
            }
        private val networkExecutor: ThreadPoolExecutor by lazy {
            val queu = PriorityBlockingQueue<Runnable>(15, Comparator<Runnable> { o1, _ ->
                if (o1 is ImageLoadRunnable) Int.MAX_VALUE
                else if (o1 is Runnable) Int.MIN_VALUE
                else 0
            })

            ThreadPoolExecutor(1, 2,
                    Long.MAX_VALUE, TimeUnit.MILLISECONDS, queu, ThreadFactoryBuilder().setNameFormat("network executor thread -%d").build())
        }
        private val mMainThreadExecutor = MainThreadExecutor()

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
    open fun onAppCreate(ctx: Context) {
    }

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
    abstract fun authWithMasterKey(name: String,
                                   masterKey: String,
                                   listener: (UserAuthResponse) -> Unit)

    @MainThread
    abstract fun authWithActiveWif(name: String,
                                   activeWif: String,
                                   listener: (UserAuthResponse) -> Unit)

    abstract fun authWithPostingWif(name: String,
                                    postingWif: String,
                                    listener: (UserAuthResponse) -> Unit)

    abstract fun requestApplicationUserDataUpdate()

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
        return "${BuildConfig.BASE_URL}${item.categoryName}/@${item.author}/${item.permlink}"
    }

    open fun onAppStop() {


    }

    open fun onAppDestroy() {


    }

    abstract val userSettingsRepository: UserSettingsRepository

    abstract val notificationsRepository: PushNotificationsRepository

}

interface ImageLoadRunnable : Runnable

package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.content.Context
import android.support.annotation.WorkerThread
import eu.bittrade.libs.golosj.Golos4J
import eu.bittrade.libs.golosj.base.models.AccountName
import eu.bittrade.libs.golosj.enums.PrivateKeyType
import eu.bittrade.libs.golosj.exceptions.SteemResponseError
import eu.bittrade.libs.golosj.util.ImmutablePair
import io.golos.golos.R
import io.golos.golos.notifications.PushNotificationsRepository
import io.golos.golos.notifications.PushNotificationsRepositoryImpl
import io.golos.golos.repository.api.GolosApi
import io.golos.golos.repository.model.*
import io.golos.golos.repository.persistence.Persister
import io.golos.golos.repository.persistence.model.AppUserData
import io.golos.golos.repository.persistence.model.GolosUserAccountInfo
import io.golos.golos.repository.services.EventType
import io.golos.golos.repository.services.GolosEvent
import io.golos.golos.repository.services.GolosServices
import io.golos.golos.repository.services.GolosServicesImpl
import io.golos.golos.screens.editor.EditorPart
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryWithComments
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.utils.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

@Suppress("NAME_SHADOWING", "LABEL_NAME_CLASH")
internal class RepositoryImpl(private val networkExecutor: Executor = Executors.newFixedThreadPool(2),
                              private val workerExecutor: Executor = Executors.newSingleThreadExecutor(),
                              private val mMainThreadExecutor: Executor,
                              private val mPersister: Persister = Persister.get,
                              private val mGolosApi: GolosApi = GolosApi.get,
                              private val mUserSettings: UserSettingsRepository = UserSettingsImpl(),
                              poster: Poster? = null,
                              notificationsRepository: PushNotificationsRepositoryImpl? = null,
                              avatarsRepository: GolosUsersRepository? = null,
                              golosServices: GolosServices? = null,
                              private val mHtmlizer: Htmlizer = KnifeHtmlizer,
                              private val mExchangesRepository: ExchangesRepository = ExchangesRepository(Executors.newSingleThreadExecutor(), mMainThreadExecutor),
                              private val mLogger: ExceptionLogger?) : Repository() {


    private val mUsersRepository: GolosUsersRepository

    private val mAppReadyStatusLiveData = MutableLiveData<ReadyStatus>()

    private val mRequests = Collections.synchronizedSet(HashSet<RepositoryRequests>())

    private val mTags = MutableLiveData<List<Tag>>()
    private val mLocalizedTags = MutableLiveData<List<LocalizedTag>>()

    //feed posts lists
    private val mFilteredMap: HashMap<StoryRequest, MutableLiveData<StoriesFeed>> = HashMap()

    //votes
    private var mVotesLiveData = Pair<Long, MutableLiveData<List<VotedUserObject>>>(Long.MIN_VALUE, MutableLiveData())

    /* //users data
     private val mUsersAccountInfo: HashMap<String, MutableLiveData<GolosUserAccountInfo>> = HashMap()
     private val mUsersSubscriptions: HashMap<String, MutableLiveData<List<UserObject>>> = HashMap()
     private val mUsersSubscribers: HashMap<String, MutableLiveData<List<UserObject>>> = HashMap()*/

    //current user data
    private val mAuthLiveData = MutableLiveData<ApplicationUser>()
    private val mLastPostLiveData = MutableLiveData<CreatePostResult>()
    //  private val mCurrentUserSubscriptions = MutableLiveData<List<UserBlogSubscription>>()
    private val mUserSubscribedTags = MutableLiveData<Set<Tag>>()

    private val mPoster: Poster = poster ?: Poster(this, mGolosApi, mLogger)
    private val mNotificationsRepository: PushNotificationsRepository
    private val mGolosServices: GolosServices

    @WorkerThread
    private fun loadStories(limit: Int,
                            type: FeedType,
                            filter: StoryFilter?,
                            truncateBody: Int, startAuthor: String?,
                            startPermlink: String?): List<StoryWithComments> {
        if (type == FeedType.PERSONAL_FEED || type == FeedType.COMMENTS || type == FeedType.BLOG) {
            if (filter?.userNameFilter == null) throw IllegalStateException(" for this types of stories," +
                    "user filter must be set")
        }
        val out = mGolosApi.getStories(if (startPermlink == null && startAuthor == null) limit else limit + 1,
                type,
                truncateBody,
                filter,
                startAuthor,
                startPermlink)

        setUpWrapperOnStoryItems(out)
        return out
    }

    init {
        mFilteredMap.apply {
            put(StoryRequest(FeedType.ACTUAL, null), MutableLiveData())
            put(StoryRequest(FeedType.POPULAR, null), MutableLiveData())
            put(StoryRequest(FeedType.NEW, null), MutableLiveData())
            put(StoryRequest(FeedType.PROMO, null), MutableLiveData())
            put(StoryRequest(FeedType.UNCLASSIFIED, null), MutableLiveData())
        }
        Transformations.map(mTags) {
            workerExecutor.execute {
                val lt = it.map { LocalizedTag(it) }
                mMainThreadExecutor.execute {
                    mLocalizedTags.value = lt
                }
            }
        }.observeForever({})
        mNotificationsRepository =
                notificationsRepository ?: PushNotificationsRepositoryImpl(mUserSettings)

        mGolosServices = golosServices ?: GolosServicesImpl(userDataProvider = object : UserDataProvider {
            override val appUserData: LiveData<ApplicationUser>
                get() = this@RepositoryImpl.appUserData
        })
        mUsersRepository = avatarsRepository ?: UsersRepositoryImpl(mPersister,
                this,
                mGolosApi)
    }

    override val usersAvatars: LiveData<Map<String, String?>>
        get() = mUsersRepository.usersAvatars

    override fun onAppCreate(ctx: Context) {
        super.onAppCreate(ctx)
        mUserSettings.setUp(ctx)
        mExchangesRepository.setUp(ctx)
        mNotificationsRepository.setUp()
        try {
            val userData = mPersister.getActiveUserData()
            if (userData != null) {
                if (userData.privateActiveWif != null || userData.privatePostingWif != null) {
                    mAuthLiveData.value = ApplicationUser(userData.userName!!, true)
                    setActiveUserAccount(userData.userName
                            ?: return, userData.privateActiveWif, userData.privatePostingWif)

                    mUsersRepository.requestUsersAccountInfoUpdate(listOf(userData.userName.orEmpty()))
                } else {
                    deleteUserdata()
                }
            }

        } catch (e: Throwable) {
            deleteUserdata()
        }
        mGolosServices.setUp()
        (mUsersRepository as UsersRepositoryImpl).setUp()

        prepareForLaunch()
    }

    override fun getEvents(type: List<EventType>?): LiveData<List<GolosEvent>> {
        return mGolosServices.getEvents(type)
    }

    override fun requestEventsUpdate(type: List<EventType>?, fromId: String?, limit: Int, completionHandler: (Unit, GolosError?) -> Unit) {
        mGolosServices.requestEventsUpdate(type, fromId, limit, completionHandler)
    }

    override fun lookupUsers(username: String): LiveData<List<String>> {
        return mUsersRepository.lookupUsers(username)
    }

    private fun getStory(blog: String?, author: String, permlink: String): StoryWithComments {
        val story = if (blog != null) mGolosApi.getStory(blog, author, permlink) { list ->
            mUsersRepository.addAccountInfo(list)
        }
        else mGolosApi.getStoryWithoutComments(author, permlink)

        setUpWrapperOnStoryItems(Collections.singletonList(story))
        return story
    }

    override fun authWithMasterKey(name: String, masterKey: String, listener: (UserAuthResponse) -> Unit) {
        networkExecutor.execute {
            try {
                val resp = auth(name, masterKey, null, null)
                mMainThreadExecutor.execute {
                    listener.invoke(resp)
                }
            } catch (e: Exception) {
                logException(e)
                mMainThreadExecutor.execute {
                    listener.invoke(UserAuthResponse(false,
                            error = GolosErrorParser.parse(e),
                            accountInfo = GolosUserAccountInfo(name)))
                }
            }
        }
    }

    override fun authWithActiveWif(name: String, activeWif: String, listener: (UserAuthResponse) -> Unit) {
        networkExecutor.execute {
            try {
                val resp = auth(name, null, activeWif, null)
                mMainThreadExecutor.execute {
                    listener.invoke(resp)
                }
            } catch (e: Exception) {
                logException(e)
                mMainThreadExecutor.execute {
                    listener.invoke(UserAuthResponse(false,
                            error = GolosErrorParser.parse(e),
                            accountInfo = GolosUserAccountInfo(name)))
                }
            }
        }
    }

    override fun authWithPostingWif(name: String, postingWif: String, listener: (UserAuthResponse) -> Unit) {
        networkExecutor.execute {
            try {
                val resp = auth(name, null, null, postingWif)
                Timber.e("resp = $resp")
                mMainThreadExecutor.execute {
                    listener.invoke(resp)
                }

            } catch (e: Exception) {
                logException(e)
                mMainThreadExecutor.execute {
                    listener.invoke(UserAuthResponse(false,
                            error = GolosErrorParser.parse(e),
                            accountInfo = GolosUserAccountInfo(name)))
                }
            }
        }
    }

    override fun getAppReadyStatus(): LiveData<ReadyStatus> {
        return mAppReadyStatusLiveData
    }

    override fun lastCreatedPost(): LiveData<CreatePostResult> {
        return mLastPostLiveData
    }

    private fun auth(userName: String, masterKey: String?, activeWif: String?, postingWif: String?): UserAuthResponse {
        val userName = userName.removePrefix("@").toLowerCase()

        val response = mGolosApi.auth(userName, masterKey, activeWif, postingWif)
        if (response.isKeyValid) {

            val userData = AppUserData.fromPositiveAuthResponse(response)
            mPersister.saveUserData(userData)
            setActiveUserAccount(userName, response.activeAuth?.second, response.postingAuth?.second)

            mMainThreadExecutor.execute {
                mUsersRepository.addAccountInfo(listOf(response.accountInfo))
                mUsersRepository.requestGolosUserSubscribersUpdate(userData.userName.orEmpty())
                mUsersRepository.requestGolosUserSubscriptionsUpdate(userData.userName.orEmpty())

                mAuthLiveData.value = ApplicationUser(userName, true)
            }
        }
        if (response.isKeyValid) {
            setUpWrapperOnStoryItems(allLiveData()
                    .map { it.value }
                    .map { it?.items }
                    .filter { it != null }
                    .map { it!! }
                    .flatMap { it }, skipHtmlizer = true)
        }
        return response
    }

    private fun setUpWrapperOnStoryItems(items: List<StoryWithComments>,
                                         skipVoteStatusTest: Boolean = false,
                                         skipEditableTest: Boolean = false,
                                         skipHtmlizer: Boolean = false) {
        val name = mAuthLiveData.value?.name
        if (!isUserLoggedIn() || name == null) {
            items.forEach {
                if (!skipEditableTest) it.storyWithState()?.isStoryEditable = false
                if (!skipVoteStatusTest) it.rootStory()?.userVotestatus = GolosDiscussionItem.UserVoteType.NOT_VOTED_OR_ZERO_WEIGHT
                if (!skipHtmlizer) it.storyWithState()?.asHtmlString = mHtmlizer.toHtml(it.storyWithState()?.story?.cleanedFromImages
                        ?: "")
                it.storyWithState()?.exchangeValues = mExchangesRepository.getExchangeLiveData().value ?: ExchangeValues.nullValues
                it.getFlataned().forEach { it.exchangeValues = mExchangesRepository.getExchangeLiveData().value ?: ExchangeValues.nullValues }
            }
            return
        }
        items.forEach {
            if (!skipVoteStatusTest) it.rootStory()?.userVotestatus = it.rootStory()?.isUserVotedOnThis(name) ?: GolosDiscussionItem.UserVoteType.NOT_VOTED_OR_ZERO_WEIGHT

            if (!skipEditableTest) it.storyWithState()?.isStoryEditable = canUserEditDiscussionItem(it.rootStory()
                    ?: return@forEach)
            if (!skipEditableTest) it.getFlataned().forEach {
                it.isStoryEditable = canUserEditDiscussionItem(it.story)
                it.story.userVotestatus = it.story.isUserVotedOnThis(name)
            }
            if (!skipHtmlizer) it.storyWithState()?.asHtmlString = mHtmlizer.toHtml(it.storyWithState()?.story?.cleanedFromImages
                    ?: "")
            it.storyWithState()?.exchangeValues = mExchangesRepository.getExchangeLiveData().value ?: ExchangeValues.nullValues
            it.getFlataned().forEach { it.exchangeValues = mExchangesRepository.getExchangeLiveData().value ?: ExchangeValues.nullValues }
        }
    }

    override fun requestApplicationUserDataUpdate() {
        if (isUserLoggedIn()) {
            val userData = mPersister.getActiveUserData()!!
            networkExecutor.execute {
                try {
                    val response = auth(userData.userName ?: "",
                            null,
                            userData.privateActiveWif,
                            userData.privatePostingWif)
                    if (!response.isKeyValid) {
                        mMainThreadExecutor.execute {
                            try {
                                deleteUserdata()
                            } catch (e: Exception) {
                                logException(e)
                            }
                        }
                    }
                } catch (e: Exception) {
                    logException(e)
                    if (e is IllegalArgumentException
                            && e.message?.contains("key must be 51 chars") == true) {
                        mMainThreadExecutor.execute { deleteUserdata() }
                    }
                }
            }
        } else {
            mMainThreadExecutor.execute {
                mAuthLiveData.value = null
            }
        }
    }

    override fun getGolosUserAccountInfos(): LiveData<Map<String, GolosUserAccountInfo>> {
        return mUsersRepository.getGolosUserAccountInfos()
    }

    override fun requestUsersAccountInfoUpdate(golosUserName: List<String>, completionHandler: (Unit, GolosError?) -> Unit) {
        traceCaller()
        mUsersRepository.requestUsersAccountInfoUpdate(golosUserName, completionHandler)
    }

    override fun getGolosUserSubscribers(golosUserName: String): LiveData<List<String>> {
        return mUsersRepository.getGolosUserSubscribers(golosUserName)
    }

    override fun requestGolosUserSubscribersUpdate(golosUserName: String, completionHandler: (Unit, GolosError?) -> Unit) {
        return mUsersRepository.requestGolosUserSubscribersUpdate(golosUserName, completionHandler)
    }

    override fun getGolosUserSubscriptions(golosUserName: String): LiveData<List<String>> {
        return mUsersRepository.getGolosUserSubscriptions(golosUserName)
    }

    override fun requestGolosUserSubscriptionsUpdate(golosUserName: String, completionHandler: (Unit, GolosError?) -> Unit) {
        return mUsersRepository.requestGolosUserSubscriptionsUpdate(golosUserName, completionHandler)
    }

    override fun subscribeOnGolosUserBlog(user: String, completionHandler: (Unit, GolosError?) -> Unit) {
        return mUsersRepository.subscribeOnGolosUserBlog(user, completionHandler)
    }

    override fun unSubscribeFromGolosUserBlog(user: String, completionHandler: (Unit, GolosError?) -> Unit) {
        return mUsersRepository.unSubscribeFromGolosUserBlog(user, completionHandler)
    }

    override fun addAccountInfo(list: List<GolosUserAccountInfo>) {
        return mUsersRepository.addAccountInfo(list)
    }

    override val currentUserSubscriptionsUpdateStatus: LiveData<Map<String, UpdatingState>>
        get() = mUsersRepository.currentUserSubscriptionsUpdateStatus

    override fun deleteUserdata() {
        try {
            mPersister.deleteUserData()
            mAuthLiveData.value = null
            allLiveData().forEach {
                it.value?.items?.forEach {
                    it.rootStory()?.userVotestatus = GolosDiscussionItem.UserVoteType.NOT_VOTED_OR_ZERO_WEIGHT
                    it.storyWithState()?.isStoryEditable = false

                    it.getFlataned().forEach {
                        it.story.userVotestatus = GolosDiscussionItem.UserVoteType.NOT_VOTED_OR_ZERO_WEIGHT
                        it.updatingState = UpdatingState.DONE
                        it.isStoryEditable = false
                    }
                }
                it.value = it.value
            }
            mNotificationsRepository.dismissAllNotifications()
        } catch (e: Exception) {
            logException(e)
        }

    }

    private fun setActiveUserAccount(userName: String, privateActiveWif: String?, privatePostingWif: String?) {
        if ((privateActiveWif != null && privateActiveWif.isNotEmpty()) ||
                (privatePostingWif != null && privatePostingWif.isNotEmpty())) {
            val keys = HashSet<ImmutablePair<PrivateKeyType, String>>()
            if (privateActiveWif != null) keys.add(ImmutablePair(PrivateKeyType.ACTIVE, privateActiveWif))
            if (privatePostingWif != null) keys.add(ImmutablePair(PrivateKeyType.POSTING, privatePostingWif))
            Golos4J.getInstance().addAccount(AccountName(userName), keys, true)
        }
    }

    override fun getStories(type: FeedType, filter: StoryFilter?): LiveData<StoriesFeed> {
        return convertFeedTypeToLiveData(type, filter)
    }

    override fun requestStoriesListUpdate(limit: Int,
                                          type: FeedType,
                                          filter: StoryFilter?,
                                          startAuthor: String?,
                                          startPermlink: String?, completionHandler: (Unit, GolosError?) -> Unit) {
        val request = StoriesRequest(limit, type, startAuthor, startPermlink, filter)
        if (mRequests.contains(request)) return
        mRequests.add(request)
        networkExecutor.execute {
            try {
                val discussions = loadStories(limit, type, filter, 1024, startAuthor, startPermlink)

                mMainThreadExecutor.execute {
                    val updatingFeed = convertFeedTypeToLiveData(type, filter)
                    var out = discussions
                    if (startAuthor != null && startPermlink != null) {
                        val current = ArrayList(updatingFeed.value?.items
                                ?: ArrayList<StoryWithComments>())
                        out = current + out.slice(1..out.lastIndex)
                    }
                    val feed = StoriesFeed(out.toArrayList(), type, filter)
                    updatingFeed.value = feed
                    completionHandler.invoke(Unit, null)
                }
                mRequests.remove(request)

            } catch (e: Exception) {
                logException(e)
                mRequests.remove(request)
                mMainThreadExecutor.execute {
                    val updatingFeed = convertFeedTypeToLiveData(type, filter)
                    updatingFeed.value = StoriesFeed(updatingFeed.value?.items ?: ArrayList(),
                            updatingFeed.value?.type ?: FeedType.NEW,
                            filter,
                            GolosErrorParser.parse(e))
                    completionHandler.invoke(Unit, GolosErrorParser.parse(e))
                }
            }
        }
    }

    override fun vote(comment: StoryWrapper, percents: Short) {
        if (percents > 100 || percents < -100) return
        voteInternal(comment, percents)
    }

    override fun cancelVote(comment: StoryWrapper) {
        voteInternal(comment, 0)
    }

    private fun voteInternal(discussionItem: StoryWrapper,
                             voteStrength: Short) {
        var isVoted = false
        var votedItem: StoryWrapper? = null
        var listOfList = allLiveData()
        val replacer = StorySearcherAndReplacer()
        listOfList.forEach {
            val storiesAll = it.value?.items ?: ArrayList()
            discussionItem.updatingState = UpdatingState.UPDATING
            val result = replacer.findAndReplace(discussionItem, storiesAll)
            if (result) {
                it.value = it.value?.setNewError(null)
            }
        }
        networkExecutor.execute {
            listOfList = allLiveData()
            try {
                listOfList.forEach {
                    val storiesAll = it.value?.items ?: ArrayList()
                    val story = discussionItem.story
                    if (!isVoted) {
                        val currentStory = if (voteStrength != 0.toShort()) mGolosApi.vote(story.author, story.permlink, voteStrength)
                        else mGolosApi.cancelVote(story.author, story.permlink)

                        currentStory.avatarPath = story.avatarPath
                        votedItem = StoryWrapper(currentStory,
                                UpdatingState.DONE,
                                mExchangesRepository.getExchangeLiveData().value
                                        ?: ExchangeValues.nullValues,
                                canUserEditDiscussionItem(currentStory),
                                mHtmlizer.toHtml(currentStory.body))

                        replacer.findAndReplace(votedItem!!, storiesAll)

                        currentStory.userVotestatus = when {
                            voteStrength > 0 -> GolosDiscussionItem.UserVoteType.VOTED
                            voteStrength < 0 -> GolosDiscussionItem.UserVoteType.FLAGED_DOWNVOTED
                            else -> GolosDiscussionItem.UserVoteType.NOT_VOTED_OR_ZERO_WEIGHT
                        }

                        mMainThreadExecutor.execute {
                            it.value = it.value?.setNewError(null)
                        }
                        isVoted = true
                    } else {
                        votedItem?.let { voteItem ->
                            val replaced = replacer.findAndReplace(voteItem, storiesAll)
                            if (replaced) {
                                mMainThreadExecutor.execute {
                                    it.value = it.value?.setNewError(null)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logException(e)
                Timber.e("error $e")
                mMainThreadExecutor.execute {
                    discussionItem.updatingState = UpdatingState.DONE
                    listOfList.forEach {
                        val storiesAll = it.value?.items ?: ArrayList()
                        val replaced = replacer.findAndReplace(discussionItem, storiesAll)
                        if (replaced) {

                            it.value = StoriesFeed(storiesAll, it.value?.type ?: FeedType.NEW,
                                    error = GolosErrorParser.parse(e), filter = it.value?.filter, isFeedActual = it.value?.isFeedActual
                                    ?: true)
                        }
                    }
                }
            }
        }
    }


    override fun requestStoryUpdate(story: StoryWithComments, completionListener: (Unit, GolosError?) -> Unit) {
        requestStoryUpdate(story.storyWithState() ?: return, completionListener)
    }

    private fun requestStoryUpdate(story: StoryWrapper, completionListener: (Unit, GolosError?) -> Unit) {
        networkExecutor.execute {
            try {
                val updatedStory = getStory(story.story.categoryName,
                        story.story.author,
                        story.story.permlink)

                val listOfList = allLiveData()
                val replacer = StorySearcherAndReplacer()

                setUpWrapperOnStoryItems(Collections.singletonList(updatedStory))

                listOfList.forEach {
                    val allItems = ArrayList(it.value?.items ?: ArrayList())
                    if (replacer.findAndReplace(updatedStory, allItems)) {
                        mMainThreadExecutor.execute {
                            it.value = StoriesFeed(allItems, it.value?.type
                                    ?: FeedType.NEW, it.value?.filter,
                                    isFeedActual = it.value?.isFeedActual ?: false)
                            completionListener.invoke(Unit, null)
                        }
                    }
                }
            } catch (e: Exception) {
                mLogger?.log(e)
            }
        }
    }

    override fun getExchangeLiveData(): LiveData<ExchangeValues> {
        return mExchangesRepository.getExchangeLiveData()
    }

    override fun requestStoryUpdate(author: String,
                                    permLink: String,
                                    blog: String?,
                                    feedType: FeedType,
                                    completionListener: (Unit, GolosError?) -> Unit) {
        if (feedType != FeedType.UNCLASSIFIED) {
            val allData = allLiveData()
            val item = allData
                    .map { it.value }
                    .filter { it != null }
                    .map { it!! }
                    .map { it.items }
                    .flatMap { it }
                    .find {
                        it.rootStory()?.author == author
                                && it.rootStory()?.permlink == permLink
                    }
            item?.let {
                requestStoryUpdate(it, completionListener)
            }
        } else {
            networkExecutor.execute {
                try {
                    val story = getStory(blog, author, permLink)

                    val liveData = convertFeedTypeToLiveData(FeedType.UNCLASSIFIED, null)
                    mMainThreadExecutor.execute {
                        liveData.value = StoriesFeed(listOf(story).toArrayList(), FeedType.UNCLASSIFIED, liveData.value?.filter)
                        completionListener.invoke(Unit, null)
                    }
                    if (blog == null) {
                        requestStoryUpdate(story, completionListener)
                    }
                } catch (e: Exception) {
                    logException(e)
                    val error = GolosErrorParser.parse(e)
                    val liveData = convertFeedTypeToLiveData(FeedType.UNCLASSIFIED, null)
                    mMainThreadExecutor.execute {
                        liveData.value = StoriesFeed(arrayListOf(), FeedType.UNCLASSIFIED, null, error)
                        completionListener.invoke(Unit, error)
                    }
                }
            }
        }
    }

    override val appUserData = mAuthLiveData

    override fun createPost(title: String,
                            content: List<EditorPart>,
                            tags: List<String>,
                            resultListener: (CreatePostResult?, GolosError?) -> Unit) {
        val tags = ArrayList(tags)
        val content = ArrayList(content)
        networkExecutor.execute {
            try {
                val result = mPoster.createPost(title, content, tags)
                mMainThreadExecutor.execute {
                    resultListener(result.first, result.second)
                    mLastPostLiveData.value = result.first ?: return@execute
                }
                networkExecutor.execute {
                    if (!isUserLoggedIn()) return@execute

                    val userName = mAuthLiveData.value?.name.orEmpty()

                    val newStory = loadStories(1, FeedType.BLOG, StoryFilter(userNameFilter = listOf(userName)),
                            1024, null, null).firstOrNull() ?: return@execute

                    val comments = convertFeedTypeToLiveData(FeedType.BLOG,
                            StoryFilter(userNameFilter = listOf(userName)))

                    mMainThreadExecutor.execute {
                        val userName = mAuthLiveData.value?.name.orEmpty()

                        comments.value = StoriesFeed(((arrayListOf(newStory) + (comments.value?.items
                                ?: ArrayList())).toArrayList()),
                                FeedType.BLOG,
                                StoryFilter(userNameFilter = userName),
                                comments.value?.error)
                        mUsersRepository.requestUsersAccountInfoUpdate(listOf(userName))
                    }
                }
            } catch (e: Exception) {
                logException(e)
                mMainThreadExecutor.execute {
                    resultListener(null, GolosErrorParser.parse(e))
                }
            } catch (e: SteemResponseError) {
                logException(e)
                mMainThreadExecutor.execute {
                    resultListener(null, GolosErrorParser.parse(e))
                }
            }
        }
    }

    override fun editPost(title: String,
                          content: List<EditorPart>,
                          tags: List<String>,
                          originalPost: StoryWrapper,
                          resultListener: (CreatePostResult?, GolosError?) -> Unit) {

        val tags = ArrayList(tags)
        val content = ArrayList(content)
        networkExecutor.execute {
            try {
                val result = mPoster.editPost(originalPost.story.permlink, title, content, tags)
                mMainThreadExecutor.execute {
                    resultListener(result.first, result.second)
                }
                requestStoryUpdate(originalPost) { _, _ -> }
            } catch (e: Exception) {
                logException(e)
                mMainThreadExecutor.execute {
                    resultListener(null, GolosErrorParser.parse(e))
                }
            }
        }
    }

    override fun createComment(toItem: StoryWrapper,
                               content: List<EditorPart>,
                               resultListener: (CreatePostResult?, GolosError?) -> Unit) {
        if (!isUserLoggedIn()) {
            resultListener.invoke(null, GolosError(ErrorCode.ERROR_AUTH, null, R.string.wrong_credentials))
            return
        }
        networkExecutor.execute {
            try {
                val result = mPoster.createComment(toItem.story, content)
                requestStoryUpdate(toItem) { _, _ ->
                    resultListener.invoke(result.first, result.second)
                }

                mMainThreadExecutor.execute {
                    mLastPostLiveData.value = result.first ?: return@execute
                }
                networkExecutor.execute {
                    if (!isUserLoggedIn()) return@execute
                    val userName = mAuthLiveData.value?.name.orEmpty()

                    val newStory = loadStories(1, FeedType.COMMENTS, StoryFilter(userNameFilter = listOf(userName)),
                            1024, null, null).firstOrNull() ?: return@execute

                    val comments = convertFeedTypeToLiveData(FeedType.COMMENTS,
                            StoryFilter(userNameFilter = listOf(userName)))

                    mMainThreadExecutor.execute {
                        val userName = mAuthLiveData.value?.name.orEmpty()
                        comments.value = StoriesFeed((arrayListOf(newStory) + ArrayList(comments.value?.items
                                ?: ArrayList())).toArrayList(),
                                FeedType.COMMENTS,
                                StoryFilter(userNameFilter = userName),
                                comments.value?.error)

                        mUsersRepository.requestUsersAccountInfoUpdate(listOf(userName))
                    }
                }
            } catch (e: Exception) {
                logException(e)

                mMainThreadExecutor.execute {
                    resultListener(null, GolosErrorParser.parse(e))
                }
            }
        }
    }

    override fun editComment(originalComment: StoryWrapper,
                             content: List<EditorPart>,
                             resultListener: (CreatePostResult?, GolosError?) -> Unit) {

        networkExecutor.execute {
            try {
                val result = mPoster.editComment(originalComment.story, content)
                mMainThreadExecutor.execute {
                    resultListener(result.first, result.second)
                    mLastPostLiveData.value = result.first ?: return@execute
                }
                requestStoryUpdate(originalComment) { _, _ -> }
            } catch (e: Exception) {
                logException(e)
                mMainThreadExecutor.execute {
                    resultListener(null, GolosErrorParser.parse(e))
                }
            }
        }
    }

    override val userSettingsRepository: UserSettingsRepository = mUserSettings

    override val notificationsRepository: PushNotificationsRepository = mNotificationsRepository

    override fun isUserLoggedIn(): Boolean {
        return mAuthLiveData.value?.isLogged == true &&
                mAuthLiveData.value?.name != null
    }

    private fun convertFeedTypeToLiveData(feedtype: FeedType,
                                          filter: StoryFilter?): MutableLiveData<StoriesFeed> {

        return if (filter == null) {
            if (feedtype == FeedType.PERSONAL_FEED ||
                    feedtype == FeedType.BLOG ||
                    feedtype == FeedType.COMMENTS) {
                throw IllegalStateException("type $feedtype is not supported without tag")
            }
            val filteredRequest = StoryRequest(feedtype, filter)
            if (!mFilteredMap.containsKey(filteredRequest)) mFilteredMap[filteredRequest] = MutableLiveData()
            return mFilteredMap[filteredRequest]!!
        } else {
            val filteredRequest = StoryRequest(feedtype, filter)
            if (mFilteredMap.containsKey(filteredRequest)) {
                mFilteredMap[filteredRequest]!!
            } else {
                val liveData = MutableLiveData<StoriesFeed>()
                mFilteredMap[filteredRequest] = liveData
                liveData
            }
        }
    }

    private fun allLiveData(): List<MutableLiveData<StoriesFeed>> {
        return mFilteredMap.values.toList()
    }

    override fun getTrendingTags(): LiveData<List<Tag>> {

        if (mTags.value == null || mTags.value?.size == 0) {
            workerExecutor.execute {
                try {
                    val tags = mPersister.getTags()
                    mMainThreadExecutor.execute {
                        mTags.value = tags
                    }
                } catch (e: Exception) {
                    logException(e)
                }
            }
        }
        return mTags
    }

    override fun getLocalizedTags(): LiveData<List<LocalizedTag>> {
        if (mLocalizedTags.value?.size ?: 0 == 0) {
            getTrendingTags()
        }
        return mLocalizedTags
    }

    override fun requestTrendingTagsUpdate(completionHandler: (List<Tag>, GolosError?) -> Unit) {
        networkExecutor.execute {
            try {
                var tags = mGolosApi.getTrendingTag("", 2997)
                tags = tags.filter { checkTagIsValid(it.name) }
                mPersister.saveTags(tags)
                tags = mPersister.getTags()

                mMainThreadExecutor.execute {
                    mTags.value = tags
                    completionHandler.invoke(tags, null)
                }
            } catch (e: Exception) {
                mMainThreadExecutor.execute {
                    completionHandler.invoke(arrayListOf(), GolosErrorParser.parse(e))
                }
                logException(e)
            }
        }
    }

    override fun getUserSubscribedTags(): LiveData<Set<Tag>> {
        if (mUserSubscribedTags.value == null) {
            val tags = mPersister.getUserSubscribedTags()
            mUserSubscribedTags.value = HashSet(tags)
        }
        return mUserSubscribedTags
    }

    override fun subscribeOnTag(tag: Tag) {
        val currentTags = HashSet(mUserSubscribedTags.value ?: ArrayList())
        currentTags.add(tag)
        mPersister.saveUserSubscribedTags(currentTags.toList())


        mUserSubscribedTags.value = currentTags
    }

    override fun unSubscribeOnTag(tag: Tag) {
        val currentTags = HashSet(mUserSubscribedTags.value ?: ArrayList())
        currentTags.remove(tag)
        mPersister.saveUserSubscribedTags(currentTags.toList())
        mUserSubscribedTags.value = currentTags
    }

    override fun getVotedUsersForDiscussion(id: Long): LiveData<List<VotedUserObject>> {

        if (id == mVotesLiveData.first) return mVotesLiveData.second

        val liveData = MutableLiveData<List<VotedUserObject>>()
        mVotesLiveData = Pair(id, liveData)

        var item: GolosDiscussionItem? =
                allLiveData()
                        .flatMap {
                            it.value?.items ?: arrayListOf()
                        }
                        .find {
                            it.rootStory()?.id == id
                        }
                        ?.rootStory()
        if (item == null)
            item = allLiveData()
                    .flatMap {
                        it.value?.items ?: arrayListOf()
                    }
                    .flatMap { it.getFlataned() }
                    .map { it.story }

                    .find {
                        it.id == id
                    }

        if (item == null || item.activeVotes.size == 0) {
            liveData.value = listOf()
            return liveData
        }
        Timber.e("ld = ${liveData.value}")

        workerExecutor.execute {
            item.let {

                val payouts = RSharesConverter
                        .convertRSharesToGbg2(it.gbgAmount, it.activeVotes.map { it.rshares }, it.votesRshares)
                val voters = it
                        .activeVotes
                        .filter { it.percent != 0 }
                        .mapIndexed { index, voteLight ->
                            VotedUserObject(voteLight.name,
                                    null,
                                    payouts[index])
                        }
                        .distinct()
                        .sorted()

                mMainThreadExecutor.execute {
                    liveData.value = voters
                }
            }
        }
        return liveData
    }
/*
    override fun lookUpUsers(nick: String): LiveData<List<GolosUserWithAvatar>> {
        val golosUsers = MutableLiveData<List<GolosUserWithAvatar>>()
        networkExecutor.execute {
            val users = mGolosApi
                    .lookUpUsers(nick)
                    .toArrayList()
            var avatars = getUserAvatarsFromDb(users.map { it.userName })
            (0 until users.size)
                    .forEach {
                        val userName = users[it].userName
                        if (avatars.containsKey(userName)) users[it] = users[it].setAvatar(avatars[userName])

                    }
            mMainThreadExecutor.execute {
                golosUsers.value = users
            }
            avatars = mGolosApi.getUserAvatars(users
                    .filter { it.avatarPath == null }
                    .map { it.userName }
                    .distinct())
            if (avatars.isNotEmpty()) {
                (0 until users.size)
                        .forEach {
                            val userName = users[it].userName
                            if (avatars.containsKey(userName)) {
                                users[it] = users[it].setAvatar(avatars[userName])
                            }
                        }
                mMainThreadExecutor.execute {
                    golosUsers.value = users
                }
            }
        }

        return golosUsers
    }*/


    override fun requestInitRetry() {
        mPersister.deleteAllStories()
        prepareForLaunch()
    }

    private fun prepareForLaunch() {
        workerExecutor.execute {
            try {
                val savedStories = mPersister.getStories()
                        .filter { it.value.items.size != 0 }
                if (savedStories.isEmpty()) {
                    requestStoriesListUpdate(20,
                            if (isUserLoggedIn()) FeedType.PERSONAL_FEED else FeedType.NEW,
                            filter = if (isUserLoggedIn()) StoryFilter(userNameFilter = mAuthLiveData.value?.name.orEmpty()) else null,
                            completionHandler = { _, e ->
                                if (e != null) mAppReadyStatusLiveData.value = ReadyStatus(false, e)
                                else mAppReadyStatusLiveData.value = ReadyStatus(true, null)
                            })

                } else {

                    savedStories.map { it.value.items }
                            .map {
                                setUpWrapperOnStoryItems(it, skipVoteStatusTest = true, skipEditableTest = true)
                                it
                            }
                    mMainThreadExecutor.execute {
                        savedStories
                                .mapValues {
                                    val items = MutableLiveData<StoriesFeed>()
                                    it.value.isFeedActual = false
                                    items.value = it.value
                                    items
                                }
                                .onEach {
                                    val key = it.key
                                    val value = it.value
                                    if (mFilteredMap.containsKey(key)) {
                                        mFilteredMap[key]?.value = value.value
                                    } else {
                                        mFilteredMap[key] = value
                                    }
                                }
                        mAppReadyStatusLiveData.value = ReadyStatus(true, null)

                    }

                }
                requestApplicationUserDataUpdate()
            } catch (e: Exception) {
                e.printStackTrace()
                logException(e)
                mMainThreadExecutor.execute {
                    mFilteredMap.clear()
                    mAppReadyStatusLiveData.value = ReadyStatus(false, GolosErrorParser.parse(e))
                }
            }
        }
    }

    override fun onAppStop() {
        super.onAppStop()
        workerExecutor.execute {
            try {
                mPersister.deleteAllStories()
                val storiesToSave = mFilteredMap
                        .filter { it.value.value != null }
                        .filter {
                            it.key.filter == null ||
                                    it.key.filter!!.tagFilter.size > 1 ||
                                    (isUserLoggedIn() &&
                                            it.key.filter!!.userNameFilter.size == 1 &&
                                            it.key.filter!!.userNameFilter[0] == mAuthLiveData.value?.name)
                        }
                        .mapValues {
                            val out = it.value.value!!.copy()
                            if (out.items.size > 40) {
                                val copy = out.items.slice(0..40)
                                out.items.clear()
                                out.items.addAll(copy)
                            }
                            out
                        }
                storiesToSave

                        .flatMap { it.value.items }
                        .filter { it.rootStory() != null }
                        .map { it.rootStory()!! }
                        .forEach {
                            var items = it.activeVotes.distinct().toArrayList()
                            if (items.size > 100) {
                                items.sortBy { -it.rshares }
                                items = items.slice(0..100).toArrayList()

                            }
                            it.activeVotes.clear()
                            it.activeVotes.addAll(items)
                        }

                mPersister.saveStories(storiesToSave)
            } catch (e: Exception) {
                e.printStackTrace()
                logException(e)
            }
        }
    }

    private fun logException(e: Throwable) {
        Timber.e(e)
        e.printStackTrace()
        mLogger?.log(e)
    }

    private fun canUserEditDiscussionItem(item: GolosDiscussionItem): Boolean {
        return isUserLoggedIn() && item.author == mAuthLiveData.value?.name.orEmpty()
    }
}
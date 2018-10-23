package io.golos.golos.repository

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
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
import io.golos.golos.screens.events.toSingletoneList
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryWithComments
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
internal class RepositoryImpl(private val networkExecutor: Executor = Executors.newSingleThreadExecutor(),
                              private val workerExecutor: Executor = Executors.newSingleThreadExecutor(),
                              private val mMainThreadExecutor: Executor,
                              private val mPersister: Persister = Persister.get,
                              private val mGolosApi: GolosApi = GolosApi.get,
                              private val mUserSettings: UserSettingsRepository = UserSettingsImpl(),
                              poster: Poster? = null,
                              notificationsRepository: PushNotificationsRepositoryImpl? = null,
                              avatarsRepository: GolosUsersRepository? = null,
                              golosServices: GolosServices? = null,
                              private val mExchangesRepository: ExchangesRepository = ExchangesRepository(Executors.newSingleThreadExecutor(), MainThreadExecutor()),
                              private val mLogger: ExceptionLogger?) : Repository() {


    private val mUsersRepository: GolosUsersRepository

    private val mAppReadyStatusLiveData = MutableLiveData<ReadyStatus>()

    private val mRequests = Collections.synchronizedSet(HashSet<RepositoryRequests>())
    private val mStoryUpdateRequests = Collections.synchronizedSet(HashSet<StoryLoadRequest>())

    private val mTags = MutableLiveData<List<Tag>>()
    private val mLocalizedTags = MutableLiveData<List<LocalizedTag>>()

    //feed posts lists
    private val mFilteredMap: HashMap<StoryRequest, MutableLiveData<StoriesFeed>> = HashMap()

    //votes
    private var mActiveVotesLiveData = Pair<Long, MutableLiveData<List<VotedUserObject>>>(Long.MIN_VALUE, MutableLiveData())

    private val mVotesState = MutableLiveData<List<GolosDiscussionItemVotingState>>()

    //current user data
    private val mAuthLiveData = MutableLiveData<ApplicationUser>()
    private val mLastPostLiveData = MutableLiveData<CreatePostResult>()
    private val mUserSubscribedTags = MutableLiveData<Set<Tag>>()
    private val mCurrentUserBlogEntries = MutableLiveData<List<GolosBlogEntry>>()

    private val mPoster: Poster = poster ?: Poster(this, mGolosApi, mLogger)
    private val mNotificationsRepository: PushNotificationsRepository
    private val mGolosServices: GolosServices

    private val mRepostingStates = MutableLiveData<Set<RepostingState>>()


    private val mLastRepost = OneShotLiveData<Unit>()
    private val mUsersBlogEntriesMap = HashMap<String, MutableLiveData<List<GolosBlogEntry>>>()


    @WorkerThread
    private fun loadDiscussions(limit: Int,
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
        if (type == FeedType.BLOG) {
            val blogOwner = filter?.userNameFilter?.firstOrNull() ?: return out

            mMainThreadExecutor.execute {
                val userBlogEntries = (getBlogEntriesFor(blogOwner) as? MutableLiveData<List<GolosBlogEntry>>)?.value.orEmpty().toHashSet()
                out.forEach {
                    userBlogEntries.add(GolosBlogEntry(it.rootStory.author, blogOwner, it.rootStory.id.toInt(), it.rootStory.permlink))
                }
                (getBlogEntriesFor(blogOwner) as? MutableLiveData<List<GolosBlogEntry>>)?.value = userBlogEntries.toList()
            }
        }

        return out
    }

    override val votingStates: LiveData<List<GolosDiscussionItemVotingState>>
        get() = mVotesState

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
        mVotesState.value = arrayListOf()
    }


    private val mRepostsMap = Transformations.map(mCurrentUserBlogEntries) { list ->
        list.asSequence().filter { it.author != it.blogOwner }.associateBy { entry -> entry.permlink }
    }

    override fun getBlogEntriesFor(user: String): LiveData<List<GolosBlogEntry>> {
        if (!mUsersBlogEntriesMap.containsKey(user)) mUsersBlogEntriesMap[user] = MutableLiveData()
        return mUsersBlogEntriesMap[user]!!
    }

    override val currentUserRepostedBlogEntries: LiveData<Map<String, GolosBlogEntry>> = mRepostsMap

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


    override val lastRepost: LiveData<Unit>
        get() = mLastRepost

    override fun getUnreadEventsCount(): LiveData<Int> {
        return mGolosServices.getFreshEventsCount()
    }

    override fun setEventsRead(listOfEventsIds: List<String>) {
        mGolosServices.markAsRead(listOfEventsIds)
    }

    override fun getEvents(type: List<EventType>?): LiveData<List<GolosEvent>> {
        return mGolosServices.getEvents(type)
    }

    override val currentUserSubscriptions: LiveData<List<String>>
        get() = mUsersRepository.currentUserSubscriptions

    override fun requestEventsUpdate(type: List<EventType>?, fromId: String?, limit: Int, markAsRead: Boolean, completionHandler: (Unit, GolosError?) -> Unit) {
        mGolosServices.requestEventsUpdate(type, fromId, limit, markAsRead, completionHandler)
    }

    override val currentUserRepostStates: LiveData<Map<String, RepostingState>> = Transformations.map(mRepostingStates) { states ->
        states.orEmpty().associateBy { it.postPermlink }
    }

    override fun getRequestStatus(forType: EventType?): LiveData<UpdatingState> {
        return mGolosServices.getRequestStatus(forType)
    }

    override fun lookupUsers(username: String): LiveData<List<String>> {
        return mUsersRepository.lookupUsers(username)
    }


    private fun getStory(blog: String?,
                         author: String,
                         permlink: String,
                         voteLimit: Int?): StoryWithComments {
        val story = if (blog != null) mGolosApi.getStory(blog, author, permlink, voteLimit) { list ->
            mUsersRepository.addAccountInfo(list)
        }
        else mGolosApi.getStoryWithoutComments(author, permlink, voteLimit)
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
                workerExecutor.execute {
                    requestBlogEntriesUpdate()
                }
            }
        }
        return response
    }

    @WorkerThread
    private fun requestBlogEntriesUpdate(iterationSize: Int = 100) {
        val currentUserName = mAuthLiveData.value?.name ?: return
        val entries = arrayListOf<GolosBlogEntry>()
        val limit = iterationSize
        val entriesFromBd = mPersister.getBlogEntries()

        val lastSavedId = entriesFromBd.firstOrNull()?.entryId ?: -1
        var fromId: Int? = null

        while (true) {
            val entriesFromBC = mGolosApi.getBlogEntries(currentUserName, fromId, limit.toShort()).toArrayList()
            entries.addAll(entriesFromBC)

            if (entriesFromBC.size != limit || lastSavedId >= entriesFromBC.lastOrNull()?.entryId ?: Int.MAX_VALUE) break
            fromId = entriesFromBC.lastOrNull()?.entryId ?: -1
        }
        mPersister.saveBlogEntries(entries)
        val result = entries.toHashSet() + entriesFromBd

        mMainThreadExecutor.execute {

            mCurrentUserBlogEntries.value = result.toList()
            Timber.e("mCurrentUserBlogEntries size = ${mCurrentUserBlogEntries.value?.size}")
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
            mVotesState.value = null
            mCurrentUserBlogEntries.value = null
            mRepostingStates.value = null
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
                val discussions = loadDiscussions(limit, type, filter, 1024, startAuthor, startPermlink)

                mMainThreadExecutor.execute {
                    val updatingFeed = convertFeedTypeToLiveData(type, filter)
                    var out = discussions
                    if (startAuthor != null && startPermlink != null) {
                        val current = ArrayList(updatingFeed.value?.items.orEmpty())

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
                            filter)
                    completionHandler.invoke(Unit, GolosErrorParser.parse(e))
                }
            }
        }
    }

    override fun vote(comment: GolosDiscussionItem, percents: Short) {
        if (percents > 100 || percents < -100) return
        voteInternal(comment, percents)
    }

    override fun repost(discussionItem: GolosDiscussionItem, completionHandler: (Unit, GolosError?) -> Unit) {
        if (!isUserLoggedIn()) return
        if (currentUserRepostedBlogEntries.value.orEmpty().containsKey(discussionItem.permlink)) return

        @MainThread
        fun addRepostState(repostingState: RepostingState) {
            val repostingStates = mRepostingStates.value.orEmpty().filter { it.postId != repostingState.postId }.toHashSet()
            repostingStates += repostingState
            mRepostingStates.value = repostingStates
        }

        addRepostState(RepostingState(discussionItem.id, discussionItem.permlink, UpdatingState.UPDATING, null))
        workerExecutor.execute {
            try {
                mGolosApi.repostPost(discussionItem.author, discussionItem.permlink)
                requestBlogEntriesUpdate(10)

                val newStory = loadDiscussions(1, FeedType.BLOG, StoryFilter(userNameFilter = appUserData.value?.name.orEmpty()),
                        1024, null, null).firstOrNull() ?: return@execute
                val comments = convertFeedTypeToLiveData(FeedType.BLOG, StoryFilter(userNameFilter = appUserData.value?.name.orEmpty()))


                mMainThreadExecutor.execute {
                    mCurrentUserBlogEntries.value = mCurrentUserBlogEntries.value.orEmpty() + GolosBlogEntry(discussionItem.author, mAuthLiveData.value?.name.orEmpty(), 0, discussionItem.permlink)
                    comments.value = comments.value?.copy(items = newStory.toSingletoneList() + comments.value?.items.orEmpty())
                    addRepostState(RepostingState(discussionItem.id, discussionItem.permlink, UpdatingState.DONE, null))
                    mLastRepost.value = Unit
                    completionHandler(Unit, null)
                }
            } catch (e: java.lang.Exception) {
                mMainThreadExecutor.execute {
                    addRepostState(RepostingState(discussionItem.id, discussionItem.permlink, UpdatingState.FAILED, GolosErrorParser.parse(e)))
                    completionHandler(Unit, GolosErrorParser.parse(e))
                }
            }
        }
    }

    override fun cancelVote(comment: GolosDiscussionItem) {
        voteInternal(comment, 0)
    }

    private fun voteInternal(discussionItem: GolosDiscussionItem,
                             voteStrength: Short) {

        val listOfList = allLiveData()
        val replacer = StorySearcherAndReplacer()

        val votingStates = mVotesState.value.orEmpty().toArrayList()
        votingStates.removeAll { it.storyId == discussionItem.id }

        mVotesState.value = votingStates + GolosDiscussionItemVotingState(discussionItem.id, voteStrength, UpdatingState.UPDATING).toSingletoneList()
        networkExecutor.execute {

            try {
                val votedDiscussion = if (voteStrength != 0.toShort()) mGolosApi.vote(discussionItem.author, discussionItem.permlink, voteStrength)
                else mGolosApi.cancelVote(discussionItem.author, discussionItem.permlink)

                mMainThreadExecutor.execute {
                    //setting voting item state to  done
                    val votingStates = mVotesState.value.orEmpty().toArrayList()
                    votingStates.forEachIndexed { index, golosDiscussionItemVotingState ->
                        if (golosDiscussionItemVotingState.storyId == votedDiscussion.id && golosDiscussionItemVotingState.votingStrength == voteStrength) {
                            votingStates[index] = golosDiscussionItemVotingState.copy(state = UpdatingState.DONE, error = null)
                        }
                    }
                    mVotesState.value = votingStates
                }
                listOfList.forEach {
                    val currentFeed = it.value ?: return@forEach
                    val result = replacer.findAndReplaceStory(votedDiscussion, currentFeed)//replacing old item with updated item
                    if (result.isChanged) {

                        mMainThreadExecutor.execute {
                            it.value = result.resultingFeed
                        }
                    }
                }
            } catch (e: Exception) {
                logException(e)
                Timber.e("error $e")
                mMainThreadExecutor.execute {
                    //setting voting item state to  done, due to error
                    val votingStates = mVotesState.value.orEmpty().toArrayList()
                    votingStates.forEachIndexed { index, golosDiscussionItemVotingState ->
                        if (golosDiscussionItemVotingState.storyId == discussionItem.id && golosDiscussionItemVotingState.votingStrength == voteStrength) {
                            votingStates[index] = golosDiscussionItemVotingState.copy(state = UpdatingState.DONE, error = GolosErrorParser.parse(e))
                        }
                    }
                    mVotesState.value = votingStates
                    requestStoryUpdate(discussionItem, { _, _ -> })
                }
            }
        }
    }


    private fun requestStoryUpdate(story: GolosDiscussionItem,
                                   completionListener: (Unit, GolosError?) -> Unit) {
        networkExecutor.execute {
            try {
                val updatedStory = getStory(story.categoryName,
                        story.author,
                        story.permlink,
                        null)
                val listOfList = allLiveData()
                val replacer = StorySearcherAndReplacer()

                listOfList.forEach {
                    val allItems = ArrayList(it.value?.items.orEmpty())
                    val result = replacer.findAndReplaceStory(updatedStory, allItems)
                    if (result) {
                        mMainThreadExecutor.execute {
                            it.value = it.value?.copy(items = allItems)
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
                                    loadVotes: Boolean,
                                    loadChildStories: Boolean,
                                    feedType: FeedType,
                                    completionListener: (Unit, GolosError?) -> Unit) {
        if (feedType != FeedType.UNCLASSIFIED) {
            val allData = allLiveData()
            val item = allData
                    .mapNotNull { it.value }
                    .map { it.items }
                    .flatMap { it }
                    .find {
                        it.rootStory.author == author
                                && it.rootStory.permlink == permLink
                    }
            item?.let {
                requestStoryUpdate(it.rootStory, completionListener)
            }
        } else {
            val request = StoryLoadRequest(author, permLink, blog, loadVotes, loadChildStories)
            if (mStoryUpdateRequests.contains(request)) return
            mStoryUpdateRequests.add(request)
            networkExecutor.execute {
                try {
                    var story = getStory(blog, author, permLink, if (loadVotes) null else 0)

                    val liveData = convertFeedTypeToLiveData(FeedType.UNCLASSIFIED, null)
                    val position = liveData.value?.items?.indexOfFirst { it.rootStory.id == story.rootStory.id }
                            ?: -1
                    mMainThreadExecutor.execute {
                        liveData.value = StoriesFeed(
                                if (position == -1) liveData.value?.items.orEmpty().toArrayList().apply {
                                    add(story)
                                } else liveData.value?.items.orEmpty().toArrayList().apply {
                                    val oldStory = this[position]
                                    if (!loadVotes) story.rootStory.activeVotes.addAll(oldStory.rootStory.activeVotes)
                                    this[position] = story
                                },
                                FeedType.UNCLASSIFIED,
                                liveData.value?.filter,

                                true)
                        completionListener.invoke(Unit, null)
                        mStoryUpdateRequests.remove(request)
                    }
                    if (blog == null && loadChildStories) {
                        requestStoryUpdate(story.rootStory, completionListener)
                    }
                } catch (e: Exception) {
                    logException(e)
                    val error = GolosErrorParser.parse(e)
                    val liveData = convertFeedTypeToLiveData(FeedType.UNCLASSIFIED, null)
                    mMainThreadExecutor.execute {
                        liveData.value = StoriesFeed(arrayListOf(), FeedType.UNCLASSIFIED, null)
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

                    val newStory = loadDiscussions(1, FeedType.BLOG, StoryFilter(userNameFilter = listOf(userName)),
                            1024, null, null).firstOrNull() ?: return@execute

                    val comments = convertFeedTypeToLiveData(FeedType.BLOG,
                            StoryFilter(userNameFilter = listOf(userName)))

                    mMainThreadExecutor.execute {
                        val userName = mAuthLiveData.value?.name.orEmpty()

                        comments.value = StoriesFeed(((arrayListOf(newStory) + (comments.value?.items
                                ?: ArrayList())).toArrayList()),
                                FeedType.BLOG,
                                StoryFilter(userNameFilter = userName))
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
                          originalPost: GolosDiscussionItem,
                          resultListener: (CreatePostResult?, GolosError?) -> Unit) {

        val tags = ArrayList(tags)
        val content = ArrayList(content)
        networkExecutor.execute {
            try {
                val result = mPoster.editPost(originalPost.permlink, title, content, tags)
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

    override fun createComment(toItem: GolosDiscussionItem,
                               content: List<EditorPart>,
                               resultListener: (CreatePostResult?, GolosError?) -> Unit) {
        if (!isUserLoggedIn()) {
            resultListener.invoke(null, GolosError(ErrorCode.ERROR_AUTH, null, R.string.wrong_credentials))
            return
        }
        networkExecutor.execute {
            try {
                val result = mPoster.createComment(toItem, content)
                requestStoryUpdate(toItem) { _, _ ->
                    resultListener.invoke(result.first, result.second)
                }

                mMainThreadExecutor.execute {
                    mLastPostLiveData.value = result.first ?: return@execute
                }
                networkExecutor.execute {
                    if (!isUserLoggedIn()) return@execute
                    val userName = mAuthLiveData.value?.name.orEmpty()

                    val newStory = loadDiscussions(1, FeedType.COMMENTS, StoryFilter(userNameFilter = listOf(userName)),
                            1024, null, null).firstOrNull() ?: return@execute

                    val comments = convertFeedTypeToLiveData(FeedType.COMMENTS,
                            StoryFilter(userNameFilter = listOf(userName)))

                    mMainThreadExecutor.execute {
                        val userName = mAuthLiveData.value?.name.orEmpty()
                        comments.value = StoriesFeed((arrayListOf(newStory) + ArrayList(comments.value?.items
                                ?: ArrayList())).toArrayList(),
                                FeedType.COMMENTS,
                                StoryFilter(userNameFilter = userName))

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

    override fun editComment(originalComment: GolosDiscussionItem,
                             content: List<EditorPart>,
                             resultListener: (CreatePostResult?, GolosError?) -> Unit) {

        networkExecutor.execute {
            try {
                val result = mPoster.editComment(originalComment, content)
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

        if (id == mActiveVotesLiveData.first) return mActiveVotesLiveData.second

        val liveData = MutableLiveData<List<VotedUserObject>>()
        mActiveVotesLiveData = Pair(id, liveData)

        var item: GolosDiscussionItem? =
                allLiveData()
                        .flatMap {
                            it.value?.items ?: arrayListOf()
                        }
                        .find {
                            it.rootStory.id == id
                        }
                        ?.rootStory
        if (item == null)
            item = allLiveData()
                    .flatMap {
                        it.value?.items ?: arrayListOf()
                    }
                    .flatMap { it.getFlataned() }
                    .map { it }

                    .find {
                        it.id == id
                    }

        if (item == null || item.activeVotes.size == 0) {
            liveData.value = listOf()
            return liveData
        }
        workerExecutor.execute {
            item.let {

                val payouts = RSharesConverter
                        .convertRSharesToGbg2(it.gbgAmount, it.activeVotes.map { it.rshares }, it.votesRshares)
                Timber.e("voters = ${it.activeVotes} size is ${it.activeVotes.size}")
                Timber.e("payouts  = ${payouts} size is ${payouts.size}")
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

    override fun requestInitRetry() {
        mPersister.deleteAllStories()
        prepareForLaunch()
    }

    private fun prepareForLaunch() {
        workerExecutor.execute {
            try {

                val savedStories = mPersister.getStories()
                        .filter { it.value.items.isNotEmpty() }
                if (savedStories.isEmpty()) {
                    requestStoriesListUpdate(20,
                            if (isUserLoggedIn()) FeedType.PERSONAL_FEED else FeedType.NEW,
                            filter = if (isUserLoggedIn()) StoryFilter(userNameFilter = mAuthLiveData.value?.name.orEmpty()) else null,
                            completionHandler = { _, e ->
                                if (e != null) mAppReadyStatusLiveData.value = ReadyStatus(false, e)
                                else mAppReadyStatusLiveData.value = ReadyStatus(true, null)
                            })

                } else {
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
                            var out = it.value.value!!.copy()

                            if (out.items.size > 10) {
                                val itemsSlice = out.items.slice(0..10)
                                out = out.copy(items = itemsSlice)
                            }
                            out
                        }

                storiesToSave

                        .flatMap { it.value.items }

                        .forEach {
                            val items = it.rootStory.activeVotes.distinct().toArrayList()
                            it.rootStory.activeVotes.clear()
                            it.rootStory.activeVotes.addAll(items)
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
}

private data class StoryLoadRequest(val author: String, val permlink: String, val blog: String?, val loadVotes: Boolean, val loadComents: Boolean)
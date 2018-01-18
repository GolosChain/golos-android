package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.support.annotation.WorkerThread
import eu.bittrade.libs.steemj.Golos4J
import eu.bittrade.libs.steemj.base.models.AccountName
import eu.bittrade.libs.steemj.enums.PrivateKeyType
import eu.bittrade.libs.steemj.exceptions.SteemResponseError
import io.golos.golos.R
import io.golos.golos.repository.api.GolosApi
import io.golos.golos.repository.model.*
import io.golos.golos.repository.persistence.Persister
import io.golos.golos.repository.persistence.model.AccountInfo
import io.golos.golos.repository.persistence.model.UserData
import io.golos.golos.screens.editor.EditorImagePart
import io.golos.golos.screens.editor.EditorPart
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.screens.story.model.SubscribeStatus
import io.golos.golos.utils.*
import org.apache.commons.lang3.tuple.ImmutablePair
import timber.log.Timber
import java.io.File
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

private data class FilteredRequest(val feedType: FeedType,
                                   val filter: StoryFilter)

@Suppress("NAME_SHADOWING")
internal class RepositoryImpl(private val mWorkerExecutor: Executor,
                              private val mMainThreadExecutor: Executor,
                              private val mPersister: Persister,
                              private val mGolosApi: GolosApi,
                              private val mLogger: ExceptionLogger?) : Repository() {
    private val mAvatarRefreshDelay = TimeUnit.DAYS.toMillis(7)

    private val mRequests = Collections.synchronizedSet(HashSet<RepositoryRequests>())

    private val mTags = MutableLiveData<List<Tag>>()

    //feed posts lists
    private val mLiveDataMap: HashMap<FeedType, MutableLiveData<StoryTreeItems>> = HashMap()
    private val mFilteredMap: HashMap<FilteredRequest, MutableLiveData<StoryTreeItems>> = HashMap()

    //users data
    private val mUsersAccountInfo: HashMap<String, MutableLiveData<AccountInfo>> = HashMap()
    private val mUsersSubscriptions: HashMap<String, MutableLiveData<List<UserObject>>> = HashMap()
    private val mUsersSubscribers: HashMap<String, MutableLiveData<List<UserObject>>> = HashMap()

    //current user data
    private val mAuthLiveData = MutableLiveData<UserData>()
    private val mLastPostLiveData = MutableLiveData<CreatePostResult>()
    private val mCurrentUserSubscriptions = MutableLiveData<List<UserBlogSubscription>>()
    private val mUserSubscribedTags = MutableLiveData<Set<Tag>>()

    private fun getStripeItems(limit: Int,
                               type: FeedType,
                               filter: StoryFilter?,
                               truncateBody: Int, startAuthor: String?,
                               startPermlink: String?): List<StoryTree> {
        if (type == FeedType.PERSONAL_FEED || type == FeedType.COMMENTS || type == FeedType.BLOG) {
            if (filter?.userNameFilter == null) throw IllegalStateException(" for this types of stories," +
                    "user filter must be set")
        }
        val out = mGolosApi.getStories(limit, type, truncateBody, filter, startAuthor, startPermlink)

        val name = mPersister.getActiveUserData()?.userName ?: ""
        out.forEach {
            if (name.isNotEmpty()) {
                it.rootStory()?.isUserUpvotedOnThis = it.rootStory()?.isUserVotedOnThis(name) ?: false
            }
            it.rootStory()?.avatarPath = getUserAvatarFromDb(it.rootStory()?.author ?: "_____absent_____")
        }
        return out
    }

    init {
        mAuthLiveData.value = mPersister.getActiveUserData()
        mLiveDataMap.apply {
            put(FeedType.ACTUAL, MutableLiveData())
            put(FeedType.POPULAR, MutableLiveData())
            put(FeedType.NEW, MutableLiveData())
            put(FeedType.PROMO, MutableLiveData())
            put(FeedType.UNCLASSIFIED, MutableLiveData())
        }
    }

    private fun getUserAvatarFromDb(username: String): String? {
        val avatar = mPersister.getAvatarForUser(username)
        val currentTime = System.currentTimeMillis()
        if (avatar != null && currentTime < (avatar.second + mAvatarRefreshDelay)) {
            return avatar.first
        }
        return null
    }

    private fun getStory(blog: String?, author: String, permlink: String): StoryTree {
        val story = if (blog != null) mGolosApi.getStory(blog, author, permlink, { list ->
            list.forEach {
                if (!mUsersAccountInfo.containsKey(it.userName)) {
                    val liveData = MutableLiveData<AccountInfo>()
                    mMainThreadExecutor.execute {
                        liveData.value = it
                        mUsersAccountInfo[it.userName ?: ""] = liveData
                    }
                }
            }

        })
        else mGolosApi.getStoryWithoutComments(author, permlink)
        val name = mPersister.getCurrentUserName()
        if (name != null) {
            story.rootStory()?.isUserUpvotedOnThis = story.rootStory()?.isUserVotedOnThis(name) ?: false
            story.getFlataned().forEach({
                it.story.isUserUpvotedOnThis = it.story.isUserVotedOnThis(name)
            })
        }
        return story
    }

    override fun authWithMasterKey(userName: String, masterKey: String, listener: (UserAuthResponse) -> Unit) {
        mWorkerExecutor.execute {
            try {
                val resp = auth(userName, masterKey, null, null)
                mMainThreadExecutor.execute {
                    listener.invoke(resp)
                }
            } catch (e: Exception) {
                logException(e)
                mMainThreadExecutor.execute {
                    listener.invoke(UserAuthResponse(false,
                            error = GolosErrorParser.parse(e),
                            accountInfo = AccountInfo(userName)))
                }
            }
        }
    }

    override fun authWithActiveWif(login: String, activeWif: String, listener: (UserAuthResponse) -> Unit) {
        mWorkerExecutor.execute {
            try {
                val resp = auth(login, null, activeWif, null)
                mMainThreadExecutor.execute {
                    listener.invoke(resp)
                }
            } catch (e: Exception) {
                logException(e)
                mMainThreadExecutor.execute {
                    listener.invoke(UserAuthResponse(false,
                            error = GolosErrorParser.parse(e),
                            accountInfo = AccountInfo(login)))
                }
            }
        }
    }

    override fun authWithPostingWif(login: String, postingWif: String, listener: (UserAuthResponse) -> Unit) {
        mWorkerExecutor.execute {
            try {
                val resp = auth(login, null, null, postingWif)
                mMainThreadExecutor.execute {
                    listener.invoke(resp)
                }

            } catch (e: Exception) {
                logException(e)
                mMainThreadExecutor.execute {
                    listener.invoke(UserAuthResponse(false,
                            error = GolosErrorParser.parse(e),
                            accountInfo = AccountInfo(login)))
                }
            }
        }
    }

    override fun lastCreatedPost(): LiveData<CreatePostResult> {
        return mLastPostLiveData
    }

    private fun auth(userName: String, masterKey: String?, activeWif: String?, postingWif: String?): UserAuthResponse {
        val userName = userName.removePrefix("@").toLowerCase()

        val response = mGolosApi.auth(userName, masterKey, activeWif, postingWif)
        if (response.isKeyValid) {
            if (response.accountInfo.avatarPath != null)
                mPersister.saveAvatarPathForUser(userName,
                        response.accountInfo.avatarPath,
                        System.currentTimeMillis())

            val userData = UserData.fromPositiveAuthResponse(response)

            mPersister.saveUserData(userData)
            setActiveUserAccount(userName, response.activeAuth?.second, response.postingAuth?.second)

            mMainThreadExecutor.execute {
                mAuthLiveData.value = userData
            }
        }
        return response
    }

    override fun requestActiveUserDataUpdate() {
        if (isUserLoggedIn()) {
            val userData = mPersister.getActiveUserData()!!
            mWorkerExecutor.execute {
                try {
                    val response = auth(userData.userName ?: "",
                            null,
                            userData.privateActiveWif,
                            userData.privatePostingWif)
                    if (!response.isKeyValid) {
                        mMainThreadExecutor.execute { deleteUserdata() }

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
            mAuthLiveData.value = null
        }
    }

    override fun getUserInfo(userName: String): LiveData<AccountInfo> {
        return if (isUserLoggedIn()
                && userName == mAuthLiveData.value?.userName) Transformations.map(mAuthLiveData, {
            it?.toAccountInfo()
        }) else {
            if (!mUsersAccountInfo.containsKey(userName)) {
                mUsersAccountInfo.put(userName, MutableLiveData())
            }
            mUsersAccountInfo[userName]!!
        }
    }

    override fun requestUserInfoUpdate(userName: String,
                                       completionHandler: (AccountInfo, GolosError?) -> Unit) {
        mWorkerExecutor.execute {
            try {
                loadSubscribersIfNeeded()

                if (isUserLoggedIn()//if we updating state of user, that is cashed in current user subscriptions
                        && mCurrentUserSubscriptions.value?.find { it.user.name == userName } != null
                        && userName != mAuthLiveData.value?.userName) {

                    if (!mUsersAccountInfo.containsKey(userName)) mUsersAccountInfo.put(userName, MutableLiveData())

                    val userInfo = mUsersAccountInfo[userName]!!
                    mMainThreadExecutor.execute {
                        userInfo.value = AccountInfo(userName, isCurrentUserSubscribed = isUserSubscribedOn(userName))
                    }
                }


                val accinfo = mGolosApi.getAccountData(userName)
                if (isUserLoggedIn() && userName == mAuthLiveData.value?.userName) {//if we updating current user
                    mMainThreadExecutor.execute {
                        mAuthLiveData.value = UserData(true, accinfo.userMotto, accinfo.avatarPath,
                                accinfo.userName, mAuthLiveData.value?.privateActiveWif, mAuthLiveData.value?.privatePostingWif,
                                accinfo.activePublicKey, accinfo.postingPublicKey, accinfo.subscibesCount,
                                accinfo.subscribersCount, accinfo.gbgAmount, accinfo.golosAmount, accinfo.golosPower,
                                accinfo.accountWorth, accinfo.postsCount, accinfo.safeGbg, accinfo.safeGolos)
                        completionHandler.invoke(accinfo, null)
                    }
                } else {
                    accinfo.isCurrentUserSubscribed = isUserSubscribedOn(accinfo.userName ?: "")
                    if (!mUsersAccountInfo.containsKey(userName)) mUsersAccountInfo.put(userName, MutableLiveData())
                    val userInfo = mUsersAccountInfo[userName]!!

                    mMainThreadExecutor.execute {
                        userInfo.value = accinfo
                        completionHandler.invoke(accinfo, null)
                    }
                }
            } catch (e: Exception) {
                logException(e)
                mMainThreadExecutor.execute {
                    completionHandler.invoke(AccountInfo(userName), GolosErrorParser.parse(e))
                }
            }
        }
    }

    @WorkerThread
    private fun isUserSubscribedOn(onAuthor: String): Boolean {
        if (isUserLoggedIn()) {
            var subs = mCurrentUserSubscriptions.value
            if (subs == null) {
                subs = mGolosApi
                        .getSubscriptions(mPersister.getCurrentUserName() ?: return false, null)
                        .map {
                            UserBlogSubscription(UserObject(it.following.name, getUserAvatarFromDb(it.following.name)),
                                    SubscribeStatus.SubscribedStatus)
                        }
                mMainThreadExecutor.execute {
                    mCurrentUserSubscriptions.value = subs
                }
            }
            return subs.find { it.user.name == onAuthor } != null
        }
        return false
    }

    private fun followOrUnfollow(isFollow: Boolean, user: String, completionHandler: (Unit, GolosError?) -> Unit) {
        if (!isUserLoggedIn()) {
            mMainThreadExecutor.execute {
                completionHandler.invoke(Unit,
                        GolosError(ErrorCode.WRONG_STATE,
                                null,
                                R.string.must_be_logged_in_for_this_action))
            }
            return
        }
        if (isUserLoggedIn() && mPersister.getCurrentUserName() == user) {
            mMainThreadExecutor.execute {
                completionHandler.invoke(Unit,
                        GolosError(ErrorCode.WRONG_STATE,
                                null,
                                R.string.you_cannot_subscribe_on_yourself))
            }
            return
        }
        if ((isFollow && isUserSubscribedOn(user)) || (!isFollow && !isUserSubscribedOn(user))) {
            mMainThreadExecutor.execute {
                completionHandler.invoke(Unit,
                        GolosError(ErrorCode.WRONG_STATE,
                                null,
                                if (isFollow) R.string.you_already_subscribed else R.string.must_be_subscribed_for_action))
            }
            return
        }


        if (isFollow) {
            val users = ArrayList(mCurrentUserSubscriptions.value ?: arrayListOf())
            users.add(UserBlogSubscription(UserObject(user, getUserAvatarFromDb(user)), SubscribeStatus(false, UpdatingState.UPDATING)))
            mCurrentUserSubscriptions.value = users
        } else {
            mCurrentUserSubscriptions.value?.find {
                it.user.name == user
            }?.status = SubscribeStatus(true, UpdatingState.UPDATING)
            mCurrentUserSubscriptions.value = mCurrentUserSubscriptions.value
        }

        mWorkerExecutor.execute {
            try {
                if (isFollow) mGolosApi.follow(user) else mGolosApi.unfollow(user)

                if (isFollow) {//update current user subscriptions list
                    mMainThreadExecutor.execute {
                        mCurrentUserSubscriptions.value?.find {
                            it.user.name == user
                        }?.status = SubscribeStatus.SubscribedStatus
                    }
                    requestSubscribesUpdate(user)
                } else {
                    mMainThreadExecutor.execute {
                        val items = ArrayList((mCurrentUserSubscriptions.value ?: arrayListOf()))
                                .filter { it.user.name != user }
                        mCurrentUserSubscriptions.value = items
                        mAuthLiveData.value?.subscibesCount = items.size.toLong()
                        mAuthLiveData.value = mAuthLiveData.value
                    }
                }


                if (isFollow
                        && mUsersSubscribers.contains(user)) {//add current user to subscribers list of cashed users data
                    val currentUserFollowObject = UserObject(mPersister.getCurrentUserName() ?: "",
                            mAuthLiveData.value?.avatarPath)
                    val subscribers = ArrayList(mUsersSubscribers[user]
                            ?.value ?: ArrayList())
                    subscribers.add(currentUserFollowObject)
                    mMainThreadExecutor.execute {
                        mUsersSubscribers[user]?.value = subscribers
                    }
                }
                val userValue = mUsersAccountInfo[user]
                userValue?.let {
                    val info = it.value
                    if (info != null) {
                        val copy = info.copy()
                        copy.isCurrentUserSubscribed = isFollow
                        if (mUsersSubscribers[user] != null) {
                            copy.subscribersCount = mUsersSubscribers[user]
                                    ?.value
                                    ?.size
                                    ?.toLong() ?: 0L
                        } else {
                            copy.subscribersCount = if (isFollow) copy.subscribersCount + 1 else copy.subscribersCount - 1
                        }
                        mMainThreadExecutor.execute {
                            it.value = copy
                        }
                    }
                }
                mMainThreadExecutor.execute {
                    completionHandler.invoke(Unit, null)
                }
            } catch (e: Exception) {
                logException(e)
                if (e is SteemResponseError) {
                    Timber.e(e.message)
                }
                mMainThreadExecutor.execute {
                    if (isFollow) {
                        mCurrentUserSubscriptions.value = mCurrentUserSubscriptions.value?.filter {
                            it.user.name != user
                        }
                    } else {
                        mCurrentUserSubscriptions.value?.find {
                            it.user.name == user
                        }?.status = SubscribeStatus(true, UpdatingState.FAILED)
                    }
                    mCurrentUserSubscriptions.value = mCurrentUserSubscriptions.value
                    completionHandler.invoke(Unit, GolosErrorParser.parse(e))
                }
            }
        }
    }

    override fun subscribeOnUserBlog(user: String, completionHandler: (Unit, GolosError?) -> Unit) {
        followOrUnfollow(true, user, completionHandler)
    }

    override fun unSubscribeOnUserBlog(user: String, completionHandler: (Unit, GolosError?) -> Unit) {
        followOrUnfollow(false, user, completionHandler)
    }

    @WorkerThread
    private fun requestSubscribesUpdate(startFrom: String?) {
        val objects = mGolosApi
                .getSubscriptions(mPersister.getCurrentUserName() ?: return, startFrom)
                .map {
                    UserBlogSubscription(UserObject(it.following.name, getUserAvatarFromDb(it.following.name)),
                            SubscribeStatus.SubscribedStatus)
                }
        if (startFrom == null) {
            mMainThreadExecutor.execute {
                mCurrentUserSubscriptions.value = objects
            }
        } else {
            val current = mCurrentUserSubscriptions.value ?: arrayListOf()
            current.forEach {
                if (it.user.avatar == null) it.user.avatar = getUserAvatarFromDb(it.user.name)
            }
            mMainThreadExecutor.execute {
                mCurrentUserSubscriptions.value = (current + objects).distinct()
                mAuthLiveData.value?.subscibesCount = mCurrentUserSubscriptions.value?.size?.toLong() ?: 0L
                mAuthLiveData.value = mAuthLiveData.value
            }
        }
    }


    @WorkerThread
    private fun loadSubscribersIfNeeded() {
        try {
            if (isUserLoggedIn() && mCurrentUserSubscriptions.value?.size ?: 0 == 0) requestSubscribesUpdate(null)
        } catch (e: Exception) {
            mLogger?.log(e)
            e.printStackTrace()
        }

    }

    override fun getSubscribersToBlog(ofUser: String): LiveData<List<UserObject>> {
        if (!mUsersSubscribers.contains(ofUser)) mUsersSubscribers.put(ofUser, MutableLiveData())
        return mUsersSubscribers[ofUser]!!
    }

    override fun getSubscriptionsToBlogs(ofUser: String): LiveData<List<UserObject>> {
        if (isUserLoggedIn() && mAuthLiveData.value?.userName == ofUser) return Transformations.map(mCurrentUserSubscriptions, {
            it.map { UserObject(it.user.name, it.user.avatar) }
        })

        if (!mUsersSubscriptions.contains(ofUser)) mUsersSubscriptions.put(ofUser, MutableLiveData())
        return mUsersSubscriptions[ofUser]!!
    }

    override fun getCurrentUserSubscriptions(): LiveData<List<UserBlogSubscription>> {
        return mCurrentUserSubscriptions
    }

    private fun requestSubscribersOrSubscriptionbsUpdate(isSubscribers: Boolean,
                                                         ofUser: String,
                                                         completionHandler: (List<UserObject>, GolosError?) -> Unit) {
        mWorkerExecutor.execute {
            try {

                if (isSubscribers) {
                    if (!mUsersSubscribers.contains(ofUser)) mUsersSubscribers.put(ofUser, MutableLiveData())
                } else {
                    if (ofUser != mAuthLiveData.value?.userName) {//check if we are  not updating current user subscriptions
                        if (!mUsersSubscriptions.contains(ofUser)) mUsersSubscriptions.put(ofUser, MutableLiveData())
                    }
                }

                val users = if (isSubscribers) mGolosApi.getSubscribers(ofUser, null) else mGolosApi.getSubscriptions(ofUser, null)
                val usersFollowObjects = users
                        .map {
                            val name = if (isSubscribers) it.follower.name else it.following.name
                            UserObject(name,
                                    getUserAvatarFromDb(name))
                        }
                mMainThreadExecutor.execute {
                    if (isSubscribers) mUsersSubscribers[ofUser]?.value = usersFollowObjects
                    else {
                        if (ofUser == mAuthLiveData.value?.userName) {
                            mCurrentUserSubscriptions.value = usersFollowObjects.map { UserBlogSubscription(it, SubscribeStatus.SubscribedStatus) }
                        } else {
                            mUsersSubscriptions[ofUser]?.value = usersFollowObjects
                        }

                    }

                    if (mUsersAccountInfo.contains(ofUser)) {
                        if (isSubscribers) mUsersAccountInfo[ofUser]?.value?.subscribersCount = usersFollowObjects.size.toLong()
                    }
                    completionHandler.invoke(usersFollowObjects, null)
                }

                val absentAvatar = usersFollowObjects.filter { it.avatar == null }.map { it.name }
                val avatars = HashMap(mGolosApi.getUserAvatars(absentAvatar))
                avatars
                        .filter { it.value != null }
                        .forEach {
                            mPersister.saveAvatarPathForUser(it.key, it.value!!, System.currentTimeMillis())
                        }
                usersFollowObjects.forEach {
                    it.avatar = getUserAvatarFromDb(it.name)
                }
                mMainThreadExecutor.execute {
                    if (isSubscribers) mUsersSubscribers[ofUser]?.value = usersFollowObjects
                    else {
                        if (ofUser == mAuthLiveData.value?.userName) {
                            mCurrentUserSubscriptions.value = usersFollowObjects.map { UserBlogSubscription(it, SubscribeStatus.SubscribedStatus) }
                        } else {
                            mUsersSubscriptions[ofUser]?.value = usersFollowObjects
                        }

                    }
                }

            } catch (e: Exception) {
                logException(e)
                mMainThreadExecutor.execute {
                    completionHandler.invoke(ArrayList(), GolosErrorParser.parse(e))
                }
            }
        }

    }

    override fun requestSubscribersUpdate(ofUser: String,
                                          completionHandler: (List<UserObject>, GolosError?) -> Unit) {
        requestSubscribersOrSubscriptionbsUpdate(true, ofUser, completionHandler)
    }

    override fun requestSubscriptionUpdate(ofUser: String,
                                           completionHandler: (List<UserObject>, GolosError?) -> Unit) {
        requestSubscribersOrSubscriptionbsUpdate(false, ofUser, completionHandler)
    }

    override fun deleteUserdata() {
        mPersister.deleteUserData()
        mAuthLiveData.value = null
        mCurrentUserSubscriptions.value = null
        allLiveData().forEach {
            it.value?.items?.forEach {
                it.rootStory()?.isUserUpvotedOnThis = false
            }
            it.value = it.value
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

    override fun getStories(type: FeedType, filter: StoryFilter?): LiveData<StoryTreeItems> {
        return convertFeedTypeToLiveData(type, filter)
    }

    override fun requestStoriesListUpdate(limit: Int,
                                          type: FeedType,
                                          filter: StoryFilter?,
                                          startAuthor: String?,
                                          startPermlink: String?, complitionHandler: (Unit, GolosError?) -> Unit) {
        val request = StoriesRequest(limit, type, startAuthor, startPermlink, filter)
        if (mRequests.contains(request)) return

        mRequests.add(request)
        mWorkerExecutor.execute {
            try {
                val discussions = getStripeItems(limit, type, filter, 1024, startAuthor, startPermlink)
                mMainThreadExecutor.execute {
                    val updatingFeed = convertFeedTypeToLiveData(type, filter)
                    var out = discussions
                    if (startAuthor != null && startPermlink != null) {
                        val current = ArrayList(updatingFeed.value?.items ?: ArrayList<StoryTree>())
                        out = current + out.subList(1, out.size)
                    }
                    updatingFeed.value = StoryTreeItems(out.toArrayList(), type, filter)
                    complitionHandler.invoke(Unit, null)
                    startLoadingAbscentAvatars(out, type, filter)
                }
                mRequests.remove(request)
                loadSubscribersIfNeeded()
            } catch (e: Exception) {
                logException(e)
                mRequests.remove(request)
                mMainThreadExecutor.execute {
                    val updatingFeed = convertFeedTypeToLiveData(type, filter)
                    updatingFeed.value = StoryTreeItems(updatingFeed.value?.items ?: ArrayList(),
                            updatingFeed.value?.type ?: FeedType.NEW,
                            filter,
                            GolosErrorParser.parse(e))
                    complitionHandler.invoke(Unit, GolosErrorParser.parse(e))
                }
            }
        }
    }

    private fun startLoadingAbscentAvatars(forItems: List<StoryTree>, feedType: FeedType, filter: StoryFilter?) {
        mWorkerExecutor.execute(object : ImageLoadRunnable {
            override fun run() {
                try {
                    val userNames = forItems
                            .filter { it.rootStory()?.avatarPath == null }
                            .distinct()
                            .map { it.rootStory()?.author ?: "" }
                    val avatars = mGolosApi.getUserAvatars(userNames)
                    avatars
                            .filter { it.value != null }
                            .forEach {
                                mPersister.saveAvatarPathForUser(it.key, it.value!!, System.currentTimeMillis())
                            }
                    var items = convertFeedTypeToLiveData(feedType, filter).value!!.items
                    items = ArrayList(items)
                            .map {
                                if (avatars.containsKey(it.rootStory()?.author ?: "")) it.deepCopy()
                                else it
                            }.toArrayList()

                    items.forEach {
                        if (avatars.containsKey(it.rootStory()?.author ?: "")) {
                            it.rootStory()?.avatarPath = avatars[it.rootStory()?.author ?: ""]
                        }
                    }

                    convertFeedTypeToLiveData(feedType, filter).let {
                        mMainThreadExecutor.execute {
                            it.value = StoryTreeItems(items, feedType, filter, it.value?.error)
                        }
                    }
                } catch (e: Exception) {
                    logException(e)
                }
            }
        })
    }


    override fun upVote(comment: GolosDiscussionItem, percents: Short) {
        vote(comment, true, percents)
    }

    override fun cancelVote(comment: GolosDiscussionItem) {
        vote(comment, false, 0)
    }

    private fun vote(discussionItem: GolosDiscussionItem, isUpvote: Boolean, voteStrength: Short) {
        var isVoted = false
        var votedItem: GolosDiscussionItem? = null
        var listOfList = allLiveData()
        val replacer = StorySearcherAndReplacer()

        listOfList.forEach {
            val storiesAll = it.value?.items ?: ArrayList()
            val result = replacer.findAndReplace(StoryWrapper(discussionItem, UpdatingState.UPDATING), storiesAll)
            if (result) {
                mMainThreadExecutor.execute {
                    it.value = StoryTreeItems(storiesAll, it.value?.type ?: FeedType.NEW, it.value?.filter)
                }
            }
        }
        mWorkerExecutor.execute {
            listOfList = allLiveData()
            listOfList.forEach {
                val storiesAll = it.value?.items ?: ArrayList()
                val result = replacer.findAndReplace(StoryWrapper(discussionItem, UpdatingState.UPDATING), storiesAll)
                if (result) {
                    try {

                        if (!isVoted) {
                            val currentStory = if (isUpvote) mGolosApi.upVote(discussionItem.author, discussionItem.permlink, voteStrength)
                            else mGolosApi.cancelVote(discussionItem.author, discussionItem.permlink)
                            currentStory.avatarPath = getUserAvatarFromDb(discussionItem.author)
                            replacer.findAndReplace(StoryWrapper(currentStory, UpdatingState.DONE), storiesAll)
                            currentStory.isUserUpvotedOnThis = isUpvote
                            votedItem = currentStory
                            mMainThreadExecutor.execute {
                                it.value = StoryTreeItems(storiesAll, it.value?.type ?: FeedType.NEW, it.value?.filter)
                            }
                            isVoted = true
                        } else {

                            val item = it
                            votedItem?.let {
                                replacer.findAndReplace(StoryWrapper(it, UpdatingState.DONE), storiesAll)
                                mMainThreadExecutor.execute {
                                    item.value = StoryTreeItems(storiesAll, item.value?.type ?: FeedType.NEW, item.value?.filter)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        logException(e)
                        if (e is SteemResponseError) {
                            Timber.e(e.error?.steemErrorDetails?.toString())
                        }
                        mMainThreadExecutor.execute {
                            if (result) {
                                val currentWorkingitem = discussionItem.clone() as GolosDiscussionItem
                                replacer.findAndReplace(StoryWrapper(currentWorkingitem, UpdatingState.FAILED), storiesAll)
                                it.value = StoryTreeItems(storiesAll, it.value?.type ?: FeedType.NEW,
                                        error = GolosErrorParser.parse(e), filter = it.value?.filter)
                            }
                        }
                    }
                }

            }
        }
    }


    override fun requestStoryUpdate(story: StoryTree) {
        mWorkerExecutor.execute {
            loadSubscribersIfNeeded()
            try {
                val updatedStory = getStory(story.rootStory()?.categoryName ?: "",
                        story.rootStory()?.author ?: "",
                        story.rootStory()?.permlink ?: "")
                updatedStory.getFlataned().forEach {
                    if (it.story.avatarPath != null) {
                        mPersister.saveAvatarPathForUser(it.story.author,
                                it.story.avatarPath ?: "",
                                System.currentTimeMillis())
                    }
                }
                val listOfList = allLiveData()
                val replacer = StorySearcherAndReplacer()
                listOfList.forEach {
                    val allItems = ArrayList(it.value?.items ?: ArrayList())
                    if (replacer.findAndReplace(updatedStory, allItems)) {
                        mMainThreadExecutor.execute {
                            it.value = StoryTreeItems(allItems, it.value?.type ?: FeedType.NEW, it.value?.filter)
                        }
                    }
                }
            } catch (e: Exception) {
                mLogger?.log(e)
                e.printStackTrace()
                Timber.e(e.message)
                if (e is SteemResponseError) {
                    Timber.e(e.error?.steemErrorDetails?.toString())
                }
                val listOfList = allLiveData()
                val replacer = StorySearcherAndReplacer()
                listOfList.forEach {
                    val allItems = ArrayList(it.value?.items ?: ArrayList())
                    val currentWorkingitem = story.rootStory()!!
                    val result = replacer
                            .findAndReplace(StoryWrapper(currentWorkingitem, UpdatingState.FAILED), allItems)
                    if (result) {
                        mMainThreadExecutor.execute {
                            it.value = StoryTreeItems(allItems, it.value?.type ?: FeedType.NEW,
                                    error = GolosErrorParser.parse(e), filter = it.value?.filter)
                        }
                    }
                }
            }
        }
    }


    override fun requestStoryUpdate(author: String,
                                    permLink: String,
                                    blog: String?,
                                    feedType: FeedType) {
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
                requestStoryUpdate(it)
            }
        } else {
            mWorkerExecutor.execute {

                loadSubscribersIfNeeded()

                try {
                    val story = getStory(blog, author, permLink)
                    val liveData = convertFeedTypeToLiveData(FeedType.UNCLASSIFIED, null)
                    mMainThreadExecutor.execute {
                        liveData.value = StoryTreeItems(listOf(story).toArrayList(), FeedType.UNCLASSIFIED, liveData.value?.filter)
                    }
                    if (blog == null) {
                        requestStoryUpdate(story)
                    }
                } catch (e: Exception) {
                    logException(e)
                    val error = GolosErrorParser.parse(e)
                    val liveData = convertFeedTypeToLiveData(FeedType.UNCLASSIFIED, null)
                    mMainThreadExecutor.execute { liveData.value = StoryTreeItems(arrayListOf(), FeedType.UNCLASSIFIED, null, error) }
                }
            }
        }
    }

    override fun getCurrentUserDataAsLiveData(): LiveData<UserData> {
        return mAuthLiveData
    }

    override fun createPost(title: String, content: List<EditorPart>, tags: List<String>, resultListener: (CreatePostResult?, GolosError?) -> Unit) {
        if (!isUserLoggedIn()) {
            resultListener.invoke(null, GolosError(ErrorCode.ERROR_AUTH, null, R.string.wrong_credentials))
            return
        }
        val tags = ArrayList(tags)
        val content = ArrayList(content)
        mWorkerExecutor.execute {
            try {
                (0 until tags.size)
                        .forEach {
                            if (tags[it].contains(Regex("[а-яА-Я]"))) {
                                tags[it] = "ru--${Translit.ru2lat(tags[it])}"
                            }
                        }
                (0 until content.size)
                        .forEach {
                            val part = content[it]
                            if (part is EditorImagePart) {
                                val newUrl = mGolosApi.uploadImage(mPersister.getCurrentUserName()!!, File(part.imageUrl))
                                content[it] = EditorImagePart(part.id, part.imageName, newUrl, pointerPosition = part.pointerPosition)
                            }
                        }
                val content = content.joinToString(separator = "\n") { it.markdownRepresentation }

                val result = mGolosApi.sendPost(mPersister.getCurrentUserName()!!, title, content, tags.toArray(Array(tags.size, { "" })))


                mMainThreadExecutor.execute {
                    result.isPost = true
                    mLastPostLiveData.value = result
                    resultListener.invoke(result, null)
                }
                mWorkerExecutor.execute {
                    val newStory = getStripeItems(1, FeedType.BLOG, StoryFilter(userNameFilter = listOf(mAuthLiveData.value?.userName ?: "")),
                            1024, null, null)[0]
                    val comments = convertFeedTypeToLiveData(FeedType.BLOG, StoryFilter(userNameFilter = listOf(mAuthLiveData.value?.userName ?: "")))

                    mMainThreadExecutor.execute {
                        comments.value = StoryTreeItems(((arrayListOf(newStory) + (comments.value?.items ?: ArrayList())).toArrayList()),
                                FeedType.BLOG, null, comments.value?.error)
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

    override fun createComment(rootStory: StoryTree,
                               to: GolosDiscussionItem,
                               content: List<EditorPart>,
                               resultListener: (CreatePostResult?, GolosError?) -> Unit) {
        if (!isUserLoggedIn()) {
            resultListener.invoke(null, GolosError(ErrorCode.ERROR_AUTH, null, R.string.wrong_credentials))
            return
        }
        mWorkerExecutor.execute {
            try {
                val content = ArrayList(content)
                (0 until content.size)
                        .forEach {
                            val part = content[it]
                            if (part is EditorImagePart) {
                                val newUrl = mGolosApi.uploadImage(mPersister.getCurrentUserName()!!, File(part.imageUrl))
                                content[it] = EditorImagePart(part.id, part.imageName, newUrl, pointerPosition = part.pointerPosition)
                            }
                        }
                val contentString = content.joinToString(separator = "\n") { it.markdownRepresentation }
                val result = mGolosApi.sendComment(mPersister.getCurrentUserName() ?: return@execute,
                        to.author,
                        to.permlink,
                        contentString,
                        rootStory.rootStory()!!.categoryName)
                requestStoryUpdate(rootStory)
                mMainThreadExecutor.execute {
                    result.isPost = false
                    mLastPostLiveData.value = result
                    resultListener.invoke(result, null)
                }
                mWorkerExecutor.execute {
                    val newStory = getStripeItems(1, FeedType.COMMENTS, StoryFilter(userNameFilter = listOf(mAuthLiveData.value?.userName ?: "")),
                            1024, null, null)[0]
                    val comments = convertFeedTypeToLiveData(FeedType.COMMENTS, StoryFilter(userNameFilter = listOf(mAuthLiveData.value?.userName ?: "")))

                    mMainThreadExecutor.execute {
                        comments.value = StoryTreeItems((arrayListOf(newStory) + ArrayList(comments.value?.items ?: ArrayList())).toArrayList(),
                                FeedType.COMMENTS, null, comments.value?.error)
                    }
                }
            } catch (e: Exception) {
                logException(e)
                if (e is SteemResponseError) {
                    Timber.e(e.message)
                }
                mMainThreadExecutor.execute {
                    resultListener(null, GolosErrorParser.parse(e))
                }
            }
        }
    }

    override fun isUserLoggedIn(): Boolean {
        val userData = mPersister.getActiveUserData()
        return userData != null && (userData.privateActiveWif != null || userData.privatePostingWif != null)
    }

    private fun convertFeedTypeToLiveData(feedtype: FeedType,
                                          filter: StoryFilter?): MutableLiveData<StoryTreeItems> {
        return if (filter == null) {
            if (!mLiveDataMap.containsKey(feedtype)) {
                throw IllegalStateException("type $feedtype is not supported without tag")
            }
            mLiveDataMap[feedtype]!!
        } else {
            val filteredRequest = FilteredRequest(feedtype, filter)
            if (mFilteredMap.containsKey(filteredRequest)) {
                mFilteredMap[filteredRequest]!!
            } else {
                val liveData = MutableLiveData<StoryTreeItems>()
                mFilteredMap.put(filteredRequest, liveData)
                liveData
            }
        }
    }

    private fun allLiveData(): List<MutableLiveData<StoryTreeItems>> {
        return ArrayList(mLiveDataMap.values) + ArrayList(mFilteredMap.values)
    }

    override fun getTrendingTags(): LiveData<List<Tag>> {
        if (mTags.value == null || mTags.value?.size == 0) {
            mWorkerExecutor.execute {
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

    override fun requestTrendingTagsUpdate(completionHandler: (List<Tag>, GolosError?) -> Unit) {
        mWorkerExecutor.execute {
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
        val liveData = MutableLiveData<List<VotedUserObject>>()
        val item: GolosDiscussionItem? =
                allLiveData()
                        .flatMap {
                            it.value?.items ?: arrayListOf()
                        }
                        .find {
                            it.rootStory()?.id == id
                        }
                        ?.rootStory()

        mWorkerExecutor.execute {
            item?.let {
                var voters = it.activeVotes.map {
                    VotedUserObject(it.key, getUserAvatarFromDb(it.key), "${it.value / 100}%")
                }
                mMainThreadExecutor.execute {
                    liveData.value = voters
                }
                try {
                    mGolosApi.getUserAvatars(voters
                            .filter { it.avatar == null }
                            .map { it.name })
                            .forEach {
                                if (it.value != null) {
                                    mPersister.saveAvatarPathForUser(it.key, it.value!!, System.currentTimeMillis())
                                }
                            }
                    voters = it.activeVotes.map {
                        VotedUserObject(it.key, getUserAvatarFromDb(it.key), "${it.value / 100}%")
                    }
                    mMainThreadExecutor.execute {
                        liveData.value = voters
                    }
                } catch (e: Exception) {
                    logException(e)
                }
            }
        }
        return liveData
    }


    private fun logException(e: Throwable) {
        Timber.e(e)
        e.printStackTrace()
        mLogger?.log(e)
    }
}
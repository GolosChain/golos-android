package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
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
import io.golos.golos.utils.*
import org.apache.commons.lang3.tuple.ImmutablePair
import timber.log.Timber
import java.io.File
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

private data class FilteredRequest(val feedType: FeedType,
                                   val filter: StoryFilter)

internal class RepositoryImpl(private val mWorkerExecutor: Executor,
                              private val mMainThreadExecutor: Executor,
                              private val mPersister: Persister,
                              private val mGolosApi: GolosApi) : Repository() {
    private val mAvatarRefreshDelay = TimeUnit.DAYS.toMillis(7)
    private val mLiveDataMap: HashMap<FeedType, MutableLiveData<StoryTreeItems>> = HashMap()
    private val mFilteredMap: HashMap<FilteredRequest, MutableLiveData<StoryTreeItems>> = HashMap()
    private val mUsersAccountInfo: HashMap<String, MutableLiveData<AccountInfo>> = HashMap()
    private val mRequests = Collections.synchronizedSet(HashSet<RepositoryRequests>())
    private val mAuthLiveData = MutableLiveData<UserData>()
    private val mLastPostLiveData = MutableLiveData<CreatePostResult>()

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

        var name = mPersister.getActiveUserData()?.userName ?: ""
        out.forEach {
            if (name != null) {
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

    private fun getUserAvatar(username: String, permlink: String?, blog: String?): String? {
        val avatar = mPersister.getAvatarForUser(username)
        val currentTime = System.currentTimeMillis()
        if (avatar != null && currentTime < (avatar.second + mAvatarRefreshDelay)) {
            return avatar.first
        }
        val ava = mGolosApi.getUserAvatar(username, permlink, blog)
        if (ava != null) {
            mPersister.saveAvatarPathForUser(username, ava, currentTime)
        }
        return ava
    }

    private fun getStory(blog: String?, author: String, permlink: String): StoryTree {
        var story = if (blog != null) mGolosApi.getStory(blog, author, permlink)
        else mGolosApi.getStoryWithoutComments(author, permlink)
        var name = mPersister.getCurrentUserName()
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

    private fun getAccountData(of: String): AccountInfo {
        return mGolosApi.getAccountData(of)
    }

    private fun auth(userName: String, masterKey: String?, activeWif: String?, postingWif: String?): UserAuthResponse {
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
                    Timber.e("requestActiveUserDataUpdate")
                    e.printStackTrace()
                    if (e is IllegalArgumentException
                            && e.message?.contains("key must be 51 chars") ?: false) {
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
            if (!mUsersAccountInfo.containsKey(userName)) mUsersAccountInfo.put(userName, MutableLiveData())
            mUsersAccountInfo[userName]!!
        }
    }

    override fun requestUserInfoUpdate(userName: String,
                                       completionHandler: (AccountInfo, GolosError?) -> Unit) {
        mWorkerExecutor.execute {
            try {
                val accinfo = mGolosApi.getAccountData(userName)
                if (isUserLoggedIn() && userName == mAuthLiveData.value?.userName) {
                    mMainThreadExecutor.execute {
                        mAuthLiveData.value = UserData(true, accinfo.userMotto, accinfo.avatarPath,
                                accinfo.userName, mAuthLiveData.value?.privateActiveWif, mAuthLiveData.value?.privatePostingWif,
                                accinfo.activePublicKey, accinfo.postingPublicKey, accinfo.subscibesCount,
                                accinfo.subscribersCount, accinfo.gbgAmount, accinfo.golosAmount, accinfo.golosPower,
                                accinfo.accountWorth, accinfo.postsCount, accinfo.safeGbg, accinfo.safeGolos)
                        completionHandler.invoke(accinfo, null)
                    }
                } else {
                    if (!mUsersAccountInfo.containsKey(userName)) mUsersAccountInfo.put(userName, MutableLiveData())
                    val userInfo = mUsersAccountInfo[userName]!!

                    mMainThreadExecutor.execute {
                        userInfo.value = accinfo
                        completionHandler.invoke(accinfo, null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mMainThreadExecutor.execute {
                    completionHandler.invoke(AccountInfo(userName), GolosErrorParser.parse(e))
                }
            }
        }
    }

    private fun getSavedActiveUserData(): UserData? {
        return mPersister.getActiveUserData()
    }

    override fun deleteUserdata() {
        mPersister.deleteUserData()
        mAuthLiveData.value = null
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
                                          feedType: FeedType,
                                          filter: StoryFilter?,
                                          startAuthor: String?,
                                          startPermlink: String?) {
        val request = StoriesRequest(limit, feedType, startAuthor, startPermlink, filter)
        if (mRequests.contains(request)) return

        mRequests.add(request)
        mWorkerExecutor.execute {
            try {
                val discussions = getStripeItems(limit, feedType, filter, 1024, startAuthor, startPermlink)
                mMainThreadExecutor.execute {
                    val updatingFeed = convertFeedTypeToLiveData(feedType, filter)
                    var out = discussions
                    if (startAuthor != null && startPermlink != null) {
                        var current = ArrayList(updatingFeed.value?.items ?: ArrayList<StoryTree>())
                        out = current + out.subList(1, out.size)
                    }
                    updatingFeed.value = StoryTreeItems(out, feedType)
                    startLoadingAbscentAvatars(out, feedType, filter)
                }
                mRequests.remove(request)
            } catch (e: Exception) {
                e.printStackTrace()
                mRequests.remove(request)
                mMainThreadExecutor.execute {
                    val updatingFeed = convertFeedTypeToLiveData(feedType, filter)
                    updatingFeed.value = StoryTreeItems(updatingFeed.value?.items ?: ArrayList(),
                            updatingFeed.value?.type ?: FeedType.NEW,
                            filter,
                            GolosErrorParser.parse(e))
                }
            }
        }
    }

    private fun startLoadingAbscentAvatars(forItems: List<StoryTree>, feedType: FeedType, filter: StoryFilter?) {
        mWorkerExecutor.execute(object : ImageLoadRunnable {
            override fun run() {
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
                        }

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
            }
        })
    }


    override fun upVote(comment: GolosDiscussionItem, percents: Short) {
        vote(comment, true, percents)
    }

    override fun cancelVote(comment: GolosDiscussionItem) {
        vote(comment, false, 0)
    }

    private fun findAndReplace(source: ArrayList<StoryTree>, newState: StoryTree): ArrayList<StoryTree> {
        (0..source.lastIndex)
                .filter { i -> source[i].rootStory()?.id == newState.rootStory()?.id }
                .forEach { i -> source[i] = newState }

        return ArrayList(source)
    }

    private fun vote(discussionItem: GolosDiscussionItem, isUpvote: Boolean, voteStrength: Short) {
        var isVoted = false
        var votedItem: GolosDiscussionItem? = null
        val listOfList = allLiveData()
        val replacer = StorySearcherAndReplacer()

        listOfList.forEach {
            val storiesAll = ArrayList(it.value?.items ?: ArrayList())
            val result = replacer.findAndReplace(StoryWrapper(discussionItem, UpdatingState.UPDATING), storiesAll)
            if (result) {
                mMainThreadExecutor.execute {
                    it.value = StoryTreeItems(storiesAll, it.value?.type ?: FeedType.NEW)
                }
            }
        }
        mWorkerExecutor.execute {

            listOfList.forEach {
                val storiesAll = ArrayList(it.value?.items ?: ArrayList())
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
                                it.value = StoryTreeItems(storiesAll, it.value?.type ?: FeedType.NEW)
                            }
                            isVoted = true
                        } else {
                            val item = it
                            votedItem?.let {
                                replacer.findAndReplace(StoryWrapper(it, UpdatingState.DONE), storiesAll)
                                mMainThreadExecutor.execute {
                                    item.value = StoryTreeItems(storiesAll, item.value?.type ?: FeedType.NEW)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Timber.e(e.message)
                        if (e is SteemResponseError) {
                            Timber.e(e.error?.steemErrorDetails?.toString())
                        }
                        mMainThreadExecutor.execute {
                            if (result) {
                                val currentWorkingitem = discussionItem.clone() as GolosDiscussionItem
                                replacer.findAndReplace(StoryWrapper(currentWorkingitem, UpdatingState.FAILED), storiesAll)
                                it.value = StoryTreeItems(storiesAll, it.value?.type ?: FeedType.NEW,
                                        error = GolosErrorParser.parse(e))
                            }
                        }
                    }
                }

            }
        }
    }


    override fun requestStoryUpdate(story: StoryTree) {
        mWorkerExecutor.execute {
            try {
                val story = getStory(story.rootStory()?.categoryName ?: "",
                        story.rootStory()?.author ?: "",
                        story.rootStory()?.permlink ?: "")
                story.getFlataned().forEach {
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
                    if (replacer.findAndReplace(story, allItems)) {
                        mMainThreadExecutor.execute {
                            it.value = StoryTreeItems(allItems, it.value?.type ?: FeedType.NEW)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Timber.e(e.message)
                if (e is SteemResponseError) {
                    Timber.e(e.error?.steemErrorDetails?.toString())
                }
                val listOfList = allLiveData()
                val replacer = StorySearcherAndReplacer()
                listOfList.forEach {
                    val allItems = ArrayList(it.value?.items ?: ArrayList())
                    var currentWorkingitem = story.rootStory()!!
                    val result = replacer
                            .findAndReplace(StoryWrapper(currentWorkingitem, UpdatingState.FAILED), allItems)
                    if (result) {
                        mMainThreadExecutor.execute {
                            it.value = StoryTreeItems(allItems, it.value?.type ?: FeedType.NEW,
                                    error = GolosErrorParser.parse(e))
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
            var item = allData
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
                try {
                    val story = getStory(blog, author, permLink)
                    val liveData = convertFeedTypeToLiveData(FeedType.UNCLASSIFIED, null)
                    mMainThreadExecutor.execute {
                        liveData.value = StoryTreeItems(listOf(story), FeedType.UNCLASSIFIED)
                    }
                    if (blog == null) {
                        requestStoryUpdate(story)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    val error = GolosErrorParser.parse(e)
                    val liveData = convertFeedTypeToLiveData(FeedType.UNCLASSIFIED, null)
                    mMainThreadExecutor.execute { liveData.value = StoryTreeItems(listOf(), FeedType.UNCLASSIFIED, null, error) }
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
                    val newStory = getStripeItems(1, FeedType.BLOG, StoryFilter(null,mAuthLiveData.value?.userName?:""),
                            1024, null, null)[0]
                    val comments = convertFeedTypeToLiveData(FeedType.BLOG, StoryFilter(null,mAuthLiveData.value?.userName?:""))

                    mMainThreadExecutor.execute {
                        comments.value = StoryTreeItems(arrayListOf(newStory) + ArrayList(comments.value?.items ?: ArrayList()),
                                FeedType.BLOG, null, comments.value?.error)
                    }
                }

            } catch (e: Exception) {
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
                    val newStory = getStripeItems(1, FeedType.COMMENTS, StoryFilter(null,mAuthLiveData.value?.userName?:""),
                            1024, null, null)[0]
                    val comments = convertFeedTypeToLiveData(FeedType.COMMENTS, StoryFilter(null,mAuthLiveData.value?.userName?:""))

                    mMainThreadExecutor.execute {
                        comments.value = StoryTreeItems(arrayListOf(newStory) + ArrayList(comments.value?.items ?: ArrayList()),
                                FeedType.COMMENTS, null, comments.value?.error)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
        if (filter == null) {
            if (!mLiveDataMap.containsKey(feedtype)) {
                throw IllegalStateException("type $feedtype is not supported without tag")
            }
            return mLiveDataMap[feedtype]!!
        } else {
            val filteredRequest = FilteredRequest(feedtype, filter)
            if (mFilteredMap.containsKey(filteredRequest)) return mFilteredMap[filteredRequest]!!
            else {
                val liveData = MutableLiveData<StoryTreeItems>()
                mFilteredMap.put(filteredRequest, liveData)
                return liveData
            }
        }
    }

    private fun allLiveData(): List<MutableLiveData<StoryTreeItems>> {
        return ArrayList(mLiveDataMap.values) + ArrayList(mFilteredMap.values)
    }
}
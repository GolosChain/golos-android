package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import eu.bittrade.libs.steemj.Golos4J
import eu.bittrade.libs.steemj.base.models.AccountName
import eu.bittrade.libs.steemj.enums.PrivateKeyType
import io.golos.golos.repository.api.GolosApi
import io.golos.golos.repository.model.*
import io.golos.golos.repository.persistence.Persister
import io.golos.golos.repository.persistence.model.AccountInfo
import io.golos.golos.repository.persistence.model.UserData
import io.golos.golos.screens.main_stripes.model.FeedType
import io.golos.golos.screens.main_stripes.viewmodel.ImageLoadRunnable
import io.golos.golos.screens.story.model.GolosDiscussionItem
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.utils.GolosErrorParser
import io.golos.golos.utils.UpdatingState
import org.apache.commons.lang3.tuple.ImmutablePair
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

internal class RepositoryImpl(private val mWorkerExecutor: Executor,
                              private val mMainThreadExecutor: Executor,
                              private val mPersister: Persister,
                              private val mGolosApi: GolosApi) : Repository() {
    private val mAvatarRefreshDelay = TimeUnit.DAYS.toMillis(7)
    private val mActualStories = MutableLiveData<StoryTreeItems>()
    private val mPopularStories = MutableLiveData<StoryTreeItems>()
    private val mNewStories = MutableLiveData<StoryTreeItems>()
    private val mPromoStories = MutableLiveData<StoryTreeItems>()
    private val mFeedStories = MutableLiveData<StoryTreeItems>()
    private val mRequests = Collections.synchronizedSet(HashSet<RepositoryRequests>())
    private val mAuthLiveData = MutableLiveData<UserData?>()

    private fun getStripeItems(limit: Int, type: FeedType, truncateBody: Int,
                               startAuthor: String?, startPermlink: String?): List<StoryTree> {
        var out: List<StoryTree>
        if (type == FeedType.PERSONAL_FEED) out = mGolosApi.getUserFeed(mPersister.getCurrentUserName() ?: "",
                20, 1024, startAuthor, startPermlink)
        else out = mGolosApi.getStories(limit, type, truncateBody, startAuthor, startPermlink)
        var name = getCurrentUserData()?.userName
        out.forEach {
            if (name != null) {
                if (it.rootStory()?.activeVotes?.filter { it.first == name }?.count() ?: 0 > 0 &&
                        it.rootStory()?.activeVotes?.filter { it.first == name }!!.first().second > 0)
                    it.rootStory()?.isUserUpvotedOnThis = true
            }
            it.rootStory()?.avatarPath = getUserAvatarFromDb(it.rootStory()?.author ?: "_____absent_____")
        }
        return out
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

    private fun getStory(blog: String, author: String, permlink: String): StoryTree {
        var story = mGolosApi.getStory(blog, author, permlink)
        var name = mPersister.getCurrentUserName()
        if (name != null) {
            if (story.rootStory()?.activeVotes?.filter { it.first == name && it.second > 0 }?.count() != 0) story.rootStory()?.isUserUpvotedOnThis = true
            story.getFlataned().forEach({
                if (it.story.activeVotes.filter { it.first == name && it.second > 0 }.count() != 0) it.story.isUserUpvotedOnThis = true
            })
        }
        return story
    }

    override fun authWithMasterKey(userName: String, masterKey: String): UserAuthResponse {
        return auth(userName, masterKey, null, null)
    }

    override fun authWithActiveWif(login: String, activeWif: String): UserAuthResponse {
        return auth(login, null, activeWif, null)
    }

    override fun authWithPostingWif(login: String, postingWif: String): UserAuthResponse {
        return auth(login, null, null, postingWif)
    }


    override fun getAccountData(of: String): AccountInfo {
        return mGolosApi.getAccountData(of)
    }

    private fun auth(userName: String, masterKey: String?, activeWif: String?, postingWif: String?): UserAuthResponse {
        val response = mGolosApi.auth(userName, masterKey, activeWif, postingWif)
        if (response.isKeyValid) {
            mPersister.saveKeys(mapOf(Pair(PrivateKeyType.ACTIVE, response.activeAuth?.second),
                    Pair(PrivateKeyType.POSTING, response.postingAuth?.second)))
            mPersister.saveCurrentUserName(userName)
        }
        return response
    }

    override fun getCurrentUserData(): UserData? {
        val name = mPersister.getCurrentUserName() ?: return null
        val keys = mPersister.getKeys(setOf(PrivateKeyType.ACTIVE, PrivateKeyType.POSTING))
        return UserData(mPersister.getAvatarForUser(name)?.first, name, keys[PrivateKeyType.ACTIVE], keys[PrivateKeyType.POSTING])
    }

    override fun deleteUserdata() {
        mPersister.saveCurrentUserName(null)
        mPersister.saveKeys(mapOf(Pair(PrivateKeyType.ACTIVE, null), Pair(PrivateKeyType.POSTING, null)))
    }

    override fun setUserAccount(userName: String, privateActiveWif: String?, privatePostingWif: String?) {
        if (privateActiveWif != null || privatePostingWif != null) {
            val keys = HashSet<ImmutablePair<PrivateKeyType, String>>()
            if (privateActiveWif != null) keys.add(ImmutablePair(PrivateKeyType.ACTIVE, privateActiveWif))
            if (privatePostingWif != null) keys.add(ImmutablePair(PrivateKeyType.POSTING, privatePostingWif))
            Golos4J.getInstance().addAccount(AccountName(userName), keys, true)
        }
    }

    override fun getStories(type: FeedType): LiveData<StoryTreeItems> {
        return when (type) {
            FeedType.ACTUAL -> mActualStories
            FeedType.POPULAR -> mPopularStories
            FeedType.NEW -> mNewStories
            FeedType.PROMO -> mPromoStories
            FeedType.PERSONAL_FEED -> mFeedStories
        }
    }

    override fun requestStoriesListUpdate(limit: Int, feedtype: FeedType, startAuthor: String?, startPermlink: String?) {
        val request = StoriesRequest(limit, feedtype, startAuthor, startPermlink)
        if (mRequests.contains(request)) return
        Timber.e("requestStoriesListUpdate " + feedtype)
        mRequests.add(request)
        mWorkerExecutor.execute() {
            try {
                val discussions = getStripeItems(limit, feedtype, 1024, startAuthor, startPermlink)
                mMainThreadExecutor.execute {
                    val updatingFeed = when (feedtype) {
                        FeedType.ACTUAL -> mActualStories
                        FeedType.POPULAR -> mPopularStories
                        FeedType.NEW -> mNewStories
                        FeedType.PROMO -> mPromoStories
                        FeedType.PERSONAL_FEED -> mFeedStories
                    }
                    var out = discussions
                    if (startAuthor != null && startPermlink != null) {
                        var current = ArrayList(updatingFeed.value?.items ?: ArrayList<StoryTree>())
                        out = current + out.subList(1, out.size)
                    }
                    updatingFeed.value = StoryTreeItems(out, feedtype)
                    startLoadingAbscentAvatars(out)
                }
                mRequests.remove(request)
            } catch (e: Exception) {
                mRequests.remove(request)
                mMainThreadExecutor.execute {
                    val updatingFeed = when (feedtype) {
                        FeedType.ACTUAL -> mActualStories
                        FeedType.POPULAR -> mPopularStories
                        FeedType.NEW -> mNewStories
                        FeedType.PROMO -> mPromoStories
                        FeedType.PERSONAL_FEED -> mFeedStories
                    }
                    updatingFeed.value = StoryTreeItems(updatingFeed.value?.items ?: ArrayList(),
                            updatingFeed.value?.type ?: FeedType.NEW,
                            GolosErrorParser.parse(e))
                }
            }
        }
    }

    private fun startLoadingAbscentAvatars(forItems: List<StoryTree>) {
        forItems.forEach {
            if (it.rootStory()?.avatarPath == null) {
                mWorkerExecutor.execute(object : ImageLoadRunnable {
                    override fun run() {
                        try {
                            val currentWorkingItem = it.deepCopy()
                            currentWorkingItem.rootStory()?.avatarPath = getUserAvatar("", null, null)
                            val listOfList = listOf(mActualStories, mPopularStories, mPromoStories, mNewStories, mFeedStories)
                            listOfList.forEach {
                                if (it.value?.items?.size ?: 0 > 0) {
                                    val replaced =
                                            findAndReplace(ArrayList(it.value?.items ?: ArrayList<StoryTree>()), currentWorkingItem)
                                    mMainThreadExecutor.execute {
                                        it.value = StoryTreeItems(replaced, it.value?.type ?: FeedType.NEW, it.value?.error)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e("error loading avatar of ${it.rootStory()?.avatarPath}")
                            e.printStackTrace()
                        }
                    }
                })
            }
        }
    }


    override fun upVote(discussionItem: GolosDiscussionItem, percents: Short) {
        Timber.e("upVote " + discussionItem.title)
        mWorkerExecutor.execute {
            val listOfList = listOf(mActualStories, mPopularStories, mPromoStories, mNewStories, mFeedStories)
            val replacer = StorySearcherAndReplacer()
            listOfList.forEach {
                var storiesAll = ArrayList(it.value?.items ?: ArrayList())
                var currentWorkingitem = discussionItem.clone() as GolosDiscussionItem
                val result = replacer.findAndReplace(StoryWrapper(currentWorkingitem, UpdatingState.UPDATING), storiesAll)
                if (result) {
                    mMainThreadExecutor.execute {
                        if (result) {
                            it.value = StoryTreeItems(storiesAll, it.value?.type ?: FeedType.NEW)
                        }
                    }
                    try {
                        val currentStory = mGolosApi.upVote(discussionItem.author, discussionItem.permlink, percents)
                        currentStory.avatarPath = getUserAvatarFromDb(discussionItem.author)
                        currentStory.isUserUpvotedOnThis = true
                        replacer.findAndReplace(StoryWrapper(currentStory, UpdatingState.DONE), storiesAll)
                        mMainThreadExecutor.execute {
                            it.value = StoryTreeItems(storiesAll, it.value?.type ?: FeedType.NEW)
                        }
                    } catch (e: Exception) {
                        Timber.e(e.message)
                        mMainThreadExecutor.execute {
                            if (result) {
                                var currentWorkingitem = discussionItem.clone() as GolosDiscussionItem
                                val result = replacer.findAndReplace(StoryWrapper(currentWorkingitem, UpdatingState.FAILED), storiesAll)
                                it.value = StoryTreeItems(storiesAll, it.value?.type ?: FeedType.NEW,
                                        error = GolosErrorParser.parse(e))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun findAndReplace(source: ArrayList<StoryTree>, newState: StoryTree): ArrayList<StoryTree> {
        (0..source.lastIndex)
                .filter { i -> source[i].rootStory()?.id == newState.rootStory()?.id }
                .forEach { i -> source[i] = newState }

        return ArrayList(source)
    }

    override fun cancelVote(discussionItem: GolosDiscussionItem) {
        Timber.e("cancelVote " + discussionItem.title)
        mWorkerExecutor.execute {
            val listOfList = listOf(mActualStories, mPopularStories, mPromoStories, mNewStories, mFeedStories)
            val replacer = StorySearcherAndReplacer()
            listOfList.forEach {
                var storiesAll = ArrayList(it.value?.items ?: ArrayList())
                val result = replacer.findAndReplace(StoryWrapper(discussionItem, UpdatingState.UPDATING), storiesAll)
                if (result) {
                    mMainThreadExecutor.execute {
                        if (result) {
                            it.value = StoryTreeItems(storiesAll, it.value?.type ?: FeedType.NEW)
                        }
                    }
                    try {
                        Timber.e("canceling vote")
                        val currentStory = mGolosApi.cancelVote(discussionItem.author, discussionItem.permlink)
                        currentStory.avatarPath = getUserAvatarFromDb(discussionItem.author)
                        replacer.findAndReplace(StoryWrapper(currentStory, UpdatingState.DONE), storiesAll)
                        currentStory.isUserUpvotedOnThis = false
                        mMainThreadExecutor.execute {
                            it.value = StoryTreeItems(storiesAll, it.value?.type ?: FeedType.NEW)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Timber.e(e.message)
                        mMainThreadExecutor.execute {
                            if (result) {
                                var currentWorkingitem = discussionItem.clone() as GolosDiscussionItem
                                val result = replacer.findAndReplace(StoryWrapper(currentWorkingitem, UpdatingState.FAILED), storiesAll)
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
            val listOfList = listOf(mActualStories, mPopularStories, mPromoStories, mNewStories, mFeedStories)
            val replacer = StorySearcherAndReplacer()
            listOfList.forEach {
                val allItems = ArrayList(it.value?.items ?: ArrayList())
                if (replacer.findAndReplace(story, allItems)) {
                    mMainThreadExecutor.execute {
                        it.value = StoryTreeItems(allItems, it.value?.type ?: FeedType.NEW)
                    }
                }
            }
        }
    }

    override fun getCurrentUserDataAsLiveData(): LiveData<UserData?> {
        return mAuthLiveData
    }
}
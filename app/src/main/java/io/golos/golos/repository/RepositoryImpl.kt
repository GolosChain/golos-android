package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import eu.bittrade.libs.steemj.Golos4J
import eu.bittrade.libs.steemj.base.models.AccountName
import eu.bittrade.libs.steemj.base.models.DiscussionQuery
import eu.bittrade.libs.steemj.base.models.Permlink
import eu.bittrade.libs.steemj.base.models.PublicKey
import eu.bittrade.libs.steemj.enums.DiscussionSortType
import eu.bittrade.libs.steemj.enums.PrivateKeyType
import eu.bittrade.libs.steemj.util.AuthUtils
import io.golos.golos.repository.model.*
import io.golos.golos.repository.persistence.Persister
import io.golos.golos.repository.persistence.model.AccountInfo
import io.golos.golos.repository.persistence.model.UserData
import io.golos.golos.screens.main_stripes.model.FeedType
import io.golos.golos.screens.main_stripes.viewmodel.ImageLoadRunnable
import io.golos.golos.screens.story.model.Comment
import io.golos.golos.screens.story.model.RootStory
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.utils.GolosErrorParser
import io.golos.golos.utils.avatarPath
import org.apache.commons.lang3.tuple.ImmutablePair
import org.bitcoinj.core.AddressFormatException
import timber.log.Timber
import java.lang.IllegalArgumentException
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

internal class RepositoryImpl(private val mWorkerExecutor: Executor,
                              private val mMainThreadExecutor: Executor,
                              private val mPersister: Persister,
                              private val mGolosApi: Golos4J) : Repository() {
    private val mAvatarRefreshDelay = TimeUnit.DAYS.toMillis(7)
    private val mActualStories = MutableLiveData<StoryItems>()
    private val mPopularStories = MutableLiveData<StoryItems>()
    private val mNewStories = MutableLiveData<StoryItems>()
    private val mPromoStories = MutableLiveData<StoryItems>()
    private val mFeedStories = MutableLiveData<StoryItems>()
    private val mRequests = Collections.synchronizedSet(HashSet<RepositoryRequests>())
    private var mStoryLiveData = MutableLiveData<StoryTreeState>()

    override fun getStripeItems(limit: Int, type: FeedType, truncateBody: Int,
                                startAuthor: String?, startPermlink: String?): List<RootStory> {
        var discussionSortType =
                when (type) {
                    FeedType.ACTUAL -> DiscussionSortType.GET_DISCUSSIONS_BY_HOT
                    FeedType.POPULAR -> DiscussionSortType.GET_DISCUSSIONS_BY_TRENDING
                    FeedType.NEW -> DiscussionSortType.GET_DISCUSSIONS_BY_CREATED
                    FeedType.PROMO -> DiscussionSortType.GET_DISCUSSIONS_BY_PROMOTED
                    FeedType.PERSONAL_FEED -> return getUserFeed(mPersister.getCurrentUserName()!!, limit, truncateBody, startAuthor, startPermlink)
                }
        val query = DiscussionQuery()
        query.limit = limit

        if (startAuthor != null) query.startAuthor = AccountName(startAuthor)
        if (startPermlink != null) query.startPermlink = Permlink(startPermlink)
        query.truncateBody = truncateBody.toLong()

        val discussions = mGolosApi.databaseMethods.getDiscussionsBy(query, discussionSortType)
        val out = ArrayList<RootStory>()
        var name = mPersister.getCurrentUserName()
        discussions.forEach {
            val story = RootStory(it, null)
            if (name != null) {
                if (story.activeVotes.filter { it.first == name && it.second > 0 }.count() > 0) story.isUserUpvotedOnThis = true
            }
            out.add(story)
        }
        return out
    }

    override fun getUserAvatar(username: String, permlink: String?, blog: String?): String? {
        val avatar = mPersister.getAvatarForUser(username)
        val currentTime = System.currentTimeMillis()
        if (avatar != null && currentTime < (avatar.second + mAvatarRefreshDelay)) {
            return avatar.first
        }

        val ava = if (permlink != null && blog != null) mGolosApi.databaseMethods.getAccountAvatar(blog, AccountName(username), Permlink(permlink))
        else mGolosApi.databaseMethods.getAccountAvatar(AccountName(username))
        if (ava != null) {
            mPersister.saveAvatarPathForUser(username, ava, currentTime)
        }
        return ava
    }

    override fun getStory(blog: String, author: String, permlink: String): StoryTree {
        var story = StoryTree(mGolosApi.databaseMethods.getStoryByRoute(blog, AccountName(author), Permlink(permlink)))
        var name = mPersister.getCurrentUserName()
        if (name != null) {
            if (story.rootStory?.activeVotes?.filter { it.first == name && it.second > 0 }?.count() != 0) story.rootStory?.isUserUpvotedOnThis = true
            story.getFlataned().forEach({
                if (it.activeVotes.filter { it.first == name && it.second > 0 }.count() != 0) it.isUserUpvotedOnThis = true
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

    override fun downVote(author: String, permlink: String): Comment {
        mGolosApi.simplifiedOperations.cancelVote(AccountName(author), Permlink(permlink))
        return getRootStoryWithoutComments(author, permlink)
    }

    override fun getUserFeed(userName: String, limit: Int, truncateBody: Int, startAuthor: String?, startPermlink: String?): List<RootStory> {
        return mGolosApi.databaseMethods.getUserFeed(AccountName(userName)).map { RootStory(it, null) }

    }

    override fun getAccountData(of: String): AccountInfo {
        if (of.isEmpty()) return AccountInfo(of, golosCount = 0.0, postsCount = 0)
        val accs = mGolosApi.databaseMethods.getAccounts(listOf(AccountName(of)))
        if (accs.size == 0) return AccountInfo(of, golosCount = 0.0, postsCount = 0)
        val acc = accs.get(index = 0)
        return AccountInfo(of, acc.avatarPath, acc.balance.amount, acc.postCount)
    }

    private fun auth(userName: String, masterKey: String?, activeWif: String?, postingWif: String?): UserAuthResponse {
        if (masterKey == null && activeWif == null && postingWif == null) return UserAuthResponse(false, userName, null, null, null, 0L, 0.0)
        val accs = mGolosApi.databaseMethods.getAccounts(listOf(AccountName(userName)))
        if (accs.size == 0) return UserAuthResponse(false, userName, null,
                null, null, 0, 0.0)
        val acc = accs.get(0)
        var postingPublicOuter = (acc.posting.keyAuths.keys.toTypedArray()[0] as PublicKey).addressFromPublicKey
        var activePublicOuter = (acc.active.keyAuths.keys.toTypedArray()[0] as PublicKey).addressFromPublicKey

        if (masterKey != null) {
            val keys = AuthUtils.generatePublicWiFs(userName, masterKey, arrayOf(PrivateKeyType.POSTING, PrivateKeyType.ACTIVE))
            return if (postingPublicOuter == keys[PrivateKeyType.POSTING] || activePublicOuter == keys[PrivateKeyType.ACTIVE]) {

                val privateKeys = AuthUtils.generatePrivateWiFs(userName, masterKey, arrayOf(PrivateKeyType.POSTING, PrivateKeyType.ACTIVE))
                mPersister.saveKeys(mapOf(Pair(PrivateKeyType.ACTIVE, privateKeys[PrivateKeyType.ACTIVE]),
                        Pair(PrivateKeyType.POSTING, privateKeys[PrivateKeyType.POSTING])))
                mPersister.saveCurrentUserName(userName)
                UserAuthResponse(true, acc.name.name,
                        Pair(postingPublicOuter, privateKeys[PrivateKeyType.POSTING]),
                        Pair(activePublicOuter, privateKeys[PrivateKeyType.ACTIVE]),
                        acc.avatarPath,
                        acc.postCount,
                        acc.balance.amount)
            } else {
                UserAuthResponse(false, acc.name.name, null,
                        null, null, 0, 0.0)
            }
        } else if (activeWif != null && postingWif != null) {
            return try {
                AuthUtils.isWiFsValid(activeWif, activePublicOuter)
                AuthUtils.isWiFsValid(postingWif, activePublicOuter)

                mPersister.saveKeys(mapOf(Pair(PrivateKeyType.ACTIVE, activeWif),
                        Pair(PrivateKeyType.POSTING, postingWif)))
                mPersister.saveCurrentUserName(userName)
                UserAuthResponse(true, acc.name.name,
                        Pair(postingPublicOuter, postingWif),
                        Pair(activePublicOuter, activeWif),
                        acc.avatarPath,
                        acc.postCount,
                        acc.balance.amount / 1000)
            } catch (e: IllegalArgumentException) {
                UserAuthResponse(false, acc.name.name, null,
                        null, null, 0, 0.0)
            } catch (e: AddressFormatException) {
                UserAuthResponse(false, acc.name.name, null,
                        null, null, 0, 0.0)
            }
        } else if (activeWif != null) {
            return try {
                AuthUtils.isWiFsValid(activeWif, activePublicOuter)

                mPersister.saveKeys(mapOf(Pair(PrivateKeyType.ACTIVE, activeWif),
                        Pair(PrivateKeyType.POSTING, null)))
                mPersister.saveCurrentUserName(userName)
                UserAuthResponse(true, acc.name.name,
                        Pair(postingPublicOuter, null),
                        Pair(activePublicOuter, activeWif),
                        acc.avatarPath,
                        acc.postCount,
                        acc.balance.amount / 1000)
            } catch (e: IllegalArgumentException) {
                UserAuthResponse(false, acc.name.name, null,
                        null, null, 0, 0.0)
            } catch (e: AddressFormatException) {
                UserAuthResponse(false, acc.name.name, null,
                        null, null, 0, 0.0)
            }

        } else {
            return try {
                AuthUtils.isWiFsValid(postingWif!!, activePublicOuter)

                mPersister.saveKeys(mapOf(Pair(PrivateKeyType.ACTIVE, null),
                        Pair(PrivateKeyType.POSTING, postingWif)))
                mPersister.saveCurrentUserName(userName)
                UserAuthResponse(true, acc.name.name,
                        Pair(postingPublicOuter, postingWif),
                        Pair(activePublicOuter, null),
                        acc.avatarPath,
                        acc.postCount,
                        acc.balance.amount / 1000)
            } catch (e: IllegalArgumentException) {
                UserAuthResponse(false, acc.name.name, null,
                        null, null, 0, 0.0)
            } catch (e: AddressFormatException) {
                UserAuthResponse(false, acc.name.name, null,
                        null, null, 0, 0.0)
            }
        }
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

    override fun upVote(author: String, permlink: String, percents: Short): Comment {
        mGolosApi.simplifiedOperations.vote(AccountName(author), Permlink(permlink), percents)
        return getRootStoryWithoutComments(author, permlink)
    }

    override fun setUserAccount(userName: String, privateActiveWif: String?, privatePostingWif: String?) {
        if (privateActiveWif != null || privatePostingWif != null) {
            val keys = HashSet<ImmutablePair<PrivateKeyType, String>>()
            if (privateActiveWif != null) keys.add(ImmutablePair(PrivateKeyType.ACTIVE, privateActiveWif))
            if (privatePostingWif != null) keys.add(ImmutablePair(PrivateKeyType.POSTING, privatePostingWif))
            mGolosApi.addAccount(AccountName(userName), keys, true)
        }
    }

    private fun getRootStoryWithoutComments(author: String, permlink: String): Comment {
        val story = Comment(mGolosApi.databaseMethods.getContent(AccountName(author), Permlink(permlink))!!, null)

        var currentUser = mPersister.getCurrentUserName()
        if (currentUser != null) {
            if (story.activeVotes.filter { it.first == currentUser && it.second > 0 }.count() != 0) story.isUserUpvotedOnThis = true
        }
        story.avatarPath = mPersister.getAvatarForUser(story.author)?.first
        return story
    }


    override fun getStories(type: FeedType): LiveData<StoryItems> {
        return when (type) {
            FeedType.ACTUAL -> mActualStories
            FeedType.POPULAR -> mPopularStories
            FeedType.NEW -> mNewStories
            FeedType.PROMO -> mPromoStories
            FeedType.PERSONAL_FEED -> mFeedStories
        }
    }

    override fun requestStoriesUpdate(limit: Int, feedtype: FeedType, startAuthor: String?, startPermlink: String?) {
        val request = StoriesRequest(limit, feedtype, startAuthor, startPermlink)
        if (mRequests.contains(request)) return
        Timber.e("requestStoriesUpdate " + feedtype)
        mRequests.add(request)
        mWorkerExecutor.execute() {
            try {
                val discussions = getStripeItems(limit, feedtype, 1024, startAuthor, startPermlink)
                var out = ArrayList<StoryItemState>()
                var name = getCurrentUserData()?.userName
                discussions.forEach {
                    if (name != null) {
                        if (it.activeVotes.filter { it.first == name }.count() > 0) it.isUserUpvotedOnThis = true
                    }
                    out.add(StoryItemState(it, UpdatingState.DONE))
                }
                Timber.e("done, out szie os ${out.size}")
                mMainThreadExecutor.execute {
                    val updatingFeed = when (feedtype) {
                        FeedType.ACTUAL -> mActualStories
                        FeedType.POPULAR -> mPopularStories
                        FeedType.NEW -> mNewStories
                        FeedType.PROMO -> mPromoStories
                        FeedType.PERSONAL_FEED -> mFeedStories
                    }
                    if (startAuthor != null && startPermlink != null) {
                        Timber.e("adding to the end")
                        Timber.e("out size is ${out.size}")
                        Timber.e("updatingFeed.value?.items is ${updatingFeed.value?.items?.size}")
                        var current = ArrayList(updatingFeed.value?.items ?: ArrayList<StoryItemState>())
                        out = ArrayList(current + out.subList(1, out.size))

                    }
                    updatingFeed.value = StoryItems(out, feedtype)
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
                    updatingFeed.value = StoryItems(updatingFeed.value?.items ?: ArrayList(),
                            updatingFeed.value?.type ?: FeedType.NEW,
                            GolosErrorParser.parse(e))
                }
            }
        }
    }

    private fun startLoadingAbscentAvatars(forItems: List<StoryItemState>) {
        forItems.forEach {
            if (it.comment.avatarPath == null) {
                mWorkerExecutor.execute(object : ImageLoadRunnable {
                    override fun run() {
                        try {
                            val currentWorkingItem = it.copy()
                            currentWorkingItem.comment.avatarPath = getUserAvatar(it.comment.author ?: "")
                            val listOfList = listOf(mActualStories, mPopularStories, mPromoStories, mNewStories, mFeedStories)
                            listOfList.forEach {
                                if (it.value?.items?.size ?: 0 > 0) {
                                    val replaced =
                                            findAndReplace(ArrayList(it.value?.items ?: ArrayList<StoryItemState>()), currentWorkingItem)
                                    mMainThreadExecutor.execute {
                                        it.value = StoryItems(replaced, it.value?.type ?: FeedType.NEW, it.value?.error)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e("error finding avatar for ${it.comment.author}")
                            e.printStackTrace()
                        }
                    }
                })
            }
        }
    }

    override fun upVoteNoCallback(comment: Comment, percents: Short) {
        Timber.e("upVoteNoCallback " + comment.title)
        mWorkerExecutor.execute {
            val listOfList = listOf(mActualStories, mPopularStories, mPromoStories, mNewStories, mFeedStories)
            listOfList.forEach {
                var storiesAll = ArrayList(it.value?.items ?: ArrayList())
                if (storiesAll.any({ it.comment.id == comment.id })) {
                    storiesAll = findAndReplace(storiesAll, StoryItemState(comment, UpdatingState.UPDATING))

                    val storiesSending = storiesAll.clone() as List<StoryItemState>
                    mMainThreadExecutor.execute {
                        it.value = StoryItems(storiesSending, it.value?.type ?: FeedType.NEW)
                    }
                    storiesAll = ArrayList(it.value?.items ?: ArrayList())

                    val currentStory = upVote(comment.author, comment.permlink, percents)

                    storiesAll = findAndReplace(storiesAll, StoryItemState(currentStory, UpdatingState.DONE))
                    currentStory.isUserUpvotedOnThis = true
                    mMainThreadExecutor.execute {
                        it.value = StoryItems(storiesAll.clone() as ArrayList<StoryItemState>, it.value?.type ?: FeedType.NEW)
                    }
                }
            }
        }
    }


    private fun findAndReplace(source: ArrayList<StoryItemState>, newState: StoryItemState): ArrayList<StoryItemState> {
        (0..source.lastIndex)
                .filter { i -> source[i].comment.id == newState.comment.id }
                .forEach { i -> source[i] = newState }

        return ArrayList(source)
    }

    override fun cancelVoteNoCallback(comment: Comment) {
        mWorkerExecutor.execute {
            val listOfList = listOf(mActualStories, mPopularStories, mPromoStories, mNewStories, mFeedStories)
            listOfList.forEach {
                var storiesAll = ArrayList(it.value?.items ?: ArrayList())
                if (storiesAll.any({ it.comment.id == comment.id })) {

                    storiesAll = findAndReplace(storiesAll, StoryItemState(comment, UpdatingState.UPDATING))
                    mMainThreadExecutor.execute { it.value = StoryItems(storiesAll.clone() as List<StoryItemState>, it.value?.type ?: FeedType.NEW) }
                    storiesAll = ArrayList(it.value?.items ?: ArrayList())
                    val currentStory = downVote(comment.author, comment.permlink)
                    storiesAll = findAndReplace(storiesAll, StoryItemState(currentStory, UpdatingState.DONE))
                    mMainThreadExecutor.execute { it.value = StoryItems(storiesAll, it.value?.type ?: FeedType.NEW) }
                }
            }
        }
    }

    override fun requestStoryUpdate(comment: Comment) {
        mWorkerExecutor.execute {
            val story = getStory(comment.categoryName, comment.author, comment.permlink)
            mMainThreadExecutor.execute {
                mStoryLiveData.value = StoryTreeState(story, null)
            }
        }
    }

    override fun getStory(comment: Comment): LiveData<StoryTreeState> {
        mStoryLiveData = MutableLiveData()
        val listOfList = listOf(mActualStories, mPopularStories, mPromoStories, mNewStories, mFeedStories)
        listOfList.forEach {
            it.value?.items?.forEach {
                if (it.comment.id == comment.id) {
                    mStoryLiveData.value = StoryTreeState(StoryTree(RootStory(it.comment), ArrayList()), null)
                    return@forEach
                }
            }
        }
        return mStoryLiveData
    }
}
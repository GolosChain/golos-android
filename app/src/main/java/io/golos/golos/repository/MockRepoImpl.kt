package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.os.Handler
import android.os.Looper
import eu.bittrade.libs.steemj.Golos4J
import eu.bittrade.libs.steemj.base.models.AccountName
import eu.bittrade.libs.steemj.base.models.Discussion
import eu.bittrade.libs.steemj.base.models.PublicKey
import eu.bittrade.libs.steemj.base.models.Story
import eu.bittrade.libs.steemj.communication.CommunicationHandler
import eu.bittrade.libs.steemj.communication.dto.ResponseWrapperDTO
import io.golos.golos.App
import io.golos.golos.repository.model.*
import io.golos.golos.repository.persistence.model.AccountInfo
import io.golos.golos.repository.persistence.model.UserData
import io.golos.golos.screens.main_stripes.model.FeedType
import io.golos.golos.screens.story.model.Comment
import io.golos.golos.screens.story.model.RootStory
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.utils.GolosErrorParser
import io.golos.golos.utils.avatarPath
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

internal class MockRepoImpl : Repository() {
    private val mActualStories = MutableLiveData<StoryItems>()
    private val mPopularStories = MutableLiveData<StoryItems>()
    private val mNewStories = MutableLiveData<StoryItems>()
    private val mPromoStories = MutableLiveData<StoryItems>()
    private val mFeedStories = MutableLiveData<StoryItems>()
    private val mExecutor = Executors.newSingleThreadExecutor()
    private val mHandler = Handler(Looper.getMainLooper())
    private val mRequests = Collections.synchronizedSet(HashSet<RepositoryRequests>())
    private var mStoryLiveData = MutableLiveData<StoryTreeState>()

    override fun getStripeItems(limit: Int, type: FeedType, truncateBody: Int, startAuthor: String?, startPermlink: String?): List<RootStory> {
        val mapper = CommunicationHandler.getObjectMapper()
        val context = App.context
        val ins = context.resources.openRawResource(context.resources.getIdentifier("stripe",
                "raw", context.packageName))
        val wrapperDTO = mapper.readValue<ResponseWrapperDTO<*>>(ins, ResponseWrapperDTO::class.java)
        val type = mapper.typeFactory.constructCollectionType(List::class.java, Discussion::class.java)
        val discussions = mapper.convertValue<List<Discussion>>(wrapperDTO.result, type)
        val out = ArrayList<RootStory>()
        var name = getCurrentUserData()?.userName
        discussions.forEach {
            val story = RootStory(it, null)
            if (name != null) {
                if (story.activeVotes.filter { it.first == name }.count() > 0) story.isUserUpvotedOnThis = true
            }
            out.add(story)
        }
        return out
    }

    override fun setUserAccount(userName: String, privateActiveWif: String?, privatePostingWif: String?) {

    }

    override fun getUserFeed(userName: String, limit: Int, truncateBody: Int, startAuthor: String?, startPermlink: String?): List<RootStory> {
        return Golos4J.getInstance().databaseMethods.getUserFeed(AccountName("cepera")).map { RootStory(it, null) }
    }

    override fun getAccountData(of: String): AccountInfo {
        return AccountInfo(of, null, 0.0, 0)
    }

    override fun getUserAvatar(username: String, permlink: String?, blog: String?): String? {
        Thread.sleep(300)
        return "https://s20.postimg.org/6bfyz1wjh/VFcp_Mpi_DLUIk.jpg"
    }

    override fun upVote(userName: String, permlink: String, percents: Short): Comment {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun downVote(author: String, permlink: String): RootStory {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getStory(blog: String, author: String, permlink: String): StoryTree {
        val mapper = CommunicationHandler.getObjectMapper()
        val context = App.context
        val ins = context.resources.openRawResource(context.resources.getIdentifier("story2",
                "raw", context.packageName))
        val wrapperDTO = mapper.readValue<ResponseWrapperDTO<*>>(ins, ResponseWrapperDTO::class.java)
        val type = mapper.typeFactory.constructCollectionType(List::class.java, Story::class.java)
        val stoeryes = mapper.convertValue<List<Story>>(wrapperDTO.result, type)
        return StoryTree(stoeryes[0])
    }

    override fun authWithMasterKey(userName: String, masterKey: String): UserAuthResponse {
        val response = Golos4J.getInstance().databaseMethods.getAccounts(listOf(AccountName("cepera")))
        val acc = response[0]
        isIserLoggedIn = true
        Timber.e("authWithMasterKey")
        Timber.e("isIserLoggedIn = " + isIserLoggedIn)
        return UserAuthResponse(true, acc.name.name,
                Pair((acc.posting.keyAuths.keys.toTypedArray()[0] as PublicKey).addressFromPublicKey, "posting-key-stub"),
                Pair((acc.active.keyAuths.keys.toTypedArray()[0] as PublicKey).addressFromPublicKey, "active-key-stub"),
                acc.avatarPath,
                acc.postCount,
                acc.balance.amount / 1000)
    }


    override fun authWithActiveWif(login: String, activeWif: String): UserAuthResponse {
        return authWithMasterKey(login, activeWif)
    }

    override fun authWithPostingWif(login: String, postingWif: String): UserAuthResponse {
        return authWithMasterKey(login, postingWif)
    }

    override fun getCurrentUserData(): UserData? {
        if (!isIserLoggedIn) return null
        return UserData(null, "cepera", "mockActiveWif", "mockPostingWif")
    }

    override fun deleteUserdata() {

    }

    companion object {
        private var isIserLoggedIn = false
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
        mExecutor.execute() {
            try {
                val mapper = CommunicationHandler.getObjectMapper()
                val context = App.context
                val ins = context.resources.openRawResource(context.resources.getIdentifier("stripe",
                        "raw", context.packageName))
                val wrapperDTO = mapper.readValue<ResponseWrapperDTO<*>>(ins, ResponseWrapperDTO::class.java)
                val type = mapper.typeFactory.constructCollectionType(List::class.java, Discussion::class.java)
                val discussions = mapper.convertValue<List<Discussion>>(wrapperDTO.result, type)
                val out = ArrayList<StoryItemState>()
                var name = getCurrentUserData()?.userName
                discussions.forEach {
                    val story = RootStory(it, null)
                    if (name != null) {
                        if (story.activeVotes.filter { it.first == name }.count() > 0) story.isUserUpvotedOnThis = true
                    }
                    out.add(StoryItemState(story, UpdatingState.DONE))
                }
                mHandler.post {
                    val updatingFeed = when (feedtype) {
                        FeedType.ACTUAL -> mActualStories
                        FeedType.POPULAR -> mPopularStories
                        FeedType.NEW -> mNewStories
                        FeedType.PROMO -> mPromoStories
                        FeedType.PERSONAL_FEED -> mFeedStories
                    }
                    val newList = out + ArrayList(updatingFeed.value?.items ?: ArrayList())
                    updatingFeed.value = StoryItems(newList, feedtype)
                    startLoadingAbscentAvatars(out)
                }
                mRequests.remove(request)
            } catch (e: Exception) {
                mRequests.remove(request)
                mHandler.post {
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
                mExecutor.execute({
                    val currentWorkingItem = it.copy()
                    currentWorkingItem.comment.avatarPath = getUserAvatar("")
                    val listOfList = listOf(mActualStories, mPopularStories, mPromoStories, mNewStories, mFeedStories)
                    listOfList.forEach {
                        if (it.value?.items?.size ?: 0 > 0) {
                            val replaced =
                                    findAndReplace(ArrayList(it.value?.items ?: ArrayList<StoryItemState>()), currentWorkingItem)
                            mHandler.post {
                                it.value = StoryItems(replaced, it.value?.type ?: FeedType.NEW, it.value?.error)
                            }
                        }
                    }
                })
            }
        }
    }

    override fun upVoteNoCallback(comment: Comment, percents: Short) {
        Timber.e("upVoteNoCallback " + comment.title)
        mExecutor.execute {
            Thread.sleep(1500)
            val listOfList = listOf(mActualStories, mPopularStories, mPromoStories, mNewStories, mFeedStories)
            listOfList.forEach {
                var storiesAll = ArrayList(it.value?.items ?: ArrayList())
                if (storiesAll.any({ it.comment.id == comment.id })) {
                    storiesAll = findAndReplace(storiesAll, StoryItemState(comment, UpdatingState.UPDATING))

                    val storiesSending = storiesAll.clone() as List<StoryItemState>
                    mHandler.post {
                        it.value = StoryItems(storiesSending, it.value?.type ?: FeedType.NEW)
                    }

                    storiesAll = ArrayList(it.value?.items ?: ArrayList())
                    val currentStory = comment.clone() as Comment
                    currentStory.activeVotes + Pair(getCurrentUserData()?.userName ?: "", 100_00)
                    currentStory.gbgAmount = currentStory.gbgAmount + 50
                    storiesAll = findAndReplace(storiesAll, StoryItemState(currentStory, UpdatingState.DONE))
                    currentStory.isUserUpvotedOnThis = true
                    mHandler.post {
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
        mExecutor.execute {
            Thread.sleep(3000)
            val listOfList = listOf(mActualStories, mPopularStories, mPromoStories, mNewStories, mFeedStories)
            listOfList.forEach {
                var storiesAll = ArrayList(it.value?.items ?: ArrayList())
                if (storiesAll.any({ it.comment.id == comment.id })) {
                    storiesAll = findAndReplace(storiesAll, StoryItemState(comment, UpdatingState.UPDATING))
                    mHandler.post { it.value = StoryItems(storiesAll.clone() as List<StoryItemState>, it.value?.type ?: FeedType.NEW) }
                    storiesAll = ArrayList(it.value?.items ?: ArrayList())
                    val currentStory = comment.clone() as Comment
                    currentStory.activeVotes - Pair(getCurrentUserData()?.userName ?: "", 100_00)
                    currentStory.gbgAmount = currentStory.gbgAmount - 50
                    storiesAll = findAndReplace(storiesAll, StoryItemState(currentStory, UpdatingState.DONE))
                    currentStory.isUserUpvotedOnThis = false
                    mHandler.post { it.value = StoryItems(storiesAll, it.value?.type ?: FeedType.NEW) }
                }
            }
        }
    }

    override fun requestStoryUpdate(comment: Comment) {
        mExecutor.execute {
            Thread.sleep(3000)
            val story = getStory(comment.categoryName, comment.author, comment.permlink)
            mHandler.post {
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
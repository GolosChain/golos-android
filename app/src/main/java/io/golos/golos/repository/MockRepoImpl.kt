package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.os.Handler
import android.os.Looper
import eu.bittrade.libs.steemj.Golos4J
import eu.bittrade.libs.steemj.base.models.AccountName
import eu.bittrade.libs.steemj.base.models.Discussion
import eu.bittrade.libs.steemj.base.models.DiscussionWithComments
import eu.bittrade.libs.steemj.base.models.PublicKey
import eu.bittrade.libs.steemj.communication.CommunicationHandler
import eu.bittrade.libs.steemj.communication.dto.ResponseWrapperDTO
import io.golos.golos.App
import io.golos.golos.repository.model.*
import io.golos.golos.repository.persistence.model.AccountInfo
import io.golos.golos.repository.persistence.model.UserData
import io.golos.golos.screens.main_stripes.model.FeedType
import io.golos.golos.screens.story.model.GolosDiscussionItem
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.utils.GolosErrorParser
import io.golos.golos.utils.UpdatingState
import io.golos.golos.utils.avatarPath
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

internal class MockRepoImpl : Repository() {
    companion object {
        @JvmStatic
        private var isIserLoggedIn = false
    }


    private val mActualStories = MutableLiveData<StoryTreeItems>()
    private val mPopularStories = MutableLiveData<StoryTreeItems>()
    private val mNewStories = MutableLiveData<StoryTreeItems>()
    private val mPromoStories = MutableLiveData<StoryTreeItems>()
    private val mFeedStories = MutableLiveData<StoryTreeItems>()
    private val mAuthLiveData = MutableLiveData<UserData?>()
    private val mExecutor = Executors.newSingleThreadExecutor()
    private val mHandler = Handler(Looper.getMainLooper())
    private val mRequests = Collections.synchronizedSet(HashSet<RepositoryRequests>())

    override fun setUserAccount(userName: String, privateActiveWif: String?, privatePostingWif: String?) {

    }

    private fun getUserFeed(userName: String, limit: Int, truncateBody: Int, startAuthor: String?, startPermlink: String?): List<StoryTree> {
        return Golos4J
                .getInstance().databaseMethods
                .getUserFeed(AccountName("cepera"))
                .map {
                    StoryTree(StoryWrapper(GolosDiscussionItem(it, null), UpdatingState.DONE), ArrayList())
                }
    }

    override fun getAccountData(of: String): AccountInfo {
        return AccountInfo(of, null, 0.0, 0)
    }

    private fun getUserAvatar(username: String, permlink: String?, blog: String?): String? {
        Thread.sleep(50)
        return "https://s20.postimg.org/6bfyz1wjh/VFcp_Mpi_DLUIk.jpg"
    }


    private fun getStory(blog: String, author: String, permlink: String): StoryTree {
        val mapper = CommunicationHandler.getObjectMapper()
        val context = App.context
        val ins = context.resources.openRawResource(context.resources.getIdentifier("story2",
                "raw", context.packageName))
        val wrapperDTO = mapper.readValue<ResponseWrapperDTO<*>>(ins, ResponseWrapperDTO::class.java)
        val type = mapper.typeFactory.constructCollectionType(List::class.java, DiscussionWithComments::class.java)
        val stoeryes = mapper.convertValue<List<DiscussionWithComments>>(wrapperDTO.result, type)
        return StoryTree(stoeryes[0])
    }

    override fun authWithMasterKey(userName: String, masterKey: String): UserAuthResponse {
        val response = Golos4J.getInstance().databaseMethods.getAccounts(listOf(AccountName("cepera")))
        val acc = response[0]
        isIserLoggedIn = true
        val out = UserAuthResponse(true, acc.name.name,
                Pair((acc.posting.keyAuths.keys.toTypedArray()[0] as PublicKey).addressFromPublicKey, "posting-key-stub"),
                Pair((acc.active.keyAuths.keys.toTypedArray()[0] as PublicKey).addressFromPublicKey, "active-key-stub"),
                acc.avatarPath,
                acc.postCount,
                acc.balance.amount / 1000)
        mHandler.post {
            mAuthLiveData.value = UserData(out.avatarPath, out.userName, out.postingAuth?.second, out.activeAuth?.second)
        }
        return out
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
        mExecutor.execute() {
            try {
                val mapper = CommunicationHandler.getObjectMapper()
                val context = App.context
                val ins = context.resources.openRawResource(context.resources.getIdentifier("stripe",
                        "raw", context.packageName))
                val wrapperDTO = mapper.readValue<ResponseWrapperDTO<*>>(ins, ResponseWrapperDTO::class.java)
                val type = mapper.typeFactory.constructCollectionType(List::class.java, Discussion::class.java)
                val discussions = mapper.convertValue<List<Discussion>>(wrapperDTO.result, type)
                val out = ArrayList<StoryTree>()
                var name = getCurrentUserData()?.userName
                discussions.forEach {
                    val story = StoryTree(StoryWrapper(GolosDiscussionItem(it, null), UpdatingState.DONE), ArrayList())
                    if (name != null) {
                        if (story.rootStory()?.activeVotes?.filter { it.first == name }?.count() ?: 0 > 0)
                            story.rootStory()?.isUserUpvotedOnThis = true
                    }

                    out.add(story)
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
                    updatingFeed.value = StoryTreeItems(newList, feedtype)
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
                mExecutor.execute({
                    val currentWorkingItem = it.deepCopy()
                    currentWorkingItem.rootStory()?.avatarPath = getUserAvatar("", null, null)
                    val listOfList = listOf(mActualStories, mPopularStories, mPromoStories, mNewStories, mFeedStories)
                    listOfList.forEach {
                        if (it.value?.items?.size ?: 0 > 0) {
                            val workingCopy = ArrayList(it.value?.items ?: ArrayList<StoryTree>()).clone() as ArrayList<StoryTree>
                            val replaced =
                                    findAndReplace(workingCopy, currentWorkingItem)
                            mHandler.post {
                                it.value = StoryTreeItems(replaced, it.value?.type ?: FeedType.NEW, it.value?.error)
                            }
                        }
                    }
                })
            }
        }
    }

    private fun findAndReplace(source: ArrayList<StoryTree>, newState: StoryTree): ArrayList<StoryTree> {
        (0..source.lastIndex)
                .filter { i -> source[i].rootStory()?.id == newState.rootStory()?.id }
                .forEach { i -> source[i] = newState }

        return ArrayList(source)
    }

    override fun upVote(discussionItem: GolosDiscussionItem, percents: Short) {
        Timber.e("upVote " + discussionItem.title)
        mExecutor.execute {
            Thread.sleep(1500)
            val listOfList = listOf(mActualStories, mPopularStories, mPromoStories, mNewStories, mFeedStories)
            val replacer = StorySearcherAndReplacer()
            listOfList.forEach {
                var currentWorkingitem = discussionItem.clone() as GolosDiscussionItem
                var storiesAll = ArrayList(it.value?.items ?: ArrayList())
                val result = replacer.findAndReplace(StoryWrapper(currentWorkingitem, UpdatingState.UPDATING), storiesAll)
                if (result) {
                    mHandler.post {
                        if (result) {
                            it.value = StoryTreeItems(storiesAll, it.value?.type ?: FeedType.NEW)
                            //todo updating status may arrive after vote done status, mb add latch?
                        }
                    }
                    currentWorkingitem = discussionItem.clone() as GolosDiscussionItem
                    currentWorkingitem.activeVotes + Pair(getCurrentUserData()?.userName ?: "", 100_00)
                    currentWorkingitem.gbgAmount = discussionItem.gbgAmount + 50
                    currentWorkingitem.isUserUpvotedOnThis = true

                    replacer.findAndReplace(StoryWrapper(currentWorkingitem, UpdatingState.DONE), storiesAll)

                    mHandler.post {
                        it.value = StoryTreeItems(storiesAll, it.value?.type ?: FeedType.NEW)
                    }
                }
            }
        }
    }


    override fun cancelVote(discussionItem: GolosDiscussionItem) {
        Timber.e("cancelVote " + discussionItem.title)
        mExecutor.execute {
            Thread.sleep(1500)
            val listOfList = listOf(mActualStories, mPopularStories, mPromoStories, mNewStories, mFeedStories)
            val replacer = StorySearcherAndReplacer()
            listOfList.forEach {
                var storiesAll = ArrayList(it.value?.items ?: ArrayList())
                var currentWorkingitem = discussionItem.clone() as GolosDiscussionItem
                val result = replacer.findAndReplace(StoryWrapper(currentWorkingitem, UpdatingState.UPDATING), storiesAll)
                if (result) {
                    mHandler.post {
                        if (result) {
                            it.value = StoryTreeItems(storiesAll, it.value?.type ?: FeedType.NEW)
                            //todo updating status may arrive after vote done status, mb add latch?
                        }
                    }
                    currentWorkingitem = discussionItem.clone() as GolosDiscussionItem
                    currentWorkingitem.activeVotes - Pair(getCurrentUserData()?.userName ?: "", 100_00)
                    currentWorkingitem.gbgAmount = discussionItem.gbgAmount - 50
                    currentWorkingitem.isUserUpvotedOnThis = false

                    replacer.findAndReplace(StoryWrapper(currentWorkingitem, UpdatingState.DONE), storiesAll)

                    mHandler.post {
                        it.value = StoryTreeItems(storiesAll, it.value?.type ?: FeedType.NEW)
                    }
                }
            }
        }
    }

    override fun requestStoryUpdate(story: StoryTree) {
        mExecutor.execute {
            Thread.sleep(3000)
            val story = getStory(story.rootStory()?.categoryName ?: "",
                    story.rootStory()?.author ?: "",
                    story.rootStory()?.permlink ?: "")
            val listOfList = listOf(mActualStories, mPopularStories, mPromoStories, mNewStories, mFeedStories)
            val replacer = StorySearcherAndReplacer()
            listOfList.forEach {
                val allItems = ArrayList(it.value?.items ?: ArrayList())
                if (replacer.findAndReplace(story, allItems)) {
                    mHandler.post {
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
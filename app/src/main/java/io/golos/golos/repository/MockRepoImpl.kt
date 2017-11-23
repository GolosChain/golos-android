package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import io.golos.golos.repository.api.GolosApi
import io.golos.golos.repository.model.*
import io.golos.golos.repository.persistence.model.AccountInfo
import io.golos.golos.repository.persistence.model.UserData
import io.golos.golos.screens.editor.EditorPart
import io.golos.golos.screens.main_stripes.model.FeedType
import io.golos.golos.screens.story.model.GolosDiscussionItem
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.utils.GolosError
import io.golos.golos.utils.GolosErrorParser
import io.golos.golos.utils.UpdatingState
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executor
import kotlin.collections.ArrayList

internal class MockRepoImpl(
        private val api: GolosApi,
        workingExecutors: Executor,
        mainThreadExecutor: Executor
) : Repository() {
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
    private val mExecutor = workingExecutors
    private val mMainThreadExecutor = mainThreadExecutor
    private val mRequests = Collections.synchronizedSet(HashSet<RepositoryRequests>())

    override fun setUserAccount(userName: String,
                                privateActiveWif: String?,
                                privatePostingWif: String?) {

    }

    override fun getAccountData(of: String): AccountInfo {
        return AccountInfo(of, null, 0.0, 0)
    }


    override fun authWithMasterKey(userName: String, masterKey: String): UserAuthResponse {
        isIserLoggedIn = true
        val out = api.auth(userName, masterKey, null, null)
        mMainThreadExecutor.execute {
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
        //  if (!isIserLoggedIn) return null
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
                var out: List<StoryTree>
                if (feedtype == FeedType.PERSONAL_FEED) out = api.getUserFeed(getCurrentUserData()!!.userName,
                        20, 1024, startAuthor, startPermlink)
                else out = api.getStories(limit, feedtype, 1024, startAuthor, startPermlink)
                var name = getCurrentUserData()?.userName
                out.forEach {
                    if (name != null) {
                        if (it.rootStory()?.activeVotes?.filter { it.first == name }?.count() ?: 0 > 0)
                            it.rootStory()?.isUserUpvotedOnThis = true
                    }
                }
                mMainThreadExecutor.execute {
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
                mExecutor.execute({
                    val currentWorkingItem = it.deepCopy()
                    currentWorkingItem.rootStory()?.avatarPath = api.getUserAvatar("", null, null)
                    val listOfList = listOf(mActualStories, mPopularStories, mPromoStories, mNewStories, mFeedStories)
                    listOfList.forEach {
                        if (it.value?.items?.size ?: 0 > 0) {
                            val workingCopy = ArrayList(it.value?.items ?: ArrayList<StoryTree>()).clone() as ArrayList<StoryTree>
                            val replaced =
                                    findAndReplace(workingCopy, currentWorkingItem)
                            mMainThreadExecutor.execute {
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
            val listOfList = listOf(mActualStories, mPopularStories, mPromoStories, mNewStories, mFeedStories)
            var currentWorkingitem = discussionItem.clone() as GolosDiscussionItem
            val replacer = StorySearcherAndReplacer()
            listOfList.forEach {
                var storiesAll = ArrayList(it.value?.items ?: ArrayList())
                val result = replacer.findAndReplace(StoryWrapper(currentWorkingitem, UpdatingState.UPDATING), storiesAll)
                if (result) {
                    mMainThreadExecutor.execute {
                        if (result) {
                            it.value = StoryTreeItems(storiesAll, it.value?.type ?: FeedType.NEW)
                        }
                    }
                }
            }
            Thread.sleep(1500)

            currentWorkingitem = discussionItem.clone() as GolosDiscussionItem
            currentWorkingitem.activeVotes + Pair(getCurrentUserData()?.userName ?: "", 100_00)
            currentWorkingitem.gbgAmount = discussionItem.gbgAmount + 50
            currentWorkingitem.isUserUpvotedOnThis = true
            listOfList.forEach {
                var storiesAll = ArrayList(it.value?.items ?: ArrayList())
                val result = replacer.findAndReplace(StoryWrapper(currentWorkingitem, UpdatingState.DONE), storiesAll)
                if (result) {
                    mMainThreadExecutor.execute {
                        it.value = StoryTreeItems(storiesAll, it.value?.type ?: FeedType.NEW)
                    }
                }
            }
        }
    }


    override fun cancelVote(discussionItem: GolosDiscussionItem) {
        Timber.e("cancelVote " + discussionItem.title)
        mExecutor.execute {
            val listOfList = listOf(mActualStories, mPopularStories, mPromoStories, mNewStories, mFeedStories)
            var currentWorkingitem = discussionItem.clone() as GolosDiscussionItem
            val replacer = StorySearcherAndReplacer()
            listOfList.forEach {
                var storiesAll = ArrayList(it.value?.items ?: ArrayList())
                val result = replacer.findAndReplace(StoryWrapper(currentWorkingitem, UpdatingState.UPDATING), storiesAll)
                if (result) {
                    mMainThreadExecutor.execute {
                        if (result) {
                            it.value = StoryTreeItems(storiesAll, it.value?.type ?: FeedType.NEW)
                        }
                    }
                }
            }
            Thread.sleep(1500)

            currentWorkingitem = discussionItem.clone() as GolosDiscussionItem
            currentWorkingitem.activeVotes - Pair(getCurrentUserData()?.userName ?: "", 100_00)
            currentWorkingitem.gbgAmount = discussionItem.gbgAmount - 50
            currentWorkingitem.isUserUpvotedOnThis = false
            listOfList.forEach {
                var storiesAll = ArrayList(it.value?.items ?: ArrayList())
                val result = replacer.findAndReplace(StoryWrapper(currentWorkingitem, UpdatingState.DONE), storiesAll)
                if (result) {
                    mMainThreadExecutor.execute {
                        it.value = StoryTreeItems(storiesAll, it.value?.type ?: FeedType.NEW)
                    }
                }
            }
        }
    }

    override fun requestStoryUpdate(story: StoryTree) {
        mExecutor.execute {
            val story = api.getStory(story.rootStory()?.categoryName ?: "",
                    story.rootStory()?.author ?: "",
                    story.rootStory()?.permlink ?: "")
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

    override fun createPost(title: String, content: List<EditorPart>, tags: List<String>, resultListener:  (Unit, GolosError?) -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
package io.golos.golos.repository

import android.support.annotation.WorkerThread
import eu.bittrade.libs.steemj.Golos4J
import eu.bittrade.libs.steemj.base.models.*
import eu.bittrade.libs.steemj.communication.CommunicationHandler
import eu.bittrade.libs.steemj.communication.dto.ResponseWrapperDTO
import eu.bittrade.libs.steemj.enums.DiscussionSortType
import eu.bittrade.libs.steemj.enums.PrivateKeyType
import eu.bittrade.libs.steemj.util.AuthUtils
import io.golos.golos.App
import io.golos.golos.repository.model.UserAuthResponse
import io.golos.golos.repository.persistence.Persister
import io.golos.golos.repository.persistence.model.AccountInfo
import io.golos.golos.repository.persistence.model.UserData
import io.golos.golos.screens.main_stripes.model.StripeFragmentType
import io.golos.golos.screens.main_stripes.viewmodel.ImageLoadRunnable
import io.golos.golos.screens.story.model.RootStory
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.utils.avatarPath
import org.apache.commons.lang3.tuple.ImmutablePair
import org.bitcoinj.core.AddressFormatException
import timber.log.Timber
import java.lang.IllegalArgumentException
import java.util.*
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

/**
 * Created by yuri on 31.10.17.
 */
abstract class Repository {

    companion object {
        val get: Repository
            get() {
                if (App.isMocked) return MockRepoImpl() else
                    return RepositoryImpl(Persister.get,
                            Golos4J.getInstance())
            }
        val sharedExecutor: ThreadPoolExecutor by lazy {
            val queu = PriorityBlockingQueue<Runnable>(15, Comparator<Runnable> { o1, o2 ->
                if (o1 is ImageLoadRunnable) Int.MAX_VALUE
                else if (o1 is Runnable) Int.MIN_VALUE
                else 0
            })
            ThreadPoolExecutor(1, 1,
                    Long.MAX_VALUE, TimeUnit.MILLISECONDS, queu)
        }

    }

    @WorkerThread
    abstract fun getStripeItems(limit: Int, type: StripeFragmentType, truncateBody: Int,
                                startAuthor: String? = null, startPermlink: String? = null): List<RootStory>

    @WorkerThread
    abstract fun upvote(userName: String, permlink: String, percents: Short): RootStory

    @WorkerThread
    abstract fun getUserAvatar(username: String, permlink: String? = null, blog: String? = null): String?

    @WorkerThread
    abstract fun getStory(blog: String, author: String, permlink: String): StoryTree


    @WorkerThread
    abstract fun authWithMasterKey(userName: String, masterKey: String): UserAuthResponse

    @WorkerThread
    abstract fun authWithActiveWif(login: String, activeWif: String): UserAuthResponse

    @WorkerThread
    abstract fun authWithPostingWif(login: String, postingWif: String): UserAuthResponse

    @WorkerThread
    abstract fun getAccountData(of: String): AccountInfo

    abstract fun getCurrentUserData(): UserData?

    abstract fun deleteUserdata()
    abstract fun setUserAccount(userName: String, privateActiveWif: String?, privatePostingWif: String?)
    @WorkerThread
    abstract fun downVote(author: String, permlink: String): RootStory

    abstract fun getUserFeed(userName: String, limit: Int, truncateBody: Int, startAuthor: String?, startPermlink: String?): List<RootStory>
}

private class RepositoryImpl(private val mPersister: Persister,
                             private val mGolosApi: Golos4J) : Repository() {
    private val mAvatarRefreshDelay = TimeUnit.DAYS.toMillis(7)

    override fun getStripeItems(limit: Int, type: StripeFragmentType, truncateBody: Int,
                                startAuthor: String?, startPermlink: String?): List<RootStory> {
        var discussionSortType =
                when (type) {
                    StripeFragmentType.ACTUAL -> DiscussionSortType.GET_DISCUSSIONS_BY_HOT
                    StripeFragmentType.POPULAR -> DiscussionSortType.GET_DISCUSSIONS_BY_TRENDING
                    StripeFragmentType.NEW -> DiscussionSortType.GET_DISCUSSIONS_BY_CREATED
                    StripeFragmentType.PROMO -> DiscussionSortType.GET_DISCUSSIONS_BY_PROMOTED
                    StripeFragmentType.FEED -> return getUserFeed(mPersister.getCurrentUserName()!!, limit, truncateBody, startAuthor, startPermlink)
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

    override fun downVote(author: String, permlink: String): RootStory {
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

    override fun upvote(author: String, permlink: String, percents: Short): RootStory {
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

    private fun getRootStoryWithoutComments(author: String, permlink: String): RootStory {
        val story = RootStory(mGolosApi.databaseMethods.getContent(AccountName(author), Permlink(permlink))!!, null)

        var currentUser = mPersister.getCurrentUserName()
        if (currentUser != null) {
            if (story.activeVotes.filter { it.first == currentUser && it.second > 0 }.count() != 0) story.isUserUpvotedOnThis = true
        }
        story.avatarPath = mPersister.getAvatarForUser(story.author)?.first
        return story
    }
}


private class MockRepoImpl : Repository() {

    override fun getStripeItems(limit: Int, type: StripeFragmentType, truncateBody: Int, startAuthor: String?, startPermlink: String?): List<RootStory> {
        val mapper = CommunicationHandler.getObjectMapper()
        val context = App.get.context
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
        return "https://s20.postimg.org/6bfyz1wjh/VFcp_Mpi_DLUIk.jpg"
    }

    override fun upvote(userName: String, permlink: String, percents: Short): RootStory {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun downVote(author: String, permlink: String): RootStory {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getStory(blog: String, author: String, permlink: String): StoryTree {
        val mapper = CommunicationHandler.getObjectMapper()
        val context = App.get.context
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
}
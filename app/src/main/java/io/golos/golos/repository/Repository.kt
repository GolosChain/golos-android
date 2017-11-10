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
import io.golos.golos.persistence.Persister
import io.golos.golos.repository.model.UserAuthResponse
import io.golos.golos.screens.main_stripes.model.StripeItem
import io.golos.golos.screens.main_stripes.model.StripeType
import io.golos.golos.screens.story.model.StoryTreeBuilder
import io.golos.golos.utils.avatarPath
import java.lang.IllegalArgumentException
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by yuri on 31.10.17.
 */
abstract class Repository {
    @WorkerThread
    abstract fun getStripeItems(limit: Int, type: StripeType, truncateBody: Int,
                                startAuthor: String? = null, startPermlink: String? = null): List<StripeItem>

    @WorkerThread
    abstract fun getUserAvatar(username: String, permlink: String? = null, blog: String? = null): String?

    @WorkerThread
    abstract fun getStory(blog: String, author: String, permlink: String): StoryTreeBuilder

    companion object {
        val get: Repository
            get() {
                if (App.isMocked) return MockRepoImpl() else
                    return RepositoryImpl(Persister.get,
                            Golos4J.getInstance())
            }
        val sharedExecutor = Executors.newSingleThreadExecutor()
    }

    @WorkerThread
    abstract fun authWithMasterKey(userName: String, masterKey: String): UserAuthResponse

    @WorkerThread
    abstract fun authWithActiveWif(login: String, activeWif: String): UserAuthResponse

    @WorkerThread
    abstract fun authWithPostingWif(login: String, postingWif: String): UserAuthResponse
}

private class RepositoryImpl(private val mPersister: Persister,
                             private val mGolosApi: Golos4J) : Repository() {
    private val mAvatarRefreshDelay = TimeUnit.DAYS.toMillis(7)

    override fun getStripeItems(limit: Int, type: StripeType, truncateBody: Int,
                                startAuthor: String?, startPermlink: String?): List<StripeItem> {
        var discussionSortType =
                when (type) {
                    StripeType.ACTUAL -> DiscussionSortType.GET_DISCUSSIONS_BY_HOT
                    StripeType.POPULAR -> DiscussionSortType.GET_DISCUSSIONS_BY_TRENDING
                    StripeType.NEW -> DiscussionSortType.GET_DISCUSSIONS_BY_CREATED
                    StripeType.PROMO -> DiscussionSortType.GET_DISCUSSIONS_BY_PROMOTED
                }
        val query = DiscussionQuery()
        query.limit = limit

        if (startAuthor != null) query.startAuthor = AccountName(startAuthor)
        if (startPermlink != null) query.startPermlink = Permlink(startPermlink)
        query.truncateBody = truncateBody.toLong()

        val discussions = mGolosApi.databaseMethods.getDiscussionsBy(query, discussionSortType)
        val out = ArrayList<StripeItem>()
        discussions.forEach {
            out.add(StripeItem(it))
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

    override fun getStory(blog: String, author: String, permlink: String): StoryTreeBuilder {
        return StoryTreeBuilder(mGolosApi.databaseMethods.getStoryByRoute(blog, AccountName(author), Permlink(permlink)))
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
                UserAuthResponse(true, acc.name.name,
                        Pair(postingPublicOuter, privateKeys[PrivateKeyType.POSTING]),
                        Pair(activePublicOuter, privateKeys[PrivateKeyType.ACTIVE]),
                        acc.avatarPath,
                        acc.postCount,
                        acc.balance.amount / 1000)
            } else {
                UserAuthResponse(false, acc.name.name, null,
                        null, null, 0, 0.0)
            }
        } else if (activeWif != null && postingWif != null) {
            return try {
                AuthUtils.isWiFsValid(activeWif, activePublicOuter)
                AuthUtils.isWiFsValid(postingWif, activePublicOuter)
                UserAuthResponse(true, acc.name.name,
                        Pair(postingPublicOuter, postingWif),
                        Pair(activePublicOuter, activeWif),
                        acc.avatarPath,
                        acc.postCount,
                        acc.balance.amount / 1000)
            } catch (e: IllegalArgumentException) {
                UserAuthResponse(false, acc.name.name, null,
                        null, null, 0, 0.0)
            }
        } else if (activeWif != null) {
            return try {
                AuthUtils.isWiFsValid(activeWif, activePublicOuter)
                UserAuthResponse(true, acc.name.name,
                        Pair(postingPublicOuter, null),
                        Pair(activePublicOuter, activeWif),
                        acc.avatarPath,
                        acc.postCount,
                        acc.balance.amount / 1000)
            } catch (e: IllegalArgumentException) {
                UserAuthResponse(false, acc.name.name, null,
                        null, null, 0, 0.0)
            }

        } else {
            return try {
                AuthUtils.isWiFsValid(postingWif!!, activePublicOuter)
                UserAuthResponse(true, acc.name.name,
                        Pair(postingPublicOuter, postingWif),
                        Pair(activePublicOuter, null),
                        acc.avatarPath,
                        acc.postCount,
                        acc.balance.amount / 1000)
            } catch (e: IllegalArgumentException) {
                UserAuthResponse(false, acc.name.name, null,
                        null, null, 0, 0.0)
            }
        }
    }
}

private class MockRepoImpl : Repository() {
    override fun getStripeItems(limit: Int, type: StripeType, truncateBody: Int, startAuthor: String?, startPermlink: String?): List<StripeItem> {
        val mapper = CommunicationHandler.getObjectMapper()
        val context = App.get.context
        val ins = context.resources.openRawResource(context.resources.getIdentifier("stripe",
                "raw", context.packageName))
        val wrapperDTO = mapper.readValue<ResponseWrapperDTO<*>>(ins, ResponseWrapperDTO::class.java)
        val type = mapper.typeFactory.constructCollectionType(List::class.java, Discussion::class.java)
        val discussions = mapper.convertValue<List<Discussion>>(wrapperDTO.result, type)

        val out = ArrayList<StripeItem>()
        discussions.forEach { out.add(StripeItem(it)) }
        return out
    }

    override fun getUserAvatar(username: String, permlink: String?, blog: String?): String? {
        return "https://s20.postimg.org/6bfyz1wjh/VFcp_Mpi_DLUIk.jpg"
    }

    override fun getStory(blog: String, author: String, permlink: String): StoryTreeBuilder {
        val mapper = CommunicationHandler.getObjectMapper()
        val context = App.get.context
        val ins = context.resources.openRawResource(context.resources.getIdentifier("story2",
                "raw", context.packageName))
        val wrapperDTO = mapper.readValue<ResponseWrapperDTO<*>>(ins, ResponseWrapperDTO::class.java)
        val type = mapper.typeFactory.constructCollectionType(List::class.java, Story::class.java)
        val stoeryes = mapper.convertValue<List<Story>>(wrapperDTO.result, type)
        return StoryTreeBuilder(stoeryes[0])
    }

    override fun authWithMasterKey(userName: String, masterKey: String): UserAuthResponse {
        val response = Golos4J.getInstance().databaseMethods.getAccounts(listOf(AccountName("golos.loto")))
        val acc = response[0]
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
}
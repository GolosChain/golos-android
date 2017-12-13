package io.golos.golos.repository.api

import android.support.annotation.VisibleForTesting
import eu.bittrade.libs.steemj.Golos4J
import eu.bittrade.libs.steemj.base.models.*
import eu.bittrade.libs.steemj.enums.DiscussionSortType
import eu.bittrade.libs.steemj.enums.PrivateKeyType
import eu.bittrade.libs.steemj.exceptions.SteemResponseError
import eu.bittrade.libs.steemj.util.AuthUtils
import io.golos.golos.R
import io.golos.golos.repository.StoryFilter
import io.golos.golos.repository.model.CreatePostResult
import io.golos.golos.repository.model.DiscussionItemFactory
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.repository.model.UserAuthResponse
import io.golos.golos.repository.persistence.model.AccountInfo
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.utils.*
import org.apache.commons.lang3.tuple.ImmutablePair
import org.bitcoinj.core.AddressFormatException
import timber.log.Timber
import java.io.File
import java.util.*

/**
 * Created by yuri on 20.11.17.
 */
@VisibleForTesting
class ApiImpl : GolosApi() {
    private var mGolosApi = Golos4J.getInstance()
    override fun getUserAvatar(username: String, permlink: String?, blog: String?): String? {
        return if (permlink != null && blog != null) mGolosApi.databaseMethods.getAccountAvatar(blog, AccountName(username), Permlink(permlink))
        else mGolosApi.databaseMethods.getAccountAvatar(AccountName(username))
    }

    override fun getUserFeed(userName: String,
                             type: FeedType,
                             limit: Int,
                             truncateBody: Int,
                             startAuthor: String?, startPermlink: String?): List<StoryTree> {
        var discussionSortType =
                when (type) {
                    FeedType.PERSONAL_FEED -> DiscussionSortType.GET_DISCUSSIONS_BY_FEED
                    FeedType.BLOG -> DiscussionSortType.GET_DISCUSSIONS_BY_BLOG
                    FeedType.COMMENTS -> DiscussionSortType.GET_DISCUSSIONS_BY_COMMENTS
                    else -> throw IllegalArgumentException("use getStories( for $type type of feed")
                }

        val query = DiscussionQuery()
        query.limit = limit
        query.truncateBody = truncateBody
        if (startAuthor != null) query.startAuthor = AccountName(startAuthor)
        else if (startAuthor == null && (type == FeedType.COMMENTS)) query.startAuthor = AccountName(userName)

        if (startPermlink != null) query.startPermlink = Permlink(startPermlink)
        query.selectAuthors = listOf(AccountName(userName))
        val discussions = mGolosApi.databaseMethods.getDiscussionsLightBy(query, discussionSortType)
        val out = ArrayList<StoryTree>()
        discussions.forEach {
            val story = StoryTree(StoryWrapper(DiscussionItemFactory.create(it, null), UpdatingState.DONE), ArrayList())
            out.add(story)
        }
        return out
    }

    override fun getStory(blog: String, author: String, permlink: String): StoryTree {
        var story = StoryTree(mGolosApi.databaseMethods.getStoryByRoute(blog, AccountName(author), Permlink(permlink)))
        return story
    }

    override fun getStoryWithoutComments(author: String, permlink: String): StoryTree {
        val story = StoryTree(StoryWrapper(DiscussionItemFactory.create(
                mGolosApi.databaseMethods.getContent(AccountName(author), Permlink(permlink))!!,
                null), UpdatingState.DONE), ArrayList())
        return story
    }

    override fun getStories(limit: Int, type: FeedType,
                            truncateBody: Int, filter: StoryFilter?,
                            startAuthor: String?,
                            startPermlink: String?): List<StoryTree> {
        var discussionSortType =
                when (type) {
                    FeedType.ACTUAL -> DiscussionSortType.GET_DISCUSSIONS_BY_HOT
                    FeedType.POPULAR -> DiscussionSortType.GET_DISCUSSIONS_BY_TRENDING
                    FeedType.NEW -> DiscussionSortType.GET_DISCUSSIONS_BY_CREATED
                    FeedType.PROMO -> DiscussionSortType.GET_DISCUSSIONS_BY_PROMOTED
                    else -> throw IllegalArgumentException("use getUserFeed() for $type type of feed")
                }
        val query = DiscussionQuery()
        query.limit = limit

        if (startAuthor != null) query.startAuthor = AccountName(startAuthor)
        if (startPermlink != null) query.startPermlink = Permlink(startPermlink)
        query.truncateBody = truncateBody
        filter?.let {
            it.tagFilter?.let {
                query.selectTags = listOf(it)
            }
        }
        val discussions = mGolosApi.databaseMethods.getDiscussionsLightBy(query, discussionSortType)
        val out = ArrayList<StoryTree>()
        discussions.forEach {
            val story = StoryTree(StoryWrapper(DiscussionItemFactory.create(it, null), UpdatingState.DONE), ArrayList())
            out.add(story)
        }
        return out
    }

    override fun auth(userName: String, masterKey: String?, activeWif: String?, postingWif: String?): UserAuthResponse {
        if (masterKey == null && activeWif == null && postingWif == null)
            return UserAuthResponse(false, userName, null, null, null,
                    error = GolosError(ErrorCode.ERROR_AUTH, null, R.string.wrong_credentials))
        if (userName.length < 3 || userName.length > 16) {
            return UserAuthResponse(false, userName, null,
                    null, null, error = GolosError(ErrorCode.ERROR_AUTH, null, R.string.wrong_credentials))
        }
        val accs = mGolosApi.databaseMethods.getAccounts(listOf(AccountName(userName)))
        if (accs.size == 0 || accs.get(0) == null) return UserAuthResponse(false, userName, null,
                null, null, error = GolosError(ErrorCode.ERROR_AUTH, null, R.string.wrong_credentials))
        val acc = accs.get(0)
        var postingPublicOuter = (acc.posting.keyAuths.keys.toTypedArray()[0] as PublicKey).addressFromPublicKey
        var activePublicOuter = (acc.active.keyAuths.keys.toTypedArray()[0] as PublicKey).addressFromPublicKey

        val votePower = acc.vestingShares.amount * 0.000268379
        val golosNum = acc.balance.amount
        val gbgAmount = acc.sbdBalance.amount
        val safeGolos = acc.savingsBalance.amount
        val safeGbg = acc.savingsSbdBalance.amount
        val accWorth = ((votePower + golosNum + safeGolos) * 0.128670406) + ((gbgAmount + safeGbg) * 0.04106528)

        if (masterKey != null) {
            val keys = AuthUtils.generatePublicWiFs(userName, masterKey, arrayOf(PrivateKeyType.POSTING, PrivateKeyType.ACTIVE))
            return if (postingPublicOuter == keys[PrivateKeyType.POSTING] || activePublicOuter == keys[PrivateKeyType.ACTIVE]) {

                val followObject = mGolosApi.followApiMethods.getFollowCount(AccountName(userName))
                val followersCount = followObject.followerCount.toLong()
                val followingCount = followObject.followingCount.toLong()

                val privateKeys = AuthUtils.generatePrivateWiFs(userName, masterKey, arrayOf(PrivateKeyType.POSTING, PrivateKeyType.ACTIVE))
                val resp = UserAuthResponse(true,
                        acc.name.name,
                        acc.moto,
                        Pair(postingPublicOuter, privateKeys[PrivateKeyType.POSTING]),
                        Pair(activePublicOuter, privateKeys[PrivateKeyType.ACTIVE]),
                        acc.avatarPath,
                        acc.postCount,
                        accWorth,
                        followingCount,
                        followersCount,
                        gbgAmount,
                        golosNum,
                        votePower,
                        safeGbg,
                        safeGolos)
                if (resp.isKeyValid) {
                    mGolosApi.addKeysToAccount(AccountName(userName), setOf(ImmutablePair(PrivateKeyType.POSTING, resp.postingAuth!!.second!!),
                            ImmutablePair(PrivateKeyType.ACTIVE, resp.activeAuth!!.second!!)))
                }
                resp
            } else {
                negativeAuthResponse(acc)
            }
        } else if (activeWif != null && postingWif != null) {
            return try {
                val isKeyValid = AuthUtils.isWiFsValid(activeWif, activePublicOuter) && AuthUtils.isWiFsValid(postingWif, postingPublicOuter)
                if (isKeyValid) {
                    val followObject = mGolosApi.followApiMethods.getFollowCount(AccountName(userName))
                    val followersCount = followObject.followerCount.toLong()
                    val followingCount = followObject.followingCount.toLong()

                    val resp = UserAuthResponse(true,
                            acc.name.name,
                            acc.moto,
                            Pair(postingPublicOuter, postingWif),
                            Pair(activePublicOuter, activeWif),
                            acc.avatarPath,
                            acc.postCount,
                            accWorth,
                            followingCount,
                            followersCount,
                            gbgAmount,
                            golosNum,
                            votePower,
                            safeGbg,
                            safeGolos)

                    mGolosApi.addKeysToAccount(AccountName(userName), setOf(ImmutablePair(PrivateKeyType.POSTING, resp.postingAuth!!.second!!),
                            ImmutablePair(PrivateKeyType.ACTIVE, resp.activeAuth!!.second!!)))
                    resp
                } else {
                    negativeAuthResponse(acc)
                }
            } catch (e: java.lang.IllegalArgumentException) {
                negativeAuthResponse(acc, e)
            } catch (e: AddressFormatException) {
                negativeAuthResponse(acc, e)
            }
        } else if (activeWif != null) {
            return try {
                val isKeyValid = AuthUtils.isWiFsValid(activeWif, activePublicOuter)
                if (isKeyValid) {
                    val followObject = mGolosApi.followApiMethods.getFollowCount(AccountName(userName))
                    val followersCount = followObject.followerCount.toLong()
                    val followingCount = followObject.followingCount.toLong()

                    val resp = UserAuthResponse(true,
                            acc.name.name,
                            acc.moto,
                            Pair(postingPublicOuter, null),
                            Pair(activePublicOuter, activeWif),
                            acc.avatarPath,
                            acc.postCount,
                            accWorth,
                            followingCount,
                            followersCount,
                            gbgAmount,
                            golosNum,
                            votePower,
                            safeGbg,
                            safeGolos)
                    mGolosApi.addKeysToAccount(AccountName(userName), ImmutablePair(PrivateKeyType.ACTIVE, resp.activeAuth!!.second!!))
                    resp
                } else {
                    negativeAuthResponse(acc)
                }
            } catch (e: java.lang.IllegalArgumentException) {
                negativeAuthResponse(acc, e)
            } catch (e: AddressFormatException) {
                negativeAuthResponse(acc, e)
            }

        } else {
            return try {
                val isKeyValid = AuthUtils.isWiFsValid(postingWif!!, postingPublicOuter)
                if (isKeyValid) {
                    val followObject = mGolosApi.followApiMethods.getFollowCount(AccountName(userName))
                    val followersCount = followObject.followerCount.toLong()
                    val followingCount = followObject.followingCount.toLong()

                    val resp = UserAuthResponse(true,
                            acc.name.name,
                            acc.moto,
                            Pair(postingPublicOuter, postingWif),
                            Pair(activePublicOuter, null),
                            acc.avatarPath,
                            acc.postCount,
                            accWorth,
                            followingCount,
                            followersCount,
                            gbgAmount,
                            golosNum,
                            votePower,
                            safeGbg,
                            safeGolos)
                    mGolosApi.addKeysToAccount(AccountName(userName), ImmutablePair(PrivateKeyType.POSTING, resp.postingAuth!!.second!!))
                    resp
                } else {
                    negativeAuthResponse(acc)
                }
            } catch (e: java.lang.IllegalArgumentException) {
                negativeAuthResponse(acc, e)
            } catch (e: AddressFormatException) {
                negativeAuthResponse(acc, e)
            }
        }
    }

    private fun negativeAuthResponse(acc: ExtendedAccount, e: Exception? = null): UserAuthResponse {
        val error = if (e != null) GolosErrorParser.parse(e) else GolosError(ErrorCode.ERROR_AUTH, null, R.string.wrong_credentials)
        return UserAuthResponse(false, acc.name.name,
                null,
                null,
                null,
                error = error)
    }

    override fun getAccountData(of: String): AccountInfo {
        if (of.isEmpty()) return AccountInfo(of, accountWorth = 0.0, postsCount = 0)
        val accs = mGolosApi.databaseMethods.getAccounts(listOf(AccountName(of)))
        if (accs.size == 0) return AccountInfo(of, accountWorth = 0.0, postsCount = 0)
        val acc = accs.get(index = 0)
        val votePower = acc.vestingShares.amount * 0.000268379
        val golosNum = acc.balance.amount
        val gbgAmount = acc.sbdBalance.amount
        val accWorth = ((votePower + golosNum) * 0.128670406) + (gbgAmount * 0.04106528)
        return AccountInfo(of, acc.avatarPath, accWorth, acc.postCount)
    }

    override fun cancelVote(author: String, permlink: String): GolosDiscussionItem {
        mGolosApi.simplifiedOperations.cancelVote(AccountName(author), Permlink(permlink))
        return getRootStoryWithoutComments(author, permlink)
    }

    private fun getRootStoryWithoutComments(author: String, permlink: String): GolosDiscussionItem {
        val story = GolosDiscussionItem(mGolosApi.databaseMethods.getContent(AccountName(author), Permlink(permlink))!!, null)

        return story
    }

    override fun upVote(author: String, permlink: String, percents: Short): GolosDiscussionItem {
        mGolosApi.simplifiedOperations.vote(AccountName(author), Permlink(permlink), percents)
        return getRootStoryWithoutComments(author, permlink)
    }

    override fun uploadImage(sendFromAccount: String, file: File): String {
        var fileLocal = file
        Timber.e(fileLocal.absolutePath)
        if (fileLocal.absolutePath.matches(Regex("^/file:.*"))) {
            fileLocal = File(fileLocal.absolutePath.removePrefix("/file:"))
        }
        val response = mGolosApi.golosIoSpecificMethods.uploadFile(AccountName(sendFromAccount), fileLocal)

        if (response.error != null) throw SteemResponseError(response.error, null)
        return response.urlString ?: "error"
    }

    override fun sendPost(sendFromAccount: String, title: String, content: String, tags: Array<String>): CreatePostResult {
        val result = mGolosApi.simplifiedOperations.createPost(AccountName(sendFromAccount), title, content, tags)
        return CreatePostResult(true, result.author.name, result.getTags().first() ?: "", result.permlink.link)
    }

    override fun sendComment(sendFromAccount: String,
                             authorOfItemToReply: String,
                             permlinkOfItemToReply: String,
                             content: String,
                             categoryName: String): CreatePostResult {
        val result = mGolosApi.simplifiedOperations.createComment(AccountName(authorOfItemToReply),
                Permlink(permlinkOfItemToReply),
                AccountName(sendFromAccount),
                content,
                Array(1, { categoryName }))
        return CreatePostResult(false, result.author.name, result.getTags().first() ?: "", result.permlink.link)
    }
}
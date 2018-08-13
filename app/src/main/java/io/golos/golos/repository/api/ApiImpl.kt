package io.golos.golos.repository.api

import eu.bittrade.libs.golosj.Golos4J
import eu.bittrade.libs.golosj.apis.follow.enums.FollowType
import eu.bittrade.libs.golosj.apis.follow.model.FollowApiObject
import eu.bittrade.libs.golosj.base.models.AccountName
import eu.bittrade.libs.golosj.base.models.DiscussionQuery
import eu.bittrade.libs.golosj.base.models.ExtendedAccount
import eu.bittrade.libs.golosj.base.models.Permlink
import eu.bittrade.libs.golosj.enums.DiscussionSortType
import eu.bittrade.libs.golosj.enums.PrivateKeyType
import eu.bittrade.libs.golosj.exceptions.SteemResponseError
import eu.bittrade.libs.golosj.util.AuthUtils
import eu.bittrade.libs.golosj.util.ImmutablePair
import io.golos.golos.R
import io.golos.golos.repository.model.*
import io.golos.golos.repository.persistence.model.GolosUser
import io.golos.golos.repository.persistence.model.GolosUserAccountInfo
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryWithComments
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.utils.*
import org.bitcoinj.core.AddressFormatException
import timber.log.Timber
import java.io.File


internal class ApiImpl : GolosApi() {
    private var mGolosApi = Golos4J.getInstance()


    override fun getStory(blog: String,
                          author: String,
                          permlink: String,
                          accountDataHandler: (List<GolosUserAccountInfo>) -> Unit): StoryWithComments {
        val rawStory = mGolosApi.databaseMethods.getStoryWithRepliesAndInvolvedAccounts(AccountName(author), Permlink(permlink), -1)
        val story = StoryWithComments(rawStory!!)
        accountDataHandler.invoke(rawStory.involvedAccounts.map { convertExtendedAccountToAccountInfo(it, false) })
        return story
    }

    override fun getStoryWithoutComments(author: String, permlink: String): StoryWithComments {
        return StoryWithComments(StoryWrapper(DiscussionItemFactory.create(
                mGolosApi.databaseMethods.getContent(AccountName(author), Permlink(permlink))!!,
                null), UpdatingState.DONE), ArrayList())
    }

    override fun getUserAvatars(names: List<String>): Map<String, String?> {
        val out = HashMap<String, String?>()
        if (names.isEmpty())return out
        names.forEach {
            out[it] = null
        }

        return mGolosApi.databaseMethods.getAccountAvatar(names.map { AccountName(it) }) ?: out
    }

    override fun getStories(limit: Int, type: FeedType,
                            truncateBody: Int, filter: StoryFilter?,
                            startAuthor: String?,
                            startPermlink: String?): List<StoryWithComments> {
        val discussionSortType =
                when (type) {
                    FeedType.ACTUAL -> DiscussionSortType.GET_DISCUSSIONS_BY_HOT
                    FeedType.POPULAR -> DiscussionSortType.GET_DISCUSSIONS_BY_TRENDING
                    FeedType.NEW -> DiscussionSortType.GET_DISCUSSIONS_BY_CREATED
                    FeedType.PROMO -> DiscussionSortType.GET_DISCUSSIONS_BY_PROMOTED
                    FeedType.PERSONAL_FEED -> DiscussionSortType.GET_DISCUSSIONS_BY_FEED
                    FeedType.BLOG -> DiscussionSortType.GET_DISCUSSIONS_BY_BLOG
                    FeedType.COMMENTS -> DiscussionSortType.GET_DISCUSSIONS_BY_COMMENTS
                    else -> throw IllegalStateException(" type $type is unsupported")
                }

        val query = DiscussionQuery()
        query.limit = limit

        if (startAuthor != null) query.startAuthor = AccountName(startAuthor)
        if (startPermlink != null) query.startPermlink = Permlink(startPermlink)

        filter?.userNameFilter?.let {
            query.selectAuthors = it.map { AccountName(it) }
        }

        if (startAuthor == null && (type == FeedType.COMMENTS)) {
            query.startAuthor = AccountName(filter!!.userNameFilter.first())
            query.selectAuthors = null
        }

        query.truncateBody = truncateBody
        filter?.let {
            it.tagFilter.let {
                query.selectTags = it
            }
        }
        val discussions = mGolosApi.databaseMethods.getDiscussionsLightBy(query, discussionSortType)
        val out = ArrayList<StoryWithComments>()
        discussions.forEach {
            if (it != null) {
                val story = StoryWithComments(StoryWrapper(DiscussionItemFactory.create(it, null), UpdatingState.DONE), ArrayList())
                out.add(story)
            }
        }
        return out
    }

    override fun auth(userName: String, masterKey: String?, activeWif: String?, postingWif: String?): UserAuthResponse {
        if (masterKey == null && activeWif == null && postingWif == null)
            return UserAuthResponse(false, null, null,
                    error = GolosError(ErrorCode.ERROR_AUTH, null, R.string.wrong_credentials),
                    accountInfo = GolosUserAccountInfo(GolosUser(userName)))

        if (userName.length < 3 || userName.length > 16) {
            return UserAuthResponse(false, null, null,
                    error = GolosError(ErrorCode.ERROR_AUTH, null, R.string.wrong_credentials),
                    accountInfo = GolosUserAccountInfo(GolosUser(userName)))
        }
        if (activeWif?.startsWith("GLS") == true
                || postingWif?.startsWith("GLS") == true) {
            return UserAuthResponse(false, null, null,
                    error = GolosError(ErrorCode.ERROR_AUTH, null, R.string.enter_private_key),
                    accountInfo = GolosUserAccountInfo(GolosUser(userName)))
        }
        val acc = getAccountInfo(userName)
        if (acc.activePublicKey.isEmpty()) return UserAuthResponse(false, null, null,
                error = GolosError(ErrorCode.ERROR_AUTH, null, R.string.wrong_credentials),
                accountInfo = GolosUserAccountInfo(GolosUser(userName)))

        if (masterKey != null) {
            val keys = AuthUtils.generatePublicWiFs(userName, masterKey, arrayOf(PrivateKeyType.POSTING, PrivateKeyType.ACTIVE))
            return if (acc.postingPublicKey == keys[PrivateKeyType.POSTING] || acc.activePublicKey == keys[PrivateKeyType.ACTIVE]) {

                val privateKeys = AuthUtils.generatePrivateWiFs(userName, masterKey, arrayOf(PrivateKeyType.POSTING, PrivateKeyType.ACTIVE))
                val resp = UserAuthResponse(true,
                        Pair(acc.postingPublicKey, privateKeys[PrivateKeyType.POSTING]),
                        Pair(acc.activePublicKey, privateKeys[PrivateKeyType.ACTIVE]),
                        accountInfo = acc)
                if (resp.isKeyValid) {
                    mGolosApi.addKeysToAccount(AccountName(userName), setOf(ImmutablePair(PrivateKeyType.POSTING, resp.postingAuth!!.second!!),
                            ImmutablePair(PrivateKeyType.ACTIVE, resp.activeAuth!!.second!!)))
                }
                resp
            } else {
                negativeAuthResponse(userName)
            }
        } else if (activeWif != null && postingWif != null) {
            return try {
                val isKeyValid = AuthUtils.isWiFsValid(activeWif, acc.activePublicKey)
                        && AuthUtils.isWiFsValid(postingWif, acc.postingPublicKey)
                if (isKeyValid) {

                    val resp = UserAuthResponse(true,
                            Pair(acc.postingPublicKey, postingWif),
                            Pair(acc.activePublicKey, activeWif),
                            accountInfo = acc)
                    mGolosApi.addKeysToAccount(AccountName(userName), setOf(ImmutablePair(PrivateKeyType.POSTING, resp.postingAuth!!.second!!),
                            ImmutablePair(PrivateKeyType.ACTIVE, resp.activeAuth!!.second!!)))
                    resp
                } else {
                    negativeAuthResponse(userName)
                }
            } catch (e: java.lang.IllegalArgumentException) {
                negativeAuthResponse(userName, e)
            } catch (e: AddressFormatException) {
                negativeAuthResponse(userName, e)
            }
        } else if (activeWif != null) {
            return try {
                val isKeyValid = AuthUtils.isWiFsValid(activeWif, acc.activePublicKey)
                if (isKeyValid) {

                    val resp = UserAuthResponse(true,
                            Pair(acc.postingPublicKey, null),
                            Pair(acc.activePublicKey, activeWif),
                            accountInfo = acc)
                    mGolosApi.addKeysToAccount(AccountName(userName), ImmutablePair(PrivateKeyType.ACTIVE, resp.activeAuth!!.second!!))
                    resp
                } else {
                    negativeAuthResponse(userName)
                }
            } catch (e: java.lang.IllegalArgumentException) {
                negativeAuthResponse(userName, e)
            } catch (e: AddressFormatException) {
                negativeAuthResponse(userName, e)
            }

        } else {
            return try {
                val isKeyValid = AuthUtils.isWiFsValid(postingWif!!, acc.postingPublicKey)
                if (isKeyValid) {
                    val resp = UserAuthResponse(true,
                            Pair(acc.postingPublicKey, postingWif),
                            Pair(acc.activePublicKey, null),
                            accountInfo = acc)
                    mGolosApi.addKeysToAccount(AccountName(userName), ImmutablePair(PrivateKeyType.POSTING, resp.postingAuth!!.second!!))
                    resp
                } else {
                    negativeAuthResponse(userName)
                }
            } catch (e: java.lang.IllegalArgumentException) {
                negativeAuthResponse(userName, e)
            } catch (e: AddressFormatException) {
                negativeAuthResponse(userName, e)
            }
        }
    }

    private fun negativeAuthResponse(username: String, e: Exception? = null): UserAuthResponse {
        val error = if (e != null) GolosErrorParser.parse(e) else GolosError(ErrorCode.ERROR_AUTH, null, R.string.wrong_credentials)
        return UserAuthResponse(false,
                null,
                null,
                error = error,
                accountInfo = GolosUserAccountInfo(GolosUser(username)))
    }

    override fun getAccountInfo(of: String): GolosUserAccountInfo {
        val accs = mGolosApi.databaseMethods.getAccounts(listOf(AccountName(of)))
        if (accs.size == 0 || accs[0] == null) return GolosUserAccountInfo(GolosUser(of))
        return convertExtendedAccountToAccountInfo(accs[0], true)
    }

    private fun convertExtendedAccountToAccountInfo(acc: ExtendedAccount, fetchSubscribersInfo: Boolean): GolosUserAccountInfo {
        val votePower = acc.vestingShares.amount * 0.000268379
        val golosNum = acc.balance.amount
        val gbgAmount = acc.sbdBalance.amount
        val safeGolos = acc.savingsBalance.amount
        val safeGbg = acc.savingsSbdBalance.amount
        val accWorth = ((votePower + golosNum + safeGolos) * 0.128670406) + ((gbgAmount + safeGbg) * 0.04106528)

        var followersCount = 0L
        var followingCount = 0L
        if (fetchSubscribersInfo) {
            val followObject = mGolosApi.followApiMethods.getFollowCount(AccountName(acc.name.name))
            followersCount = followObject.followerCount.toLong()
            followingCount = followObject.followingCount.toLong()
        }

        val postingPublicOuter = (acc.posting.keyAuths.keys.toTypedArray().firstOrNull())?.addressFromPublicKey
                ?: ""
        val activePublicOuter = (acc.active.keyAuths.keys.toTypedArray().firstOrNull())?.addressFromPublicKey
                ?: ""

        return GolosUserAccountInfo(GolosUser(acc.name.name, acc.avatarPath),
                acc.moto,
                acc.postCount,
                accWorth,
                followingCount,
                followersCount,
                gbgAmount,
                golosNum,
                votePower,
                safeGbg,
                safeGolos,
                postingPublicOuter,
                activePublicOuter,
                votingPower = acc.votingPower,
                location = acc.location ?: "",
                website = acc.webSite ?: "",
                registrationDate = acc.created.dateTimeAsTimestamp,
                userCover = acc.cover)
    }

    override fun cancelVote(author: String, permlink: String): GolosDiscussionItem {
        mGolosApi.simplifiedOperations.cancelVote(AccountName(author), Permlink(permlink))
        return getRootStoryWithoutComments(author, permlink)
    }

    private fun getRootStoryWithoutComments(author: String, permlink: String): GolosDiscussionItem {
        return DiscussionItemFactory.create(mGolosApi.databaseMethods.getContent(AccountName(author), Permlink(permlink))!!, null)
    }

    override fun vote(author: String, permlink: String, percents: Short): GolosDiscussionItem {
        mGolosApi.simplifiedOperations.vote(AccountName(author), Permlink(permlink), percents)
        return getRootStoryWithoutComments(author, permlink)
    }

    override fun uploadImage(sendFromAccount: String, file: File): String {
        val fileLocal = File(file.absolutePath.removePrefix("/file:"))

        val response = mGolosApi.golosIoSpecificMethods.uploadFile(AccountName(sendFromAccount), fileLocal)

        if (response.error != null) {
            Timber.e(response.error)
            throw SteemResponseError(response.error, null)
        }
        return response.urlString ?: "error"
    }

    override fun sendPost(sendFromAccount: String, title: String, content: String, tags: Array<String>): CreatePostResult {
        val result = mGolosApi.simplifiedOperations.createPost(AccountName(sendFromAccount), title, content, tags)
        return CreatePostResult(true, result.author.name, result.getTags().first(), result.permlink.link)
    }

    override fun editPost(originalComentPermlink: String, sendFromAccount: String, title: String, content: String, tags: Array<String>): CreatePostResult {
        val result = mGolosApi.simplifiedOperations.updatePost(
                AccountName(sendFromAccount),
                Permlink(originalComentPermlink),
                title,
                content,
                tags)
        return CreatePostResult(true, result.author.name, result.getTags().first(), result.permlink.link)
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
        return CreatePostResult(false, result.author.name, result.getTags().first(), result.permlink.link)
    }

    override fun editComment(sendFromAccount: String,
                             authorOfItemToReply: String,
                             permlinkOfItemToReply: String,
                             originalComentPermlink: String,
                             content: String,
                             categoryName: String): CreatePostResult {
        val result = mGolosApi.simplifiedOperations.updateComment(AccountName(authorOfItemToReply),
                Permlink(permlinkOfItemToReply),
                Permlink(originalComentPermlink),
                AccountName(sendFromAccount),
                content,
                Array(1, { categoryName }))
        return CreatePostResult(false, result.author.name, result.getTags().first(), result.permlink.link)
    }

    override fun getSubscriptions(forUser: String, startFrom: String?): List<FollowApiObject> {
        return getSubscribesOrSubscribers(false, forUser, startFrom)
    }

    override fun getSubscribers(forUser: String, startFrom: String?): List<FollowApiObject> {
        return getSubscribesOrSubscribers(true, forUser, startFrom)
    }

    @Suppress("NAME_SHADOWING")
    private fun getSubscribesOrSubscribers(isSubscribers: Boolean,
                                           forUser: String,
                                           startFrom: String?): List<FollowApiObject> {

        val forUser = AccountName(forUser)
        if (startFrom == null) {
            val followObject = Golos4J.getInstance().followApiMethods.getFollowCount(forUser)
            var count = if (isSubscribers) followObject.followerCount else followObject.followingCount
            if (count == 0) return listOf()
            var frs = if (isSubscribers) Golos4J.getInstance().followApiMethods.getFollowers(forUser,
                    AccountName(""),
                    FollowType.BLOG,
                    if (count < 100) count.toShort() else 100)
            else Golos4J.getInstance().followApiMethods.getFollowing(forUser,
                    AccountName(""),
                    FollowType.BLOG,
                    if (count < 100) count.toShort() else 100)

            if (count > 100) {
                count -= 100
                val times = count / 99
                (0 until times)
                        .forEach {
                            var new = if (isSubscribers) Golos4J.getInstance().followApiMethods.getFollowers(forUser,
                                    frs.last().follower,
                                    FollowType.BLOG, 100)
                            else Golos4J.getInstance().followApiMethods.getFollowing(forUser,
                                    frs.last().following,
                                    FollowType.BLOG, 100)

                            new = new.subList(1, new.size)
                            frs = frs + new
                        }
                var new = if (isSubscribers) Golos4J.getInstance().followApiMethods.getFollowers(forUser,
                        frs.last().follower,
                        FollowType.BLOG, 100)
                else Golos4J.getInstance().followApiMethods.getFollowing(forUser,
                        frs.last().following,
                        FollowType.BLOG, 100)

                new = new.subList(1, new.size)
                frs = frs + new
            }
            return frs.filter { it != null }
        } else {
            var frs = if (isSubscribers) Golos4J.getInstance().followApiMethods.getFollowers(forUser,
                    AccountName(startFrom),
                    FollowType.BLOG,
                    100)
            else Golos4J.getInstance().followApiMethods.getFollowing(forUser,
                    AccountName(startFrom),
                    FollowType.BLOG,
                    100)

            if (frs.size == 100) {
                while (true) {
                    var new = if (isSubscribers) Golos4J.getInstance().followApiMethods.getFollowers(forUser,
                            frs.last().following,
                            FollowType.BLOG, 100)
                    else Golos4J.getInstance().followApiMethods.getFollowing(forUser,
                            frs.last().following,
                            FollowType.BLOG, 100)
                    new = new.subList(1, new.size)
                    frs = frs + new
                    if (new.size != 99) break
                }
            }
            return frs.filterNotNull()
        }
    }

    override fun follow(user: String) {
        Golos4J.getInstance().simplifiedOperations.follow(AccountName(user))
    }

    override fun unfollow(user: String) {
        Golos4J.getInstance().simplifiedOperations.unfollow(AccountName(user))
    }

    override fun getTrendingTag(startFrom: String, maxCount: Int): List<Tag> {

        return if (maxCount <= 1000) Golos4J.getInstance().databaseMethods.getTrendingTags(startFrom, maxCount)
                .filterNotNull()
                .map { Tag(it) }
        else {
            val out = Golos4J.getInstance().databaseMethods.getTrendingTags(startFrom, 1000)
                    .filterNotNull()
                    .map { Tag(it) }.toArrayList()

            val times = (maxCount / 1000) - 1
            (0 until times)
                    .forEach {
                        val new = Golos4J.getInstance().databaseMethods.getTrendingTags(out.last().name, 1000)
                                .filterNotNull()
                                .map { Tag(it) }
                                .toList()
                        out.addAll(new.subList(1, new.size))
                    }
            val last = maxCount - (1000 * (times + 1)) + times + 1
            if (last > 0) {
                val new = Golos4J.getInstance().databaseMethods.getTrendingTags(out.last().name, last)
                        .filterNotNull()
                        .map { Tag(it) }
                        .toList()
                out.addAll(new.subList(1, new.size))
            }

            return out
        }
    }

    override fun getGolosUsers(nick: String): List<GolosUser> {
        return mGolosApi.databaseMethods.lookupAccounts(nick, 10)
                .filter { it != null }
                .map { GolosUser(it) }
    }
}

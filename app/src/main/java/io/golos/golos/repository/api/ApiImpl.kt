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
import io.golos.golos.repository.persistence.model.GolosUserAccountInfo
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryCommentsHierarchyResolver
import io.golos.golos.screens.story.model.StoryWithComments
import io.golos.golos.utils.*
import org.bitcoinj.core.AddressFormatException
import timber.log.Timber
import java.io.File


internal class ApiImpl : GolosApi() {
    private var mGolosApi = Golos4J.getInstance()
    private val mStoryConverter = StoryCommentsHierarchyResolver


    override fun getStory(blog: String,
                          author: String,
                          permlink: String,
                          voteLimit: Int?,
                          accountDataHandler: (List<GolosUserAccountInfo>) -> Unit): StoryWithComments {
        val rawStory = mGolosApi.databaseMethods.getStoryWithRepliesAndInvolvedAccounts(AccountName(author), Permlink(permlink), voteLimit
                ?: -1)

        val story = mStoryConverter.resolve(rawStory!!)
        accountDataHandler.invoke(rawStory.involvedAccounts.map { convertExtendedAccountToAccountInfo(it, false) })
        return story
    }

    override fun getBlogEntries(ofAuthor: String, fromId: Int?, limit: Short): List<GolosBlogEntry> {
        return mGolosApi.followApiMethods.getBlogEntries(AccountName(ofAuthor), fromId ?: 0, limit)
                .mapNotNull { GolosBlogEntry(it.author.name, it.blog.name, it.entryId, it.permlink.link) }
    }

    override fun repostPost(ofAuthor: String, permlink: String) {
        mGolosApi.simplifiedOperations.reblog(AccountName(ofAuthor), Permlink(permlink))
    }

    override fun getStoryWithoutComments(author: String, permlink: String, voteLimit: Int?): StoryWithComments {
        val rawStory = mGolosApi.databaseMethods.getContent(AccountName(author), Permlink(permlink), voteLimit
                ?: -1)
        return StoryWithComments(DiscussionItemFactory.create(rawStory!!), arrayListOf())

    }

    override fun getStories(limit: Int, type: FeedType,
                            truncateBody: Int,
                            filter: StoryFilter?,
                            startAuthor: String?,
                            startPermlink: String?): List<StoryWithComments> {

        val out = ArrayList<StoryWithComments>()

        val discussions = if (type == FeedType.ANSWERS) {
            if (startAuthor == null && filter == null) throw IllegalArgumentException(" for this type of feed, start author must be not null")
            mGolosApi.databaseMethods.getRepliesLightByLastUpdate(AccountName(startAuthor
                    ?: filter?.userNameFilter?.first()),
                    if (startPermlink != null) Permlink(startPermlink) else null, limit)
        } else {
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
            mGolosApi.databaseMethods.getDiscussionsLightBy(query, discussionSortType)
        }

        discussions.forEach {
            if (it != null) {
                val story = StoryWithComments(DiscussionItemFactory.create(it), ArrayList())
                out.add(story)
            }
        }
        return out
    }

    override fun auth(userName: String, masterKey: String?, activeWif: String?, postingWif: String?): UserAuthResponse {
        if (masterKey == null && activeWif == null && postingWif == null)
            return UserAuthResponse(false, null, null,
                    error = GolosError(ErrorCode.ERROR_AUTH, null, R.string.wrong_credentials),
                    accountInfo = GolosUserAccountInfo(userName))

        if (userName.length < 3 || userName.length > 16) {
            return UserAuthResponse(false, null, null,
                    error = GolosError(ErrorCode.ERROR_AUTH, null, R.string.wrong_credentials),
                    accountInfo = GolosUserAccountInfo(userName))
        }
        if (activeWif?.startsWith("GLS") == true
                || postingWif?.startsWith("GLS") == true) {
            return UserAuthResponse(false, null, null,
                    error = GolosError(ErrorCode.ERROR_AUTH, null, R.string.enter_private_key),
                    accountInfo = GolosUserAccountInfo(userName))
        }
        val acc = getGolosUsers(listOf(userName), true)[userName]
                ?: return negativeAuthResponse(userName)

        if (acc.activePublicKey.isEmpty()) return UserAuthResponse(false, null, null,
                error = GolosError(ErrorCode.ERROR_AUTH, null, R.string.wrong_credentials),
                accountInfo = GolosUserAccountInfo(userName))

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
                accountInfo = GolosUserAccountInfo(username))
    }

    override fun getGolosUsers(names: List<String>, fetchSubsInfo: Boolean): Map<String, GolosUserAccountInfo> {
        val accs = mGolosApi.databaseMethods.getAccounts(names.map { AccountName(it) }).filterNotNull()

        if (accs.isEmpty()) return emptyMap()

        return accs
                .map { convertExtendedAccountToAccountInfo(it, fetchSubsInfo) }
                .associateBy { it.userName }


    }


    private fun convertExtendedAccountToAccountInfo(acc: ExtendedAccount,
                                                    fetchSubscribersInfo: Boolean): GolosUserAccountInfo {
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

        return GolosUserAccountInfo(acc.name.name,
                acc.avatarPath,
                acc.moto,
                acc.shownName,
                acc.postCount,
                accWorth,
                gbgAmount,
                golosNum,
                votePower,
                safeGbg,
                safeGolos,
                followersCount.toInt(),
                followingCount.toInt(),

                postingPublicOuter,
                activePublicOuter,
                acc.votingPower,
                acc.location ?: "",
                acc.webSite ?: "",
                acc.created.dateTimeAsTimestamp,
                acc.cover,
                System.currentTimeMillis())
    }

    override fun cancelVote(author: String, permlink: String): GolosDiscussionItem {
        mGolosApi.simplifiedOperations.cancelVote(AccountName(author), Permlink(permlink))
        return getStoryWithoutComments(author, permlink, 10_000).rootStory
    }

    private fun getRootStoryWithoutComments(author: String, permlink: String): GolosDiscussionItem {
        return DiscussionItemFactory.create(mGolosApi.databaseMethods.getContent(AccountName(author), Permlink(permlink))!!)
    }

    override fun vote(author: String, permlink: String, percents: Short): GolosDiscussionItem {
        mGolosApi.simplifiedOperations.vote(AccountName(author), Permlink(permlink), percents)
        return getStoryWithoutComments(author, permlink, 10_000).rootStory
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
                Array(1) { categoryName })
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
                Array(1) { categoryName })
        return CreatePostResult(false, result.author.name, result.getTags().first(), result.permlink.link)
    }

    override fun getGolosUserSubscriptions(forUser: String, startFrom: String?): List<String> {
        return getSubscribesOrSubscribers(false, forUser, startFrom).mapNotNull { it.following.name }
    }

    override fun getGolosUserSubscribers(forUser: String, startFrom: String?): List<String> {
        return getSubscribesOrSubscribers(true, forUser, startFrom).mapNotNull { it.follower.name }
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
            val out = frs.asSequence().filter {
                it != null
                        && it.follower?.name != null
                        && it.follower.name.isNotEmpty()
                        && it.following?.name != null
                        && it.following.name.isNotEmpty()
            }
                    .distinct().toList()
            return out
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
            return frs
                    .asSequence()
                    .filterNotNull()
                    .filter {
                        it.follower?.name != null
                                && it.follower.name.isNotEmpty()
                                && it.following?.name != null
                                && it.following.name.isNotEmpty()
                    }.distinct().toList()
        }
    }

    override fun subscribe(onUser: String) {
        Golos4J.getInstance().simplifiedOperations.follow(AccountName(onUser))
    }

    override fun unSubscribe(fromUser: String) {
        Golos4J.getInstance().simplifiedOperations.unfollow(AccountName(fromUser))
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

    override fun lookUpUsers(nick: String): List<String> {
        return mGolosApi.databaseMethods.lookupAccounts(nick, 10)
    }
}

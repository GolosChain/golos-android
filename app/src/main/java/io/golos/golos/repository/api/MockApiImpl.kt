package io.golos.golos.repository.api

import eu.bittrade.libs.steemj.Golos4J
import eu.bittrade.libs.steemj.apis.follow.model.FollowApiObject
import eu.bittrade.libs.steemj.base.models.AccountName
import eu.bittrade.libs.steemj.base.models.PublicKey
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.repository.model.CreatePostResult
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.repository.model.Tag
import io.golos.golos.repository.model.UserAuthResponse
import io.golos.golos.repository.persistence.model.AccountInfo
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryWithComments
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.utils.UpdatingState
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Created by yuri on 20.11.17.
 */
internal class MockApiImpl : GolosApi() {
    private val originalService = ApiImpl()
    override fun getUserAvatar(username: String, permlink: String?, blog: String?): String? {
        Thread.sleep(50)
        return "https://s20.postimg.org/6bfyz1wjh/VFcp_Mpi_DLUIk.jpg"
    }

    override fun getUserFeed(userName: String, type: FeedType, limit: Int, truncateBody: Int, startAuthor: String?, startPermlink: String?): List<StoryWithComments> {
        return Golos4J.getInstance().databaseMethods
                .getUserFeed(AccountName("cepera"))
                .map {
                    StoryWithComments(StoryWrapper(GolosDiscussionItem(it, null), UpdatingState.DONE), ArrayList())
                }
    }

    override fun getStory(blog: String, author: String, permlink: String, h: (List<AccountInfo>) -> Unit): StoryWithComments {
        /* val mapper = CommunicationHandler.getObjectMapper()
         val context = App.context
         val ins = context.resources.openRawResource(context.resources.getIdentifier("story",
                 "raw", context.packageName))
         val wrapperDTO = mapper.readValue<ResponseWrapperDTO<*>>(ins, ResponseWrapperDTO::class.java)
         val type = mapper.typeFactory.constructCollectionType(List::class.java, DiscussionWithComments::class.java)
         val stoeryes = mapper.convertValue<List<DiscussionWithComments>>(wrapperDTO.result, type)
         return StoryTree(stoeryes[0])*/
        //https://golos.io/ru--novyijgod/@optimist/zachem-nam-nuzhen-novyi-god
        return originalService.getStory("ru--novyijgod", "optimist", "zachem-nam-nuzhen-novyi-god")
    }

    fun authWithMasterKey(userName: String, masterKey: String): UserAuthResponse {
        val response = Golos4J.getInstance().databaseMethods.getAccounts(listOf(AccountName("cepera")))
        val acc = response[0]
        val out = UserAuthResponse(true,
                Pair((acc.posting.keyAuths.keys.toTypedArray()[0] as PublicKey).addressFromPublicKey, "posting-key-stub"),
                Pair((acc.active.keyAuths.keys.toTypedArray()[0] as PublicKey).addressFromPublicKey, "active-key-stub"),
                accountInfo = AccountInfo("cepera"))
        return out
    }

    override fun getStories(limit: Int, type: FeedType, truncateBody: Int, filter: StoryFilter?, startAuthor: String?, startPermlink: String?): List<StoryWithComments> {
       /* val mapper = CommunicationHandler.getObjectMapper()
        val context = App.context
        val ins = context.resources.openRawResource(context.resources.getIdentifier("stripe",
                "raw", context.packageName))
        val wrapperDTO = mapper.readValue<ResponseWrapperDTO<*>>(ins, ResponseWrapperDTO::class.java)
        val type = mapper.typeFactory.constructCollectionType(List::class.java, Discussion::class.java)
        val stories = mapper.convertValue<List<Discussion>>(wrapperDTO.result, type)

        val out = ArrayList<StoryTree>()
        stories.forEach {
            val story = StoryTree(StoryWrapper(GolosDiscussionItem(it, null), UpdatingState.DONE), ArrayList())
            out.add(story)
        }*/
        //https://golos.io/ru--novyijgod/@optimist/zachem-nam-nuzhen-novyi-god
        return originalService.getStories(20,
                FeedType.ACTUAL,
                1024,
                StoryFilter(tagFilter = "ru--novyijgod", userNameFilter = "optimist"),
                null,
                "zachem-nam-nuzhen-novyi-god")
    }

    override fun auth(userName: String, masterKey: String?, activeWif: String?, postingWif: String?): UserAuthResponse {
        return authWithMasterKey(userName, masterKey ?: "")
    }

    override fun getAccountData(of: String): AccountInfo {
        return originalService.getAccountData(of)
    }

    override fun cancelVote(author: String, permlink: String): GolosDiscussionItem {
        return originalService.cancelVote(author, permlink)
    }

    override fun upVote(author: String, permlink: String, percents: Short): GolosDiscussionItem {
        return originalService.upVote(author, permlink, percents)
    }

    override fun uploadImage(sendFromAccount: String, file: File): String {
        Thread.sleep(500)
        return "mock_image_url_${UUID.randomUUID()}"
    }

    override fun sendPost(sendFromAccount: String, title: String, content: String, tags: Array<String>): CreatePostResult {
        return originalService.sendPost(sendFromAccount, title, content, tags)
    }

    override fun sendComment(sendFromAccount: String, authorOfItemToReply: String, permlinkOfItemToReply: String, content: String, categoryName: String): CreatePostResult {
        return originalService.sendComment(sendFromAccount, authorOfItemToReply, content, permlinkOfItemToReply, categoryName)
    }

    override fun getStoryWithoutComments(author: String, permlink: String): StoryWithComments {
        return originalService.getStoryWithoutComments(author, permlink)
    }

    override fun getUserAvatars(names: List<String>): Map<String, String?> {
        return HashMap<String, String>()
    }

    override fun getSubscriptions(forUser: String, startFrom: String?): List<FollowApiObject> {
        return ArrayList()
    }

    override fun follow(user: String) {

    }

    override fun unfollow(user: String) {

    }

    override fun getSubscribers(forUser: String, startFrom: String?): List<FollowApiObject> {
        return ArrayList()
    }

    override fun getTrendingTag(startFrom: String, maxCount: Int): List<Tag> {
        return ArrayList()
    }
}
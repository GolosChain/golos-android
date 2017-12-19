package io.golos.golos.repository.api

import eu.bittrade.libs.steemj.Golos4J
import eu.bittrade.libs.steemj.base.models.AccountName
import eu.bittrade.libs.steemj.base.models.Discussion
import eu.bittrade.libs.steemj.base.models.DiscussionWithComments
import eu.bittrade.libs.steemj.base.models.PublicKey
import eu.bittrade.libs.steemj.communication.CommunicationHandler
import eu.bittrade.libs.steemj.communication.dto.ResponseWrapperDTO
import io.golos.golos.App
import io.golos.golos.repository.StoryFilter
import io.golos.golos.repository.model.CreatePostResult
import io.golos.golos.repository.model.UserAuthResponse
import io.golos.golos.repository.persistence.model.AccountInfo
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.utils.UpdatingState
import io.golos.golos.utils.avatarPath
import java.io.File
import java.util.*

/**
 * Created by yuri on 20.11.17.
 */
internal class MockApiImpl : GolosApi() {
    override fun getUserAvatar(username: String, permlink: String?, blog: String?): String? {
        Thread.sleep(50)
        return "https://s20.postimg.org/6bfyz1wjh/VFcp_Mpi_DLUIk.jpg"
    }

    override fun getUserFeed(userName: String, type: FeedType, limit: Int, truncateBody: Int, startAuthor: String?, startPermlink: String?): List<StoryTree> {
        return Golos4J.getInstance().databaseMethods
                .getUserFeed(AccountName("cepera"))
                .map {
                    StoryTree(StoryWrapper(GolosDiscussionItem(it, null), UpdatingState.DONE), ArrayList())
                }
    }

    override fun getStory(blog: String, author: String, permlink: String): StoryTree {
        val mapper = CommunicationHandler.getObjectMapper()
        val context = App.context
        val ins = context.resources.openRawResource(context.resources.getIdentifier("story",
                "raw", context.packageName))
        val wrapperDTO = mapper.readValue<ResponseWrapperDTO<*>>(ins, ResponseWrapperDTO::class.java)
        val type = mapper.typeFactory.constructCollectionType(List::class.java, DiscussionWithComments::class.java)
        val stoeryes = mapper.convertValue<List<DiscussionWithComments>>(wrapperDTO.result, type)
        return StoryTree(stoeryes[0])
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

    override fun getStories(limit: Int, type: FeedType, truncateBody: Int, filter: StoryFilter?, startAuthor: String?, startPermlink: String?): List<StoryTree> {
        val mapper = CommunicationHandler.getObjectMapper()
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
        }
        return out
    }

    override fun auth(userName: String, masterKey: String?, activeWif: String?, postingWif: String?): UserAuthResponse {
        return authWithMasterKey(userName, masterKey ?: "")
    }

    override fun getAccountData(of: String): AccountInfo {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun cancelVote(author: String, permlink: String): GolosDiscussionItem {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun upVote(author: String, permlink: String, percents: Short): GolosDiscussionItem {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun uploadImage(sendFromAccount: String, file: File): String {
       Thread.sleep(500)
        return "mock_image_url_${UUID.randomUUID()}"
    }

    override fun sendPost(sendFromAccount: String, title: String, content: String, tags: Array<String>): CreatePostResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sendComment(sendFromAccount: String, authorOfItemToReply: String, permlinkOfItemToReply: String, content: String, categoryName: String): CreatePostResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getStoryWithoutComments(author: String, permlink: String): StoryTree {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUserAvatars(names: List<String>): Map<String, String?> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
package io.golos.golos.repository.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import eu.bittrade.libs.golosj.base.models.VoteLight
import io.golos.golos.screens.story.model.Row

@JsonIgnoreProperties(ignoreUnknown = true)
data class GolosDiscussionItem constructor(val url: String,
                                           val id: Long,
                                           val title: String,
                                           val categoryName: String,
                                           val tags: List<String>,
                                           val images: MutableList<String>,
                                           val links: List<String>,
                                           val votesNum: Int,
                                           val upvotesNum: Int,
                                           val downvotesNum: Int,
                                           val votesRshares: Long,
                                           val netRshares: Long,
                                           val commentsCount: Int,
                                           val permlink: String,
                                           val gbgAmount: Double,
                                           val pendingPayoutValue: Double,
                                           val totalPayoutValue: Double,
                                           val curatorPayoutValue: Double,
                                           val body: String,
                                           val bodyLength: Long,
                                           val author: String,
                                           var rebloggedBy: String?,
                                           val format: Format = Format.HTML,
                                           val children: MutableList<GolosDiscussionItem> = ArrayList(),
                                           val parentPermlink: String,
                                           val parentAuthor: String,
                                           val childrenCount: Int,
                                           var level: Int = 0,
                                           val reputation: Long,
                                           val lastUpdated: Long,
                                           val created: Long,
                                           val activeVotes: MutableList<VoteLight>,
                                           var type: ItemType = ItemType.PLAIN,
                                           var cleanedFromImages: String,
                                           val parts: MutableList<Row> = ArrayList()) : Cloneable {

    companion object {
        val emptyItem = GolosDiscussionItem("", 0L, "", "", emptyList(), arrayListOf(), listOf(),
                0, 0, 0, 0L, 0L, 0, "", 0.0, 0.0, 0.0, 0.0, "", 0L, "", parentPermlink = "",
                activeVotes = arrayListOf(), childrenCount = 0, created = 0L, lastUpdated = 0L, parentAuthor = "", reputation = 0, cleanedFromImages = "", rebloggedBy = "")
    }


    val isRootStory: Boolean
        get() = parentAuthor.isEmpty()


    public fun isUserVotedOnThis(userName: String): UserVoteType {
        val item = activeVotes.find { it.name == userName }
        return if (item != null && item.percent > 0) UserVoteType.VOTED
        else if (item != null && item.percent < 0) UserVoteType.FLAGED_DOWNVOTED
        else UserVoteType.NOT_VOTED_OR_ZERO_WEIGHT
    }

    override fun toString(): String {
        return "GolosDiscussionItem(url='$url', id=$id, title='$title', categoryName='$categoryName', " +
                "images=$images, links=$links, votesNum=$votesNum, gbgAmount=$gbgAmount," +
                " pendingPayoutValue=$pendingPayoutValue, totalPayoutValue=$totalPayoutValue," +
                " curatorPayoutValue=$curatorPayoutValue, author='$author'), " +
                "netRshares = $netRshares"
    }

    enum class Format {
        HTML, MARKDOWN
    }

    enum class ItemType {
        PLAIN, PLAIN_WITH_IMAGE, IMAGE_FIRST
    }

    enum class UserVoteType {
        VOTED, NOT_VOTED_OR_ZERO_WEIGHT, FLAGED_DOWNVOTED
    }
}




package io.golos.golos.repository.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import eu.bittrade.libs.golosj.base.models.VoteLight
import io.golos.golos.screens.story.model.Row
import io.golos.golos.screens.story.model.StoryWrapper

@JsonIgnoreProperties(ignoreUnknown = true)
data class GolosDiscussionItem internal constructor(val url: String,
                                                    val id: Long,
                                                    val title: String,
                                                    val categoryName: String,
                                                    val tags: List<String>,
                                                    val images: MutableList<String>,
                                                    val links: List<String>,
                                                    val votesNum: Int,
                                                    val votesRshares: Long,
                                                    val commentsCount: Int,
                                                    val permlink: String,
                                                    val gbgAmount: Double,
                                                    val body: String,
                                                    val bodyLength: Long,
                                                    val author: String,
                                                    val format: Format = Format.HTML,
                                                    var avatarPath: String? = null,
                                                    val children: MutableList<StoryWrapper> = ArrayList(),
                                                    val parentPermlink: String,
                                                    val parentAuthor: String,
                                                    val childrenCount: Int,
                                                    var level: Int = 0,
                                                    val reputation: Long,
                                                    val lastUpdated: Long,
                                                    val created: Long,
                                                    var userVotestatus: UserVoteType,
                                                    val activeVotes: MutableList<VoteLight>,
                                                    var type: ItemType = ItemType.PLAIN,
                                                    val firstRebloggedBy: String,
                                                    var cleanedFromImages: String,
                                                    val parts: MutableList<Row> = ArrayList()) : Cloneable {


    val isRootStory: Boolean
        get() = tags.contains(parentPermlink)


    override fun toString(): String {
        return "Comment(id=$id, title='$title', permlink='$permlink')"
    }


    fun isUserVotedOnThis(userName: String): UserVoteType {
        val item = activeVotes.find { it.name == userName }
        return if (item != null && item.percent > 0) UserVoteType.VOTED
        else if (item != null && item.percent < 0) UserVoteType.FLAGED_DOWNVOTED
        else UserVoteType.NOT_VOTED_OR_ZERO_WEIGHT
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosDiscussionItem) return false

        if (url != other.url) return false
        if (id != other.id) return false
        if (title != other.title) return false
        if (categoryName != other.categoryName) return false
        if (votesNum != other.votesNum) return false
        if (commentsCount != other.commentsCount) return false
        if (permlink != other.permlink) return false
        if (gbgAmount != other.gbgAmount) return false
        if (body != other.body) return false
        if (author != other.author) return false
        if (format != other.format) return false
        if (avatarPath != other.avatarPath) return false
        if (parentPermlink != other.parentPermlink) return false
        if (childrenCount != other.childrenCount) return false
        if (level != other.level) return false
        if (reputation != other.reputation) return false
        if (lastUpdated != other.lastUpdated) return false
        if (created != other.created) return false
        if (userVotestatus != other.userVotestatus) return false
        if (images != other.images) return false

        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + categoryName.hashCode()
        result = 31 * result + votesNum
        result = 31 * result + commentsCount
        result = 31 * result + permlink.hashCode()
        result = 31 * result + gbgAmount.hashCode()
        result = 31 * result + body.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + (avatarPath?.hashCode() ?: 0)
        result = 31 * result + parentPermlink.hashCode()
        result = 31 * result + childrenCount
        result = 31 * result + level
        result = 31 * result + reputation.hashCode()
        result = 31 * result + lastUpdated.hashCode()
        result = 31 * result + created.hashCode()
        result = 31 * result + userVotestatus.hashCode()
        result = 31 * result + images.hashCode()
        return result
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




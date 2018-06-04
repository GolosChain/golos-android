package io.golos.golos.repository.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import eu.bittrade.libs.steemj.base.models.Discussion
import eu.bittrade.libs.steemj.base.models.ExtendedAccount
import eu.bittrade.libs.steemj.base.models.VoteLight
import io.golos.golos.screens.story.model.*
import io.golos.golos.utils.Regexps
import io.golos.golos.utils.mapper
import io.golos.golos.utils.toArrayList
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

@JsonIgnoreProperties(ignoreUnknown = true)
data class GolosDiscussionItem internal constructor(val url: String,
                                                    val id: Long,
                                                    val title: String,
                                                    val categoryName: String,
                                                    val tags: MutableList<String> = ArrayList(),
                                                    val images: MutableList<String> = ArrayList(),
                                                    val links: MutableList<String> = ArrayList(),
                                                    val votesNum: Int,
                                                    val votesRshares: Long,
                                                    val commentsCount: Int,
                                                    val permlink: String,
                                                    var gbgAmount: Double,
                                                    var body: String,
                                                    var bodyLength: Long,
                                                    val author: String,
                                                    var format: Format = Format.HTML,
                                                    var avatarPath: String? = null,
                                                    var children: MutableList<StoryWrapper> = ArrayList(),
                                                    var parentPermlink: String,
                                                    var parentAuthor: String,
                                                    var childrenCount: Int,
                                                    var level: Int = 0,
                                                    val reputation: Long,
                                                    val lastUpdated: Long,
                                                    val created: Long,
                                                    var userVotestatus: UserVoteType = UserVoteType.NOT_VOTED_OR_ZERO_WEIGHT,
                                                    val activeVotes: MutableList<VoteLight> = arrayListOf(),
                                                    var type: ItemType = ItemType.PLAIN,
                                                    val firstRebloggedBy: String,
                                                    var cleanedFromImages: String,
                                                    var parts: MutableList<Row> = ArrayList()) : Cloneable {


    val isRootStory: Boolean
        get() = tags.contains(parentPermlink)


    constructor(discussion: Discussion, account: ExtendedAccount?) : this(
            url = discussion.url ?: "",
            id = discussion.id,
            title = discussion.title ?: "",
            categoryName = discussion.category ?: "",
            votesNum = discussion.netVotes,
            votesRshares = discussion.voteRshares,
            commentsCount = discussion.children,
            permlink = discussion.permlink?.link ?: "",
            gbgAmount = discussion.pendingPayoutValue?.amount ?: 0.0,
            body = discussion.body ?: "",
            bodyLength = discussion.bodyLength.toLongOrNull() ?: 0L,
            author = discussion.author?.name ?: "",
            parentPermlink = discussion.parentPermlink.link ?: "",
            parentAuthor = discussion.parentAuthor.name ?: "",
            childrenCount = discussion.children,
            reputation = discussion.authorReputation,
            lastUpdated = discussion.lastUpdate?.dateTimeAsTimestamp ?: 0,
            created = discussion.created?.dateTimeAsTimestamp ?: 0,
            firstRebloggedBy = discussion.firstRebloggedBy?.name ?: "",
            cleanedFromImages = "") {


        val tagsString = discussion.jsonMetadata

        activeVotes.addAll(discussion.activeVotes.map { VoteLight(it.voter.name, it.rshares.toLong(), it.percent / 100) })

        var json: JSONObject? = null
        try {
            json = JSONObject(tagsString)
            if (json.has("tags")) {
                val tagsArray = json.getJSONArray("tags")
                if (tagsArray.length() > 0) {
                    (0 until tagsArray.length()).mapTo(tags) { tagsArray[it].toString() }
                }
            }
        } catch (e: JSONException) {
            Timber.e(tagsString)
            e.printStackTrace()
        }
        if (json != null) {
            try {
                if (json.has("format")) {
                    val format = json.getString("format")
                    this.format = if (format.equals("markdown", true)) Format.MARKDOWN else Format.HTML
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        if (json != null) {
            try {
                if (json.has("image")) {
                    val imageArray = json.getJSONArray("image")
                    if (imageArray.length() > 0) {
                        (0 until imageArray.length()).mapTo(images) { imageArray[it].toString() }
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        if (json != null) {
            try {
                if (json.has("links")) {
                    val linksArray = json.getJSONArray("links")
                    if (linksArray.length() > 0) {
                        (0 until linksArray.length()).mapTo(links) { linksArray[it].toString() }
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        body = discussion.body ?: ""
        val toRowsParser = StoryParserToRows
        parts = toRowsParser.parse(this).toArrayList()
        if (parts.size == 0) {
            Timber.e("fail on story id is ${id}\n body =  ${body}")
        } else {
            if (parts[0] is ImageRow) {
                type = ItemType.IMAGE_FIRST
            } else {
                if (images.size != 0) type = ItemType.PLAIN_WITH_IMAGE
            }
        }
        cleanedFromImages = if (parts.size == 0)
            body.replace(Regexps.markdownImageRegexp, "")
                    .replace(Regexps.anyImageLink, "")
                    .replace(Regex("(\\*)"), "*")
        else {
            parts.joinToString("\n") {
                if (it is TextRow) it.text.replace(Regexps.markdownImageRegexp, "")
                        .replace(Regex("(\\*)"), "*")
                else ""
            }
        }
        if (account != null) {
            try {
                val node: JsonNode? = mapper.readTree(account.jsonMetadata)
                avatarPath = node?.get("profile")?.get("profile_image")?.asText()
            } catch (e: Exception) {
                Timber.e("error parsing string ${account.jsonMetadata}")
                e.printStackTrace()
            }
        }

    }

    override public fun clone(): Any {
        return super.clone()
    }

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




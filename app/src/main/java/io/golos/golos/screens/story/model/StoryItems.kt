package io.golos.golos.screens.story.model

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.bittrade.libs.steemj.base.models.Discussion
import eu.bittrade.libs.steemj.base.models.ExtendedAccount
import io.golos.golos.utils.Regexps
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

/**
 * Created by yuri on 06.11.17.
 */
val mapper by lazy {
    val mapper = jacksonObjectMapper()
    mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
    mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
    mapper
}

class RootStory : Comment {
    constructor(url: String, id: Long, title: String, categoryName: String, tags: ArrayList<String>, images: ArrayList<String>, links: ArrayList<String>, votesNum: Int, commentsCount: Int, permlink: String, gbgAmount: Double, body: String, author: String, format: Format, avatarPath: String?, children: List<Comment>, parentPermlink: String, childrenCount: Int, level: Int, gbgCostInDollars: Double, reputation: Long, lastUpdated: Long, created: Long, isUserUpvotedOnThis: Boolean, activeVotes: ArrayList<Pair<String, Int>>, type: ItemType, firstRebloggedBy: String) : super(url, id, title, categoryName, tags, images, links, votesNum, commentsCount, permlink, gbgAmount, body, author, format, avatarPath, children, parentPermlink, childrenCount, level, gbgCostInDollars, reputation, lastUpdated, created, isUserUpvotedOnThis, activeVotes, type, firstRebloggedBy)
    constructor(discussion: Discussion, account: ExtendedAccount?) : super(discussion, account)
    constructor(comment: Comment) : super(comment.url, comment.id, comment.title, comment.categoryName, comment.tags, comment.images,
            comment.links, comment.votesNum, comment.commentsCount, comment.permlink, comment.gbgAmount, comment.body, comment.author,
            comment.format, comment.avatarPath, comment.children, comment.parentPermlink, comment.childrenCount, 0, comment.gbgCostInDollars
            , comment.reputation, comment.lastUpdated, comment.created, comment.isUserUpvotedOnThis, comment.activeVotes,
            comment.type, comment.firstRebloggedBy)

    override public fun clone(): Any {
        return super.clone()
    }


    fun cleanedFromImages(): String {
        val toRowsParser = StoryParserToRows()
        val out = toRowsParser.parse(this)
        if (out.size == 0)
            return body.replace(Regexps.markdownImageRegexp, "")
                    .replace(Regexps.anyImageLink, "")
                    .replace(Regex("(\\*)"), "*")
        else {
            return out.joinToString("\n") {
                if (it is TextRow) it.text.replace(Regexps.markdownImageRegexp, "")
                        .replace(Regexps.anyImageLink, "")
                        .replace(Regex("(\\*)"), "*")
                else ""
            }
        }
    }


    override fun toString(): String {
        return "RootStory(id=$id, title='$title'\n" +
                "avatar = $avatarPath)"
    }


}

open class Comment(val url: String,
                   val id: Long,
                   val title: String,
                   val categoryName: String,
                   val tags: ArrayList<String> = ArrayList(),
                   val images: ArrayList<String> = ArrayList(),
                   val links: ArrayList<String> = ArrayList(),
                   val votesNum: Int,
                   val commentsCount: Int,
                   val permlink: String,
                   val gbgAmount: Double,
                   var body: String,
                   val author: String,
                   var format: Format = Format.HTML,
                   var avatarPath: String? = null,
                   var children: List<Comment> = ArrayList<Comment>(),
                   var parentPermlink: String,
                   var childrenCount: Int,
                   var level: Int = 0,
                   var gbgCostInDollars: Double = 0.04106528,
                   val reputation: Long,
                   val lastUpdated: Long,
                   val created: Long,
                   var isUserUpvotedOnThis: Boolean = false,
                   val activeVotes: ArrayList<Pair<String, Int>> = ArrayList(),
                   var type: ItemType = ItemType.PLAIN,
                   val firstRebloggedBy: String) : Cloneable {

    val payoutInDollars: Double
        get() = gbgAmount * gbgCostInDollars


    constructor(discussion: Discussion, account: ExtendedAccount?) : this(
            url = discussion.url ?: "",
            id = discussion.id,
            title = discussion.title ?: "",
            categoryName = discussion.category ?: "",
            votesNum = discussion.netVotes,
            commentsCount = discussion.children,
            permlink = discussion.permlink?.link ?: "",
            childrenCount = discussion.children,
            reputation = discussion.authorReputation,
            lastUpdated = discussion.lastUpdate?.dateTimeAsTimestamp ?: 0,
            created = discussion.created?.dateTimeAsTimestamp ?: 0,
            parentPermlink = discussion.parentPermlink.link ?: "",
            firstRebloggedBy = discussion.firstRebloggedBy?.name ?: "",
            gbgAmount = discussion.pendingPayoutValue?.amount ?: 0.0,
            body = discussion.body ?: "",
            author = discussion.author?.name ?: "") {

        val tagsString = discussion.jsonMetadata
        discussion.activeVotes?.forEach {
            activeVotes.add(Pair(it.voter.name, it.percent / 100))
        }
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
        if (account != null) {
            try {
                val node: JsonNode? = mapper.readTree(account?.jsonMetadata)
                avatarPath = node?.get("profile")?.get("profile_image")?.asText()
            } catch (e: Exception) {
                Timber.e("error parsing string ${account?.jsonMetadata}")
                e.printStackTrace()
            }
        }
        val toRowsParser = StoryParserToRows()
        val out = toRowsParser.parse(this)
        if (out.size == 0) {
            Timber.e("fail on story id is ${id}\n body =  ${body}")
        } else {
            if (out[0] is ImageRow) {
                type = ItemType.IMAGE_FIRST
            } else {
                if (images.size != 0) type = ItemType.PLAIN_WITH_IMAGE
            }
        }
    }

    override public fun clone(): Any {
        return super.clone()
    }

    override fun toString(): String {
        return "Comment(id=$id, title='$title', permlink='$permlink')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Comment) return false

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
        if (gbgCostInDollars != other.gbgCostInDollars) return false
        if (reputation != other.reputation) return false
        if (lastUpdated != other.lastUpdated) return false
        if (created != other.created) return false
        if (isUserUpvotedOnThis != other.isUserUpvotedOnThis) return false

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
        result = 31 * result + gbgCostInDollars.hashCode()
        result = 31 * result + reputation.hashCode()
        result = 31 * result + lastUpdated.hashCode()
        result = 31 * result + created.hashCode()
        result = 31 * result + isUserUpvotedOnThis.hashCode()
        return result
    }


}

enum class Format {
    HTML, MARKDOWN
}

enum class ItemType {
    PLAIN, PLAIN_WITH_IMAGE, IMAGE_FIRST
}

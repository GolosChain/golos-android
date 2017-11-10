package io.golos.golos.screens.story.model

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.bittrade.libs.steemj.base.models.Discussion
import eu.bittrade.libs.steemj.base.models.ExtendedAccount
import io.golos.golos.screens.main_stripes.model.Format
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

class RootStory(discussion: Discussion, account: ExtendedAccount) : Comment(discussion, account) {
    val firstRebloggedBy: String

    init {
        firstRebloggedBy = discussion.firstRebloggedBy?.name ?: ""
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RootStory

        if (url != other.url) return false
        if (id != other.id) return false
        if (title != other.title) return false
        if (payoutInDollats != other.payoutInDollats) return false
        if (categoryName != other.categoryName) return false
        if (tags != other.tags) return false
        if (images != other.images) return false
        if (links != other.links) return false
        if (votesNum != other.votesNum) return false
        if (commentsCount != other.commentsCount) return false
        if (permlink != other.permlink) return false
        if (gbgAmount != other.gbgAmount) return false
        if (body != other.body) return false
        if (author != other.author) return false
        if (firstRebloggedBy != other.firstRebloggedBy) return false
        if (format != other.format) return false
        if (avatarPath != other.avatarPath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + payoutInDollats.hashCode()
        result = 31 * result + categoryName.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + images.hashCode()
        result = 31 * result + links.hashCode()
        result = 31 * result + votesNum
        result = 31 * result + commentsCount
        result = 31 * result + permlink.hashCode()
        result = 31 * result + gbgAmount.hashCode()
        result = 31 * result + body.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + firstRebloggedBy.hashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + (avatarPath?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "RootStory(id=$id, title='$title')"
    }


}

open class Comment(discussion: Discussion, account: ExtendedAccount) {
    val url: String
    val id: Long
    val title: String
    val payoutInDollats: String
    val categoryName: String
    val tags: ArrayList<String> = ArrayList()
    val images = ArrayList<String>()
    val links = ArrayList<String>()
    val votesNum: Int
    val commentsCount: Int
    val permlink: String
    val gbgAmount: Double
    val body: String
    val author: String
    var format: Format = Format.HTML
    var avatarPath: String? = null
    var children = ArrayList<Comment>()
    var parentPermlink: String
    var childrenCount: Int
    var level = 0
    var gbgCostInDollars = 0.04106528
    val reputation: Long
    val lastUpdated: Long
    val created: Long
    val payoutValueInDollars: Double
        get() = gbgAmount * gbgCostInDollars


    init {
        id = discussion.id
        url = discussion.url ?: ""
        title = discussion.title ?: ""
        categoryName = discussion.category ?: ""
        votesNum = discussion.netVotes
        commentsCount = discussion.children
        permlink = discussion.permlink?.link ?: ""
        childrenCount = discussion.children
        val tagsString = discussion.jsonMetadata
        reputation = discussion.authorReputation
        lastUpdated = discussion.lastUpdate?.dateTimeAsTimestamp ?: 0
        created = discussion.created?.dateTimeAsTimestamp ?: 0
        parentPermlink = discussion.parentPermlink.link ?: ""
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
        gbgAmount = discussion.pendingPayoutValue?.amount ?: 0.0
        payoutInDollats = String.format("$%.2f", (gbgAmount * 0.04106528))
        body = discussion.body ?: ""
        author = discussion.author?.name ?: ""
        try {
            val node: JsonNode? = mapper.readTree(account.jsonMetadata)
            avatarPath = node?.get("profile")?.get("profile_image")?.asText()
        } catch (e: Exception) {
            Timber.e("error parsing string ${account.jsonMetadata}")
            e.printStackTrace()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Comment

        if (url != other.url) return false
        if (id != other.id) return false
        if (title != other.title) return false
        if (payoutInDollats != other.payoutInDollats) return false
        if (categoryName != other.categoryName) return false
        if (tags != other.tags) return false
        if (images != other.images) return false
        if (links != other.links) return false
        if (votesNum != other.votesNum) return false
        if (commentsCount != other.commentsCount) return false
        if (permlink != other.permlink) return false
        if (gbgAmount != other.gbgAmount) return false
        if (body != other.body) return false
        if (author != other.author) return false
        if (format != other.format) return false
        if (avatarPath != other.avatarPath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + payoutInDollats.hashCode()
        result = 31 * result + categoryName.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + images.hashCode()
        result = 31 * result + links.hashCode()
        result = 31 * result + votesNum
        result = 31 * result + commentsCount
        result = 31 * result + permlink.hashCode()
        result = 31 * result + gbgAmount.hashCode()
        result = 31 * result + body.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + (avatarPath?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Comment(id=$id, title='$title', permlink='$permlink')"
    }
}


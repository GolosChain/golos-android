package io.golos.golos.repository.model

import com.fasterxml.jackson.databind.JsonNode
import eu.bittrade.libs.steemj.base.models.Discussion
import eu.bittrade.libs.steemj.base.models.DiscussionLight
import eu.bittrade.libs.steemj.base.models.ExtendedAccount
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.story.model.StoryParserToRows
import io.golos.golos.screens.story.model.TextRow
import io.golos.golos.utils.Regexps
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

/**
 * Created by yuri on 27.11.17.
 */
object DiscussionItemFactory {
    fun create(discussion: Discussion, account: ExtendedAccount?): GolosDiscussionItem {
        val url = discussion.url ?: ""
        val id = discussion.id
        val title = discussion.title ?: ""
        val categoryName = discussion.category ?: ""
        val votesNum = discussion.netVotes
        val commentsCount = discussion.children
        val permlink = discussion.permlink?.link ?: ""
        val childrenCount = discussion.children
        val reputation = discussion.authorReputation
        val lastUpdated = discussion.lastUpdate?.dateTimeAsTimestamp ?: 0
        val created = discussion.created?.dateTimeAsTimestamp ?: 0
        val parentPermlink = discussion.parentPermlink.link ?: ""
        val firstRebloggedBy = discussion.firstRebloggedBy?.name ?: ""
        val gbgAmount = discussion.pendingPayoutValue?.amount ?: 0.0
        val body = discussion.body ?: ""
        val author = discussion.author?.name ?: ""
        val cleanedFromImages = ""

        val item = GolosDiscussionItem(url, id, title, categoryName, votesNum = votesNum,
                commentsCount = commentsCount, permlink = permlink, childrenCount = childrenCount, reputation = reputation,
                lastUpdated = lastUpdated, created = created, parentPermlink = parentPermlink, firstRebloggedBy = firstRebloggedBy,
                gbgAmount = gbgAmount, body = body, author = author, cleanedFromImages = cleanedFromImages)
        setDataFromTagsString(discussion.jsonMetadata, item)
        discussion.activeVotes?.forEach {
            item.activeVotes.put(it.voter.name, it.percent / 100)
        }
        setTypeOfItem(item)
        account?.let { setAvatar(item, it) }
        return item
    }

    fun create(discussion: DiscussionLight, account: ExtendedAccount?): GolosDiscussionItem {
        val url = discussion.url ?: ""
        val id = discussion.id
        val title = discussion.title ?: ""
        val categoryName = discussion.category ?: ""
        val votesNum = discussion.netVotes
        val commentsCount = discussion.children
        val permlink = discussion.permlink ?: ""
        val childrenCount = discussion.children
        val reputation = discussion.authorReputation
        val lastUpdated = discussion.lastUpdate?.dateTimeAsTimestamp ?: 0
        val created = discussion.created?.dateTimeAsTimestamp ?: 0
        val parentPermlink = discussion.parentPermlink ?: ""
        val firstRebloggedBy = discussion.firstRebloggedBy ?: ""
        val gbgAmount = discussion.pendingPayoutValue?.amount ?: 0.0
        val body = discussion.body ?: ""
        val author = discussion.author ?: ""
        val cleanedFromImages = ""
        val item = GolosDiscussionItem(url, id, title, categoryName, votesNum = votesNum,
                commentsCount = commentsCount, permlink = permlink, childrenCount = childrenCount, reputation = reputation,
                lastUpdated = lastUpdated, created = created, parentPermlink = parentPermlink, firstRebloggedBy = firstRebloggedBy,
                gbgAmount = gbgAmount, body = body, author = author, cleanedFromImages = cleanedFromImages)

        setDataFromTagsString(discussion.jsonMetadata, item)
        item.activeVotes.putAll(discussion.votes)
        setTypeOfItem(item)
        account?.let { setAvatar(item, it) }
        return item
    }

    private fun setDataFromTagsString(tags: String, to: GolosDiscussionItem) {
        var json: JSONObject? = null
        try {
            json = JSONObject(tags)
            if (json.has("tags")) {
                val tagsArray = json.getJSONArray("tags")
                if (tagsArray.length() > 0) {
                    (0 until tagsArray.length())
                            .mapTo(to.tags)
                            { tagsArray[it].toString() }
                }
            }
        } catch (e: JSONException) {
            Timber.e(tags)
            e.printStackTrace()
        }
        if (json != null) {
            try {
                if (json.has("format")) {
                    val format = json.getString("format")
                    to.format = if (format.equals("markdown", true)) Format.MARKDOWN else Format.HTML
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
                        (0 until imageArray.length()).mapTo(to.images) { imageArray[it].toString() }
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
                        (0 until linksArray.length()).mapTo(to.links) { linksArray[it].toString() }
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    private fun setTypeOfItem(golosDiscussionItem: GolosDiscussionItem) {
        val toRowsParser = StoryParserToRows()
        val out = toRowsParser.parse(golosDiscussionItem)
        if (out.size == 0) {
            Timber.e("fail on story id is ${golosDiscussionItem.id}\n body =  ${golosDiscussionItem.body}")
        } else {
            if (out[0] is ImageRow) {
                golosDiscussionItem.type = ItemType.IMAGE_FIRST
            } else {
                if (golosDiscussionItem.images.size != 0) golosDiscussionItem.type = ItemType.PLAIN_WITH_IMAGE
            }
        }
        golosDiscussionItem.cleanedFromImages = if (out.size == 0)
            golosDiscussionItem.body.replace(Regexps.markdownImageRegexp, "")
                    .replace(Regexps.anyImageLink, "")
                    .replace(Regex("(\\*)"), "*")
        else {
            out.joinToString("\n") {
                if (it is TextRow) it.text.replace(Regexps.markdownImageRegexp, "")
                        .replace(Regex("(\\*)"), "*")
                else ""
            }
        }
    }

    private fun setAvatar(golosDiscussionItem: GolosDiscussionItem, extendedAccount: ExtendedAccount) {
        try {
            val node: JsonNode? = mapper.readTree(extendedAccount.jsonMetadata)
            golosDiscussionItem.avatarPath = node?.get("profile")?.get("profile_image")?.asText()
        } catch (e: Exception) {
            Timber.e("error parsing string ${extendedAccount.jsonMetadata}")
            e.printStackTrace()
        }

    }

}
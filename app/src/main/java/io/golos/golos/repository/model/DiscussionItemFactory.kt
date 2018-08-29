package io.golos.golos.repository.model

import com.fasterxml.jackson.databind.JsonNode
import eu.bittrade.libs.golosj.base.models.Discussion
import eu.bittrade.libs.golosj.base.models.DiscussionLight
import eu.bittrade.libs.golosj.base.models.ExtendedAccount
import eu.bittrade.libs.golosj.base.models.VoteLight
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.story.model.StoryParserToRows
import io.golos.golos.screens.story.model.TextRow
import io.golos.golos.utils.Regexps
import io.golos.golos.utils.mapper
import io.golos.golos.utils.toArrayList
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

object DiscussionItemFactory {
    fun create(discussion: Discussion, account: ExtendedAccount?): GolosDiscussionItem {
        val metadata = getMetadataFromItem(discussion.jsonMetadata)

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
        val totalRshares = discussion.voteRshares

        val item = GolosDiscussionItem(url, id, title, categoryName, votesNum = votesNum,
                votesRshares = totalRshares,
                commentsCount = commentsCount, permlink = permlink, gbgAmount = gbgAmount, body = body,
                bodyLength = discussion.bodyLength.toLongOrNull()
                        ?: 0L, author = author, parentPermlink = parentPermlink,
                parentAuthor = discussion.parentAuthor.name ?: "", childrenCount = childrenCount,
                reputation = reputation, lastUpdated = lastUpdated, created = created,
                firstRebloggedBy = firstRebloggedBy, cleanedFromImages = cleanedFromImages)

        discussion.activeVotes?.forEach {
            item.activeVotes.add(VoteLight(it.voter.name, it.rshares.toLong(), it.percent / 100))
        }

        item.format = metadata.format
        item.links.addAll(metadata.links)
        item.images.addAll(metadata.images)
        item.tags.addAll(metadata.tags)

        setTypeOfItem(item)
        account?.let { setAvatar(item, it) }
        checkImages(item)
        return item
    }

    fun create(discussion: DiscussionLight, account: ExtendedAccount?): GolosDiscussionItem {
        val metadata = getMetadataFromItem(discussion.jsonMetadata)

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
        val totalRshares = discussion.voteRshares
        val item = GolosDiscussionItem(url, id, title, categoryName, votesNum = votesNum,
                votesRshares = totalRshares,
                commentsCount = commentsCount, permlink = permlink, gbgAmount = gbgAmount, body = body,
                bodyLength = discussion.bodyLength, author = author, parentPermlink = parentPermlink, parentAuthor = discussion.parentAuthor
                ?: "", childrenCount = childrenCount, reputation = reputation, lastUpdated = lastUpdated, created = created, firstRebloggedBy = firstRebloggedBy,
                cleanedFromImages = cleanedFromImages)

        item.format = metadata.format
        item.links.addAll(metadata.links)
        item.images.addAll(metadata.images)
        item.tags.addAll(metadata.tags)

        item.activeVotes.addAll(discussion.votes)
        setTypeOfItem(item)
        account?.let { setAvatar(item, it) }
        checkImages(item)
        return item
    }

    private fun getMetadataFromItem(tags: String): GolosDiscussionItemMetadata {
        val metadata = GolosDiscussionItemMetadata()
        if (tags.isNullOrEmpty()) return metadata

        var json: JSONObject? = null
        try {
            json = JSONObject(tags)
            if (json.has("tags")) {

                val tagsJson = json["tags"]
                val tagsList = ArrayList<String>()


                if (tagsJson is JSONArray) {
                    (0 until tagsJson.length())
                            .forEach {
                                tagsList.add(tagsJson[it].toString())
                            }
                } else if (tagsJson is String) {
                    tagsJson.split(" ").let { tagsList.addAll(it) }
                }

                metadata.tags.addAll(tagsList)

            }
        } catch (e: JSONException) {
            Timber.e(tags)
            e.printStackTrace()
        }
        if (json != null) {
            try {
                if (json.has("format")) {
                    val format = json.getString("format")
                    metadata.format = if (format.equals("markdown", true)) GolosDiscussionItem.Format.MARKDOWN else GolosDiscussionItem.Format.HTML
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
                        (0 until imageArray.length()).mapTo(metadata.images) { imageArray[it].toString() }
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
                        (0 until linksArray.length()).mapTo(metadata.links) { linksArray[it].toString() }
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return metadata
    }

    private fun checkImages(golosDiscussionItem: GolosDiscussionItem) {
        if (golosDiscussionItem.images.isEmpty()) {
            golosDiscussionItem.images.addAll(golosDiscussionItem.parts.filter { it is ImageRow }.map { it as ImageRow }.map { it.src })
        }
    }

    private fun setTypeOfItem(golosDiscussionItem: GolosDiscussionItem) {
        val toRowsParser = StoryParserToRows
        golosDiscussionItem.parts = toRowsParser.parse(golosDiscussionItem).toArrayList()
        if (golosDiscussionItem.parts.size == 0) {
            Timber.e("fail on story id is ${golosDiscussionItem.id}\n body =  ${golosDiscussionItem.body}")
        } else {
            if (golosDiscussionItem.parts[0] is ImageRow) {
                golosDiscussionItem.type = GolosDiscussionItem.ItemType.IMAGE_FIRST
            } else {
                if (golosDiscussionItem.images.size != 0) golosDiscussionItem.type = GolosDiscussionItem.ItemType.PLAIN_WITH_IMAGE
            }
        }
        golosDiscussionItem.cleanedFromImages = if (golosDiscussionItem.parts.isEmpty())
            golosDiscussionItem.body.replace(Regexps.markdownImageRegexp, "")
                    .replace(Regexps.anyImageLink, "")
                    .replace(Regex("(\\*)"), "*")
        else {
            golosDiscussionItem.parts.joinToString("\n") {
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

    private data class GolosDiscussionItemMetadata(
            val tags: MutableList<String> = arrayListOf(),
            val images: MutableList<String> = arrayListOf(),
            val links: MutableList<String> = arrayListOf(),
            var format: GolosDiscussionItem.Format = GolosDiscussionItem.Format.HTML)
}
package io.golos.golos.repository.model

import eu.bittrade.libs.golosj.base.models.Discussion
import eu.bittrade.libs.golosj.base.models.DiscussionLight
import eu.bittrade.libs.golosj.base.models.VoteLight
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.story.model.StoryParserToRows
import io.golos.golos.screens.story.model.TextRow
import io.golos.golos.utils.Regexps
import io.golos.golos.utils.toArrayList
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

object DiscussionItemFactory {
    fun create(discussion: Discussion): GolosDiscussionItem {
        val metadata = getMetadataFromItem(discussion.jsonMetadata)

        val url = discussion.url ?: ""
        val id = discussion.id
        val title = discussion.title ?: ""
        val categoryName = discussion.category ?: ""


        val votesNum = discussion.activeVotesCount?.toInt() ?: discussion.netVotes
        val commentsCount = discussion.children
        val permlink = discussion.permlink?.link ?: ""
        val childrenCount = discussion.children
        val reputation = discussion.authorReputation
        val lastUpdated = discussion.lastUpdate?.dateTimeAsTimestamp ?: 0
        val created = discussion.created?.dateTimeAsTimestamp ?: 0
        val parentPermlink = discussion.parentPermlink.link ?: ""
        val firstRebloggedBy = ""
        val gbgAmount = discussion.pendingPayoutValue?.amount ?: 0.0
        val body = discussion.body ?: ""
        val author = discussion.author?.name ?: ""
        val cleanedFromImages = ""
        val totalRshares = discussion.voteRshares

        var upvotes: Int = 0
        var downvotes: Int = 0

        discussion.activeVotes.forEach {
            if (it.percent > 0) upvotes += 1
            else if (it.percent < 0) downvotes += 1
        }

        val item = GolosDiscussionItem(url, id, title, categoryName, votesNum = votesNum,
                votesRshares = totalRshares,
                commentsCount = commentsCount, permlink = permlink, gbgAmount = gbgAmount, body = body,
                bodyLength = discussion.bodyLength.toLongOrNull()
                        ?: 0L, author = author, parentPermlink = parentPermlink,
                parentAuthor = discussion.parentAuthor.name ?: "", childrenCount = childrenCount,
                reputation = reputation, lastUpdated = lastUpdated, created = created,
                cleanedFromImages = cleanedFromImages,
                format = metadata.format,
                links = metadata.links,
                images = metadata.images,
                tags = metadata.tags,
                upvotesNum = upvotes,
                downvotesNum = downvotes,
                activeVotes = discussion.activeVotes
                        ?.map { VoteLight(it.voter.name, it.rshares.toLong(), it.percent / 100) }.orEmpty().toArrayList(),
                level = discussion.depth.toInt())


        setTypeOfItem(item)
        checkImages(item)
        return item
    }

    fun create(discussion: DiscussionLight): GolosDiscussionItem {
        val metadata = getMetadataFromItem(discussion.jsonMetadata)

        val url = discussion.url ?: ""
        val id = discussion.id
        val title = discussion.title ?: ""

        val categoryName = discussion.category ?: ""

        val votesNum = discussion.activeVotesCount?.toInt() ?: discussion.netVotes
        val commentsCount = discussion.children
        val permlink = discussion.permlink ?: ""
        val childrenCount = discussion.children
        val reputation = discussion.authorReputation
        val lastUpdated = discussion.lastUpdate?.dateTimeAsTimestamp ?: 0
        val created = discussion.created?.dateTimeAsTimestamp ?: 0
        val parentPermlink = discussion.parentPermlink ?: ""
        val firstRebloggedBy = ""
        val gbgAmount = discussion.pendingPayoutValue?.amount ?: 0.0
        val body = discussion.body ?: ""
        val author = discussion.author ?: ""
        val cleanedFromImages = ""
        val totalRshares = discussion.voteRshares

        var upvotes: Int = 0
        var downvotes: Int = 0
        discussion.votes.forEach {
            if (it.percent > 0) upvotes += 1
            else if (it.percent < 0) downvotes += 1
        }
        val item = GolosDiscussionItem(url, id, title, categoryName, votesNum = votesNum,
                votesRshares = totalRshares,
                commentsCount = commentsCount, permlink = permlink, gbgAmount = gbgAmount, body = body,
                bodyLength = discussion.bodyLength, author = author, parentPermlink = parentPermlink, parentAuthor = discussion.parentAuthor
                ?: "", childrenCount = childrenCount, reputation = reputation, lastUpdated = lastUpdated,
                created = created,
                cleanedFromImages = cleanedFromImages, format = metadata.format,
                links = metadata.links,
                images = metadata.images,
                tags = metadata.tags,
                upvotesNum = upvotes,
                downvotesNum = downvotes,
                activeVotes = discussion.votes,
                level = discussion.depth.toInt())

        setTypeOfItem(item)
        checkImages(item)
        return item
    }

    private fun getMetadataFromItem(tags: String): GolosDiscussionItemMetadata {
        val metadata = GolosDiscussionItemMetadata()
        if (tags.isEmpty()) return metadata

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
                if (json.has("image") && json.get("image") != null) {

                    val imageArray = json.getJSONArray("image")
                    if (imageArray.length() > 0) {
                        (0 until imageArray.length()).mapTo(metadata.images) { imageArray[it].toString() }
                    }
                }
            } catch (e: JSONException) {
                Timber.e(" image = ${json.get("image")}")
                e.printStackTrace()
            }
        }
        if (json != null) {
            try {
                if (json.has("links") && json.get("links") != null) {
                    val linksArray = json.getJSONArray("links")
                    if (linksArray.length() > 0) {
                        (0 until linksArray.length()).mapTo(metadata.links) { linksArray[it].toString() }
                    }
                }
            } catch (e: JSONException) {
                Timber.e(" links = ${json.get("links")}")

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
        golosDiscussionItem.parts.addAll(toRowsParser.parse(golosDiscussionItem).toArrayList())
        if (golosDiscussionItem.parts.size == 0) {
            Timber.e("fail on rootWrapper id is ${golosDiscussionItem.id}\n body =  ${golosDiscussionItem.body}")
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


    private data class GolosDiscussionItemMetadata(
            val tags: MutableList<String> = arrayListOf(),
            val images: MutableList<String> = arrayListOf(),
            val links: MutableList<String> = arrayListOf(),
            var format: GolosDiscussionItem.Format = GolosDiscussionItem.Format.HTML)
}
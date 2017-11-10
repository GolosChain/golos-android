package io.golos.golos.screens.main_stripes.model

import android.support.annotation.VisibleForTesting
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import eu.bittrade.libs.steemj.base.models.Discussion
import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup


data class StripeItem(val discussion: Discussion) {
    val url: String
    val id: Long
    val title: String
    @JsonProperty("payoutInDollats")
    val payoutInDollats: String
    @JsonProperty("categoryName")
    val categoryName: String
    @JsonProperty("tags")
    val tags: ArrayList<String> = ArrayList()
    @JsonProperty("images")
    val images = ArrayList<String>()
    @JsonProperty("links")
    val links = ArrayList<String>()
    @JsonProperty("votesNum")
    val votesNum: Int
    @JsonProperty("postsCount")
    val commentsCount: Int
    @JsonProperty("permlink")
    val permlink: String
    @JsonProperty("gbgAmount")
    val gbgAmount: Double
    @JsonProperty("body")
    val body: String
    @JsonProperty("author")
    val author: String
    @JsonProperty("firstRebloggedBy")
    val firstRebloggedBy: String
    @JsonProperty("format")
    var format: Format = Format.HTML
    @JsonProperty("type")
    var type: StripeItemType = StripeItemType.PLAIN
    @JsonProperty("avatarPath")
    var avatarPath: String? = null
    val reputation: Long

    init {

        id = discussion.id
        url = discussion.url ?: ""
        title = discussion.title ?: ""
        categoryName = discussion.category ?: ""
        votesNum = discussion.netVotes
        commentsCount = discussion.children
        permlink = discussion.permlink?.link ?: ""
        reputation = discussion.authorReputation
        val tagsString = discussion.jsonMetadata
        var json: JSONObject? = null
        try {
            if (tagsString != null && tagsString.isNotEmpty()) {
                json = JSONObject(tagsString)
                if (json.has("tags")) {
                    val tagsArray = json.getJSONArray("tags")
                    if (tagsArray.length() > 0) {
                        (0 until tagsArray.length()).mapTo(tags) { tagsArray[it].toString() }
                    }
                }
            }
        } catch (e: JSONException) {
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
                        (0 until imageArray.length()).mapTo(images) {
                            var image = imageArray[it].toString()
                            if (image.endsWith("/")) image = image.removeSuffix("/")
                            image
                        }
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
        var body = discussion.body ?: ""
        author = discussion.author?.name ?: ""

        firstRebloggedBy = discussion.firstRebloggedBy?.name ?: ""

        if (format == Format.HTML) {
            try {
                parseAsHtml(body)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (type == StripeItemType.PLAIN && images.isNotEmpty()) type = StripeItemType.PLAIN_WITH_IMAGE
        } else if (format == Format.MARKDOWN) {
            if (body.contains(htmlImageRegexp)) {
                format = Format.HTML
                parseAsHtml(body)
            } else {
                body = body.replace(allHtml, "").replace("_", "")
                val document = parser.parse(body)
                if (document.firstChild != null && checkNodeIsImage(document.firstChild)) {
                    type = StripeItemType.IMAGE_FIRST
                } else if (images.isNotEmpty()) type = StripeItemType.PLAIN_WITH_IMAGE
                else type = StripeItemType.PLAIN
            }


        } else if (images.isNotEmpty()) type = StripeItemType.PLAIN_WITH_IMAGE
        else type = StripeItemType.PLAIN

        if (type == StripeItemType.PLAIN_WITH_IMAGE) {
            if (format == Format.HTML) this.body = body.replace(htmlImageRegexp, "").replace(anyImageLink, "")
            else this.body = body
                    .replace(markdownImageRegexp, "")
                    .replace(anyImageLink, "")
                    .replace(Regex("(\\*)"), "*")
        } else if (type == StripeItemType.IMAGE_FIRST) {
            this.body = ""
        } else {
            this.body = body
        }
    }

    @VisibleForTesting
    fun removeImagesFromMarkdown(s: String): String {
        val document = parser.parse(s)
        if (document.firstChild != null && checkNodeIsImage(document.firstChild)) document.firstChild.unlink()
        if (document.firstChild != null && checkNodeIsImage(document.firstChild)) document.firstChild.unlink()
        return document.toString()
    }

    @VisibleForTesting
    fun checkNodeIsImage(node: Node): Boolean {
        return (node is Image || (node.firstChild != null && node is Paragraph && node.firstChild is Image)
                || (node.firstChild != null && node is Paragraph && node.firstChild is Text
                && (node.firstChild as Text).literal.matches(markdownImageRegexpAndContents))
                || (node.firstChild != null && node is Paragraph && node.firstChild is Link
                && (node.firstChild as Link).destination.contains(markdownImageRegexpAndContents)))
    }

    private fun parseAsHtml(html: String) {
        try {
            val cleaned = html.replace(Regex("<p>|</p>|<b>|</b>|\\n|<br>|</br>|&nbsp;"), "")
            val document = Jsoup.parse(cleaned)
            val ets = document.body()
            if (ets.childNodes().size > 0) {
                val elements = ets.childNodes()
                val str = elements[0].toString()
                if (str.contains(markdownImageRegexpAndContents) || str.contains(htmlImageRegexp)) {
                    type = StripeItemType.IMAGE_FIRST
                }

            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (type == StripeItemType.PLAIN && images.isNotEmpty()) type = StripeItemType.PLAIN_WITH_IMAGE

    }


    companion object {
        private val markdownImageRegexpAndContents = Regex("!\\[.*]\\(.*\\)|^(?:([^:/?#]+):)?(?:([^/?#]*))?([^?#]*\\.(?:jpg|gif|png))(?:\\?([^#]*))?(?:#(.*))?")
        private val markdownImageRegexp = Regex("\\[.*]\\(.*\\)")
        private val htmlImageRegexp = Regex("<.*><img.*>")
        private val allHtml = Regex("(<([^>]+)>)")
        private val anyImageLink = Regex("https?:[^)]+\\.(?:jpg|jpeg|gif|png)")
        private val gramsInUntion = 31.1034768
        private val parser = Parser.builder().build()
        private val mapper = jacksonObjectMapper()
    }


    override fun toString(): String {
        return "StripeItem { name is $author\n" +
                "title is $title\n" +
                "avatar is $avatarPath }"

    }

    fun makeCopy(): StripeItem {
        val strVal = mapper.writeValueAsString(this)
        return mapper.readValue(strVal)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StripeItem

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
        if (type != other.type) return false
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
        result = 31 * result + type.hashCode()
        result = 31 * result + (avatarPath?.hashCode() ?: 0)
        return result
    }
}

enum class Format {
    HTML, MARKDOWN
}

enum class StripeItemType {
    PLAIN, PLAIN_WITH_IMAGE, IMAGE_FIRST
}


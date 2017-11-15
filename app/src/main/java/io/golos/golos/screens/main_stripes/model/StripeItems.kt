package io.golos.golos.screens.main_stripes.model

import android.support.annotation.VisibleForTesting
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import eu.bittrade.libs.steemj.base.models.Discussion
import io.golos.golos.screens.story.model.*
import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.jsoup.Jsoup
import timber.log.Timber


data class StripeItem(val discussion: Discussion) {
    val url: String
    val id: Long
    val title: String
    @JsonProperty("payoutInDollars")
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
    var body: String
    @JsonProperty("author")
    val author: String
    @JsonProperty("firstRebloggedBy")
    val firstRebloggedBy: String
    @JsonProperty("format")
    var format: Format = Format.HTML
    @JsonProperty("type")
    var type: ItemType = ItemType.PLAIN
    @JsonProperty("avatarPath")
    var avatarPath: String? = null
    val reputation: Long

    init {
        val comment = Comment(discussion, null)
        this.url = comment.url
        this.id = comment.id
        this.title = comment.title
        this.payoutInDollats = comment.payoutInDollars.toString()
        this.categoryName = comment.categoryName
        this.votesNum = comment.votesNum
        this.commentsCount = comment.commentsCount
        this.permlink = comment.permlink
        this.gbgAmount = comment.gbgAmount
        this.reputation = comment.reputation
        this.author = comment.author
        this.firstRebloggedBy = discussion.firstRebloggedBy?.name ?: ""
        var body = discussion.body
        this.format = comment.format
        this.images.addAll(comment.images)
        this.links.addAll(comment.links)
        val toRowsParser = StoryParserToRows()
        val out = toRowsParser.parse(comment)

        if (out.isEmpty()) {
            Timber.e("parserFail")
            if (format == Format.HTML) {
                try {
                    parseAsHtml(body)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (type == ItemType.PLAIN && images.isNotEmpty()) type = ItemType.PLAIN_WITH_IMAGE
            } else if (format == Format.MARKDOWN) {
                if (body.contains(htmlImageRegexp)) {
                    format = Format.HTML
                    parseAsHtml(body)
                } else {
                    body = body.replace(allHtml, "").replace("_", "")
                    try {
                        val document = parser.parse(body)
                        if (document.firstChild != null && checkNodeIsImage(document.firstChild)) {
                            type = ItemType.IMAGE_FIRST
                        } else if (images.isNotEmpty()) type = ItemType.PLAIN_WITH_IMAGE
                        else type = ItemType.PLAIN
                    } catch (e: ExceptionInInitializerError) {
                        e.printStackTrace()
                        Timber.e("fail on getLocalizedError $body")
                    }
                }


            } else if (images.isNotEmpty()) type = ItemType.PLAIN_WITH_IMAGE
            else type = ItemType.PLAIN

            if (type == ItemType.PLAIN_WITH_IMAGE) {
                if (format == Format.HTML) this.body = body.replace(htmlImageRegexp, "").replace(anyImageLink, "")
                else this.body = body
                        .replace(markdownImageRegexp, "")
                        .replace(anyImageLink, "")
                        .replace(Regex("(\\*)"), "*")
            } else if (type == ItemType.IMAGE_FIRST) {
                this.body = ""
            } else {
                this.body = body
            }
        } else {
            if (out[0] is ImageRow) {
                type = ItemType.IMAGE_FIRST
            } else {
                if (images.size != 0) type = ItemType.PLAIN_WITH_IMAGE
            }
            this.body = out.joinToString("\n") {
                if (it is TextRow) it.text
                else ""
            }
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
                    type = ItemType.IMAGE_FIRST
                }

            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (type == ItemType.PLAIN && images.isNotEmpty()) type = ItemType.PLAIN_WITH_IMAGE

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



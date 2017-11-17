package io.golos.golos.screens.story.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import eu.bittrade.libs.steemj.base.models.Discussion
import eu.bittrade.libs.steemj.base.models.DiscussionWithComments
import eu.bittrade.libs.steemj.base.models.ExtendedAccount
import io.golos.golos.utils.UpdatingState

/**
 * Created by yuri on 06.11.17.
 */
data class StoryWrapper(
        @JsonProperty("story")
        val story: GolosDiscussionItem,
        @JsonProperty("updatingState")
        val updatingState: UpdatingState)

@JsonIgnoreProperties(ignoreUnknown = true)
class StoryTree(rootStory: StoryWrapper?,
                comments: List<StoryWrapper>) : Cloneable {

    @JsonProperty("rootStory")
    private var mRootStoryWrapper: StoryWrapper? = rootStory
    @JsonProperty("comments")
    var commentsWithState: ArrayList<StoryWrapper> = ArrayList(comments)

    fun rootStory(): GolosDiscussionItem? {
        return mRootStoryWrapper?.story
    }

    fun comments(): List<GolosDiscussionItem> {

        return commentsWithState.map { it.story }
    }

    fun rootStoryWrapper(): StoryWrapper? {
        return mRootStoryWrapper
    }

    constructor(discussionWithComments: DiscussionWithComments) : this(null, ArrayList()) {
        if (discussionWithComments.discussions.isEmpty()) {
            commentsWithState = ArrayList()
        } else {
            val rootStories = discussionWithComments.discussions.find { it.parentAuthor.isEmpty } ?: throw IllegalStateException("no root stories")
            mRootStoryWrapper = StoryWrapper(GolosDiscussionItem(rootStories, discussionWithComments.involvedAccounts.find { it.name.name == rootStories.author.name }!!),
                    UpdatingState.DONE)
            discussionWithComments.discussions.removeAll { it.permlink.link == rootStories.permlink.link }
            var firstLevelDiscussion = findAlldiscussionsWithParentPermlink(discussionWithComments.discussions, mRootStoryWrapper!!.story.permlink)
            discussionWithComments.discussions.removeAll { firstLevelDiscussion.contains(it) }
            val allComments = discussionWithComments.discussions.map { convert(it, discussionWithComments.involvedAccounts) }

            val comments = firstLevelDiscussion.map { convert(it, discussionWithComments.involvedAccounts) }
            comments.forEach { it.children = findAssociations(ArrayList(allComments), it, 1) }
            commentsWithState = ArrayList(comments.map { StoryWrapper(it, UpdatingState.DONE) })
        }
    }

    private fun findAlldiscussionsWithParentPermlink(discussions: List<Discussion>, permlink: String): List<Discussion> {
        return discussions.filter { it.parentPermlink.link == permlink }
    }

    private fun convert(discussion: Discussion, accounts: List<ExtendedAccount>): GolosDiscussionItem {
        return GolosDiscussionItem(discussion, accounts.filter { it.name.name == discussion.author.name }.first())
    }

    fun getFlataned(): List<GolosDiscussionItem> {
        return comments().flatMap { listOf(it) + getChildrend(it) }
    }

    private fun getChildrend(of: GolosDiscussionItem): List<GolosDiscussionItem> {
        if (of.children.isEmpty()) return ArrayList()
        val out = of.children
        return out.flatMap { listOf(it) + getChildrend(it) }
    }

    private fun findAssociations(from: ArrayList<GolosDiscussionItem>, to: GolosDiscussionItem, level: Int): ArrayList<GolosDiscussionItem> {
        if (to.childrenCount == 0) return ArrayList()
        if (from.contains(to)) {
            from.remove(to)
        }
        val out = from.filter { it.parentPermlink == to.permlink }
        out.forEach {
            it.level = level
            it.children = ArrayList(findAssociations(from, it, level + 1))
        }
        return ArrayList(out)
    }

    override fun toString(): String {
        return "StoryTree(story=${rootStory()}\n," +
                " comments = ${comments()}"
    }

    override fun clone(): Any {
        return super.clone()

    }

    fun deepCopy(): StoryTree {
       return mapper.readValue(mapper.writeValueAsString(this), StoryTree::class.java)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StoryTree) return false

        if (mRootStoryWrapper != other.mRootStoryWrapper) return false
        if (commentsWithState != other.commentsWithState) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mRootStoryWrapper?.hashCode() ?: 0
        result = 31 * result + commentsWithState.hashCode()
        return result
    }

}
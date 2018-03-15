package io.golos.golos.screens.story.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import eu.bittrade.libs.steemj.base.models.Discussion
import eu.bittrade.libs.steemj.base.models.DiscussionWithComments
import eu.bittrade.libs.steemj.base.models.ExtendedAccount
import io.golos.golos.repository.model.DiscussionItemFactory
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.utils.UpdatingState

/**
 * Created by yuri on 06.11.17.
 */
data class StoryWrapper(
        @JsonProperty("story")
        val story: GolosDiscussionItem,
        @JsonProperty("updatingState")
        var updatingState: UpdatingState,
        @JsonProperty("isStoryEditable")
        var isStoryEditable: Boolean = false,
        @JsonProperty("asHtmlString")
        var asHtmlString: CharSequence? = null) {

    fun copyChangingUpdatingState(newState: UpdatingState): StoryWrapper {
        return StoryWrapper(story, newState, isStoryEditable, asHtmlString)
    }
}

data class SubscribeStatus(val isCurrentUserSubscribed: Boolean,
                           val updatingState: UpdatingState) {
    companion object {
        private val unSubscribed = SubscribeStatus(false, UpdatingState.DONE)
        private val subScribed = SubscribeStatus(true, UpdatingState.DONE)

        public val UnsubscribedStatus = unSubscribed
        public val SubscribedStatus = subScribed
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class StoryWithComments(rootStory: StoryWrapper?,
                        comments: List<StoryWrapper>) : Cloneable {

    @JsonProperty("rootStory")
    private var mRootStoryWrapper: StoryWrapper? = rootStory
    @JsonProperty("comments")
    private var mCommentsWithState: ArrayList<StoryWrapper> = ArrayList(comments)

    fun rootStory(): GolosDiscussionItem? {
        return mRootStoryWrapper?.story
    }


    fun comments(): List<GolosDiscussionItem> {

        return mCommentsWithState.map { it.story }
    }

    fun storyWithState(): StoryWrapper? {
        return mRootStoryWrapper
    }

    fun commentsWithState(): ArrayList<StoryWrapper> {
        return mCommentsWithState
    }

    fun setUpLevels() {
        setUpLevels(0, mRootStoryWrapper?.story?.children ?: return)
    }

    private fun setUpLevels(currentDepth: Int, stories: List<StoryWrapper>) {
        stories.forEach { it.story.level = currentDepth }
        stories.map { it.story.children }.forEach { setUpLevels(currentDepth + 1, it) }
    }

    constructor(discussionWithComments: DiscussionWithComments) : this(null, ArrayList()) {
        if (discussionWithComments.discussions.isEmpty()) {
            mCommentsWithState = ArrayList()
        } else {
            var rootStories = discussionWithComments.discussions.find { it.parentAuthor.isEmpty }
            if (rootStories == null) {
                rootStories = discussionWithComments.discussions.find {
                    val currentDiscussion = it
                    discussionWithComments.discussions.find { it.permlink == currentDiscussion.parentPermlink } == null
                }
            }
            if (rootStories == null) throw IllegalStateException("root story not found")
            mRootStoryWrapper = StoryWrapper(DiscussionItemFactory.create(rootStories, discussionWithComments
                    .involvedAccounts
                    .find { it.name.name == rootStories!!.author.name }!!),
                    UpdatingState.DONE)
            discussionWithComments.discussions.removeAll { it.permlink.link == rootStories!!.permlink.link }
            val firstLevelDiscussion = findAlldiscussionsWithParentPermlink(discussionWithComments.discussions, mRootStoryWrapper!!.story.permlink)
            discussionWithComments.discussions.removeAll { firstLevelDiscussion.contains(it) }
            val allComments = discussionWithComments.discussions.map { convert(it, discussionWithComments.involvedAccounts) }.map { StoryWrapper(it, UpdatingState.DONE) }

            val comments = firstLevelDiscussion.map { convert(it, discussionWithComments.involvedAccounts) }.map { StoryWrapper(it, UpdatingState.DONE) }

            comments.forEach { it.story.children = createCommentsTree(ArrayList(allComments), it, 1) }

            mCommentsWithState = ArrayList(comments)
        }
    }

    fun replaceComment(replaceWith: StoryWrapper): Boolean {
        return replaceComment(replaceWith, mCommentsWithState)
    }

    private fun replaceComment(replaceWith: StoryWrapper, src: ArrayList<StoryWrapper>): Boolean {
        var changes = false
        (0..src.lastIndex)
                .forEach {
                    if (src[it].story.childrenCount > 0) {
                        changes = replaceComment(replaceWith, src[it].story.children) || changes
                    }
                    if (replaceWith.story.id == src[it].story.id) {
                        val replacingItem = src[it].story
                        src[it] = replaceWith
                        src[it].story.children = replacingItem.children
                        src[it].story.level = replacingItem.level
                        changes = true
                    }
                }
        return changes
    }

    private fun findAlldiscussionsWithParentPermlink(discussions: List<Discussion>, permlink: String): List<Discussion> {
        return discussions.filter { it.parentPermlink.link == permlink }
    }

    private fun convert(discussion: Discussion, accounts: List<ExtendedAccount>): GolosDiscussionItem {
        return DiscussionItemFactory.create(discussion, accounts.filter { it.name.name == discussion.author.name }.first())
    }

    fun getFlataned(): List<StoryWrapper> {
        return mCommentsWithState.flatMap { listOf(it) + getChildren(it) }
    }

    private fun getChildren(of: StoryWrapper): List<StoryWrapper> {
        if (of.story.children.isEmpty()) return ArrayList()
        val out = of.story.children
        return out.flatMap { listOf(it) + getChildren(it) }
    }

    private fun createCommentsTree(from: ArrayList<StoryWrapper>, to: StoryWrapper, level: Int): ArrayList<StoryWrapper> {
        if (to.story.childrenCount == 0) return ArrayList()
        if (from.contains(to)) {
            from.remove(to)
        }
        val out = from.filter { it.story.parentPermlink == to.story.permlink }
        out.forEach {
            it.story.level = level
            it.story.children = ArrayList(createCommentsTree(from, it, level + 1))
        }
        return ArrayList(out)
    }

    override fun toString(): String {
        return "StoryTree(story=${rootStory()}\n," +
                " comments = ${comments()}"
    }

    override public fun clone(): Any {
        return super.clone()

    }

    fun deepCopy(): StoryWithComments {
        val rootWrapper = if (mRootStoryWrapper != null) {
            StoryWrapper(mRootStoryWrapper!!.story.copy(), mRootStoryWrapper!!.updatingState, mRootStoryWrapper!!.isStoryEditable)
        } else null
        return StoryWithComments(rootWrapper, mCommentsWithState.clone() as ArrayList<StoryWrapper>)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StoryWithComments) return false

        if (mRootStoryWrapper != other.mRootStoryWrapper) return false
        if (mCommentsWithState != other.mCommentsWithState) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mRootStoryWrapper?.hashCode() ?: 0
        result = 31 * result + mCommentsWithState.hashCode()
        return result
    }

}
package io.golos.golos.screens.story.model

import eu.bittrade.libs.golosj.base.models.Discussion
import eu.bittrade.libs.golosj.base.models.DiscussionWithComments
import io.golos.golos.repository.model.DiscussionItemFactory
import io.golos.golos.repository.model.ExchangeValues
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.repository.model.GolosDiscussionItemVotingState
import io.golos.golos.repository.persistence.model.GolosUserAccountInfo
import io.golos.golos.utils.UpdatingState
import io.golos.golos.utils.toArrayList

/**
 * Created by yuri on 06.11.17.
 */
data class StoryWrapper(
        val story: GolosDiscussionItem,
        val voteUpdatingState: GolosDiscussionItemVotingState? = null,
        val voteStatus: GolosDiscussionItem.UserVoteType = GolosDiscussionItem.UserVoteType.NOT_VOTED_OR_ZERO_WEIGHT,
        val authorAccountInfo: GolosUserAccountInfo? = null,
        val exchangeValues: ExchangeValues = ExchangeValues.nullValues,
        val isStoryEditable: Boolean = false,
        var asHtmlString: CharSequence? = null) {
    override fun toString(): String {
        return "StoryWrapper(story=$story, voteUpdatingState=$voteUpdatingState, voteStatus=$voteStatus, authorAccountInfo=$authorAccountInfo, isStoryEditable=$isStoryEditable)"
    }
}

data class SubscribeStatus(val isCurrentUserSubscribed: Boolean,
                           val updatingState: UpdatingState) {
    companion object {
        private val unSubscribed = SubscribeStatus(false, UpdatingState.DONE)
        private val subScribed = SubscribeStatus(true, UpdatingState.DONE)

        public val UnsubscribedStatus = unSubscribed
        public val SubscribedStatus = subScribed

        fun create(isCurrentUserSubscribed: Boolean,
                   updatingState: UpdatingState): SubscribeStatus {
            return if (!isCurrentUserSubscribed && updatingState == UpdatingState.DONE) return unSubscribed
            else if (isCurrentUserSubscribed && updatingState == UpdatingState.DONE) return subScribed
            else SubscribeStatus(isCurrentUserSubscribed, updatingState)
        }
    }
}


data class StoryWrapperWithComment(val rootWrapper: StoryWrapper, val comments: List<StoryWrapper>)


object StoryCommentsHierarchyResolver {
    fun resolve(discussionWithComments: DiscussionWithComments): StoryWithComments {
        if (discussionWithComments.discussions.isEmpty()) {

            return StoryWithComments(GolosDiscussionItem.emptyItem, arrayListOf())

        } else {
            var rootStories = discussionWithComments.discussions.find { it.parentAuthor.isEmpty }
            if (rootStories == null) {
                rootStories = discussionWithComments.discussions.find {
                    val currentDiscussion = it
                    discussionWithComments.discussions.find { it.permlink == currentDiscussion.parentPermlink } == null
                }
            }
            if (rootStories == null) throw IllegalStateException("root rootWrapper not found")
            val rootDiscussionItem = convert(rootStories)

            discussionWithComments.discussions.removeAll { it.permlink.link == rootStories.permlink.link }

            val firstLevelDiscussion = findAlldiscussionsWithParentPermlink(discussionWithComments.discussions, rootDiscussionItem.permlink)

            discussionWithComments.discussions.removeAll { firstLevelDiscussion.contains(it) }

            val allComments = discussionWithComments.discussions.asSequence().map { convert(it) }.toList()

            val comments = firstLevelDiscussion.map { convert(it) }

            comments
                    .forEach {
                        it.children.clear()
                        it.children.addAll(createCommentsTree(ArrayList(allComments), it, 1))
                    }

            return StoryWithComments(rootDiscussionItem, comments.toArrayList())
        }
    }

    private fun findAlldiscussionsWithParentPermlink(discussions: List<Discussion>, permlink: String): List<Discussion> {
        return discussions.filter { it.parentPermlink.link == permlink }
    }

    private fun convert(discussion: Discussion): GolosDiscussionItem {
        return DiscussionItemFactory.create(discussion)
    }

    private fun createCommentsTree(from: ArrayList<GolosDiscussionItem>, to: GolosDiscussionItem, level: Int): ArrayList<GolosDiscussionItem> {
        if (to.childrenCount == 0) return ArrayList()
        if (from.contains(to)) {
            from.remove(to)
        }
        val out = from.filter { it.parentPermlink == to.permlink }
        out.forEach {
            it.level = level
            it.children.clear()
            it.children.addAll(createCommentsTree(from, it, level + 1))
        }
        return ArrayList(out)
    }
}


data class StoryWithComments(val rootStory: GolosDiscussionItem,
                             val comments: MutableList<GolosDiscussionItem>) {


    @Synchronized
    fun setUpLevels() {
        setUpLevels(0, rootStory
                .children
                .toArrayList())
    }

    private fun setUpLevels(currentDepth: Int, stories: List<GolosDiscussionItem>) {
        stories.forEach { it.level = currentDepth }
        stories.map { it.children }.forEach { setUpLevels(currentDepth + 1, it) }
    }

    /*constructor(discussionWithComments: DiscussionWithComments) : this(null, ArrayList()) {
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
            if (rootStories == null) throw IllegalStateException("root rootWrapper not found")
            mRootStoryWrapper = StoryWrapper(DiscussionItemFactory.create(rootStories, discussionWithComments
                    .involvedAccounts
                    .find { it.name.name == rootStories.author.name }!!),
                    UpdatingState.DONE)
            discussionWithComments.discussions.removeAll { it.permlink.link == rootStories.permlink.link }
            val firstLevelDiscussion = findAlldiscussionsWithParentPermlink(discussionWithComments.discussions, mRootStoryWrapper!!.rootWrapper.permlink)
            discussionWithComments.discussions.removeAll { firstLevelDiscussion.contains(it) }
            val allComments = discussionWithComments.discussions.map { convert(it, discussionWithComments.involvedAccounts) }.map { StoryWrapper(it, UpdatingState.DONE) }

            val comments = firstLevelDiscussion.map { convert(it, discussionWithComments.involvedAccounts) }.map { StoryWrapper(it, UpdatingState.DONE) }

            comments
                    .forEach {
                        it.rootWrapper.children.clear()
                        it.rootWrapper.children.addAll(createCommentsTree(ArrayList(allComments), it, 1))
                    }

            mCommentsWithState = ArrayList(comments)
        }
    }*/

    fun replaceComment(replaceWith: GolosDiscussionItem): Boolean {
        return replaceComment(replaceWith, comments)
    }

    private fun replaceComment(replaceWith: GolosDiscussionItem, src: MutableList<GolosDiscussionItem>): Boolean {
        var changes = false
        (0..src.lastIndex)
                .forEach {
                    val currentStory = src[it]

                    if (replaceWith.id == currentStory.id) {

                        currentStory.children.forEach {
                            if (!replaceWith.children.contains(it)) replaceWith.children.add(it)
                        }

                        replaceWith.level = currentStory.level
                        src[it] = replaceWith

                        changes = true
                    } else if (currentStory.childrenCount > 0) {
                        changes = replaceComment(replaceWith, currentStory.children) || changes
                    }
                }
        return changes
    }

//    private fun findAlldiscussionsWithParentPermlink(discussions: List<Discussion>, permlink: String): List<Discussion> {
//        return discussions.filter { it.parentPermlink.link == permlink }
//    }
//
//    private fun convert(discussion: Discussion, accounts: List<ExtendedAccount>): GolosDiscussionItem {
//        return DiscussionItemFactory.create(discussion, accounts.filter { it.name.name == discussion.author.name }.first())
//    }

    fun getFlataned(): List<GolosDiscussionItem> {
        setUpLevels()
        return comments.flatMap { listOf(it) + getChildren(it) }
    }

    private fun getChildren(of: GolosDiscussionItem): List<GolosDiscussionItem> {
        if (of.children.isEmpty()) return ArrayList()
        val out = of.children
        return out.flatMap { listOf(it) + getChildren(it) }
    }

//
//    private fun createCommentsTree(from: ArrayList<StoryWrapper>, to: StoryWrapper, level: Int): ArrayList<StoryWrapper> {
//        if (to.rootWrapper.childrenCount == 0) return ArrayList()
//        if (from.contains(to)) {
//            from.remove(to)
//        }
//        val out = from.filter { it.rootWrapper.parentPermlink == to.rootWrapper.permlink }
//        out.forEach {
//            it.rootWrapper.level = level
//            it.rootWrapper.children.clear()
//            it.rootWrapper.children.addAll(createCommentsTree(from, it, level + 1))
//        }
//        return ArrayList(out)
//    }
//
//    override fun toString(): String {
//        return "StoryTree(rootWrapper=${rootStory()}\n," +
//                " comments = ${comments()}"
//    }
//
//    override public fun clone(): Any {
//        return super.clone()
//
//    }
//
//    fun deepCopy(): StoryWithComments {
//        val rootWrapper = if (mRootStoryWrapper != null) {
//            StoryWrapper(mRootStoryWrapper!!.rootWrapper.copy(),
//                    mRootStoryWrapper!!.updatingState,
//                    mRootStoryWrapper!!.exchangeValues,
//                    mRootStoryWrapper!!.isStoryEditable,
//                    mRootStoryWrapper!!.asHtmlString)
//        } else null
//        return StoryWithComments(rootWrapper, mCommentsWithState.clone() as ArrayList<StoryWrapper>)
//    }
//
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (other !is StoryWithComments) return false
//
//        if (mRootStoryWrapper != other.mRootStoryWrapper) return false
//        if (mCommentsWithState != other.mCommentsWithState) return false
//
//        return true
//    }
//
//    override fun hashCode(): Int {
//        var result = mRootStoryWrapper?.hashCode() ?: 0
//        result = 31 * result + mCommentsWithState.hashCode()
//        return result
//    }

}
package io.golos.golos.screens.story.model

import eu.bittrade.libs.steemj.base.models.Discussion
import eu.bittrade.libs.steemj.base.models.ExtendedAccount
import eu.bittrade.libs.steemj.base.models.Story

/**
 * Created by yuri on 06.11.17.
 */
class StoryTreeBuilder(story: Story) {
    var rootStory: RootStory? = null
    val comments: List<Comment>

    init {
        if (story.discussions.isEmpty()) {
            comments = ArrayList()
        } else {
            val rootStories = story.discussions.find { it.parentAuthor.isEmpty } ?: throw IllegalStateException("no root stories")
            rootStory = RootStory(rootStories, story.involvedAccounts.find { it.name.name == rootStories.author.name }!!)
            story.discussions.removeAll { it.permlink.link == rootStories.permlink.link }
            var firstLevelDiscussion = findAlldiscussionsWithParentPermlink(story.discussions, rootStory!!.permlink)
            story.discussions.removeAll { firstLevelDiscussion.contains(it) }
            val allComments = story.discussions.map { convert(it, story.involvedAccounts) }
            comments = firstLevelDiscussion.map { convert(it, story.involvedAccounts) }
            comments.forEach { it.children = findAssociations(ArrayList(allComments), it, 1) }
        }
    }

    private fun findAlldiscussionsWithParentPermlink(discussions: List<Discussion>, permlink: String): List<Discussion> {
        return discussions.filter { it.parentPermlink.link == permlink }
    }

    private fun convert(discussion: Discussion, accounts: List<ExtendedAccount>): Comment {
        return Comment(discussion, accounts.filter { it.name.name == discussion.author.name }.first())
    }

    fun getFlataned(): List<Comment> {
        return comments.flatMap { listOf(it) + getChildrend(it) }
    }

    private fun getChildrend(of: Comment): List<Comment> {
        if (of.children.isEmpty()) return ArrayList()
        val out = of.children
        return  out.flatMap { listOf(it) + getChildrend(it) }
    }

    private fun findAssociations(from: ArrayList<Comment>, to: Comment, level: Int): ArrayList<Comment> {
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
        return "StoryTree(rootStory=$rootStory\n," +
                " comments = $comments)"
    }


}
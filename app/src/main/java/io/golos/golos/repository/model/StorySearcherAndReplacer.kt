package io.golos.golos.repository.model

import io.golos.golos.screens.story.model.StoryWithComments
import io.golos.golos.utils.toArrayList

/**
 * Created by yuri on 17.11.17.
 */


data class ReplaceResult(val isChanged: Boolean, val resultingFeed: StoriesFeed)

class StorySearcherAndReplacer {
    fun findAndReplaceStory(replacer: GolosDiscussionItem, inList: StoriesFeed): ReplaceResult {
        var isChanged = false
        val itemsCopy = inList.items.toArrayList()

        (0..itemsCopy.lastIndex)
                .filter { itemsCopy[it].rootStory.id == replacer.id }
                .forEach {
                    isChanged = true
                    val oldItem = itemsCopy[it]
                    itemsCopy[it] = StoryWithComments(replacer, oldItem.comments)
                    if (oldItem.rootStory.rebloggedBy != null) itemsCopy[it].rootStory.rebloggedBy = oldItem.rootStory.rebloggedBy
                }

        itemsCopy.forEach {
            isChanged = it.replaceComment(replacer) || isChanged
        }

        return ReplaceResult(isChanged, if (isChanged) inList.copy(items = itemsCopy) else inList)
    }

    fun findAndReplaceStory(newState: StoryWithComments, source: ArrayList<StoryWithComments>): Boolean {
        if (source.size == 0) return false
        var isChanged = false

        (0..source.lastIndex)
                .filter { i -> source[i].rootStory.id == newState.rootStory.id }
                .forEach { i ->
                    val oldItem = source[i]
                    source[i] = newState
                    if (oldItem.rootStory.rebloggedBy != null) source[i].rootStory.rebloggedBy = oldItem.rootStory.rebloggedBy
                    isChanged = true
                }
        if (!isChanged) {

            val root = newState.rootStory

            root.children.clear()
            root.children.addAll(newState.comments)
            source.forEachIndexed { index, storyWithComments ->
                val comments = storyWithComments.getFlataned()
                if (comments.find { it.id == root.id } != null) {
                    isChanged = true
                    storyWithComments.replaceComment(root)
                }
            }
        }
        return isChanged
    }
}
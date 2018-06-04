package io.golos.golos.repository.model

import io.golos.golos.screens.story.model.StoryWithComments
import io.golos.golos.screens.story.model.StoryWrapper

/**
 * Created by yuri on 17.11.17.
 */
class StorySearcherAndReplacer {
    fun findAndReplace(replacer: StoryWrapper, inList: ArrayList<StoryWithComments>): Boolean {
        var isChanged = false
        if (inList.any({ it.rootStory()?.id == replacer.story.id })) {
            isChanged = true
            (0..inList.lastIndex)
                    .filter { inList[it].rootStory()?.id == replacer.story.id }
                    .forEach {
                        inList[it] = StoryWithComments(replacer, inList[it].commentsWithState())
                    }

        }
        inList.forEach {
            isChanged = it.replaceComment(replacer) || isChanged
        }
        return isChanged
    }

    fun findAndReplace(newState: StoryWithComments, source: ArrayList<StoryWithComments>): Boolean {
        if (source.size == 0) return false
        var isChanged = false
        (0..source.lastIndex)
                .filter { i -> source[i].rootStory()?.id == newState.rootStory()?.id }
                .forEach { i ->
                    source[i] = newState
                    isChanged = true
                }
        if (!isChanged) {
            val root = newState.storyWithState()
            root?.let {
                root.story.children.clear()
                root.story.children.addAll(newState.commentsWithState())
                source.forEach { storyWithComments ->
                    val firstLevelComments = storyWithComments.commentsWithState()

                    val isChangedSecondLevel = findAndReplaceInternal(it, firstLevelComments)
                    if (isChangedSecondLevel) {
                        isChanged = true
                        storyWithComments.rootStory()?.children?.clear()
                        storyWithComments.rootStory()?.children?.addAll(firstLevelComments)
                    }
                }
            }
        }
        if (isChanged) source.forEach { it.setUpLevels() }
        return isChanged
    }

    private fun findAndReplaceInternal(newState: StoryWrapper, source: MutableList<StoryWrapper>): Boolean {
        var isChanged = false
        if (source.size == 0) return false
        (0..source.lastIndex)
                .filter { i -> source[i].story.id == newState.story.id }
                .forEach { i ->
                    source[i] = newState
                    isChanged = true
                }
        if (!isChanged) {

            source.forEach {
                val isChildChanged = findAndReplaceInternal(newState, it.story.children)
                if (isChildChanged) isChanged = true
            }
        }
        return isChanged;
    }
}
package io.golos.golos.repository.model

import ParseTest
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.utils.UpdatingState
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

/**
 * Created by yuri on 17.11.17.
 */
class StorySearcherAndReplacerTest {
    @Test
    fun findAndReplace() {
        val tree = ParseTest.readStoryFromResourse("story.json")
        var comment = tree.commentsWithState()[1].copy()
        comment = StoryWrapper(comment.story, UpdatingState.UPDATING)
        comment.story.body = "replaced body"
        var result = tree.replaceComment(comment)
        assertEquals(true, result)
        assertTrue(tree.commentsWithState()[1].updatingState == UpdatingState.UPDATING)
        assertTrue(tree.commentsWithState()[1].story.body == "replaced body")
        assertTrue(tree.commentsWithState()[1].story.childrenCount == 1)
        assertTrue(tree.commentsWithState()[1].story.children.size == 1)

        comment = tree.commentsWithState()[3].story.children[0]
        comment = StoryWrapper(comment.story, UpdatingState.UPDATING)
        comment.story.body = "replaced body"
        result = tree.replaceComment(comment)
        assertEquals(true, result)
        assertTrue(tree.commentsWithState()[3].story.children[0].updatingState == UpdatingState.UPDATING)
        assertTrue(tree.commentsWithState()[3].story.children[0].story.body == "replaced body")
        assertTrue(tree.commentsWithState()[3].story.children[0].story.childrenCount == 1)
        assertTrue(tree.commentsWithState()[3].story.children[0].story.children.size == 1)

        comment = ParseTest.readStoryFromResourse("story4.json").storyWithState()!!
        result = tree.replaceComment(comment)
        assertEquals(false, result)
    }

    @Test
    fun replaceComment() {

    }
}
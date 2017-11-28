package io.golos.golos

import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.story.model.StoryParserToRows
import io.golos.golos.screens.story.model.TextRow
import org.junit.Assert
import org.junit.Test

/**
 * Created by yuri on 28.11.17.
 */
class StoryParserTests {

    @Test
    fun test1() {
        val tree = Utils.readStoryFromResourse("story.json")
        val rowParser = StoryParserToRows()
        val rows = rowParser.parse(tree.rootStory()!!)
        Assert.assertEquals(5, rows.size)
        Assert.assertTrue(rows[1] is ImageRow)
        Assert.assertEquals("https://s1.postimg.org/2ujuhodmlr/hf17.jpg", (rows[1] as ImageRow).src)
        Assert.assertTrue(rows[0] is TextRow)
        Assert.assertTrue(rows[3] is ImageRow)
        Assert.assertTrue(rows[4] is TextRow)
    }

    @Test
    fun test2() {
        val tree = Utils.readStoryFromResourse("story2.json")
        val rowParser = StoryParserToRows()
        val rows = rowParser.parse(tree.rootStory()!!)
        Assert.assertTrue(rows.size > 1)
        Assert.assertTrue(rows[0] is TextRow)
        Assert.assertTrue((rows[0] as TextRow).text.contains("Андрей Килин, режиссер киносериала "))
        Assert.assertTrue(rows[1] is ImageRow)
        Assert.assertEquals("https://s1.postimg.org/7payn9bp33/image.png", (rows[1] as ImageRow).src)
        Assert.assertTrue(rows[2] is TextRow)
        Assert.assertTrue(rows[4] is TextRow)
        Assert.assertTrue((rows[6] as TextRow).text.contains("<a href=\"https://youtu.be/391KGNqUwkc\">https://youtu.be/391KGNqUwkc</a>"))
        Assert.assertTrue(rows[3] is ImageRow)
        Assert.assertTrue(rows[5] is ImageRow)
    }
    @Test
    fun test3() {
        val tree = Utils.readStoryFromResourse("story3.json")
        val rowParser = StoryParserToRows()
        val rows = rowParser.parse(tree.rootStory()!!)
        Assert.assertTrue(rows.size > 1)
        Assert.assertTrue(rows[0] is TextRow)
        Assert.assertTrue(rows[3] is ImageRow)
        Assert.assertTrue(rows[4] is TextRow)
        Assert.assertTrue(rows[10] is TextRow)
        Assert.assertTrue(rows[1] is ImageRow)
        Assert.assertTrue(rows[5] is ImageRow)
    }
}
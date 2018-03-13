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
        val rowParser = StoryParserToRows
        val rows = rowParser.parse(tree.rootStory()!!)
        Assert.assertEquals(5, rows.size)
        Assert.assertTrue(rows[1] is ImageRow)
        Assert.assertTrue((rows[1] as ImageRow).src == "https://s1.postimg.org/2ujuhodmlr/hf17.jpg")
        Assert.assertEquals("https://s1.postimg.org/2ujuhodmlr/hf17.jpg", (rows[1] as ImageRow).src)
        Assert.assertTrue(rows[0] is TextRow)
        Assert.assertTrue(rows[3] is ImageRow)
        Assert.assertTrue((rows[3] as ImageRow).src == "https://cloud.githubusercontent.com/assets/410789/25783476/0c523dd6-335d-11e7-9165-f5ffc4fc8b5e.png")
        Assert.assertTrue(rows[4] is TextRow)
    }

    @Test
    fun test2() {
        val tree = Utils.readStoryFromResourse("story2.json")
        val rowParser = StoryParserToRows
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
        val rowParser = StoryParserToRows
        val rows = rowParser.parse(tree.rootStory()!!)
        Assert.assertTrue(rows.size > 1)
        Assert.assertTrue(rows[0] is TextRow)
        Assert.assertTrue(rows[3] is ImageRow)
        Assert.assertTrue(rows[4] is TextRow)
        Assert.assertTrue(rows[10] is TextRow)
        Assert.assertTrue(rows[1] is ImageRow)
        Assert.assertTrue(rows[5] is ImageRow)
    }
    @Test
    fun test4() {
        val tree = Utils.readStoryFromResourse("story7.json")
        val rowParser = StoryParserToRows
        var rows = rowParser.parse(tree.rootStory()!!)
        Assert.assertTrue(rows.size > 1)
        val boosterComment = tree.getFlataned().find { it.story.author == "booster" }!!
        rows = rowParser.parse(boosterComment.story)
        Assert.assertTrue((rows[0] as TextRow).text.contains("<a href =\"https://golos.blog/@Booster\">@Booster</a>"))

        val upvoteComment = tree.getFlataned().find { it.story.author == "upvote50-50" }!!
        rows = rowParser.parse(upvoteComment.story)
        //todo support clickable images
    }
    @Test
    fun test5() {
        val tree = Utils.readStoryFromResourse("story8.json")
        val rowParser = StoryParserToRows
        var rows = rowParser.parse(tree.rootStory()!!)
        Assert.assertTrue(rows.size > 1)
        Assert.assertTrue(rows[0] is ImageRow)
        Assert.assertTrue(rows[1] is TextRow)

        val text = (rows[1] as TextRow).text
        Assert.assertTrue(text.contains("<a href =\"https://golos.blog/@kotbegemot\">@kotbegemot</a>"))

        val marinaComment = tree.getFlataned().find { it.story.author == "marina" }!!
        rows = rowParser.parse(marinaComment.story)
        Assert.assertTrue((rows[0] as TextRow).text.contains("<a href =\"https://golos.blog/@arcange\">@arcange</a>"))
        Assert.assertTrue((rows[0] as TextRow).text.contains("<a href=\"https://golos.io/ru--golos/@golos/anons-khf-0-2-aka-17-18-29-05-2017\">https://golos.io/ru--golos/@golos/anons-khf-0-2-aka-17-18-29-05-2017</a>"))

        val kitComments  = tree.getFlataned().find { it.story.author == "dobryj.kit" }!!
        rows = rowParser.parse(kitComments.story)
        Assert.assertTrue((rows[0] as TextRow).text.contains(" <a href=\"https://golos.io/@litrbooh\" rel=\"nofollow\">litrbooh</a>"))
        Assert.assertTrue((rows[0] as TextRow).text.contains("<a href=\"https://golos.io/@vika-teplo\" rel=\"nofollow\">vika-teplo</a>"))
        Assert.assertTrue((rows[0] as TextRow).text.contains("<a href=\"https://golos.blog/ru--delegaty/@dobryj.kit/dobryi-kit-delegat\" rel=\"nofollow\">"))
        println(rows)
    }
    @Test
    fun test9() {
        val tree = Utils.readStoryFromResourse("story9.json")
        val rowParser = StoryParserToRows
        var rows = rowParser.parse(tree.rootStory()!!)
        Assert.assertTrue(rows.size > 1)
        Assert.assertTrue(rows[0] is ImageRow)
        Assert.assertTrue(rows[2] is ImageRow)
    }
    @Test
    fun test11() {
        val tree = Utils.readStoryFromResourse("story11.json")
        val rowParser = StoryParserToRows
        var rows = rowParser.parse(tree.rootStory()!!)
        Assert.assertTrue(rows.size > 1)
        Assert.assertTrue(rows[1] is ImageRow)
        Assert.assertTrue((rows[1] as ImageRow).src == "http://storage6.static.itmages.ru/i/17/0924/h_1506279023_2776210_bda460a049.jpg")

        Assert.assertTrue(rows[3] is ImageRow)
        Assert.assertTrue((rows[3] as ImageRow).src == "http://storage5.static.itmages.ru/i/17/1226/h_1514289553_9188862_d656df349b.jpg")

        Assert.assertTrue(rows[5] is ImageRow)
        Assert.assertTrue((rows[5] as ImageRow).src == "http://storage1.static.itmages.ru/i/17/1226/h_1514289332_9104582_07de855c1c.jpg")

        Assert.assertTrue(rows[7] is ImageRow)
        Assert.assertTrue((rows[3] as ImageRow).src == "http://storage1.static.itmages.ru/i/17/0923/h_1506198219_3111825_717fa89727.jpg")
    }
}
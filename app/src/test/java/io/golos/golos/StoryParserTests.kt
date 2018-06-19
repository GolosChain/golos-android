package io.golos.golos

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.golos.golos.repository.api.GolosApi
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.story.model.StoryParserToRows
import io.golos.golos.screens.story.model.TextRow
import io.golos.golos.utils.Regexps
import io.golos.golos.utils.mapper
import io.golos.golos.utils.replaceSb
import junit.framework.Assert.*
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
        Assert.assertTrue((rows[0] as TextRow).text.contains("<a href =\"https://golos.io/@Booster\">@Booster</a>"))

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
        Assert.assertTrue(text.contains("<a href =\"https://golos.io/@kotbegemot\">@kotbegemot</a>"))

        val marinaComment = tree.getFlataned().find { it.story.author == "marina" }!!
        rows = rowParser.parse(marinaComment.story)
        Assert.assertTrue((rows[0] as TextRow).text.contains("<a href =\"https://golos.io/@arcange\">@arcange</a>"))
        Assert.assertTrue((rows[0] as TextRow).text.contains("<a href=\"https://golos.io/ru--golos/@golos/anons-khf-0-2-aka-17-18-29-05-2017\">https://golos.io/ru--golos/@golos/anons-khf-0-2-aka-17-18-29-05-2017</a>"))

        val kitComments = tree.getFlataned().find { it.story.author == "dobryj.kit" }!!
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
        Assert.assertTrue((rows[7] as ImageRow).src == "http://storage1.static.itmages.ru/i/17/0923/h_1506198219_3111825_717fa89727.jpg")
    }

    @Test
    fun testStories7() {
        val stories = Utils.readStoriesFromResourse("stripe7.json")
        Assert.assertTrue(stories.size > 1)
        Assert.assertTrue(stories[2].rootStory()!!.type == GolosDiscussionItem.ItemType.IMAGE_FIRST)
        Assert.assertTrue(stories[3].rootStory()!!.type == GolosDiscussionItem.ItemType.IMAGE_FIRST)

    }

    @Test
    fun testStory8() {
        val stories = Utils.readStoriesFromResourse("story_two_lines.json").first()
        val rowParser = StoryParserToRows
        var rows = rowParser.parse(stories.rootStory()!!)
        Assert.assertTrue(rows.size == 1)
        Assert.assertTrue("parser must not swallow new lines", (rows.first() as TextRow).text.contains("<br>"))


    }

    @Test
    fun testStory9() {
        val story = Utils.readStoriesFromResourse("story13.json").first()
        val rows = StoryParserToRows.parse(story.rootStory()!!)
        assertEquals("there must be 4 photos",4,rows.size)
        rows.forEach {
            if (it is TextRow) fail("there is no text")
        }

    }

    @Test
    fun testStory10() {
        val str = "sdgsdg<br><br>sdg<br>vbsg<br>"
        val result = StringBuilder(str)
        result.replaceSb("<br>".toRegex(), { "\n" })
        assertTrue(!result.toString().contains("<br>"))
    }


    @Test
    fun nicksParseTest() {
        val subscribers = GolosApi.get.getSubscribers("arcange", null)
        println(subscribers.size)
        subscribers
                .map { "@${it.follower.name}" }
                .forEach {
                    if (!it.matches(Regexps.userRegexp)) fail("regexps not matched $it")
                }
    }

    @Test
    fun testSerialization() {
        val list = listOf(TestDataA("test"), TestDataB(100L), TestDataA("test1"))

        val string = jacksonObjectMapper().writeValueAsString(list)

        val type = mapper.typeFactory.constructCollectionType(List::class.java, TestData::class.java)
        val out = jacksonObjectMapper().readValue<List<TestData>>(string, type)

        assertEquals(out[0], TestDataA("test"))
        assertEquals(out[1], TestDataB(100L))
        assertEquals(out[2], TestDataA("test1"))
        println(string)
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(value = [(JsonSubTypes.Type(value = TestDataA::class, name = "objectOfTypeA")),
        (JsonSubTypes.Type(value = TestDataB::class, name = "objectOfTypeB"))]
    )
    abstract class TestData

    data class TestDataA(val stringProperty: String) : TestData() {
        val type = "objectOfTypeA"
    }

    data class TestDataB(val longProperty: Long) : TestData() {
        val type = "objectOfTypeB"
    }
}


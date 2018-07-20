package io.golos.golos.screens.editor

import android.text.Html
import android.text.Spannable
import io.golos.golos.screens.editor.knife.KnifeBulletSpan
import io.golos.golos.screens.editor.knife.KnifeParser
import io.golos.golos.screens.editor.knife.NumberedMarginSpan
import io.golos.golos.screens.story.model.StoryParserToRows
import junit.framework.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UtilsTest {

    @Test
    fun testInTheWord() {
        Assert.assertFalse(" a".asSpannable().isWithinWord(0))
        Assert.assertTrue(" ab".asSpannable().isWithinWord(1))
        Assert.assertTrue(" ab".asSpannable().isWithinWord(2))
        Assert.assertFalse(" ab".asSpannable().isWithinWord(3))
        Assert.assertFalse(" ab ".asSpannable().isWithinWord(4))
        Assert.assertFalse("dfgdfg".asSpannable().isWithinWord(6))
    }

    @Test
    fun testGetStartOfWord() {
        Assert.assertEquals(-1, " a ".asSpannable().getStartOfWord(0))
        Assert.assertEquals(1, " ab ".asSpannable().getStartOfWord(1))
        Assert.assertEquals(1, " ab ".asSpannable().getStartOfWord(2))
        Assert.assertEquals(0, "ab ".asSpannable().getStartOfWord(1))
        Assert.assertEquals(0, "abfgjfgj".asSpannable().getStartOfWord(3))
        Assert.assertEquals(4, "123 456".asSpannable().getStartOfWord(4))
        Assert.assertEquals(4, "123 456".asSpannable().getStartOfWord(5))
        Assert.assertEquals(4, "123 456".asSpannable().getStartOfWord(6))
        Assert.assertEquals(1, " \"er\"".asSpannable().getStartOfWord(4))
    }

    @Test
    fun testGetEndOfWord() {
        println("is part of word = ${'"'.isPartOfWord()}")
        Assert.assertEquals(-1, " a ".asSpannable().getEndOfWord(0))
        Assert.assertEquals(2, " ab ".asSpannable().getEndOfWord(1))
        Assert.assertEquals(2, " ab ".asSpannable().getEndOfWord(2))
        Assert.assertEquals(1, "ab ".asSpannable().getEndOfWord(1))
        Assert.assertEquals("abfgjfgj".lastIndex, "abfgjfgj".asSpannable().getEndOfWord(3))
        Assert.assertEquals("123 456".lastIndex, "123 456".asSpannable().getEndOfWord(4))
        Assert.assertEquals("123 456".lastIndex, "123 456".asSpannable().getEndOfWord(5))
        Assert.assertEquals("123 456".lastIndex, "123 456".asSpannable().getEndOfWord(6))
        Assert.assertEquals("123 456 ".lastIndex - 1, "123 456 ".asSpannable().getEndOfWord(6))
        Assert.assertEquals(4, " \"er\"".asSpannable().getEndOfWord(4))
    }

    @Test
    fun getSubstring() {
        println(" \" is char = ${'"'.isPartOfWord()}")
        var spannable = "123 sdg12".asSpannable()
        var startOfWord = spannable.getStartOfWord(4)
        var endOfWord = spannable.getEndOfWord(4)
        Assert.assertEquals("sdg12", spannable.subSequence(startOfWord, endOfWord + 1).toString())

        startOfWord = spannable.getStartOfWord(0)
        endOfWord = spannable.getEndOfWord(0)
        Assert.assertEquals("123", spannable.subSequence(startOfWord, endOfWord + 1).toString())

        spannable = "sdgsdg \"123\"".asSpannable()
        startOfWord = spannable.getStartOfWord(4)
        endOfWord = spannable.getEndOfWord(4)
        Assert.assertEquals("sdgsdg", spannable.subSequence(startOfWord, endOfWord + 1).toString())

        startOfWord = spannable.getStartOfWord(10)
        endOfWord = spannable.getEndOfWord(10)
        Assert.assertEquals("\"123\"", spannable.subSequence(startOfWord, endOfWord + 1).toString())

        spannable = " \"er\"".asSpannable()
        startOfWord = spannable.getStartOfWord(4)
        endOfWord = spannable.getEndOfWord(4)
        Assert.assertEquals("\"er\"", spannable.subSequence(startOfWord, endOfWord + 1).toString())
    }

    @Test
    fun getWordPosition() {
        var spanned = "a\nb".asSpannable()
        Assert.assertEquals(0, spanned.getLineOfWordPosition(1))
        Assert.assertEquals(1, spanned.getLineOfWordPosition(2))
        spanned = "a\nbsdgsdg sdgsdg \nd".asSpannable()
        Assert.assertEquals(2, spanned.getLineOfWordPosition(17))
    }


    @Test
    fun getLineStartEndPosition() {
        var spanned = "a\nb".asSpannable()
        var result = spanned.getParagraphBounds(0)
        Assert.assertEquals(0 to 1, result)
        result = spanned.getParagraphBounds(3)
        Assert.assertEquals(2 to 3, result)
        spanned = "a\nbse\n123".asSpannable()
        result = spanned.getParagraphBounds(6)
        Assert.assertEquals(6 to 9, result)
        result = spanned.getParagraphBounds("a\nbse\n123".length)
        Assert.assertEquals(6 to 9, result)
    }

    @Test
    fun testTrim() {
        var text = " 123 ".asSpannable()
        (0 until 20).forEach { text.trimStartAndEnd() }
        Assert.assertEquals("123", text.toString())
        text = "\n 123 ".asSpannable()
        Assert.assertEquals("123", text.trimStartAndEnd().toString())
    }

    @Test
    fun testFromHtml() {
        var spanable = "12\n12".asSpannable()
        spanable.setSpan(KnifeBulletSpan(0, 0, 0), 0, 3, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        spanable.setSpan(KnifeBulletSpan(0, 0, 0), 3, 5, Spannable.SPAN_INCLUSIVE_INCLUSIVE)

        var html = KnifeParser.toHtml(spanable)

        println("html has line break = ${Html.toHtml(spanable).contains("\n")}")
        Assert.assertEquals("<ul><li>12</li><br><li>12</li><br></ul>", html)
        spanable = "12\n12\n34\n34".asSpannable()
        spanable.setSpan(KnifeBulletSpan(0, 0, 0), 0, 2, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        spanable.setSpan(KnifeBulletSpan(0, 0, 0), 3, 5, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        spanable.setSpan(NumberedMarginSpan(0, 0, 0), 6, 8, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        spanable.setSpan(NumberedMarginSpan(0, 0, 0), 9, 11, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        html = KnifeParser.toHtml(spanable)
        val parts = StoryParserToRows.parse(html)
        println(parts)
    }

    @Test
    fun testKnifeNumberedParser() {
        val text = "<br>\n" +
                "    <br>\n" +
                "    <h3>Добрый день, дорогие читатели!</h3>\n" +
                "    <br>\n" +
                "    <p>Очередной сезон самого книжного конкурса Голоса открыт.</p> \n" +
                "    <p>Участвуя в нём, вы можете поделиться своими любимыми произведениями, а читая подготовленные обзоры, найти для себя много интересных новинок, которые могут занять достойное место в вашей библиотеке.</p> \n" +
                "    <p>Традиционные слова признательности  <a href =\"https://golos.io/@el-puzo\">@el-puzo</a>, который дал мне возможность продолжить это благое начинание. Спасибо, Евгений, за отличную идею!</p> \n" +
                "    <br>\n" +
                "    <br>\n" +
                "    <h3>Даты проведения</h3>\n" +
                "    <br>\n" +
                "    <p>Конкурс проводится регулярно во вторую половину месяца.<br>Даты этого сезона: <strong>15-31 июля</strong><br>Подведение итогов: <strong>5 августа</strong></p> \n" +
                "    <br>\n" +
                "    <br>\n" +
                "    <h3>Условия участия</h3>\n" +
                "    <br>\n" +
                "    <p>В конкурсе могут принимать участие как новички, так и уже зрелые жители Голоса.</p> \n" +
                "    <p>Процедура участия следующая. Вы заявляете о своей готовности комментарием к этому посту и получаете букву, а с ней и возможность поделиться пятью книгами, которые затронули ваше сердце и повлияли на вашу жизнь.</p> \n" +
                "    <br>\n" +
                "    <br>\n" +
                "    <h3>Награды победителям</h3>\n" +
                "    <br>\n" +
                "    <p>Призовой фонд — 100 Голосов</p> \n" +
                "    <p>1-е место — 50 Голосов<br>2-е место — 30 Голосов<br>3-е место — 20 Голосов</p> \n" +
                "    <p><strong>Спонсор этого сезона:</strong>  <a href =\"https://golos.io/@amidabudda\">@amidabudda</a></p> \n" +
                "    <p>Определение призёров доверено судейской коллегии.<br><strong>Жюри:</strong>  <a href =\"https://golos.io/@smotritelmayaka\">@smotritelmayaka</a>,  <a href =\"https://golos.io/@amidabudda\">@amidabudda</a> и Ян Бадевский,  <a href =\"https://golos.io/@zaebooka\">@zaebooka</a></p> \n" +
                "    <br>\n" +
                "    <br>\n" +
                "    <h3>Оформление конкурсных постов</h3>\n" +
                "    <br>\n" +
                "    <ol>\n" +
                "     <br>\n" +
                "     <li><br><p>Обязательный тег — пятькнигнабукву. Остальные по желанию.</p> </li>\n" +
                "     <br>\n" +
                "     <li><br><p>В вашем обзоре должна быть ссылка на правила участия.</p> </li>\n" +
                "     <br>\n" +
                "     <li><br><p>Отправляйте ссылку на вашу работу комментарием к этому посту.</p> </li>\n" +
                "     <br>\n" +
                "    </ol>\n" +
                "    <br>\n" +
                "    <br>\n" +
                "    <br>\n" +
                "    <p>Репост правил необязателен. Если сделаете, нам будет приятно)</p> \n" +
                "    <br>\n" +
                "    <p><strong>Приглашаю всех авторов Голоса собрать свой алфавит любимых книг!</strong></p> \n" +
                "    <p>С уважением,<br>ваша  <a href =\"https://golos.io/@amidabudda\">@amidabudda</a></p>"
        val out = KnifeParser.fromHtml(text)
    }
}
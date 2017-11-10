package io.golos.golos.screens.story.model

import io.golos.golos.screens.main_stripes.model.Format
import io.golos.golos.utils.ImageVisitor
import io.golos.golos.utils.TextVisitor
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode

/**
 * Created by yuri on 08.11.17.
 */
data class StoryTextPart(val text: String) : Row()

data class StoryImagePart(val src: String) : Row()

sealed class Row

class StoryParserToRows {
    private val allHtml = Regex("(<([^>]+)>)")
    private val anyImageLink = Regex("https?:[^)]+\\.(?:jpg|jpeg|gif|png)")
    private val imgRegexp = Regex("<img.*>")
    private val markdownChecker = Regex("\\[[^]]+\\]\\(https?:\\/\\/\\S+\\)")
    private val trashTags = Regex("<p>|</p>|<b>|</b>|\\n|<br>|</br>|&nbsp;")
    fun parse(story: Comment): List<Row> {

        val out = ArrayList<Row>()
        if (story.body.isEmpty()) return out
        var str = story.body
        if (story.format == Format.MARKDOWN || str.contains(markdownChecker)) {
            var node = Parser.builder().build().parse(str)
            node?.accept(ImageVisitor())
            node?.accept(TextVisitor())
            str = HtmlRenderer.builder().build().render(node)
        }
        try {
            val doc = Jsoup.parse(str)
            var ets = doc.body().children()
            var body = doc.body()
            if (ets.size == 1 && ets[0].children().html().isNotEmpty()) {
                ets = ets[0].children()
            }
            if (ets.size == 0) {
                ets = doc.body().children()
            }
            if (ets.size == 0) {
                val nodes = doc.body().childNodes()
                if (nodes.size != 0) {
                    nodes.forEach({
                        out.add(StoryTextPart(it.toString()))
                    })
                    return out
                }
            }
            if (ets.size == 0) {
                println("parser fail on $story")
                out.add(StoryTextPart(str))
                return out
            }
            ets!!.forEach {
                if (!(it is TextNode && it.text().isEmpty()) && !it.html().replace(trashTags, "").isEmpty()) {
                    if (it.hasAttr("src")) {
                        out.add(StoryImagePart(it.attr("src")))
                    } else if (it.children().hasAttr("src")) {
                        out.add(StoryImagePart(it.children().attr("src")))
                    } else {
                        out.add(StoryTextPart(it.html()))
                    }
                }

            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return out
    }
}
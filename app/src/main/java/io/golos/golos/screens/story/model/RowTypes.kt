package io.golos.golos.screens.story.model

import io.golos.golos.utils.ImageVisitor
import io.golos.golos.utils.Regexps
import io.golos.golos.utils.TextVisitor
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

data class TextRow(val text: String) : Row()

data class ImageRow(val src: String) : Row()

sealed class Row

class StoryParserToRows {
    private val allHtml = Regex("(<([^>]+)>)")
    private val anyImageLink = Regex("https?:[^)]+\\.(?:jpg|jpeg|gif|png)")
    private val imgRegexp = Regex("<img.*>")
    private val markdownChecker = Regex("\\[[^]]+\\]\\(https?:\\/\\/\\S+\\)")
    private val trashTags = Regex("<p>|</p>|<b>|</b>|\\n|<br>|</br>|&nbsp;")
    private var list: List<Element>? = null
    fun parse(story: GolosDiscussionItem): List<Row> {

        val out = ArrayList<Row>()
        if (story.body.isEmpty()) return out
        var str = story.body
        if (story.format == Format.MARKDOWN || str.contains(markdownChecker)) {
            str = str.replace(Regexps.imageWithWhitespace) {
                "![](${it.value.trim()})"
            }
            str = str.replace(Regexps.linkWithWhiteSpace) {
                "[](${it.value.trim()})"
            }
            var node = Parser.builder().build().parse(str)
            str = HtmlRenderer.builder().build().render(node)
        }
        try {
            str = str.replace("\\s@[a-zA-Z]{5,16}[0-9]{0,6}".toRegex()) {
                "<a href = \"https://golos.blog/${it.value.trim()}\"> ${it.value.trim()}</a>"
            }
            str = str.replace(Regexps.imageWithWhitespace) {
                "<img src = \"${it.value.trim()}\">"
            }
            str = str.replace(Regexps.imageWithCenterTage) {
                "<img src = \"${it.value
                        .trim()
                        .replace("<center>", "")
                        .replace("</center>", "")}\">"
            }
            str = str.replace(Regexps.linkWithWhiteSpace) {
                "<a href = \"${it.value.trim()}\"> ${it.value.trim()}</a>"
            }
            val doc = Jsoup.parse(str)
            var ets = doc.body().children()

            var body = doc.body()
            list = listChildren(body)
            if (ets.size == 1 && ets[0].children().html().isNotEmpty()) {
                if (ets[0].ownText().isNotEmpty()) out.add(TextRow(ets[0].ownText()))
                ets = ets[0].children()


            }

            if (ets.size == 0) {
                ets = doc.body().children()
            }
            if (ets.size == 0) {
                val nodes = doc.body().childNodes()
                if (nodes.size != 0) {
                    nodes.forEach({
                        out.add(TextRow(it.toString()))
                    })
                    return out
                }
            }
            if (ets.size == 0) {
                println("parser fail on $story")
                out.add(TextRow(str))
                return out
            }
            if (ets!!.size > 0) {
                ets!!.forEach {
                    if (it.hasAttr("src")) {
                        out.add(ImageRow(it.attr("src")))
                    } else if (it.children().hasAttr("src")) {
                        out.add(ImageRow(it.children().attr("src")))
                    } else if (it.html().contains(anyImageLink)) {
                        out.add(ImageRow(it.html()))
                    } else if (it.html().isNotEmpty()) {
                        out.add(TextRow(it.html()))
                    }
                }
            } else {
                list!!.forEach {
                    if (!it.html().replace(trashTags, "").isEmpty()) {
                        if (it.hasAttr("src")) {
                            out.add(ImageRow(it.attr("src")))
                        } else if (it.children().hasAttr("src")) {
                            out.add(ImageRow(it.children().attr("src")))
                        } else if (it.html().isNotEmpty()) {
                            out.add(TextRow(it.html()))
                        }
                    }
                }
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
        return out
    }

    private fun listChildren(element: Element): List<Element> {
        if (element.children().count() == 0) return ArrayList()
        val out = ArrayList<Element>()
        out.addAll(element.children().flatMap { listOf(it) + listChildren(it) })
        return out
    }

    private fun listLastChildren(element: Element): List<Element> {
        //   val ownText = element.ownText()

        if (element.children().count() == 0) return listOf(element)
        // if (element.children().count() == 1) return listOf(element.child(0))

        val out = ArrayList<Element>()
        out.addAll(element.children().flatMap { listLastChildren(it) })
        return out
    }
}

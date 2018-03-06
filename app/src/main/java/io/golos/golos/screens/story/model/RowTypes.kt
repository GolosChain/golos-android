package io.golos.golos.screens.story.model

import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.utils.Regexps
import io.golos.golos.utils.Regexps.markdownChecker
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.safety.Whitelist

data class TextRow(val text: String) : Row() {
}

data class ImageRow(val src: String) : Row()

sealed class Row

class StoryParserToRows {
    fun parse(story: GolosDiscussionItem): List<Row> {

        val out = ArrayList<Row>()
        if (story.body.isEmpty()) return out
        var str = story.body
        str = str.replace("<center>", "").replace("</center>", "")
        if (story.format == GolosDiscussionItem.Format.MARKDOWN || str.contains(markdownChecker)) {
            str = str.replace(Regexps.imageWithWhitespace) {
                " ![](${it.value.trim()})"
            }
            str = str.replace(Regexps.linkWithWhiteSpace) {
                " [](${it.value.trim()})"
            }
            try {
                val node = Parser.builder().build().parse(str)
                str = HtmlRenderer.builder().build().render(node)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            val whiteList = Whitelist.basicWithImages()
            whiteList.addTags("h1", "h2", "h3", "h4")
            str = Jsoup.clean(str, whiteList)
            str = str.replace("(?<!/)(@[a-zA-Z.\\-]{3,16}[0-9]{0,6}){1}(?!(/))(?!(</a>))\\b".toRegex()) {
                if (!it.value.contains("a href")) " <a href =\"https://golos.io/${it.value.trim()}\">${it.value.trim()}</a>"
                else it.value
            }
            str = str.replace(Regexps.imageLinkWithoutSrctag) {
                val index = str.indexOf(it.value)
                if (index == 0 || str.get(index - 1).toString() != "\"")
                    " <img src=\"${it.value.trim()}\">"
                else it.value
            }
            str = str.replace(Regexps.imageWithCenterTage) {
                " <img src=\"${it.value
                        .trim()
                        .replace("<center>", "")
                        .replace("</center>", "")}\">"
            }
            str = str.replace(Regexps.linkWithWhiteSpace) {
                if (it.value.contains("http")) "<a href=\"${it.value.trim()}\">${it.value.trim()}</a>"
                else it.value
            }
            str = str.replace(Regexps.linkWithoutATag) {
                val value = it.value.trim().replace(Regexps.trashTags, "")
                " <a href=\"$value\">$value</a>"
            }
            str = str.replace("&nbsp;", "")
            var strings = str.split(Regex("<img.+?>"))
            val stringsCleaned = ArrayList<String>()
            val images = Regexps.imageExtract.findAll(str)
            val iter = images.iterator()
            val imagesList = ArrayList<String>()
            iter.forEach {
                if (it.groupValues.size == 1) imagesList.add(it.groupValues[0])
                else if (it.groupValues.size == 2) imagesList.add(it.groupValues[1])
            }
            var isFirstImage = false
            stringsCleaned.add(strings[0])
            (1 until strings.size).forEach {
                if (!strings[it].isEmpty() && !strings[it].matches(Regexps.trashTags)) {
                    stringsCleaned.add(strings[it])
                }
            }
            strings = stringsCleaned
            (0 until strings.size)
                    .forEach {
                        if (it == 0) {
                            if ((strings[0].isEmpty() || strings[0].matches(Regexps.trashTags)) && imagesList.isNotEmpty()) {
                                out.add(ImageRow(imagesList[0]))
                                isFirstImage = true
                            } else {
                                out.add(TextRow(strings[0]))
                                if (imagesList.lastIndex >= it) {
                                    out.add(ImageRow(imagesList[it]))
                                }
                            }
                        } else {
                            if (!isFirstImage) {
                                out.add(TextRow(strings[it]))
                                if (imagesList.lastIndex >= it) {
                                    out.add(ImageRow(imagesList[it]))
                                }
                            } else {
                                out.add(TextRow(strings[it]))
                                if (imagesList.lastIndex >= it) {
                                    out.add(ImageRow(imagesList[it]))
                                }
                            }
                        }
                    }
            /* val doc = Jsoup.parse(str)
             var ets = doc.body().children()

             var body = doc.body()
             var list = listChildren(body)
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
                 ets.forEach {
                     if (it.hasAttr("src")) {
                         out.add(ImageRow(it.attr("src")))
                     } else if (it.children().hasAttr("src") && it.children().size == 1) {
                         out.add(ImageRow(it.children().attr("src")))
                     } else if (it.html().isNotEmpty()) {
                         var htmlString = it.html()
                         if (htmlString.matches(Regexps.link)) {
                             htmlString = htmlString.replace(Regexps.link) {
                                 "<a href =\"${it.value.trim()}\">${it.value.trim()}</a>"
                             }
                         }
                         out.add(TextRow(htmlString))
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
             }*/


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

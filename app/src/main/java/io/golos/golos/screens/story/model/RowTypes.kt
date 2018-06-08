package io.golos.golos.screens.story.model

import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.utils.Regexps
import io.golos.golos.utils.Regexps.markdownChecker
import io.golos.golos.utils.removeString
import io.golos.golos.utils.replaceSb
import io.golos.golos.utils.toHtml
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist
import java.util.*

data class TextRow(val text: String) : Row() {
}

data class ImageRow(val src: String) : Row()

sealed class Row

object StoryParserToRows {
    private const val centerStart = "<center>"
    private const val centerEnd = "</center>"
    private const val nbsp = "&nbsp;"
    private val newLine = "\n".toRegex()
    private val newLineReplaserRegexp = "$#$".toRegex()
    private val newLineReplaser = "$#$"


    fun parse(story: GolosDiscussionItem,
              checkEmptyHtml: Boolean = false,
              skipHtmlClean: Boolean = false): List<Row> {

        val out = ArrayList<Row>()
        if (story.body.isEmpty()) return out
        if (story.body.matches(Regexps.anyImageLink)) return Collections.singletonList(ImageRow(story.body))

        val str = StringBuilder(story.body)


        str.removeString(centerStart).removeString(centerEnd)

        if (story.format == GolosDiscussionItem.Format.MARKDOWN || str.contains(markdownChecker)) {
            str.replaceSb(Regexps.imageWithWhitespace) {
                " ![](${it.value.trim()})"
            }
            str.replaceSb(Regexps.linkWithWhiteSpace) {
                " [](${it.value.trim()})"
            }
            if (!skipHtmlClean) {
                try {
                    val node = Parser.builder().build().parse(str.toString())
                    str.setLength(0)
                    str.append(HtmlRenderer.builder().build().render(node))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }
        try {
            if (!skipHtmlClean) {
                str.replaceSb("(\n)+".toRegex()) {
                    //workaround, for jsoup cleaner deleting \n
                    "<br>"
                }

                val whiteList = Whitelist.basicWithImages()
                whiteList.addTags("h1", "h2", "h3", "h4")
                try {
                    var tempStr = str.toString()
                    str.setLength(0)
                    str.append(Jsoup.clean(tempStr, whiteList))
                } catch (e: Exception) {
                    e.printStackTrace()
                }

              /* str.replaceSb("#!!".toRegex()) {
                    "\n"
                }*/
            }

            if (!skipHtmlClean) {
                str.replaceSb(Regexps.userRegexp) {
                    if (!it.value.contains("a href")) " <a href =\"https://golos.io/${it.value.trim()}\">${it.value.trim()}</a>"
                    else it.value
                }
            }

            str.replaceSb(Regexps.imageLinkWithoutSrctag) {
                val index = str.indexOf(it.value)
                if (index == 0 || str.get(index - 1).toString() != "\"")
                    " <img src=\"${it.value.trim()}\">"
                else it.value
            }

            str.replaceSb(Regexps.imageWithCenterTage) {
                " <img src=\"${it.value
                        .trim()

                        .replace("<center>", "")
                        .replace("</center>", "")}\">"
            }

            str.replaceSb(Regexps.linkWithWhiteSpace) {
                if (it.value.contains("http")) "<a href=\"${it.value.trim()}\">${it.value.trim()}</a>"
                else it.value
            }
            str.replaceSb(Regexps.linkWithoutATag) {
                val value = it.value.trim().replace(Regexps.trashTags, "")
                " <a href=\"$value\">$value</a>"
            }

            str.removeString(nbsp)
            str.replace("(<br>)+".toRegex(),"<br>")
            if (str.startsWith("<br>"))str.delete(0,5)

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
            stringsCleaned.add(strings[0])/*.replace(("(\n)+".toRegex()), "<br>"))*/
            (1 until strings.size).forEach {
                if (!strings[it].isEmpty() && !strings[it].matches(Regexps.trashTags)) {
                    stringsCleaned.add(strings[it])/*.replace(("(\n)+".toRegex()), "<br>")*/
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


        } catch (e: Exception) {
            e.printStackTrace()
        }

        return if (checkEmptyHtml) out.filter {
            it is ImageRow || (it is TextRow && it.text.toHtml().isNotEmpty())
        } else
            out

    }

}

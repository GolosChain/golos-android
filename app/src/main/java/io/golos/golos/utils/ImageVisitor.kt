package io.golos.golos.utils

import org.commonmark.node.AbstractVisitor
import org.commonmark.node.HtmlBlock
import org.commonmark.node.Text
import timber.log.Timber

/**
 * Created by yuri on 07.11.17.
 */
class ImageVisitor : AbstractVisitor() {
    var out: String? = null
    override fun visit(htmlBlock: HtmlBlock?) {
        super.visit(htmlBlock)
        htmlBlock?.let {
            var string = htmlBlock.literal.replace(allHtml, "")
            if (string.matches(anyImageLink) && !string.contains("src")) {
                string = string.replace(allHtml, "")
                htmlBlock.literal = "<img src=$string/>"
            } else if (htmlBlock.literal.contains(anyImageLink)) {
                var string = htmlBlock.literal
                val result = anyImageLink.find(string, 0)
                result?.let {
                    if (it.groupValues.isNotEmpty()) {
                        string = string.replace(anyImageLink, "<img src=${it.groupValues[0]}/>")
                        htmlBlock.literal = string
                    }
                }
            }
        }
    }

    companion object {
        private val anyImageLink = Regex("https?:[^)]+\\.(?:jpg|jpeg|gif|png)")
        private val allHtml = Regex("(<([^>]+)>)")
    }

}

class TextVisitor : AbstractVisitor() {
    var out: String? = null

    override fun visit(textBlock: Text?) {
        super.visit(textBlock)
        textBlock?.let {
            var string = textBlock.literal.replace(allHtml, "")
            if (string.matches(anyImageLink) && !string.contains("src")) {
                string = string.replace(allHtml, "")
                textBlock.literal = ""//""<img src=\"$string\"/>"
                val htmlblock = HtmlBlock()
                htmlblock.literal = "<img src=\"$string\"/>"
                textBlock.parent.prependChild(htmlblock)
            }
        }
    }

    companion object {
        private val anyImageLink = Regex("https?:[^)]+\\.(?:jpg|jpeg|gif|png)")
        private val allHtml = Regex("(<([^>]+)>)")
    }
}

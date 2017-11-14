package io.golos.golos.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.commonmark.parser.Parser

object Regexps {
    val markdownImageRegexpAndContents = Regex("!\\[.*]\\(.*\\)|^(?:([^:/?#]+):)?(?:([^/?#]*))?([^?#]*\\.(?:jpg|gif|png))(?:\\?([^#]*))?(?:#(.*))?")
    val markdownImageRegexp = Regex("\\[.*]\\(.*\\)")
    val htmlImageRegexp = Regex("<.*><img.*>")
    val allHtml = Regex("(<([^>]+)>)")
    val anyImageLink = Regex("https?:[^)]+\\.(?:jpg|jpeg|gif|png)")
    val gramsInUntion = 31.1034768
    val parser = Parser.builder().build()
    val mapper = jacksonObjectMapper()
}
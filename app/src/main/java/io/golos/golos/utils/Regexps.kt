package io.golos.golos.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.commonmark.parser.Parser

object Regexps {
    val markdownImageRegexpAndContents = Regex("!\\[.*]\\(.*\\)|^(?:([^:/?#]+):)?(?:([^/?#]*))?([^?#]*\\.(?:jpg|gif|png))(?:\\?([^#]*))?(?:#(.*))?")
    val markdownImageRegexp = Regex("\\[.*]\\(.*\\)")
    val htmlImageRegexp = Regex("<.*><img.*>")
    val allHtml = Regex("(<([^>]+)>)")
    val anyImageLink = Regex("(https||http)?://[^/\\s]+/\\S+\\.(jpg|png|gif)")
    val imageWithWhitespace= Regex("(https||http)?://[^/\\s]+/\\S+\\.(jpg|png|gif)\\s")
    val imageWithCenterTage= Regex("<center>(https||http)?://[^/\\s]+/\\S+\\.(jpg|png|gif)</center>")
    val linkWithWhiteSpace= Regex("(https://||http://)?[a-zA-Z_0-9\\-]+(\\.\\w[a-zA-Z_0-9\\-]+)+(/[#&\\n\\-=?\\+\\%/\\.\\w]+)?\\s")
    val gramsInUntion = 31.1034768
    val parser = Parser.builder().build()
    val mapper = jacksonObjectMapper()
}
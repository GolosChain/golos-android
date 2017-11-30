package io.golos.golos.utils

object Regexps {
    val markdownImageRegexpAndContents = Regex("!\\[.*]\\(.*\\)|^(?:([^:/?#]+):)?(?:([^/?#]*))?([^?#]*\\.(?:jpg|gif|png))(?:\\?([^#]*))?(?:#(.*))?")
    val markdownImageRegexp = Regex("\\[.*]\\(.*\\)")
    val htmlImageRegexp = Regex("<.*><img.*>")
    val allHtml = Regex("(<([^>]+)>)")
    val anyImageLink = Regex("(https||http)?://[^/\\s]+/\\S+\\.(jpg|png|gif)")
    val imageWithWhitespace = Regex("(https||http)?://[^/\\s]+/\\S+\\.(jpg|png|gif)\\s")
    val imageWithCenterTage = Regex("<center>(https||http)?://[^/\\s]+/\\S+\\.(jpg|png|gif)</center>")
    val imageLinkWithoutSrctag = Regex("(https||http)?://[^/\\s]+/\\S+\\.(jpg|png|gif)[^\"]")
    val linkWithWhiteSpace = Regex("(https://||http://)?[a-zA-Z_0-9\\-]+(\\.\\w[a-zA-Z_0-9\\-]+)+(/[#&\\n\\-=?\\+\\%/\\.\\w@a]+)?\\s")
    val linkWithoutATag = Regex("<.?>(https://||http://)?[a-zA-Z_0-9\\-]+(\\.\\w[a-zA-Z_0-9\\-]+)+(/[#&\\n\\-=?\\+\\%/\\.\\w@a]+)?<.+?>")
    val link = Regex("(https://||http://)?[a-zA-Z_0-9\\-]+(\\.\\w[a-zA-Z_0-9\\-]+)+(/[#&\\n\\-=?\\+\\%/\\.\\w@a]+)")
    val imageExtract = Regex("<img\\s.*?src=(?:'|\")([^'\">]+)(?:'|\")")
    val markdownChecker = Regex("\\[[^]]+\\]\\(https?:\\/\\/\\S+\\)")
    val gramsInUntion = 31.1034768
    val trashTags = Regex("<p>|</p>|<b>|</b>|\\n|<br>|</br>|&nbsp;")
}
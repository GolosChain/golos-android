package io.golos.golos.utils

object Regexps {
    val markdownImageRegexpAndContents = Regex("!\\[.*]\\(.*\\)|^(?:([^:/?#]+):)?(?:([^/?#]*))?([^?#]*\\.(?:jpg|gif|png))(?:\\?([^#]*))?(?:#(.*))?")
    val markdownImageRegexp = Regex("\\[.*]\\(.*\\)")
    val htmlImageRegexp = Regex("<.*><img.*>")
    val allHtml = Regex("(<([^>]+)>)")
    val anyImageLink = Regex("(https||http)?://[^/\\s]+/\\S+\\.(jpg|png|gif|jpeg)")
    val imageWithWhitespace = Regex("(https||http)?://[^/\\s]+/\\S+\\.(jpg|png|gif|jpeg)\\s")
    val imageWithCenterTage = Regex("<center>(https||http)?://[^/\\s]+/\\S+\\.(jpg|png|gif|jpeg)</center>")
    val imageLinkWithoutSrctag = Regex("(https||http)?://[^/\\s]+/\\S+\\.(jpg|png|gif|jpeg)")
    val linkWithWhiteSpace = Regex("(https://||http://)?[a-zA-Z_0-9\\-]+(\\.\\w[a-zA-Z_0-9\\-]+)+(/[#&\\n\\-=?\\+\\%/\\.\\w@a]+)?\\s")
    val linkWithoutATag = Regex("<.?>(https://||http://)?[a-zA-Z_0-9\\-]+(\\.\\w[a-zA-Z_0-9\\-]+)+(/[#&\\n\\-=?\\+\\%/\\.\\w@a]+)?<.+?>")
    val link = Regex("(https://||http://)?[a-zA-Z_0-9\\-]+(\\.\\w[a-zA-Z_0-9\\-]+)+(/[#&\\n\\-=?\\+\\%/\\.\\w@a]+)")
    val imageExtract = Regex("<img\\s.*?src=(?:'|\")([^'\">]+)(?:'|\")")
    val markdownChecker = Regex("\\[[^]]+\\]\\(https?:\\/\\/\\S+\\)")
    val gramsInUntion = 31.1034768
    val trashTags = Regex("<p>|</p>|<b>|</b>|\\n|<br>|</br>|&nbsp;")
    val userRegexp = Regex("@([a-zA-Z.\\-]{3,16}[0-9]{0,6})")
    val storyLink = Regex("([a-z0-9\\-]+)/@([a-zA-Z.\\-]{3,16}[0-9]{0,6})/([a-z0-9\\-]+)")
    val storyLinkNoBlog = Regex("@([a-zA-Z.\\-]{3,16}[0-9]{0,6})/([a-z0-9-]+)[/]?")
    val linkToGolosBoard = Regex("https://imgp.golos.io/.+/http://golosboard.com/.+/.+.png")
    val wrongTagRegexp = Regex("(u\\w{1,4}){6,}")
    val imgpFindRegexp = Regex("https://imgp.golos.io/.*(?=http)")

}
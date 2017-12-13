package io.golos.golos.utils

/**
 * Created by yuri on 12.12.17.
 */
object GolosLinkMatcher {
    fun match(url: String): MatchResult {
        if (url.startsWith("https://golos.io/")
                || url.startsWith("https://golos.blog/")
                ) {
            val link = url.replace("https://golos.io/", "")
                    .replace("https://golos.blog/", "")
            if (link.matches(Regexps.storyLink)) {//its link
                val parts = link.split("/")
                if (parts.size == 3) {
                    return StoryLinkMatch(parts[1].substring(1), parts[0], parts[2])
                }
            }
        } else if (url.startsWith("https://goldvoice.club/")) {//https://goldvoice.club/@sinte/o-socialnykh-psikhopatakh-chast-3-o-tikhonyakh-mechtatelyakh-stesnitelnykh/
            val link = url.replace("https://goldvoice.club/", "")
            if (link.matches(Regexps.storyLinkNoBlog)) {
                val parts = link.split("/")
                if (parts.size == 2 || parts.size == 3) {
                    return StoryLinkMatch(parts[0].substring(1), null, parts[1])
                }
            }
        }
        return NoMatch()
    }
}

data class StoryLinkMatch(val author: String,
                          val blog: String?,
                          val permlink: String) : MatchResult()

class NoMatch : MatchResult()

sealed class MatchResult
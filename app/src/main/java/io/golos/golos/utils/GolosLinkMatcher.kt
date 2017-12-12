package io.golos.golos.utils

/**
 * Created by yuri on 12.12.17.
 */
object GolosLinkMatcher {
    fun match(url: String): MatchResult {
        if (url.startsWith("https://golos.io/") || url.startsWith("https://golos.blog/")) {
            val link = url.replace("https://golos.io/", "").replace("https://golos.blog/", "")
            if (link.matches(Regexps.storyLink)) {//its link
                val parts = link.split("/")
                if (parts.size == 3) {
                    return StoryLinkMatch(parts[1].substring(1), parts[0], parts[2])
                }
            }
        }
        return NoMatch()
    }
}

class StoryLinkMatch(val author: String,
                     val blog: String,
                     val permlink: String) : MatchResult()

class NoMatch : MatchResult()

sealed class MatchResult
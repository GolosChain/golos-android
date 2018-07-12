package io.golos.golos.utils

/**
 * Created by yuri on 12.12.17.
 */
object GolosLinkMatcher {
    fun match(url: String): MatchResult {
        if (allGolosUrls().find {
                    url.startsWith(it)
                } != null) {
            var link = url
            allGolosUrls().forEach {
                link = link.removePrefix(it)
            }
            if (link.matches(Regexps.storyLink)) {//it is link a story
                val parts = link.split("/")
                if (parts.size == 3) {
                    return StoryLinkMatch(parts[1].substring(1), parts[0], parts[2])
                }
            } else if (link.matches(Regexps.userRegexp)) {//it is link a user profile
                return UserLinkMatch(link.substring(1).toLowerCase())
            }
        } else if (url.startsWith("https://goldvoice.club/")) {//https://goldvoice.club/@sinte/o-socialnykh-psikhopatakh-chast-3-o-tikhonyakh-mechtatelyakh-stesnitelnykh/
            val link = url.replace("https://goldvoice.club/", "")
            if (link.matches(Regexps.storyLinkNoBlog)) {
                val parts = link.split("/")
                if (parts.size == 2 || parts.size == 3) {
                    return StoryLinkMatch(parts[0].substring(1), null, parts[1])
                }
            } else if (link.matches(Regexps.userRegexp)) {
                return UserLinkMatch(link.substring(1).toLowerCase())
            }
        }
        return NoMatch()
    }


}

data class StoryLinkMatch(val author: String,
                          val blog: String?,
                          val permlink: String) : MatchResult()

data class UserLinkMatch(val user: String) : MatchResult()

class NoMatch : MatchResult()

sealed class MatchResult
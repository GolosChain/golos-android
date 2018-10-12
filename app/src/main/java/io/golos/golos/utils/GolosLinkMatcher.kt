package io.golos.golos.utils

import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.utils.Regexps.feedRegexp
import timber.log.Timber

/**
 * Created by yuri on 12.12.17.
 */
object GolosLinkMatcher {
    fun match(url: String): GolosMatchResult {
        if (allGolosUrls().find {
                    url.startsWith(it)
                } != null) {
            var link = url
            allGolosUrls().forEach {
                link = link.removePrefix(it).replace("app/", "")
            }
            if (link.matches(Regexps.storyLink)) {//it is link a rootWrapper
                val parts = link.split("/")
                if (parts.size == 3) {
                    return StoryLinkMatch(parts[1].substring(1), parts[0], parts[2])
                }
            } else if (link.matches(Regexps.userRegexp)) {//it is link a user profile
                return UserLinkMatch(link.substring(1).toLowerCase())
            } else if (link.matches(feedRegexp)) {
                val feedType = fromString(link) ?: return NoMatch()
                return FeedMatch(feedType)
            } else if (link.matches(Regexps.categoryRegexp)) {
                val items = link.split("/")
                if (items.size == 2) {
                    return CategoryMatch(items[1], fromString(items[0]))
                }
            } else if (link.matches(Regexps.userFeedLink)) {
                val sts = link.split("/")
                if (sts.size == 2) {
                    return UserProfileSectionMatch(sts[0], fromString(sts[1]))
                }
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
                          val permlink: String) : GolosMatchResult()

data class UserLinkMatch(val user: String) : GolosMatchResult()

data class UserProfileSectionMatch(val user: String, val feedType: FeedType?) : GolosMatchResult()

data class FeedMatch(val feedType: FeedType) : GolosMatchResult()
data class CategoryMatch(val category: String,
                         val feedType: FeedType?) : GolosMatchResult()

class NoMatch : GolosMatchResult()

sealed class GolosMatchResult
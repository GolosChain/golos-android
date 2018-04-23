package io.golos.golos.repository.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class NotificationsDisplaySetting(
        @JsonProperty("playSoundWhenAppStopped")
        val playSoundWhenAppStopped: Boolean,
        @JsonProperty("showUpvoteNotifs")
        val showUpvoteNotifs: Boolean,
        @JsonProperty("showNewCommentNotifs")
        val showNewCommentNotifs: Boolean,
        @JsonProperty("showTransferNotifs")
        val showTransferNotifs: Boolean) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NotificationsDisplaySetting) return false

        if (playSoundWhenAppStopped != other.playSoundWhenAppStopped) return false
        if (showUpvoteNotifs != other.showUpvoteNotifs) return false
        if (showNewCommentNotifs != other.showNewCommentNotifs) return false
        if (showTransferNotifs != other.showTransferNotifs) return false

        return true
    }



    fun setPLaySound(play: Boolean) = if (play == playSoundWhenAppStopped)
        this else NotificationsDisplaySetting(play, showUpvoteNotifs, showNewCommentNotifs, showTransferNotifs)

    fun setShowUpvoteNotifs(show: Boolean) = if (show == showUpvoteNotifs)
        this else NotificationsDisplaySetting(playSoundWhenAppStopped, show, showNewCommentNotifs, showTransferNotifs)

    fun setShowNewCommentNotifs(show: Boolean) = if (show == showNewCommentNotifs)
        this else NotificationsDisplaySetting(playSoundWhenAppStopped, showUpvoteNotifs, show, showTransferNotifs)

    fun setShowTransferNotifss(show: Boolean) = if (show == showTransferNotifs)
        this else NotificationsDisplaySetting(playSoundWhenAppStopped, showUpvoteNotifs, showNewCommentNotifs, show)

    override fun hashCode(): Int {
        var result = playSoundWhenAppStopped.hashCode()
        result = 31 * result + showUpvoteNotifs.hashCode()
        result = 31 * result + showNewCommentNotifs.hashCode()
        result = 31 * result + showTransferNotifs.hashCode()
        return result
    }

    override fun toString(): String {
        return "NotificationsDisplaySetting(playSoundWhenAppStopped=$playSoundWhenAppStopped, showUpvoteNotifs=$showUpvoteNotifs, showNewCommentNotifs=$showNewCommentNotifs, showTransferNotifs=$showTransferNotifs)"
    }
}
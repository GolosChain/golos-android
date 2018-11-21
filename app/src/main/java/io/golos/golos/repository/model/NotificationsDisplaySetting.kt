package io.golos.golos.repository.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class NotificationsDisplaySetting(
        @JsonProperty("playSoundWhenAppStopped")
        val playSoundWhenAppStopped: Boolean,
        @JsonProperty("vote")
        val showUpvoteNotifs: Boolean,
        @JsonProperty("flag")
        val showFlagNotifs: Boolean,
        @JsonProperty("reply")
        val showNewCommentNotifs: Boolean,
        @JsonProperty("transfer")
        val showTransferNotifs: Boolean,
        @JsonProperty("subscribe")
        val showSubscribeNotifs: Boolean,
        @JsonProperty("unsubscribe")
        val showUnSubscribeNotifs: Boolean,
        @JsonProperty("mention")
        val showMentions: Boolean,
        @JsonProperty("repost")
        val showReblog: Boolean,
        @JsonProperty("witnessVote")
        val showWitnessVote: Boolean,
        @JsonProperty("witnessCancelVote")
        val showWitnessCancelVote: Boolean,
        @JsonProperty("award")
        val showAward: Boolean,
        @JsonProperty("curatorAward")
        val showCurationAward: Boolean) {

    private fun createNew(
            playSoundWhenAppStopped: Boolean = this.playSoundWhenAppStopped,
            showUpvoteNotifs: Boolean = this.showUpvoteNotifs,
            showFlagNotifs: Boolean = this.showFlagNotifs,
            showNewCommentNotifs: Boolean = this.showNewCommentNotifs,
            showTransferNotifs: Boolean = this.showTransferNotifs,
            showSubscribeNotifs: Boolean = this.showSubscribeNotifs,
            showUnSubscribeNotifs: Boolean = this.showUnSubscribeNotifs,
            showMentions: Boolean = this.showMentions,
            showReblog: Boolean = this.showReblog,
            showWitnessVote: Boolean = this.showWitnessVote,
            showWitnessCancelVote: Boolean = this.showWitnessCancelVote,
            showAward: Boolean = this.showAward,
            showCurationAward: Boolean = this.showCurationAward): NotificationsDisplaySetting {

        return NotificationsDisplaySetting(playSoundWhenAppStopped, showUpvoteNotifs, showFlagNotifs,
                showNewCommentNotifs, showTransferNotifs, showSubscribeNotifs, showUnSubscribeNotifs,
                showMentions, showReblog, showWitnessVote, showWitnessCancelVote, showAward, showCurationAward)
    }


    fun setPLaySound(play: Boolean) = createNew(playSoundWhenAppStopped = play)
    fun setShowUpvoteNotifs(show: Boolean) = createNew(showUpvoteNotifs = show)
    fun setShowFlags(show: Boolean) = createNew(showFlagNotifs = show)
    fun setShowNewCommentNotifs(show: Boolean) = createNew(showNewCommentNotifs = show)
    fun setShowTransferNotifs(show: Boolean) = createNew(showTransferNotifs = show)
    fun setShowSubscribeNotifs(show: Boolean) = createNew(showSubscribeNotifs = show)
    fun setShowUnSubscribeNotifs(show: Boolean) = createNew(showUnSubscribeNotifs = show)
    fun setShowMentionsNotifs(show: Boolean) = createNew(showMentions = show)
    fun setShowReblogNotifs(show: Boolean) = createNew(showReblog = show)
    fun setShowWitnessVote(show: Boolean) = createNew(showWitnessVote = show)
    fun setShowAward(show: Boolean) = createNew(showAward = show)
    fun setShowCurationAward(show: Boolean) = createNew(showCurationAward = show)
    fun setShowWitnessCancelVote(show: Boolean) = createNew(showWitnessCancelVote = show)




    override fun toString(): String {
        return "NotificationsDisplaySetting(playSoundWhenAppStopped=$playSoundWhenAppStopped, vote=$showUpvoteNotifs, flag=$showFlagNotifs, reply=$showNewCommentNotifs, transfer=$showTransferNotifs, subscribe=$showSubscribeNotifs, unsubscribe=$showUnSubscribeNotifs, mention=$showMentions, repost=$showReblog, witnessVote=$showWitnessVote, witnessCancelVote=$showWitnessCancelVote)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NotificationsDisplaySetting) return false

        if (playSoundWhenAppStopped != other.playSoundWhenAppStopped) return false
        if (showUpvoteNotifs != other.showUpvoteNotifs) return false
        if (showFlagNotifs != other.showFlagNotifs) return false
        if (showNewCommentNotifs != other.showNewCommentNotifs) return false
        if (showTransferNotifs != other.showTransferNotifs) return false
        if (showSubscribeNotifs != other.showSubscribeNotifs) return false
        if (showUnSubscribeNotifs != other.showUnSubscribeNotifs) return false
        if (showMentions != other.showMentions) return false
        if (showReblog != other.showReblog) return false
        if (showWitnessVote != other.showWitnessVote) return false
        if (showWitnessCancelVote != other.showWitnessCancelVote) return false
        if (showAward != other.showAward) return false
        if (showCurationAward != other.showCurationAward) return false

        return true
    }

    override fun hashCode(): Int {
        var result = playSoundWhenAppStopped.hashCode()
        result = 31 * result + showUpvoteNotifs.hashCode()
        result = 31 * result + showFlagNotifs.hashCode()
        result = 31 * result + showNewCommentNotifs.hashCode()
        result = 31 * result + showTransferNotifs.hashCode()
        result = 31 * result + showSubscribeNotifs.hashCode()
        result = 31 * result + showUnSubscribeNotifs.hashCode()
        result = 31 * result + showMentions.hashCode()
        result = 31 * result + showReblog.hashCode()
        result = 31 * result + showWitnessVote.hashCode()
        result = 31 * result + showWitnessCancelVote.hashCode()
        result = 31 * result + showAward.hashCode()
        result = 31 * result + showCurationAward.hashCode()
        return result
    }


}
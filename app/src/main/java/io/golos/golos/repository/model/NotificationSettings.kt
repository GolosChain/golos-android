package io.golos.golos.repository.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class NotificationSettings(@JsonProperty("vote") val vote: Boolean?,
                           @JsonProperty("flag") val flag: Boolean?,
                           @JsonProperty("reply") val reply: Boolean?,
                           @JsonProperty("transfer") val transfer: Boolean?,
                           @JsonProperty("subscribe") val subscribe: Boolean?,
                           @JsonProperty("unsubscribe") val unsubscribe: Boolean?,
                           @JsonProperty("mention") val mention: Boolean?,
                           @JsonProperty("repost") val repost: Boolean?,
                           @JsonProperty("message") val message: Boolean?,
                           @JsonProperty("witnessVote") val witnessVote: Boolean?,
                           @JsonProperty("witnessCancelVote") val witnessCancelVote: Boolean?,
                           @JsonProperty("reward") val reward: Boolean?,
                           @JsonProperty("curatorReward") val curatorReward: Boolean?) {

    override fun toString(): String {
        return "NotificationSettings(vote=$vote, flag=$flag, reply=$reply, transfer=$transfer, subscribe=$subscribe, unsubscribe=$unsubscribe, mention=$mention, repost=$repost, message=$message, witnessVote=$witnessVote, witnessCancelVote=$witnessCancelVote, reward=$reward, curatorReward=$curatorReward)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotificationSettings

        if (vote != other.vote) return false
        if (flag != other.flag) return false
        if (reply != other.reply) return false
        if (transfer != other.transfer) return false
        if (subscribe != other.subscribe) return false
        if (unsubscribe != other.unsubscribe) return false
        if (mention != other.mention) return false
        if (repost != other.repost) return false
        if (message != other.message) return false
        if (witnessVote != other.witnessVote) return false
        if (witnessCancelVote != other.witnessCancelVote) return false
        if (reward != other.reward) return false
        if (curatorReward != other.curatorReward) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vote?.hashCode() ?: 0
        result = 31 * result + (flag?.hashCode() ?: 0)
        result = 31 * result + (reply?.hashCode() ?: 0)
        result = 31 * result + (transfer?.hashCode() ?: 0)
        result = 31 * result + (subscribe?.hashCode() ?: 0)
        result = 31 * result + (unsubscribe?.hashCode() ?: 0)
        result = 31 * result + (mention?.hashCode() ?: 0)
        result = 31 * result + (repost?.hashCode() ?: 0)
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + (witnessVote?.hashCode() ?: 0)
        result = 31 * result + (witnessCancelVote?.hashCode() ?: 0)
        result = 31 * result + (reward?.hashCode() ?: 0)
        result = 31 * result + (curatorReward?.hashCode() ?: 0)
        return result
    }
}
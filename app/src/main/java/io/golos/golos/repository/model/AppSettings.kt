package io.golos.golos.repository.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class AppSettings(@JsonProperty("loudNotification") val loudNotification: Boolean?,
                  @JsonProperty("feedMode") val feedMode: GolosAppSettings.FeedMode?,
                  @JsonProperty("nsfwMode") val nsfwMode: GolosAppSettings.NSFWMode?,
                  @JsonProperty("displayImagesMode") val displayImagesMode: GolosAppSettings.DisplayImagesMode?,
                  @JsonProperty("nighModeEnable") val nighModeEnable: Boolean?,
                  @JsonProperty("chosenCurrency") val chosenCurrency: GolosAppSettings.GolosCurrency?,
                  @JsonProperty("bountyDisplay") val bountyDisplay: GolosAppSettings.GolosBountyDisplay?,
                  @JsonProperty("appLanguage") val appLanguage: GolosAppSettings.GolosLanguage?,
                  @JsonProperty("defaultUpVotePower") val defaultUpVotePower: Byte?) {



    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppSettings

        if (loudNotification != other.loudNotification) return false
        if (feedMode != other.feedMode) return false
        if (nsfwMode != other.nsfwMode) return false
        if (displayImagesMode != other.displayImagesMode) return false
        if (nighModeEnable != other.nighModeEnable) return false
        if (chosenCurrency != other.chosenCurrency) return false
        if (bountyDisplay != other.bountyDisplay) return false
        if (appLanguage != other.appLanguage) return false
        if (defaultUpVotePower != other.defaultUpVotePower) return false

        return true
    }

    override fun hashCode(): Int {
        var result = loudNotification?.hashCode() ?: 0
        result = 31 * result + (feedMode?.hashCode() ?: 0)
        result = 31 * result + (nsfwMode?.hashCode() ?: 0)
        result = 31 * result + (displayImagesMode?.hashCode() ?: 0)
        result = 31 * result + (nighModeEnable?.hashCode() ?: 0)
        result = 31 * result + (chosenCurrency?.hashCode() ?: 0)
        result = 31 * result + (bountyDisplay?.hashCode() ?: 0)
        result = 31 * result + (appLanguage?.hashCode() ?: 0)
        result = 31 * result + (defaultUpVotePower ?: 0)
        return result
    }

    override fun toString(): String {
        return "AppSettings(loudNotification=$loudNotification, feedMode=$feedMode, nsfwMode=$nsfwMode, displayImagesMode=$displayImagesMode, nighModeEnable=$nighModeEnable, chosenCurrency=$chosenCurrency, bountyDisplay=$bountyDisplay, appLanguage=$appLanguage, defaultUpVotePower=$defaultUpVotePower)"
    }
}
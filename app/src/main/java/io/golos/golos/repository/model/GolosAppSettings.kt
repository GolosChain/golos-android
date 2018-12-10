package io.golos.golos.repository.model

/**
 * Created by yuri yurivladdurain@gmail.com on 14/11/2018.
 */


data class GolosAppSettings(val loudNotification: Boolean,
                            val feedMode: FeedMode,
                            val nsfwMode: NSFWMode,
                            val displayImagesMode: DisplayImagesMode,
                            val nighModeEnable: Boolean,
                            val chosenCurrency: GolosCurrency,
                            val bountyDisplay: GolosBountyDisplay,
                            val language: GolosLanguage,
                            val defaultUpvotePower: Byte,
                            val voteForYouStory:Boolean) {

    enum class DisplayImagesMode {
        DISPLAY,
        DISPLAY_NOT
    }

    enum class FeedMode {
        COMPACT, FULL
    }

    enum class NSFWMode {
        DISPLAY, DISPLAY_NOT
    }

    enum class GolosCurrency {
        USD, RUB, GBG
    }


    enum class GolosLanguage {
        RU, EN
    }

    enum class GolosBountyDisplay {

        INTEGER, ONE_PLACE, TWO_PLACES, THREE_PLACES;

        fun formatNumber(double: Double): String {
            return when (this) {
                INTEGER -> String.format("%.0f", double)
                ONE_PLACE -> String.format("%.1f", double)
                TWO_PLACES -> String.format("%.2f", double)
                THREE_PLACES -> String.format("%.3f", double)
            }
        }
    }
}
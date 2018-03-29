package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.content.Context

/**
 * Created by yuri on 05.03.18.
 */
interface UserSettingsRepository {
    fun setStoriesCompactMode(isCompact: Boolean)
    fun isStoriesCompactMode(): LiveData<Boolean>
    fun isNSFWShow(): LiveData<Boolean>
    fun setIsNSFWShown(isShown: Boolean)
    fun setUserVotedForApp(isVotedForApp: Boolean)
    fun isUserVotedForApp(): Boolean
    fun setVoteQuestionMade(isMade: Boolean)
    fun isVoteQuestionMad(): Boolean
    fun setShowImages(isShown: Boolean)
    fun isImagesShown(): LiveData<Boolean>
    fun setNightMode(isNight: Boolean)
    fun setCurrency(currency: GolosCurrency)
    fun getCurrency(): LiveData<GolosCurrency>
    fun setBountDisplay(display: GolosBountyDisplay)
    fun getBountDisplay(): LiveData<GolosBountyDisplay>
    fun isNightMode(): Boolean
    fun setUp(ctx: Context)

    enum class GolosCurrency {
        USD, RUB, GBG
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
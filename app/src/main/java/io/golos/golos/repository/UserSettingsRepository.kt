package io.golos.golos.repository

import android.arch.lifecycle.LiveData

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
    fun isNightMode(): Boolean
}
package io.golos.golos.screens.settings

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import io.golos.golos.App

/**
 * Created by yuri on 05.02.18.
 */
object UserSettings {
    private val sharedPrefName = "UserSettings"
    private val mCompactLiveData = MutableLiveData<Boolean>()
    private val mShowImageLiveData = MutableLiveData<Boolean>()
    private val mShowNSFWLiveData = MutableLiveData<Boolean>()

    fun setUp() {
        mCompactLiveData.value = App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getBoolean("isCompact", false)
        mShowImageLiveData.value = App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getBoolean("isShown", true)
        mShowNSFWLiveData.value = App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getBoolean("showNsfw", false)
    }

    fun setStoriesCompactMode(isCompact: Boolean) {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putBoolean("isCompact", isCompact).apply()
        mCompactLiveData.value = isCompact
    }

    fun isStoriesCompactMode(): LiveData<Boolean> {
        return mCompactLiveData
    }

    fun isNSFWShow(): LiveData<Boolean> {
        return mShowNSFWLiveData
    }

    fun setIsNSFWShown(isShown: Boolean) {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putBoolean("showNsfw", isShown).apply()
        mShowNSFWLiveData.value = isShown
    }

    fun setUserVotedForApp(isVotedForApp: Boolean) {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putBoolean("isVotedForApp", isVotedForApp).apply()
    }

    fun isUserVotedForApp(): Boolean {
        return App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getBoolean("isVotedForApp", false)
    }

    fun setVoteQuestionMade(isMade: Boolean) {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putBoolean("setVoteQuestionMade", isMade).apply()
    }

    fun isVoteQuestionMad(): Boolean {
        return App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getBoolean("setVoteQuestionMade", false)
    }

    fun setShowImages(isShown: Boolean) {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putBoolean("isShown", isShown).apply()
        mShowImageLiveData.value = isShown
    }

    fun isImagesShown(): LiveData<Boolean> {
        return mShowImageLiveData
    }


    fun setNightMode(isNight: Boolean) {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putBoolean("isNight", isNight).apply()
    }

    fun isNightMode(): Boolean {
        return App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getBoolean("isNight", false)
    }


}
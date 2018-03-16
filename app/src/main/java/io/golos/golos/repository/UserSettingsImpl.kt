package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import io.golos.golos.App

/**
 * Created by yuri on 05.02.18.
 */
internal class UserSettingsImpl : UserSettingsRepository {
    private val sharedPrefName = "UserSettings"
    private val mCompactLiveData = MutableLiveData<Boolean>()
    private val mShowImageLiveData = MutableLiveData<Boolean>()
    private val mShowNSFWLiveData = MutableLiveData<Boolean>()

    fun setUp(context: Context) {
        mCompactLiveData.value = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getBoolean("isCompact", false)
        mShowImageLiveData.value = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getBoolean("isShown", true)
        mShowNSFWLiveData.value = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getBoolean("showNsfw", false)
    }

    override fun setStoriesCompactMode(isCompact: Boolean) {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putBoolean("isCompact", isCompact).apply()
        mCompactLiveData.value = isCompact
    }

    override fun isStoriesCompactMode(): LiveData<Boolean> {
        return mCompactLiveData
    }

    override fun isNSFWShow(): LiveData<Boolean> {
        return mShowNSFWLiveData
    }

    override fun setIsNSFWShown(isShown: Boolean) {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putBoolean("showNsfw", isShown).apply()
        mShowNSFWLiveData.value = isShown
    }

    override fun setUserVotedForApp(isVotedForApp: Boolean) {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putBoolean("isVotedForApp", isVotedForApp).apply()
    }

    override fun isUserVotedForApp(): Boolean {
        return App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getBoolean("isVotedForApp", false)
    }

    override fun setVoteQuestionMade(isMade: Boolean) {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putBoolean("setVoteQuestionMade", isMade).apply()
    }

    override fun isVoteQuestionMad(): Boolean {
        return App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getBoolean("setVoteQuestionMade", false)
    }

    override fun setShowImages(isShown: Boolean) {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putBoolean("isShown", isShown).apply()
        mShowImageLiveData.value = isShown
    }

    override fun isImagesShown(): LiveData<Boolean> {
        return mShowImageLiveData
    }


    override fun setNightMode(isNight: Boolean) {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putBoolean("isNight", isNight).apply()
    }

    override fun isNightMode(): Boolean {
        return App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getBoolean("isNight", false)
    }
}
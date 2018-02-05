package io.golos.golos.screens.settings

import android.content.Context
import android.content.SharedPreferences
import io.golos.golos.App

/**
 * Created by yuri on 05.02.18.
 */
object UserSettings : SharedPreferences.OnSharedPreferenceChangeListener {
    private val sharedPrefName = "UserSettings"
    private val compactModeListeners = ArrayList<(Boolean) -> Unit>()

    fun setUp() {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(this)
    }

    fun setStoriesCompactMode(isCompact: Boolean) {
        App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).edit().putBoolean("isCompact", isCompact).apply()
    }

    fun getStoriesCompactMode(): Boolean {
        return App.context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getBoolean("isCompact", false)
    }

    fun registerCompactModeChangeListener(listener: (Boolean) -> Unit) {
        compactModeListeners.add(listener)
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
        p1?.let {
            when (it) {
                "isCompact" -> {
                    val isCompact = getStoriesCompactMode()
                    compactModeListeners.forEach { it.invoke(isCompact) }
                }
            }
        }
    }
}
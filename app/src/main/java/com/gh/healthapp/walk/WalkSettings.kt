package com.gh.healthapp.walk

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WalkSettingsState(val autoPause: Boolean = true, val voice: Boolean = true, val kmAnnounce: Boolean = true)

class WalkSettings(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("walk_settings", Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(read())
    val state: StateFlow<WalkSettingsState> = mutableState.asStateFlow()
    private val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> mutableState.value = read() }
    init { preferences.registerOnSharedPreferenceChangeListener(listener) }
    private fun read() = WalkSettingsState(preferences.getBoolean("autoPause", true), preferences.getBoolean("voice", true), preferences.getBoolean("kmAnnounce", true))
    fun getAutoPause() = preferences.getBoolean("autoPause", true)
    fun getVoice() = preferences.getBoolean("voice", true)
    fun getKmAnnounce() = preferences.getBoolean("kmAnnounce", true)
    fun setAutoPause(value: Boolean) { preferences.edit().putBoolean("autoPause", value).apply() }
    fun setVoice(value: Boolean) { preferences.edit().putBoolean("voice", value).apply() }
    fun setKmAnnounce(value: Boolean) { preferences.edit().putBoolean("kmAnnounce", value).apply() }
}

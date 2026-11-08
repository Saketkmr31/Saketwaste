package com.surwi.compass360.data.preferences

import android.content.Context
import java.util.Locale

enum class SurwiLanguage { HINDI, ENGLISH }

class SurwiPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("surwi_preferences", Context.MODE_PRIVATE)

    fun language(): SurwiLanguage =
        runCatching { SurwiLanguage.valueOf(preferences.getString(KEY_LANGUAGE, SurwiLanguage.ENGLISH.name)!!) }
            .getOrDefault(SurwiLanguage.ENGLISH)

    fun setLanguage(language: SurwiLanguage) {
        preferences.edit().putString(KEY_LANGUAGE, language.name).apply()
    }

    fun selectedCity(): String? = preferences.getString(KEY_CITY, null)

    fun setSelectedCity(city: String?) {
        preferences.edit().putString(KEY_CITY, city?.trim()?.takeIf { it.isNotEmpty() }).apply()
    }

    companion object {
        private const val KEY_LANGUAGE = "language"
        private const val KEY_CITY = "selected_city"
    }
}

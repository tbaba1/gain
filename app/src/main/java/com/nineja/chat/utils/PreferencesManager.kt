package com.naijachat.naija_chat.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.naijachat.naija_chat.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    private val gson = Gson()

    // User Session Management
    fun saveUserSession(user: User) {
        val userJson = gson.toJson(user)
        preferences.edit()
            .putString(KEY_USER_DATA, userJson)
            .putString(KEY_USER_ID, user.id)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            .apply()
    }

    fun getUserSession(): User? {
        val userJson = preferences.getString(KEY_USER_DATA, null)
        return if (userJson != null) {
            try {
                gson.fromJson(userJson, User::class.java)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    fun getCurrentUserId(): String? {
        return preferences.getString(KEY_USER_ID, null)
    }

    fun isLoggedIn(): Boolean {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun clearUserSession() {
        preferences.edit()
            .remove(KEY_USER_DATA)
            .remove(KEY_USER_ID)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_LOGIN_TIME)
            .apply()
    }

    // App Settings
    fun setDarkMode(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun isDarkModeEnabled(): Boolean {
        return preferences.getBoolean(KEY_DARK_MODE, false)
    }

    fun setAutoPlayVideos(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_PLAY, enabled).apply()
    }

    fun isAutoPlayEnabled(): Boolean {
        return preferences.getBoolean(KEY_AUTO_PLAY, true)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    fun areNotificationsEnabled(): Boolean {
        return preferences.getBoolean(KEY_NOTIFICATIONS, true)
    }

    fun setDataSaverMode(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DATA_SAVER, enabled).apply()
    }

    fun isDataSaverEnabled(): Boolean {
        return preferences.getBoolean(KEY_DATA_SAVER, false)
    }

    // Video Quality Settings
    fun setVideoQuality(quality: String) {
        preferences.edit().putString(KEY_VIDEO_QUALITY, quality).apply()
    }

    fun getVideoQuality(): String {
        return preferences.getString(KEY_VIDEO_QUALITY, "HD") ?: "HD"
    }

    // Language Settings
    fun setLanguage(language: String) {
        preferences.edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun getLanguage(): String {
        return preferences.getString(KEY_LANGUAGE, "english") ?: "english"
    }

    // Regional Settings
    fun setRegion(region: String) {
        preferences.edit().putString(KEY_REGION, region).apply()
    }

    fun getRegion(): String {
        return preferences.getString(KEY_REGION, "lagos") ?: "lagos"
    }

    // Content Preferences
    fun setPreferredGenres(genres: List<String>) {
        val genresJson = gson.toJson(genres)
        preferences.edit().putString(KEY_PREFERRED_GENRES, genresJson).apply()
    }

    fun getPreferredGenres(): List<String> {
        val genresJson = preferences.getString(KEY_PREFERRED_GENRES, null)
        return if (genresJson != null) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(genresJson, type) ?: listOf("afrobeats", "comedy")
            } catch (e: Exception) {
                listOf("afrobeats", "comedy")
            }
        } else {
            listOf("afrobeats", "comedy")
        }
    }

    // Cache Settings
    fun setCacheSize(sizeInMB: Int) {
        preferences.edit().putInt(KEY_CACHE_SIZE, sizeInMB).apply()
    }

    fun getCacheSize(): Int {
        return preferences.getInt(KEY_CACHE_SIZE, 500) // Default 500MB
    }

    fun setOfflineMode(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_OFFLINE_MODE, enabled).apply()
    }

    fun isOfflineModeEnabled(): Boolean {
        return preferences.getBoolean(KEY_OFFLINE_MODE, false)
    }

    // Privacy Settings
    fun setPrivateAccount(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PRIVATE_ACCOUNT, enabled).apply()
    }

    fun isAccountPrivate(): Boolean {
        return preferences.getBoolean(KEY_PRIVATE_ACCOUNT, false)
    }

    fun setAllowComments(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ALLOW_COMMENTS, enabled).apply()
    }

    fun areCommentsAllowed(): Boolean {
        return preferences.getBoolean(KEY_ALLOW_COMMENTS, true)
    }

    fun setAllowDirectMessages(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ALLOW_DM, enabled).apply()
    }

    fun areDirectMessagesAllowed(): Boolean {
        return preferences.getBoolean(KEY_ALLOW_DM, true)
    }

    // AI Recommendation Settings
    fun setPersonalizedRecommendations(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PERSONALIZED_RECS, enabled).apply()
    }

    fun arePersonalizedRecommendationsEnabled(): Boolean {
        return preferences.getBoolean(KEY_PERSONALIZED_RECS, true)
    }

    fun setNigerianContentBoost(boost: Float) {
        preferences.edit().putFloat(KEY_NIGERIAN_BOOST, boost).apply()
    }

    fun getNigerianContentBoost(): Float {
        return preferences.getFloat(KEY_NIGERIAN_BOOST, 0.6f)
    }

    // Creator Settings
    fun setCreatorMode(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_CREATOR_MODE, enabled).apply()
    }

    fun isCreatorModeEnabled(): Boolean {
        return preferences.getBoolean(KEY_CREATOR_MODE, false)
    }

    fun setMonetizationEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_MONETIZATION, enabled).apply()
    }

    fun isMonetizationEnabled(): Boolean {
        return preferences.getBoolean(KEY_MONETIZATION, false)
    }

    // Interaction History for AI (Local Cache)
    fun saveInteractionLocally(interaction: Any) {
        val interactionsJson = preferences.getString(KEY_INTERACTIONS, "[]")
        try {
            val type = object : TypeToken<MutableList<Map<String, Any>>>() {}.type
            val interactions: MutableList<Map<String, Any>> = gson.fromJson(interactionsJson, type) ?: mutableListOf()
            
            val interactionMap = gson.fromJson(gson.toJson(interaction), Map::class.java) as Map<String, Any>
            interactions.add(interactionMap)
            
            // Keep only last 100 interactions locally
            if (interactions.size > 100) {
                interactions.removeAt(0)
            }
            
            val updatedJson = gson.toJson(interactions)
            preferences.edit().putString(KEY_INTERACTIONS, updatedJson).apply()
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    fun getLocalInteractions(): List<Map<String, Any>> {
        val interactionsJson = preferences.getString(KEY_INTERACTIONS, "[]")
        return try {
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            gson.fromJson(interactionsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearLocalInteractions() {
        preferences.edit().putString(KEY_INTERACTIONS, "[]").apply()
    }

    // App Usage Statistics
    fun incrementAppOpenCount() {
        val count = preferences.getInt(KEY_APP_OPENS, 0)
        preferences.edit().putInt(KEY_APP_OPENS, count + 1).apply()
    }

    fun getAppOpenCount(): Int {
        return preferences.getInt(KEY_APP_OPENS, 0)
    }

    fun setLastAppOpenTime() {
        preferences.edit().putLong(KEY_LAST_OPEN, System.currentTimeMillis()).apply()
    }

    fun getLastAppOpenTime(): Long {
        return preferences.getLong(KEY_LAST_OPEN, 0L)
    }

    // Onboarding and Tutorial
    fun setOnboardingCompleted(completed: Boolean) {
        preferences.edit().putBoolean(KEY_ONBOARDING_DONE, completed).apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return preferences.getBoolean(KEY_ONBOARDING_DONE, false)
    }

    fun setTutorialShown(tutorialKey: String, shown: Boolean) {
        preferences.edit().putBoolean("tutorial_$tutorialKey", shown).apply()
    }

    fun isTutorialShown(tutorialKey: String): Boolean {
        return preferences.getBoolean("tutorial_$tutorialKey", false)
    }

    // Version and Update Management
    fun setAppVersion(version: String) {
        preferences.edit().putString(KEY_APP_VERSION, version).apply()
    }

    fun getAppVersion(): String {
        return preferences.getString(KEY_APP_VERSION, "1.0.0") ?: "1.0.0"
    }

    fun setUpdatePromptShown(version: String, shown: Boolean) {
        preferences.edit().putBoolean("update_prompt_$version", shown).apply()
    }

    fun isUpdatePromptShown(version: String): Boolean {
        return preferences.getBoolean("update_prompt_$version", false)
    }

    // Emergency/Debug Settings
    fun enableDebugMode(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DEBUG_MODE, enabled).apply()
    }

    fun isDebugModeEnabled(): Boolean {
        return preferences.getBoolean(KEY_DEBUG_MODE, false)
    }

    fun clearAllPreferences() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "naijachat_preferences"
        
        // User Session Keys
        private const val KEY_USER_DATA = "user_data"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LOGIN_TIME = "login_time"
        
        // App Settings Keys
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_AUTO_PLAY = "auto_play"
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_DATA_SAVER = "data_saver"
        private const val KEY_VIDEO_QUALITY = "video_quality"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_REGION = "region"
        private const val KEY_PREFERRED_GENRES = "preferred_genres"
        
        // Cache Keys
        private const val KEY_CACHE_SIZE = "cache_size"
        private const val KEY_OFFLINE_MODE = "offline_mode"
        
        // Privacy Keys
        private const val KEY_PRIVATE_ACCOUNT = "private_account"
        private const val KEY_ALLOW_COMMENTS = "allow_comments"
        private const val KEY_ALLOW_DM = "allow_dm"
        
        // AI Keys
        private const val KEY_PERSONALIZED_RECS = "personalized_recs"
        private const val KEY_NIGERIAN_BOOST = "nigerian_boost"
        private const val KEY_INTERACTIONS = "interactions"
        
        // Creator Keys
        private const val KEY_CREATOR_MODE = "creator_mode"
        private const val KEY_MONETIZATION = "monetization"
        
        // App Usage Keys
        private const val KEY_APP_OPENS = "app_opens"
        private const val KEY_LAST_OPEN = "last_open"
        
        // Onboarding Keys
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        
        // Version Keys
        private const val KEY_APP_VERSION = "app_version"
        
        // Debug Keys
        private const val KEY_DEBUG_MODE = "debug_mode"
    }
}
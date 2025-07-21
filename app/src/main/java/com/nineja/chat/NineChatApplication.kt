package com.nineja.chat

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NineChatApplication : Application() {

    private lateinit var analytics: FirebaseAnalytics
    private lateinit var crashlytics: FirebaseCrashlytics
    private lateinit var remoteConfig: FirebaseRemoteConfig

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        initializeFirebase()
        
        // Initialize Analytics
        initializeAnalytics()
        
        // Initialize Crashlytics
        initializeCrashlytics()
        
        // Initialize Remote Config
        initializeRemoteConfig()
        
        // Initialize App Check
        initializeAppCheck()
        
        // Initialize Push Notifications
        initializePushNotifications()
        
        // Set up global exception handler
        setupGlobalExceptionHandler()
        
        Log.d(TAG, "9jaChat Application initialized successfully")
    }

    private fun initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private fun initializeAnalytics() {
        try {
            analytics = FirebaseAnalytics.getInstance(this)
            analytics.setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)
            
            // Set default user properties
            analytics.setUserProperty("app_version", BuildConfig.VERSION_NAME)
            analytics.setUserProperty("country", "Nigeria")
            analytics.setUserProperty("platform", "Android")
            
            Log.d(TAG, "Firebase Analytics initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Analytics initialization failed", e)
        }
    }

    private fun initializeCrashlytics() {
        try {
            crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
            
            // Set custom keys
            crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            crashlytics.setCustomKey("debug_mode", BuildConfig.DEBUG_MODE)
            
            Log.d(TAG, "Firebase Crashlytics initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Crashlytics initialization failed", e)
        }
    }

    private fun initializeRemoteConfig() {
        try {
            remoteConfig = FirebaseRemoteConfig.getInstance()
            
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600) // 1 hour for release
                .build()
            
            remoteConfig.setConfigSettingsAsync(configSettings)
            
            // Set default values
            val defaultConfig = mapOf(
                "enable_live_streaming" to true,
                "enable_creator_fund" to true,
                "enable_virtual_gifts" to true,
                "min_video_duration" to 3,
                "max_video_duration" to 180,
                "creator_fund_cpm" to 2.5f,
                "nigerian_content_boost" to 0.6f,
                "maintenance_mode" to false,
                "force_update_version" to "1.0.0",
                "welcome_message" to "Welcome to 9jaChat! 🇳🇬",
                "trending_hashtags" to "#Naija,#Afrobeats,#Comedy,#Lagos",
                "supported_languages" to "en,yo,ig,ha"
            )
            
            remoteConfig.setDefaultsAsync(defaultConfig)
            
            // Fetch and activate
            remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Remote config fetched and activated")
                } else {
                    Log.w(TAG, "Remote config fetch failed")
                }
            }
            
            Log.d(TAG, "Firebase Remote Config initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Remote Config initialization failed", e)
        }
    }

    private fun initializeAppCheck() {
        try {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                SafetyNetAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "Firebase App Check initialized")
        } catch (e: Exception) {
            Log.e(TAG, "App Check initialization failed", e)
        }
    }

    private fun initializePushNotifications() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result
                Log.d(TAG, "FCM Registration Token: $token")
                
                // Send token to your server if needed
                // sendTokenToServer(token)
            }
            
            // Subscribe to general topics
            FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            FirebaseMessaging.getInstance().subscribeToTopic("nigeria")
            FirebaseMessaging.getInstance().subscribeToTopic("android")
            
            Log.d(TAG, "Firebase Messaging initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Push notifications initialization failed", e)
        }
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                // Log to Crashlytics
                FirebaseCrashlytics.getInstance().recordException(exception)
                
                // Log critical info
                Log.e(TAG, "Uncaught exception on thread ${thread.name}", exception)
                
                // Track crash event
                FirebaseAnalytics.getInstance(this).logEvent("app_crash", null)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in exception handler", e)
            } finally {
                // Call the default handler
                defaultHandler?.uncaughtException(thread, exception)
            }
        }
    }

    fun getRemoteConfig(): FirebaseRemoteConfig = remoteConfig
    
    fun getAnalytics(): FirebaseAnalytics = analytics
    
    fun getCrashlytics(): FirebaseCrashlytics = crashlytics

    companion object {
        private const val TAG = "NineChatApplication"
    }
}
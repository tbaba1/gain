package com.nineja.chat.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object PermissionUtils {
    
    // Permission constants
    const val CAMERA_PERMISSION_CODE = 100
    const val AUDIO_PERMISSION_CODE = 101
    const val STORAGE_PERMISSION_CODE = 102
    const val ALL_PERMISSIONS_CODE = 103
    
    // Required permissions for video recording
    val CAMERA_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    
    val STORAGE_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    
    val ALL_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    
    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if audio recording permission is granted
     */
    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if storage permissions are granted
     */
    fun hasStoragePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun hasAllPermissions(context: Context): Boolean {
        return ALL_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if camera and audio permissions are granted (minimum for recording)
     */
    fun hasCameraAndAudioPermissions(context: Context): Boolean {
        return CAMERA_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get list of missing permissions
     */
    fun getMissingPermissions(context: Context, permissions: Array<String>): List<String> {
        return permissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Request camera permission from Activity
     */
    fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }
    
    /**
     * Request audio permission from Activity
     */
    fun requestAudioPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            AUDIO_PERMISSION_CODE
        )
    }
    
    /**
     * Request storage permissions from Activity
     */
    fun requestStoragePermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            STORAGE_PERMISSIONS,
            STORAGE_PERMISSION_CODE
        )
    }
    
    /**
     * Request all permissions from Activity
     */
    fun requestAllPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            ALL_PERMISSIONS,
            ALL_PERMISSIONS_CODE
        )
    }
    
    /**
     * Request camera and audio permissions from Activity
     */
    fun requestCameraAndAudioPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            CAMERA_PERMISSIONS,
            CAMERA_PERMISSION_CODE
        )
    }
    
    /**
     * Request camera permission from Fragment
     */
    fun requestCameraPermission(fragment: Fragment) {
        fragment.requestPermissions(
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }
    
    /**
     * Request audio permission from Fragment
     */
    fun requestAudioPermission(fragment: Fragment) {
        fragment.requestPermissions(
            arrayOf(Manifest.permission.RECORD_AUDIO),
            AUDIO_PERMISSION_CODE
        )
    }
    
    /**
     * Request all permissions from Fragment
     */
    fun requestAllPermissions(fragment: Fragment) {
        fragment.requestPermissions(
            ALL_PERMISSIONS,
            ALL_PERMISSIONS_CODE
        )
    }
    
    /**
     * Check if we should show permission rationale
     */
    fun shouldShowPermissionRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
    
    /**
     * Check if we should show permission rationale for any of the permissions
     */
    fun shouldShowPermissionRationale(activity: Activity, permissions: Array<String>): Boolean {
        return permissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }
    
    /**
     * Open app settings for manual permission grant
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    /**
     * Handle permission request result
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onShowRationale: () -> Unit = {}
    ) {
        when (requestCode) {
            CAMERA_PERMISSION_CODE,
            AUDIO_PERMISSION_CODE,
            STORAGE_PERMISSION_CODE,
            ALL_PERMISSIONS_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
        }
    }
    
    /**
     * Get permission name for display
     */
    fun getPermissionName(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> "Camera"
            Manifest.permission.RECORD_AUDIO -> "Microphone"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage"
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Storage"
            else -> "Unknown"
        }
    }
    
    /**
     * Get permission description for rationale
     */
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> "Camera access is needed to record videos"
            Manifest.permission.RECORD_AUDIO -> "Microphone access is needed to record audio"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage access is needed to save and access media files"
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Storage access is needed to save videos and photos"
            else -> "This permission is required for the app to function properly"
        }
    }
}
package com.naijachat.naija_chat.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtils {

    // Permission request codes
    const val REQUEST_CAMERA_PERMISSION = 100
    const val REQUEST_AUDIO_PERMISSION = 101
    const val REQUEST_STORAGE_PERMISSION = 102
    const val REQUEST_ALL_PERMISSIONS = 103

    // Permission arrays
    val CAMERA_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    val STORAGE_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    val ALL_REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.VIBRATE
    )

    /**
     * Check if a single permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if all permissions in an array are granted
     */
    fun arePermissionsGranted(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            isPermissionGranted(context, permission)
        }
    }

    /**
     * Check camera permissions
     */
    fun hasCameraPermissions(context: Context): Boolean {
        return arePermissionsGranted(context, CAMERA_PERMISSIONS)
    }

    /**
     * Check storage permissions
     */
    fun hasStoragePermissions(context: Context): Boolean {
        return arePermissionsGranted(context, STORAGE_PERMISSIONS)
    }

    /**
     * Check all required permissions
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return arePermissionsGranted(context, ALL_REQUIRED_PERMISSIONS)
    }

    /**
     * Request a single permission
     */
    fun requestPermission(activity: Activity, permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
    }

    /**
     * Request multiple permissions
     */
    fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    /**
     * Request camera permissions
     */
    fun requestCameraPermissions(activity: Activity) {
        requestPermissions(activity, CAMERA_PERMISSIONS, REQUEST_CAMERA_PERMISSION)
    }

    /**
     * Request storage permissions
     */
    fun requestStoragePermissions(activity: Activity) {
        requestPermissions(activity, STORAGE_PERMISSIONS, REQUEST_STORAGE_PERMISSION)
    }

    /**
     * Request all required permissions
     */
    fun requestAllPermissions(activity: Activity) {
        requestPermissions(activity, ALL_REQUIRED_PERMISSIONS, REQUEST_ALL_PERMISSIONS)
    }

    /**
     * Check if we should show rationale for a permission
     */
    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * Check if we should show rationale for any of the permissions
     */
    fun shouldShowRationale(activity: Activity, permissions: Array<String>): Boolean {
        return permissions.any { permission ->
            shouldShowRationale(activity, permission)
        }
    }

    /**
     * Handle permission result
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: (deniedPermissions: List<String>) -> Unit
    ) {
        if (grantResults.isNotEmpty()) {
            val deniedPermissions = mutableListOf<String>()
            
            for (i in permissions.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i])
                }
            }
            
            if (deniedPermissions.isEmpty()) {
                onGranted()
            } else {
                onDenied(deniedPermissions)
            }
        } else {
            onDenied(permissions.toList())
        }
    }

    /**
     * Get missing permissions from a list of required permissions
     */
    fun getMissingPermissions(context: Context, requiredPermissions: Array<String>): List<String> {
        return requiredPermissions.filter { permission ->
            !isPermissionGranted(context, permission)
        }
    }

    /**
     * Open app settings to allow user to grant permissions manually
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    /**
     * Get human-readable permission name
     */
    fun getPermissionName(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> "Camera"
            Manifest.permission.RECORD_AUDIO -> "Microphone"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage Read"
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Storage Write"
            Manifest.permission.READ_MEDIA_IMAGES -> "Media Images"
            Manifest.permission.READ_MEDIA_VIDEO -> "Media Videos"
            Manifest.permission.ACCESS_NETWORK_STATE -> "Network State"
            Manifest.permission.VIBRATE -> "Vibrate"
            else -> permission.substringAfterLast(".")
        }
    }

    /**
     * Get permission rationale message
     */
    fun getPermissionRationale(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> 
                "Camera permission is needed to record videos and take photos"
            Manifest.permission.RECORD_AUDIO -> 
                "Microphone permission is needed to record audio for your videos"
            Manifest.permission.READ_EXTERNAL_STORAGE -> 
                "Storage permission is needed to access your photos and videos"
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> 
                "Storage permission is needed to save your videos and photos"
            Manifest.permission.READ_MEDIA_IMAGES -> 
                "Permission is needed to access your images for video creation"
            Manifest.permission.READ_MEDIA_VIDEO -> 
                "Permission is needed to access your videos"
            else -> "This permission is required for the app to function properly"
        }
    }

    /**
     * Check if permission is permanently denied
     */
    fun isPermissionPermanentlyDenied(activity: Activity, permission: String): Boolean {
        return !isPermissionGranted(activity, permission) && 
               !shouldShowRationale(activity, permission)
    }

    /**
     * Get permanently denied permissions
     */
    fun getPermanentlyDeniedPermissions(activity: Activity, permissions: Array<String>): List<String> {
        return permissions.filter { permission ->
            isPermissionPermanentlyDenied(activity, permission)
        }
    }
}
package com.naijachat.naija_chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.naijachat.naija_chat.databinding.ActivityMainBinding
import com.naijachat.naija_chat.ui.camera.CameraActivity
import com.naijachat.naija_chat.utils.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            openCamera()
        } else {
            showPermissionDeniedMessage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupBottomNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setupWithNavController(navController)

        // Handle camera button separately
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_camera -> {
                    checkPermissionsAndOpenCamera()
                    false // Don't let navigation handle this
                }
                else -> {
                    navController.navigate(item.itemId)
                    true
                }
            }
        }

        // Show/hide bottom navigation based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_camera -> hideBottomNavigation()
                else -> showBottomNavigation()
            }
        }
    }

    private fun checkPermissionsAndOpenCamera() {
        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            openCamera()
        } else {
            // Show rationale if needed
            val shouldShowRationale = missingPermissions.any { permission ->
                shouldShowRequestPermissionRationale(permission)
            }

            if (shouldShowRationale) {
                showPermissionRationale(requiredPermissions)
            } else {
                permissionLauncher.launch(requiredPermissions)
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }

    private fun showPermissionRationale(permissions: Array<String>) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.permission_camera_title))
            .setMessage(getString(R.string.permission_camera_message))
            .setPositiveButton(getString(R.string.grant_permission)) { _, _ ->
                permissionLauncher.launch(permissions)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showPermissionDeniedMessage() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.permission_denied))
            .setMessage(getString(R.string.permission_camera_message))
            .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->
                PermissionUtils.openAppSettings(this)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showBottomNavigation() {
        binding.bottomNavigation.visibility = View.VISIBLE
    }

    private fun hideBottomNavigation() {
        binding.bottomNavigation.visibility = View.GONE
    }
}
package com.nineja.chat

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
import com.google.android.material.snackbar.Snackbar
import com.nineja.chat.databinding.ActivityMainBinding
import com.nineja.chat.ui.camera.CameraActivity
import com.nineja.chat.utils.PermissionUtils

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    
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
        
        // Handle camera tab click separately
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_camera -> {
                    checkPermissionsAndOpenCamera()
                    false // Don't navigate normally
                }
                else -> {
                    navController.navigate(item.itemId)
                    true
                }
            }
        }
        
        // Show/hide bottom navigation based on current destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment,
                R.id.discoverFragment,
                R.id.inboxFragment,
                R.id.profileFragment -> {
                    showBottomNavigation()
                }
                else -> {
                    hideBottomNavigation()
                }
            }
        }
    }
    
    private fun checkPermissionsAndOpenCamera() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isEmpty()) {
            openCamera()
        } else {
            if (missingPermissions.any { shouldShowRequestPermissionRationale(it) }) {
                showPermissionRationale()
            } else {
                permissionLauncher.launch(requiredPermissions)
            }
        }
    }
    
    private fun openCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
        // Add custom transition animation
        overridePendingTransition(R.anim.slide_up, R.anim.fade_out)
    }
    
    private fun showPermissionRationale() {
        Snackbar.make(
            binding.root,
            "Camera and audio permissions are needed to record videos",
            Snackbar.LENGTH_LONG
        ).setAction("Allow") {
            permissionLauncher.launch(requiredPermissions)
        }.show()
    }
    
    private fun showPermissionDeniedMessage() {
        Snackbar.make(
            binding.root,
            "Permissions denied. Please enable them in settings to use camera.",
            Snackbar.LENGTH_LONG
        ).setAction("Settings") {
            PermissionUtils.openAppSettings(this)
        }.show()
    }
    
    private fun showBottomNavigation() {
        binding.bottomNavigation.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(200)
            .withStartAction {
                binding.bottomNavigation.visibility = View.VISIBLE
            }
            .start()
    }
    
    private fun hideBottomNavigation() {
        binding.bottomNavigation.animate()
            .translationY(binding.bottomNavigation.height.toFloat())
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.bottomNavigation.visibility = View.GONE
            }
            .start()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
    
    fun selectTab(tabId: Int) {
        binding.bottomNavigation.selectedItemId = tabId
    }
    
    fun getCurrentTabId(): Int {
        return binding.bottomNavigation.selectedItemId
    }
}
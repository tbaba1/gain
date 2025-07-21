package com.example.myapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.myapp.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        
        // Set up click listeners
        binding.btnIncrement.setOnClickListener {
            viewModel.increment()
        }
        
        binding.btnDecrement.setOnClickListener {
            viewModel.decrement()
        }
        
        binding.btnReset.setOnClickListener {
            viewModel.reset()
            showSnackbar("Counter reset!")
        }
        
        binding.fab.setOnClickListener { view ->
            showSnackbar("Hello from FAB!")
        }
    }
    
    private fun observeViewModel() {
        viewModel.counter.observe(this) { count ->
            binding.tvCounter.text = count.toString()
            
            // Update UI based on counter value
            binding.btnDecrement.isEnabled = count > 0
            
            // Change color based on value
            val color = when {
                count == 0 -> getColor(android.R.color.holo_blue_dark)
                count > 0 -> getColor(android.R.color.holo_green_dark)
                else -> getColor(android.R.color.holo_red_dark)
            }
            binding.tvCounter.setTextColor(color)
        }
    }
    
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
package com.example.myapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    
    private val _counter = MutableLiveData<Int>().apply { value = 0 }
    val counter: LiveData<Int> = _counter
    
    fun increment() {
        _counter.value = (_counter.value ?: 0) + 1
    }
    
    fun decrement() {
        val currentValue = _counter.value ?: 0
        if (currentValue > 0) {
            _counter.value = currentValue - 1
        }
    }
    
    fun reset() {
        _counter.value = 0
    }
}
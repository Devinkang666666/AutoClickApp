package com.example.a002.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.a002.model.AutomationAction
import com.example.a002.model.AutomationConfig
import com.example.a002.model.Point
import com.example.a002.service.AutomationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AutomationViewModel(application: Application) : AndroidViewModel(application) {
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordedActions = MutableStateFlow<List<AutomationAction>>(emptyList())
    val recordedActions: StateFlow<List<AutomationAction>> = _recordedActions.asStateFlow()

    private val _savedConfigs = MutableStateFlow<List<AutomationConfig>>(emptyList())
    val savedConfigs: StateFlow<List<AutomationConfig>> = _savedConfigs.asStateFlow()

    private val _currentConfig = MutableStateFlow<AutomationConfig?>(null)
    val currentConfig: StateFlow<AutomationConfig?> = _currentConfig.asStateFlow()

    private val recordedPoints = mutableListOf<Point>()
    private var lastActionTime = 0L

    fun toggleRecording() {
        _isRecording.value = !_isRecording.value
        if (_isRecording.value) {
            recordedPoints.clear()
            _recordedActions.value = emptyList()
            lastActionTime = System.currentTimeMillis()
        }
    }

    fun recordTap(x: Float, y: Float, pressure: Float = 1.0f) {
        if (!_isRecording.value) return
        
        val currentTime = System.currentTimeMillis()
        val delayAfter = if (lastActionTime == 0L) 0 else currentTime - lastActionTime
        lastActionTime = currentTime

        val tap = AutomationAction.Tap(x, y, pressure, delayAfter = delayAfter)
        _recordedActions.value = _recordedActions.value + tap
    }

    fun recordSwipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 300) {
        if (!_isRecording.value) return

        val currentTime = System.currentTimeMillis()
        val delayAfter = if (lastActionTime == 0L) 0 else currentTime - lastActionTime
        lastActionTime = currentTime

        val swipe = AutomationAction.Swipe(startX, startY, endX, endY, duration, delayAfter = delayAfter)
        _recordedActions.value = _recordedActions.value + swipe
    }

    fun saveCurrentRecording(name: String) {
        if (_recordedActions.value.isEmpty()) return

        val config = AutomationConfig(
            name = name,
            actions = _recordedActions.value,
            interval = 1000,
            repeatCount = -1,
            powerSaveMode = false
        )

        _savedConfigs.value = _savedConfigs.value + config
        _recordedActions.value = emptyList()
        _isRecording.value = false
    }

    fun startAutomation(config: AutomationConfig) {
        _currentConfig.value = config
        AutomationService.getInstance()?.startAutomation(config.actions, config.interval)
    }

    fun stopAutomation() {
        _currentConfig.value = null
        AutomationService.getInstance()?.stopAutomation()
    }

    fun deleteConfig(config: AutomationConfig) {
        _savedConfigs.value = _savedConfigs.value - config
    }
}

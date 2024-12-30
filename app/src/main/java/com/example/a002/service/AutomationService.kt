package com.example.a002.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.example.a002.model.AutomationAction
import com.example.a002.model.Point
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AutomationService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var powerSaveMode = false
    
    private val _serviceStatus = MutableStateFlow<Boolean>(false)
    val serviceStatus: StateFlow<Boolean> = _serviceStatus.asStateFlow()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used in this implementation
    }

    override fun onInterrupt() {
        isRunning = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _serviceStatus.value = true
        instance = this
    }

    private fun performGesture(
        gesture: GestureDescription,
        callback: ((Boolean) -> Unit)? = null
    ) {
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                callback?.invoke(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                callback?.invoke(false)
            }
        }, null)
    }

    fun performTap(point: Point, callback: ((Boolean) -> Unit)? = null) {
        val path = Path()
        path.moveTo(point.x, point.y)
        
        val stroke = GestureDescription.StrokeDescription(
            path, 
            0, 
            100
        )
        
        val gesture = GestureDescription.Builder()
            .addStroke(stroke)
            .build()

        performGesture(gesture, callback)
    }

    fun performMultiTap(points: List<Point>, callback: ((Boolean) -> Unit)? = null) {
        val builder = GestureDescription.Builder()
        
        points.forEachIndexed { index, point ->
            val path = Path()
            path.moveTo(point.x, point.y)
            
            val stroke = GestureDescription.StrokeDescription(
                path, 
                0, 
                100
            )
            builder.addStroke(stroke)
        }

        performGesture(builder.build(), callback)
    }

    fun performSwipe(
        startPoint: Point,
        endPoint: Point,
        duration: Long = 300,
        callback: ((Boolean) -> Unit)? = null
    ) {
        val path = Path()
        path.moveTo(startPoint.x, startPoint.y)
        path.lineTo(endPoint.x, endPoint.y)

        val stroke = GestureDescription.StrokeDescription(
            path, 
            0, 
            duration
        )

        val gesture = GestureDescription.Builder()
            .addStroke(stroke)
            .build()

        performGesture(gesture, callback)
    }

    fun performCustomPath(
        points: List<Point>,
        duration: Long = 500,
        callback: ((Boolean) -> Unit)? = null
    ) {
        if (points.size < 2) return

        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }

        val stroke = GestureDescription.StrokeDescription(
            path, 
            0, 
            duration
        )

        val gesture = GestureDescription.Builder()
            .addStroke(stroke)
            .build()

        performGesture(gesture, callback)
    }

    fun startAutomation(actions: List<AutomationAction>, interval: Long) {
        isRunning = true
        var index = 0

        val runnable = object : Runnable {
            override fun run() {
                if (!isRunning) return
                
                if (index >= actions.size) {
                    index = 0
                    if (!actions[0].repeat) {
                        isRunning = false
                        return
                    }
                }

                val action = actions[index]
                when (action) {
                    is AutomationAction.Tap -> {
                        performTap(Point(action.x, action.y, action.pressure))
                    }
                    is AutomationAction.Swipe -> {
                        performSwipe(
                            Point(action.startX, action.startY),
                            Point(action.endX, action.endY),
                            action.duration
                        )
                    }
                    is AutomationAction.MultiTap -> {
                        performMultiTap(action.points)
                    }
                    is AutomationAction.CustomPath -> {
                        performCustomPath(action.points, action.duration)
                    }
                }

                val nextDelay = if (powerSaveMode) {
                    (interval + action.delayAfter) * 1.5
                } else {
                    interval + action.delayAfter
                }.toLong()

                index++
                handler.postDelayed(this, nextDelay)
            }
        }

        handler.post(runnable)
    }

    fun stopAutomation() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    fun setPowerSaveMode(enabled: Boolean) {
        powerSaveMode = enabled
    }

    companion object {
        private var instance: AutomationService? = null

        fun getInstance(): AutomationService? = instance
    }
}

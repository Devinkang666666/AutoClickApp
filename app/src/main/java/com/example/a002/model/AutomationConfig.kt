package com.example.a002.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AutomationConfig(
    val name: String,
    val actions: List<AutomationAction>,
    val interval: Long,
    val repeatCount: Int = -1, // -1 means infinite
    val powerSaveMode: Boolean = false
) : Parcelable

@Parcelize
sealed class AutomationAction : Parcelable {
    abstract val repeat: Boolean
    abstract val delayAfter: Long
    
    @Parcelize
    data class Tap(
        val x: Float, 
        val y: Float,
        val pressure: Float = 1.0f,
        override val repeat: Boolean = true,
        override val delayAfter: Long = 0
    ) : AutomationAction()
    
    @Parcelize
    data class Swipe(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val duration: Long = 300,
        override val repeat: Boolean = true,
        override val delayAfter: Long = 0
    ) : AutomationAction()
    
    @Parcelize
    data class MultiTap(
        val points: List<Point>,
        val pressure: Float = 1.0f,
        override val repeat: Boolean = true,
        override val delayAfter: Long = 0
    ) : AutomationAction()
    
    @Parcelize
    data class CustomPath(
        val points: List<Point>,
        val duration: Long = 500,
        override val repeat: Boolean = true,
        override val delayAfter: Long = 0
    ) : AutomationAction()
}

@Parcelize
data class Point(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
) : Parcelable

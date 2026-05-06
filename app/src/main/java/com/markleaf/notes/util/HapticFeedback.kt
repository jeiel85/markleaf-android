package com.markleaf.notes.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Utility class for providing haptic feedback on user interactions.
 * Uses modern haptic APIs on Android 10+ (API 29+) with fallback to
 * older vibration APIs on older devices.
 */
object HapticFeedback {

    /**
     * Provides a light haptic feedback for subtle interactions
     * like toggling switches or selecting items.
     */
    fun light(context: Context) {
        performHaptic(context, HapticFeedbackConstants.CLOCK_TICK)
    }

    /**
     * Provides a medium haptic feedback for standard actions
     * like button presses or confirmations.
     */
    fun medium(context: Context) {
        performHaptic(context, HapticFeedbackConstants.CONTEXT_CLICK)
    }

    /**
     * Provides a strong haptic feedback for important actions
     * like deletions or critical confirmations.
     */
    fun strong(context: Context) {
        performHaptic(context, HapticFeedbackConstants.LONG_PRESS)
    }

    /**
     * Provides a success haptic feedback for positive actions
     * like completing a task or saving.
     */
    fun success(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performHaptic(context, HapticFeedbackConstants.CONFIRM)
        } else {
            medium(context)
        }
    }

    /**
     * Provides an error/warning haptic feedback for error states
     * or destructive actions.
     */
    fun error(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performHaptic(context, HapticFeedbackConstants.REJECT)
        } else {
            strong(context)
        }
    }

    private fun performHaptic(context: Context, feedbackConstant: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use haptic feedback constants directly on Android 11+
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (feedbackConstant) {
                    HapticFeedbackConstants.CLOCK_TICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    HapticFeedbackConstants.CONTEXT_CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    HapticFeedbackConstants.LONG_PRESS -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                    HapticFeedbackConstants.CONFIRM -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                    else -> VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
    }

    /**
     * Extension function to perform haptic feedback on a View.
     * This uses the view's built-in haptic feedback mechanism.
     */
    fun View.performHaptic(feedbackConstant: Int = HapticFeedbackConstants.CONTEXT_CLICK) {
        this.performHapticFeedback(feedbackConstant)
    }
}
package com.example.core.security

import android.view.Window
import android.view.WindowManager
import androidx.annotation.VisibleForTesting

/**
 * Centralized configuration switch for Android screenshot, screen-recording,
 * and Recent Apps preview snapshot protection ([WindowManager.LayoutParams.FLAG_SECURE]).
 *
 * Flip [DEFAULT_ENABLE_SCREEN_PROTECTION] to `true` to enable protection uniformly across
 * BOTH Debug and Release builds, including MainActivity, standard dialogs, and full-screen windows.
 */
object ScreenProtectionConfig {
    const val DEFAULT_ENABLE_SCREEN_PROTECTION: Boolean = false

    @Volatile
    private var overrideForTesting: Boolean? = null

    val isScreenProtectionEnabled: Boolean
        get() = overrideForTesting ?: DEFAULT_ENABLE_SCREEN_PROTECTION

    fun applyToWindow(window: Window) {
        if (isScreenProtectionEnabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    @VisibleForTesting
    internal fun setEnabledForTesting(enabled: Boolean?) {
        overrideForTesting = enabled
    }
}

package com.example

import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.stringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.ui.navigation.ChevalierAppShell
import com.example.ui.theme.ChevalierAcademyTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ManagementViewModel
import com.example.ui.viewmodel.PreferencesUiState
import com.example.ui.viewmodel.SchoolDataViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels {
        val app = application as ChevalierApplication
        MainViewModel.Factory(app.container.userPreferencesRepository)
    }

    private val schoolDataViewModel: SchoolDataViewModel by viewModels {
        val app = application as ChevalierApplication
        SchoolDataViewModel.Factory(app.container.schoolRepository)
    }

    private val managementViewModel: ManagementViewModel by viewModels {
        val app = application as ChevalierApplication
        ManagementViewModel.Factory(
            managementRepository = app.container.managementRepository,
            backupRestoreManager = app.container.backupRestoreManager,
            preferencesRepository = app.container.userPreferencesRepository,
            centralBackendStatus = app.container.centralBackendStatus
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { splashScreenView ->
                splashScreenView.remove()
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        @Suppress("DEPRECATION")
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.statusBars())
            show(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
        setContent {
            val preferencesUiState by mainViewModel.preferencesUiState.collectAsStateWithLifecycle()
            val readyPrefs = (preferencesUiState as? PreferencesUiState.Ready)?.state
            val currentLanguage = readyPrefs?.selectedLanguage ?: AppLanguage.ENGLISH
            val currentThemeMode = readyPrefs?.themeMode ?: AppThemeMode.SYSTEM
            val isDarkTheme = when (currentThemeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            val localizedStrings = stringsFor(currentLanguage)

            DisposableEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.argb(0x80, 0x1b, 0x1b, 0x1b))
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.argb(0x80, 0x1b, 0x1b, 0x1b))
                    }
                )
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    show(WindowInsetsCompat.Type.statusBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                    isAppearanceLightStatusBars = !isDarkTheme
                }
                onDispose {}
            }

            CompositionLocalProvider(LocalAppStrings provides localizedStrings) {
                ChevalierAcademyTheme(themeMode = currentThemeMode) {
                    ChevalierAppShell(
                        mainViewModel = mainViewModel,
                        schoolDataViewModel = schoolDataViewModel,
                        managementViewModel = managementViewModel,
                        currentLanguage = currentLanguage,
                        currentThemeMode = currentThemeMode
                    )
                }
            }
        }
    }
}

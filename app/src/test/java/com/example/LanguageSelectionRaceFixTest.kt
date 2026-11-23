package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.stringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.ui.navigation.ChevalierAppShell
import com.example.ui.screens.LanguageSelectionScreen
import com.example.ui.theme.ChevalierAcademyTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ManagementViewModel
import com.example.ui.viewmodel.PreferencesUiState
import com.example.ui.viewmodel.SchoolDataViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LanguageSelectionRaceFixTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private val viewModelStores = mutableListOf<ViewModelStore>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        composeTestRule.mainClock.autoAdvance = true
        viewModelStores.forEach { it.clear() }
        viewModelStores.clear()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private fun awaitCondition(timeoutMs: Long = 6_000L, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeTestRule.waitForIdle()
            if (condition()) return
            Thread.sleep(20)
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeTestRule.waitForIdle()
        assertTrue("Timed out waiting for condition", condition())
    }

    @Test
    fun `selecting Tamil and immediately tapping Continue confirms Tamil even before parent selectedLanguage updates`() {
        val previewSelections = mutableListOf<AppLanguage>()
        val confirmedSelections = mutableListOf<AppLanguage>()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    // Simulate slow async DataStore parent prop remaining stuck on ENGLISH
                    LanguageSelectionScreen(
                        selectedLanguage = AppLanguage.ENGLISH,
                        onLanguagePreviewSelected = { previewSelections.add(it) },
                        onConfirmLanguage = { confirmedSelections.add(it) }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("language_option_ta").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("language_continue_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(AppLanguage.TAMIL), previewSelections)
        assertEquals(listOf(AppLanguage.TAMIL), confirmedSelections)
    }

    @Test
    fun `selecting Hindi and immediately tapping Continue confirms Hindi even after prior Tamil preview`() {
        val previewSelections = mutableListOf<AppLanguage>()
        val confirmedSelections = mutableListOf<AppLanguage>()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    // Simulate slow async DataStore parent prop remaining stuck on ENGLISH
                    LanguageSelectionScreen(
                        selectedLanguage = AppLanguage.ENGLISH,
                        onLanguagePreviewSelected = { previewSelections.add(it) },
                        onConfirmLanguage = { confirmedSelections.add(it) }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("language_option_ta").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("language_option_hi").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("language_continue_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(AppLanguage.TAMIL, AppLanguage.HINDI), previewSelections)
        assertEquals(listOf(AppLanguage.HINDI), confirmedSelections)
    }

    @Test
    fun `MainViewModel selectLanguage updates state synchronously and persists Tamil and Hindi cleanly`() {
        val prefsRepo = UserPreferencesRepository(context)
        runBlocking {
            prefsRepo.restorePreferences(
                language = AppLanguage.ENGLISH,
                isOnboarded = false,
                themeMode = AppThemeMode.SYSTEM
            )
        }

        val store = ViewModelStore().also { viewModelStores.add(it) }
        val mainViewModel = ViewModelProvider(
            store,
            MainViewModel.Factory(prefsRepo)
        )[MainViewModel::class.java]

        var latestState: PreferencesUiState = PreferencesUiState.Loading
        composeTestRule.setContent {
            val state by mainViewModel.preferencesUiState.collectAsStateWithLifecycle()
            latestState = state
        }
        awaitCondition { latestState is PreferencesUiState.Ready }

        // Switching to Tamil -> immediate UI state + DataStore persistence
        mainViewModel.selectLanguage(AppLanguage.TAMIL, markOnboarded = false)
        mainViewModel.selectLanguage(AppLanguage.TAMIL, markOnboarded = true)
        awaitCondition {
            runBlocking {
                val p = prefsRepo.preferencesFlow.first()
                p.selectedLanguage == AppLanguage.TAMIL && p.isLanguageSelectedOnLaunch
            }
        }
        val tamilReady = (latestState as PreferencesUiState.Ready).state
        assertEquals(AppLanguage.TAMIL, tamilReady.selectedLanguage)
        assertTrue(tamilReady.isLanguageSelectedOnLaunch)

        // Rapid switch Tamil -> Hindi -> DataStore converges to Hindi
        mainViewModel.selectLanguage(AppLanguage.HINDI, markOnboarded = true)
        awaitCondition {
            runBlocking { prefsRepo.preferencesFlow.first().selectedLanguage } == AppLanguage.HINDI
        }
        val hindiPersisted = runBlocking { prefsRepo.preferencesFlow.first() }
        assertEquals(AppLanguage.HINDI, hindiPersisted.selectedLanguage)
        assertTrue(hindiPersisted.isLanguageSelectedOnLaunch)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w412dp-h915dp")
    fun `onboarding navigation confirms Hindi cleanly and navigates to Home with persisted preference`() {
        val app = ApplicationProvider.getApplicationContext<ChevalierApplication>()
        runBlocking {
            app.container.userPreferencesRepository.restorePreferences(
                language = AppLanguage.ENGLISH,
                isOnboarded = false,
                themeMode = AppThemeMode.LIGHT
            )
        }

        val store = ViewModelStore().also { viewModelStores.add(it) }
        val mainViewModel = ViewModelProvider(
            store,
            MainViewModel.Factory(app.container.userPreferencesRepository)
        )[MainViewModel::class.java]
        val schoolDataViewModel = ViewModelProvider(
            store,
            SchoolDataViewModel.Factory(app.container.schoolRepository)
        )[SchoolDataViewModel::class.java]
        val managementViewModel = ViewModelProvider(
            store,
            ManagementViewModel.Factory(
                app.container.managementRepository,
                app.container.backupRestoreManager,
                app.container.userPreferencesRepository,
                app.container.centralBackendStatus
            )
        )[ManagementViewModel::class.java]

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            val prefsState by mainViewModel.preferencesUiState.collectAsStateWithLifecycle()
            val ready = (prefsState as? PreferencesUiState.Ready)?.state
            val lang = ready?.selectedLanguage ?: AppLanguage.ENGLISH
            CompositionLocalProvider(LocalAppStrings provides stringsFor(lang)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    ChevalierAppShell(
                        mainViewModel = mainViewModel,
                        schoolDataViewModel = schoolDataViewModel,
                        managementViewModel = managementViewModel,
                        currentLanguage = lang,
                        currentThemeMode = AppThemeMode.LIGHT
                    )
                }
            }
        }

        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeBy(5_100)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("language_selection_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("language_option_hi").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("language_continue_button").performScrollTo().performClick()

        awaitCondition {
            runCatching {
                composeTestRule.onNodeWithTag("language_selection_screen").assertDoesNotExist()
                composeTestRule.onNodeWithTag("home_screen_list").assertIsDisplayed()
                composeTestRule.onAllNodesWithText(stringsFor(AppLanguage.HINDI).socialHighlightsTitle)
                    .onFirst()
                    .assertIsDisplayed()
                runBlocking {
                    val persisted = app.container.userPreferencesRepository.preferencesFlow.first()
                    persisted.selectedLanguage == AppLanguage.HINDI && persisted.isLanguageSelectedOnLaunch
                }
            }.getOrDefault(false)
        }
    }
}

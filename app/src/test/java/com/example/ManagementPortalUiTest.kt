package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.stringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.ChevalierDatabase
import com.example.data.media.MediaStudioManager
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ManagementRepository
import com.example.ui.screens.management.ManagementPortalScreen
import com.example.ui.screens.management.ParentPortalAdminModule
import com.example.ui.theme.ChevalierAcademyTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ManagementViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ManagementPortalUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: ChevalierDatabase
    private lateinit var viewModelStore: ViewModelStore
    private lateinit var mainViewModel: MainViewModel
    private lateinit var managementViewModel: ManagementViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ChevalierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefs = UserPreferencesRepository(context)
        val media = MediaStudioManager(context)
        val mgmtRepo = ManagementRepository(database, media)
        val backup = BackupRestoreManager(context, database, prefs)
        viewModelStore = ViewModelStore()
        mainViewModel = ViewModelProvider(
            viewModelStore,
            MainViewModel.Factory(prefs)
        )[MainViewModel::class.java]
        managementViewModel = ViewModelProvider(
            viewModelStore,
            ManagementViewModel.Factory(mgmtRepo, backup)
        )[ManagementViewModel::class.java]
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idle()
        database.close()
    }

    @Test
    fun `first launch management portal prompts master admin setup, reveals recovery key, and unlocks dashboard`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    ManagementPortalScreen(
                        managementViewModel = managementViewModel,
                        mainViewModel = mainViewModel,
                        currentLanguage = AppLanguage.ENGLISH,
                        currentThemeMode = AppThemeMode.LIGHT,
                        onExitToPublicApp = {}
                    )
                }
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeTestRule.onNodeWithTag("master_admin_setup_screen").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        composeTestRule.onNodeWithTag("setup_pin_input").performScrollTo().performTextInput("246810")
        composeTestRule.onNodeWithTag("setup_confirm_pin_input").performScrollTo().performTextInput("246810")
        composeTestRule.onNodeWithTag("create_master_admin_btn").performScrollTo().performClick()

        composeTestRule.waitUntil(timeoutMillis = 8_000) {
            runCatching {
                composeTestRule.onNodeWithTag("master_recovery_key_card").performScrollTo().assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        composeTestRule.onNodeWithTag("generated_recovery_code_text").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_recovery_saved_btn").performScrollTo().performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeTestRule.onNodeWithTag("management_dashboard_shell").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        composeTestRule.onNodeWithTag("management_dashboard_grid").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_dashboard_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_hero_banner_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mgmt_header_back_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mgmt_top_search_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mgmt_top_notifications_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mgmt_top_menu_btn").assertIsDisplayed()
    }

    @Test
    fun `parent card opens all eleven parent portal management modules with dedicated screens actions scope and back navigation`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    ManagementPortalScreen(
                        managementViewModel = managementViewModel,
                        mainViewModel = mainViewModel,
                        currentLanguage = AppLanguage.ENGLISH,
                        currentThemeMode = AppThemeMode.LIGHT,
                        onExitToPublicApp = {}
                    )
                }
            }
        }

        // Complete first-time master admin setup to unlock Admin Dashboard
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeTestRule.onNodeWithTag("master_admin_setup_screen").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeTestRule.onNodeWithTag("setup_pin_input").performScrollTo().performTextInput("246810")
        composeTestRule.onNodeWithTag("setup_confirm_pin_input").performScrollTo().performTextInput("246810")
        composeTestRule.onNodeWithTag("create_master_admin_btn").performScrollTo().performClick()

        composeTestRule.waitUntil(timeoutMillis = 8_000) {
            runCatching {
                composeTestRule.onNodeWithTag("confirm_recovery_saved_btn").performScrollTo().assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeTestRule.onNodeWithTag("confirm_recovery_saved_btn").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeTestRule.onNodeWithTag("management_dashboard_grid").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        // 1. Open Parent card from the finalized 8-card Admin Dashboard grid
        composeTestRule.onNodeWithTag("management_dashboard_grid")
            .performScrollToNode(hasTestTag("admin_dash_card_parent"))
        composeTestRule.onNodeWithTag("admin_dash_card_parent").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_hub_parent").assertIsDisplayed()
        assertEquals(11, ParentPortalAdminModule.entries.size)

        // 2. Verify all 11 Parent Portal management modules are reachable inside Parent hub
        ParentPortalAdminModule.entries.forEach { module ->
            composeTestRule.onNodeWithTag("admin_hub_parent")
                .performScrollToNode(hasTestTag("ppm_submenu_card_${module.key}"))
            composeTestRule.onNodeWithTag("ppm_submenu_card_${module.key}").assertIsDisplayed()
        }

        // 3. Open Student Profile Management, verify scope chips, actions, Preview dialog, and TopBar Back
        composeTestRule.onNodeWithTag("admin_hub_parent")
            .performScrollToNode(hasTestTag("ppm_submenu_card_student_profile"))
        composeTestRule.onNodeWithTag("ppm_submenu_card_student_profile").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ppm_screen_student_profile").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ppm_screen_student_profile")
            .performScrollToNode(hasTestTag("ppm_scope_selector_card"))
        composeTestRule.onNodeWithTag("ppm_scope_individual").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ppm_scope_bulk_class").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ppm_screen_student_profile")
            .performScrollToNode(hasTestTag("ppm_actions_card"))
        composeTestRule.onNodeWithTag("ppm_action_add").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ppm_action_edit").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ppm_action_preview").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ppm_preview_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ppm_preview_dialog_close").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ppm_action_publish").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mgmt_subscreen_back_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_hub_parent").assertIsDisplayed()

        // 4. Open Emergency Information Management and return via screen Back button
        composeTestRule.onNodeWithTag("admin_hub_parent")
            .performScrollToNode(hasTestTag("ppm_submenu_card_emergency_info"))
        composeTestRule.onNodeWithTag("ppm_submenu_card_emergency_info").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ppm_screen_emergency_info").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ppm_screen_emergency_info")
            .performScrollToNode(hasTestTag("ppm_back_to_dashboard_btn"))
        composeTestRule.onNodeWithTag("ppm_back_to_dashboard_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_hub_parent").assertIsDisplayed()

        // 5. Return from Parent Hub to Admin Dashboard Home Page
        composeTestRule.onNodeWithTag("mgmt_subscreen_back_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("management_dashboard_grid").assertIsDisplayed()
    }
}

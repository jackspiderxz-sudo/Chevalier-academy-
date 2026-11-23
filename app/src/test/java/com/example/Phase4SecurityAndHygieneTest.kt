package com.example

import android.os.Looper
import android.view.WindowManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.window.SecureFlagPolicy
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.core.security.ScreenProtectionConfig
import com.example.ui.components.SecureDialogProperties
import com.example.ui.screens.management.ParentPortalAdminModule
import com.example.ui.screens.management.ParentPortalAdminModuleScreen
import com.example.ui.theme.ChevalierAcademyTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase4SecurityAndHygieneTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun tearDown() {
        ScreenProtectionConfig.setEnabledForTesting(null)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun parentPortalManagementDialogs_followCentralizedSecureDialogAndWindowConfig() {
        composeTestRule.setContent {
            ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                ParentPortalAdminModuleScreen(
                    module = ParentPortalAdminModule.STUDENT_PROFILE,
                    currentLanguage = AppLanguage.ENGLISH,
                    onBackToDashboard = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ppm_screen_student_profile")
            .performScrollToNode(hasTestTag("ppm_actions_card"))

        // 1. Default OFF state: SecureFlagPolicy.SecureOff and FLAG_SECURE OFF on both dialogs
        assertFalse(ScreenProtectionConfig.isScreenProtectionEnabled)
        assertEquals(SecureFlagPolicy.SecureOff, SecureDialogProperties.securePolicy)

        composeTestRule.onNodeWithTag("ppm_action_preview").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ppm_preview_dialog").assertIsDisplayed()
        val previewOffDialog = ShadowDialog.getLatestDialog()
        assertNotNull(previewOffDialog)
        val previewOffFlags = previewOffDialog!!.window?.attributes?.flags ?: 0
        assertFalse(
            "Expected FLAG_SECURE OFF on ppm_preview_dialog when setting is false",
            (previewOffFlags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        )
        composeTestRule.onNodeWithTag("ppm_preview_dialog_close").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ppm_action_add").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ppm_trial_action_dialog").assertIsDisplayed()
        val actionOffDialog = ShadowDialog.getLatestDialog()
        assertNotNull(actionOffDialog)
        val actionOffFlags = actionOffDialog!!.window?.attributes?.flags ?: 0
        assertFalse(
            "Expected FLAG_SECURE OFF on ppm_trial_action_dialog when setting is false",
            (actionOffFlags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        )
        composeTestRule.onNodeWithTag("ppm_trial_action_dialog_close").performClick()
        composeTestRule.waitForIdle()

        // 2. Enabled ON state: SecureFlagPolicy.SecureOn and FLAG_SECURE ON on both dialogs
        ScreenProtectionConfig.setEnabledForTesting(true)
        assertTrue(ScreenProtectionConfig.isScreenProtectionEnabled)
        assertEquals(SecureFlagPolicy.SecureOn, SecureDialogProperties.securePolicy)

        composeTestRule.onNodeWithTag("ppm_action_preview").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ppm_preview_dialog").assertIsDisplayed()
        val previewOnDialog = ShadowDialog.getLatestDialog()
        assertNotNull(previewOnDialog)
        val previewOnFlags = previewOnDialog!!.window?.attributes?.flags ?: 0
        assertTrue(
            "Expected FLAG_SECURE ON on ppm_preview_dialog when setting is true",
            (previewOnFlags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        )
        composeTestRule.onNodeWithTag("ppm_preview_dialog_close").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ppm_action_publish").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ppm_trial_action_dialog").assertIsDisplayed()
        val actionOnDialog = ShadowDialog.getLatestDialog()
        assertNotNull(actionOnDialog)
        val actionOnFlags = actionOnDialog!!.window?.attributes?.flags ?: 0
        assertTrue(
            "Expected FLAG_SECURE ON on ppm_trial_action_dialog when setting is true",
            (actionOnFlags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        )
        composeTestRule.onNodeWithTag("ppm_trial_action_dialog_close").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun legacyManagementLoginScreenRemoved_whileActiveManagementPortalScreenRemains() {
        val updatesClass = Class.forName("com.example.ui.screens.UpdatesAndSettingsScreensKt")
        val legacyMethods = updatesClass.declaredMethods.filter { it.name == "ManagementLoginScreen" }
        assertTrue(
            "Legacy ManagementLoginScreen composable must be removed from UpdatesAndSettingsScreensKt",
            legacyMethods.isEmpty()
        )

        val portalClass = Class.forName("com.example.ui.screens.management.ManagementPortalScreenKt")
        val activePortalMethods = portalClass.declaredMethods.filter { it.name == "ManagementPortalScreen" }
        assertTrue(
            "Active ManagementPortalScreen composable must remain intact",
            activePortalMethods.isNotEmpty()
        )
    }
}

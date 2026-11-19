package com.example

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.managementStringsFor
import com.example.core.i18n.stringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.ContentWorkflowStatus
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.StaffPermissionEntity
import com.example.data.media.MediaStudioManager
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ContentSnapshotSerializer
import com.example.data.repository.ManagementRepository
import com.example.data.repository.PublishSaveMode
import com.example.ui.screens.management.ApprovalCenterScreen
import com.example.ui.screens.management.BackupRestoreScreen
import com.example.ui.screens.management.NotificationsScreen
import com.example.ui.screens.management.StaffManagementScreen
import com.example.ui.theme.ChevalierAcademyTheme
import com.example.ui.viewmodel.ManagementAuthUiState
import com.example.ui.viewmodel.ManagementViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
class ManagementPhase1UxTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: ChevalierDatabase
    private lateinit var mgmtRepo: ManagementRepository
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var backupManager: BackupRestoreManager
    private lateinit var viewModelStore: ViewModelStore
    private lateinit var managementViewModel: ManagementViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ChevalierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefsRepo = UserPreferencesRepository(context)
        mgmtRepo = ManagementRepository(database, MediaStudioManager(context))
        backupManager = BackupRestoreManager(context, database, prefsRepo)
        viewModelStore = ViewModelStore()
        managementViewModel = ViewModelProvider(
            viewModelStore,
            ManagementViewModel.Factory(mgmtRepo, backupManager)
        )[ManagementViewModel::class.java]
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        database.close()
    }

    private fun authenticateMasterAdmin() {
        runBlocking {
            mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow()
        }
        managementViewModel.loginWithPin("admin", "246810")
        var spins = 0
        while (managementViewModel.authUiState.value !is ManagementAuthUiState.Authenticated && spins < 120) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(20)
            spins++
        }
        assertTrue(managementViewModel.authUiState.value is ManagementAuthUiState.Authenticated)
    }

    private fun awaitCondition(timeoutMs: Long = 5_000L, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            composeTestRule.waitForIdle()
            if (condition()) return
            Thread.sleep(20)
        }
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        composeTestRule.waitForIdle()
        assertTrue(
            "Timed out after ${timeoutMs}ms (vmError=${managementViewModel.errorMessage.value})",
            condition()
        )
    }

    private fun awaitTagDisplayed(tag: String, scrollTo: Boolean = false) {
        awaitCondition {
            runCatching {
                val node = composeTestRule.onNodeWithTag(tag, useUnmergedTree = false)
                if (scrollTo) node.performScrollTo()
                node.assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun awaitTagDoesNotExist(tag: String) {
        awaitCondition {
            runCatching {
                composeTestRule.onNodeWithTag(tag).assertDoesNotExist()
                true
            }.getOrDefault(false)
        }
    }

    @Test
    fun `reject dialog stays open with inline error on blank reason and closes after valid reason`() {
        authenticateMasterAdmin()
        runBlocking {
            val master = (managementViewModel.authUiState.value as ManagementAuthUiState.Authenticated).session
            val staff = mgmtRepo.createStaffAccount(
                actor = master,
                username = "writer1",
                displayName = "Writer One",
                role = ManagementRole.STAFF,
                initialPin = "112233",
                permissions = listOf(
                    StaffPermissionEntity(
                        accountId = 0,
                        sectionKey = ManagementSection.NEWS.key,
                        canView = true,
                        canCreate = true,
                        canEdit = true
                    )
                )
            ).getOrThrow()
            mgmtRepo.saveManagedContentRecord(
                actor = staff,
                section = ManagementSection.NEWS,
                recordId = 0L,
                recordTitle = "Pending Science Fair",
                proposedJson = ContentSnapshotSerializer.newsToJson(
                    NewsItemEntity(title = "Pending Science Fair", summary = "Summary", fullBody = "Body", publishedDateText = "13 Nov 2026")
                ),
                saveMode = PublishSaveMode.SUBMIT_FOR_APPROVAL,
                changeSummary = "Needs verification"
            ).getOrThrow()
        }

        val mStrings = managementStringsFor(AppLanguage.ENGLISH)
        val pendingId = runBlocking { mgmtRepo.pendingApprovals.first().single().id }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    ApprovalCenterScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                }
            }
        }
        awaitTagDisplayed("reject_btn_$pendingId")

        composeTestRule.onNodeWithTag("reject_btn_$pendingId").performClick()
        awaitTagDisplayed("reject_reason_input")

        // Submit blank reason -> dialog MUST remain open with inline error
        composeTestRule.onNodeWithTag("confirm_reject_submit_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reject_reason_input").assertIsDisplayed()
        composeTestRule.onNodeWithText(mStrings.approvalReasonRequiredError).assertIsDisplayed()

        // Verify pending approval was NOT rejected
        val stillPending = runBlocking { mgmtRepo.pendingApprovals.first() }
        assertEquals(1, stillPending.size)
        assertEquals(ContentWorkflowStatus.PENDING_APPROVAL, stillPending.single().status)

        // Enter valid reason -> dialog closes & approval becomes REJECTED
        composeTestRule.onNodeWithTag("reject_reason_input").performTextInput("Needs principal sign-off")
        composeTestRule.onNodeWithTag("confirm_reject_submit_btn").performClick()
        awaitCondition {
            runBlocking { mgmtRepo.pendingApprovals.first().isEmpty() }
        }
        awaitTagDoesNotExist("reject_reason_input")
    }

    @Test
    fun `staff deletion requires confirmation and preserves form data with inline error on invalid create`() {
        authenticateMasterAdmin()
        val staffId = runBlocking {
            val master = (managementViewModel.authUiState.value as ManagementAuthUiState.Authenticated).session
            mgmtRepo.createStaffAccount(
                actor = master,
                username = "helper1",
                displayName = "Campus Helper",
                role = ManagementRole.STAFF,
                initialPin = "123456",
                permissions = emptyList()
            ).getOrThrow().account.id
        }

        val mStrings = managementStringsFor(AppLanguage.ENGLISH)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    StaffManagementScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                }
            }
        }
        awaitTagDisplayed("delete_staff_btn_$staffId")

        // Open create dialog and submit short PIN -> dialog stays open, shows inline error, preserves inputs
        composeTestRule.onNodeWithTag("create_staff_account_btn").performClick()
        awaitTagDisplayed("staff_username_input")
        composeTestRule.onNodeWithTag("staff_username_input").performTextInput("newuser")
        composeTestRule.onNodeWithTag("staff_displayname_input").performTextInput("New User")
        composeTestRule.onNodeWithTag("staff_pin_input").performTextInput("12")
        composeTestRule.onNodeWithTag("staff_dialog_submit_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("staff_dialog_submit_btn").assertIsDisplayed()
        composeTestRule.onNodeWithText(mStrings.staffPinInvalidError).assertIsDisplayed()
        composeTestRule.onNodeWithText("New User").assertIsDisplayed()

        // Dismiss create dialog
        composeTestRule.onNodeWithText(mStrings.cancelBtn).performClick()
        awaitTagDoesNotExist("staff_dialog_submit_btn")

        // Tap delete on helper1 -> confirmation dialog appears, account NOT deleted yet
        composeTestRule.onNodeWithTag("delete_staff_btn_$staffId").performClick()
        awaitTagDisplayed("delete_staff_confirm_dialog")
        assertEquals(2, runBlocking { mgmtRepo.accountsWithPermissions.first().size })

        // Confirm deletion -> account removed and dialog closes
        composeTestRule.onNodeWithTag("confirm_delete_staff_btn").performClick()
        awaitCondition {
            runBlocking { mgmtRepo.accountsWithPermissions.first().size == 1 }
        }
        awaitTagDoesNotExist("delete_staff_confirm_dialog")
    }

    @Test
    fun `clear all notifications requires confirmation dialog before clearing`() {
        authenticateMasterAdmin()
        val mStrings = managementStringsFor(AppLanguage.ENGLISH)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    NotificationsScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                }
            }
        }
        awaitTagDisplayed("notifications_clear_all_btn")

        val initialCount = runBlocking { mgmtRepo.allNotifications.first().size }
        assertTrue(initialCount >= 1)

        composeTestRule.onNodeWithTag("notifications_clear_all_btn").performClick()
        awaitTagDisplayed("clear_notifications_confirm_dialog")
        assertEquals(initialCount, runBlocking { mgmtRepo.allNotifications.first().size })

        composeTestRule.onNodeWithTag("confirm_clear_notifications_btn").performClick()
        awaitCondition {
            runBlocking { mgmtRepo.allNotifications.first().isEmpty() }
        }
        awaitTagDoesNotExist("clear_notifications_confirm_dialog")
    }

    @Test
    fun `backup screen validates passwords before SAF picker and toggles visibility`() {
        authenticateMasterAdmin()
        val mStrings = managementStringsFor(AppLanguage.ENGLISH)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    BackupRestoreScreen(managementViewModel = managementViewModel, mStrings = mStrings)
                }
            }
        }
        composeTestRule.waitForIdle()

        // Short password blocks export SAF launch with inline error
        composeTestRule.onNodeWithTag("backup_create_password_input").performTextInput("123")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("export_backup_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(mStrings.backupPasswordTooShortError).assertIsDisplayed()

        // Mismatch blocks export SAF launch with inline error
        composeTestRule.onNodeWithTag("backup_create_password_input").performTextInput("456")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("backup_create_confirm_input").performTextInput("999999")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("export_backup_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(mStrings.backupPasswordMismatchError).assertIsDisplayed()

        // Password visibility toggles are present and clickable
        composeTestRule.onNodeWithTag("backup_create_password_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("backup_create_confirm_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("backup_restore_password_toggle").performClick()
        composeTestRule.waitForIdle()

        // Blank restore password blocks restore SAF launch with inline error
        composeTestRule.onNodeWithTag("inspect_restore_backup_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(mStrings.backupRestorePasswordRequiredError).assertIsDisplayed()

        // ViewModel-level blank password check on inspect & restore
        managementViewModel.inspectBackupUri(Uri.parse("content://test/backup.chvbak"), "   ")
        assertTrue(managementViewModel.errorMessage.value?.isNotBlank() == true)
    }

    @Test
    fun `management strings provide complete distinct EN TA HI translations and localized date formatting`() {
        val en = managementStringsFor(AppLanguage.ENGLISH)
        val ta = managementStringsFor(AppLanguage.TAMIL)
        val hi = managementStringsFor(AppLanguage.HINDI)

        assertNotEquals(en.statusPublished, ta.statusPublished)
        assertNotEquals(en.statusPublished, hi.statusPublished)
        assertNotEquals(ta.statusPublished, hi.statusPublished)

        assertNotEquals(en.deleteStaffTitle, ta.deleteStaffTitle)
        assertNotEquals(en.deleteStaffTitle, hi.deleteStaffTitle)
        assertNotEquals(en.approvalReasonRequiredError, ta.approvalReasonRequiredError)
        assertNotEquals(en.approvalReasonRequiredError, hi.approvalReasonRequiredError)
        assertNotEquals(en.backupPasswordMismatchError, ta.backupPasswordMismatchError)
        assertNotEquals(en.backupPasswordMismatchError, hi.backupPasswordMismatchError)

        val sampleTime = 1_763_000_000_000L
        assertTrue(en.formatTimestamp(sampleTime).isNotBlank())
        assertTrue(ta.formatTimestamp(sampleTime).isNotBlank())
        assertTrue(hi.formatTimestamp(sampleTime).isNotBlank())
        assertEquals("en", en.locale.language)
        assertEquals("ta", ta.locale.language)
        assertEquals("hi", hi.locale.language)
    }
}

package com.example

import android.content.Context
import android.os.Looper
import android.view.WindowManager
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.window.SecureFlagPolicy
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.managementStringsFor
import com.example.core.i18n.stringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.core.security.ScreenProtectionConfig
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.SchoolHighlightEntity
import com.example.data.local.entity.SchoolPostCategory
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostStatus
import com.example.data.local.entity.SchoolPostType
import com.example.data.media.YouTubeFetchedMetadata
import com.example.data.media.YouTubeMetadataFetcher
import com.example.data.preferences.UserPreferencesRepository
import com.example.ui.components.AnnouncementDetailDialog
import com.example.ui.components.NewsDetailDialog
import com.example.ui.components.PhotoLightboxDialog
import com.example.ui.components.SecureDialogProperties
import com.example.ui.components.SecureFullScreenDialogProperties
import com.example.ui.screens.AllHighlightsDialog
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.HomeSocialFeedScreen
import com.example.ui.screens.PublicYouTubeMetadataCache
import com.example.ui.screens.YouTubePlayerLifecycleHooks
import com.example.ui.screens.YouTubeVideoPlayerDialog
import com.example.ui.screens.management.UniversalContentEditorScaffold
import com.example.ui.theme.ChevalierAcademyTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeSocialPhase31HardeningTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var inMemoryDb: ChevalierDatabase
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var originalHttpProvider: (String) -> String?

    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        inMemoryDb = Room.inMemoryDatabaseBuilder(context, ChevalierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefsRepo = UserPreferencesRepository(context)
        originalHttpProvider = YouTubeMetadataFetcher.httpGetProvider
        YouTubeMetadataFetcher.httpGetProvider = { null }
        PublicYouTubeMetadataCache.clear()
        runBlocking {
            prefsRepo.restorePreferences(
                language = AppLanguage.ENGLISH,
                isOnboarded = true,
                themeMode = AppThemeMode.LIGHT,
                deviceAlertsEnabled = false,
                likedPostIds = emptySet(),
                bookmarkedPostIds = emptySet()
            )
        }
    }

    @After
    fun tearDown() {
        YouTubeMetadataFetcher.httpGetProvider = originalHttpProvider
        PublicYouTubeMetadataCache.clear()
        runBlocking {
            prefsRepo.restorePreferences(
                language = AppLanguage.ENGLISH,
                isOnboarded = true,
                themeMode = AppThemeMode.LIGHT,
                deviceAlertsEnabled = false,
                likedPostIds = emptySet(),
                bookmarkedPostIds = emptySet()
            )
        }
        inMemoryDb.close()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    // ---------------------------------------------------------------------------------------------
    // TASK 1 — YouTube WebView Lifecycle
    // ---------------------------------------------------------------------------------------------

    @Test
    fun task1_youtubePlayerDialog_pausesOnPause_resumesOnResume_andCleansUpObserverOnDismiss() {
        val owner = TestLifecycleOwner()
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        var showDialog by mutableStateOf(true)
        var pauseCount = 0
        var resumeCount = 0
        var cleanupCount = 0
        var externalOpenedUrl: String? = null

        composeTestRule.setContent {
            ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                CompositionLocalProvider(
                    LocalAppStrings provides stringsFor(AppLanguage.ENGLISH),
                    LocalLifecycleOwner provides owner
                ) {
                    if (showDialog) {
                        YouTubeVideoPlayerDialog(
                            youtubeUrl = "https://www.youtube.com/watch?v=M7lc1UVf-VE",
                            title = "Annual Day Highlights",
                            onDismiss = { showDialog = false },
                            onOpenExternal = { externalOpenedUrl = it },
                            lifecycleHooks = YouTubePlayerLifecycleHooks(
                                onPause = { pauseCount++ },
                                onResume = { resumeCount++ },
                                onDisposeCleanup = { cleanupCount++ }
                            )
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        // Observer attached while dialog is active
        assertTrue(owner.registry.observerCount >= 1)
        assertEquals(0, pauseCount)
        assertEquals(0, resumeCount)

        // External fallback button still works
        composeTestRule.onNodeWithTag("youtube_player_external_fallback_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals("https://www.youtube.com/watch?v=M7lc1UVf-VE", externalOpenedUrl)

        // Activity receives ON_PAUSE -> WebView pauses
        composeTestRule.runOnIdle {
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        composeTestRule.waitForIdle()
        assertEquals(1, pauseCount)
        assertEquals(0, resumeCount)

        // Activity returns to ON_RESUME -> WebView resumes
        composeTestRule.runOnIdle {
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        composeTestRule.waitForIdle()
        assertEquals(1, pauseCount)
        assertEquals(1, resumeCount)

        // Dismiss dialog -> playback cleaned up and lifecycle observer removed
        composeTestRule.onNodeWithTag("youtube_player_close_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, cleanupCount)
        assertEquals(0, owner.registry.observerCount)

        // Subsequent lifecycle events after dismiss do not invoke callbacks
        composeTestRule.runOnIdle {
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        composeTestRule.waitForIdle()
        assertEquals(1, pauseCount)
        assertEquals(1, resumeCount)
    }

    // ---------------------------------------------------------------------------------------------
    // TASK 2 — FLAG_SECURE for Remaining Dialogs
    // ---------------------------------------------------------------------------------------------

    @Test
    fun task2_publicAndAdminDialogs_followCentralizedScreenProtectionConfig() {
        var activeDialog by mutableStateOf("none")
        val news = NewsItemEntity(
            id = 1L,
            title = "Science Exhibition",
            summary = "Annual showcase",
            fullBody = "Students exhibited robotics and green energy prototypes.",
            publishedDateText = "12 Aug 2026",
            isFeatured = true,
            isVerified = true,
            isPublished = true,
            isApproved = true
        )
        val announcement = AnnouncementEntity(
            id = 2L,
            title = "Term Exam Notice",
            message = "Timetable published on notice board.",
            issuedDateText = "15 Aug 2026",
            priorityLabel = "Important",
            isVerified = true,
            isPublished = true,
            isApproved = true
        )
        val photo = GalleryMediaEntity(
            id = 3L,
            albumId = 1L,
            title = "Campus Gate",
            caption = "Main entrance",
            mediaUri = "file:///nonexistent/campus.jpg",
            mediaType = "IMAGE",
            isVerified = true,
            isPublished = true,
            isApproved = true
        )
        val highlight = SchoolHighlightEntity(
            id = 4L,
            youtubeShortsUrl = "https://www.youtube.com/shorts/M7lc1UVf-VE",
            title = "Morning Assembly",
            status = SchoolPostStatus.PUBLISHED,
            createdAtMillis = 1_000L,
            publishedAtMillis = 1_000L,
            expiresAtMillis = 86_401_000L
        )

        composeTestRule.setContent {
            ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                    when (activeDialog) {
                        "news" -> NewsDetailDialog(newsItem = news, onDismiss = {})
                        "announcement" -> AnnouncementDetailDialog(announcement = announcement, onDismiss = {})
                        "lightbox" -> PhotoLightboxDialog(
                            photos = listOf(photo),
                            currentIndex = 0,
                            onIndexChange = {},
                            onDismiss = {}
                        )
                        "highlights" -> AllHighlightsDialog(
                            activeHighlights = listOf(highlight),
                            nowMillis = 2_000L,
                            onPlayHighlight = { _, _ -> },
                            onDismiss = {}
                        )
                        "editor" -> UniversalContentEditorScaffold(
                            mStrings = managementStringsFor(AppLanguage.ENGLISH),
                            title = "Edit School Post",
                            canEdit = true,
                            canPublish = true,
                            isDirty = false,
                            isBusy = false,
                            isFullScreenDialog = true,
                            onClose = {},
                            onSaveAction = {},
                            editContent = { Text("Editor Body") },
                            previewContent = { Text("Preview Body") }
                        )
                    }
                }
            }
        }

        val cases = listOf("news", "announcement", "lightbox", "highlights", "editor")

        // 1. Default OFF state: SecureFlagPolicy.SecureOff and FLAG_SECURE absent on all dialogs
        assertFalse(ScreenProtectionConfig.isScreenProtectionEnabled)
        assertEquals(SecureFlagPolicy.SecureOff, SecureDialogProperties.securePolicy)
        assertEquals(SecureFlagPolicy.SecureOff, SecureFullScreenDialogProperties.securePolicy)

        for (dialogKey in cases) {
            composeTestRule.runOnIdle { activeDialog = dialogKey }
            composeTestRule.waitForIdle()
            val latestDialog = ShadowDialog.getLatestDialog()
            assertNotNull("Expected dialog for $dialogKey", latestDialog)
            val flags = latestDialog!!.window?.attributes?.flags ?: 0
            assertFalse(
                "Expected FLAG_SECURE OFF on $dialogKey dialog window when centralized setting is false",
                (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
            )
            composeTestRule.runOnIdle { activeDialog = "none" }
            composeTestRule.waitForIdle()
        }

        // 2. Centralized ON state: SecureFlagPolicy.SecureOn and FLAG_SECURE active on all dialogs
        ScreenProtectionConfig.setEnabledForTesting(true)
        try {
            assertTrue(ScreenProtectionConfig.isScreenProtectionEnabled)
            assertEquals(SecureFlagPolicy.SecureOn, SecureDialogProperties.securePolicy)
            assertEquals(SecureFlagPolicy.SecureOn, SecureFullScreenDialogProperties.securePolicy)

            for (dialogKey in cases) {
                composeTestRule.runOnIdle { activeDialog = dialogKey }
                composeTestRule.waitForIdle()
                val latestDialog = ShadowDialog.getLatestDialog()
                assertNotNull("Expected dialog for $dialogKey", latestDialog)
                val flags = latestDialog!!.window?.attributes?.flags ?: 0
                assertTrue(
                    "Expected FLAG_SECURE ON on $dialogKey dialog window when centralized setting is true",
                    (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
                )
                composeTestRule.runOnIdle { activeDialog = "none" }
                composeTestRule.waitForIdle()
            }
        } finally {
            ScreenProtectionConfig.setEnabledForTesting(null)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // TASK 3 — Tamil and Hindi Localization
    // ---------------------------------------------------------------------------------------------

    @Test
    fun task3_socialFeedStrings_areLocalizedInEnglishTamilAndHindi_andRenderInUi() {
        val en = stringsFor(AppLanguage.ENGLISH)
        val ta = stringsFor(AppLanguage.TAMIL)
        val hi = stringsFor(AppLanguage.HINDI)

        // Verify distinct non-blank localized labels across all 3 languages
        val checks = listOf(
            Triple(en.titleBookmarks, ta.titleBookmarks, hi.titleBookmarks),
            Triple(en.socialHighlightsTitle, ta.socialHighlightsTitle, hi.socialHighlightsTitle),
            Triple(en.socialMoreLabel, ta.socialMoreLabel, hi.socialMoreLabel),
            Triple(en.socialFeedTitle, ta.socialFeedTitle, hi.socialFeedTitle),
            Triple(en.socialAllPostsFilter, ta.socialAllPostsFilter, hi.socialAllPostsFilter),
            Triple(en.socialLikeLabel, ta.socialLikeLabel, hi.socialLikeLabel),
            Triple(en.socialLikedLabel, ta.socialLikedLabel, hi.socialLikedLabel),
            Triple(en.socialBookmarkLabel, ta.socialBookmarkLabel, hi.socialBookmarkLabel),
            Triple(en.socialBookmarkedLabel, ta.socialBookmarkedLabel, hi.socialBookmarkedLabel),
            Triple(en.socialPinnedBadge, ta.socialPinnedBadge, hi.socialPinnedBadge),
            Triple(en.socialHighlightsEmptyTitle, ta.socialHighlightsEmptyTitle, hi.socialHighlightsEmptyTitle),
            Triple(en.socialFeedEmptyTitle, ta.socialFeedEmptyTitle, hi.socialFeedEmptyTitle),
            Triple(en.socialBookmarksEmptyTitle, ta.socialBookmarksEmptyTitle, hi.socialBookmarksEmptyTitle),
            Triple(en.socialEmbedUnavailableTitle, ta.socialEmbedUnavailableTitle, hi.socialEmbedUnavailableTitle),
            Triple(en.socialPlayHighlightDesc, ta.socialPlayHighlightDesc, hi.socialPlayHighlightDesc),
            Triple(en.socialPlayYouTubeVideoDesc, ta.socialPlayYouTubeVideoDesc, hi.socialPlayYouTubeVideoDesc)
        )
        checks.forEach { (e, t, h) ->
            assertTrue("English string blank", e.isNotBlank())
            assertTrue("Tamil string blank", t.isNotBlank())
            assertTrue("Hindi string blank", h.isNotBlank())
            assertNotEquals("Tamil should differ from English for '$e'", e, t)
            assertNotEquals("Hindi should differ from English for '$e'", e, h)
            assertNotEquals("Tamil and Hindi should differ for '$e'", t, h)
        }

        // Internal category constants remain untranslated
        assertEquals(
            listOf("General", "Academics", "Events", "Sports", "Achievements", "Campus Life", "Announcement"),
            SchoolPostCategory.defaults
        )

        // Verify Tamil rendering on HomeSocialFeedScreen
        var currentLang by mutableStateOf(AppLanguage.TAMIL)
        val profile = ChevalierDatabase.verifiedDefaultProfile()
        val pinnedPost = SchoolPostEntity(
            id = 50L,
            postType = SchoolPostType.ADMIN_CUSTOM_VIDEO,
            category = SchoolPostCategory.EVENTS,
            title = "Annual Sports Meet",
            description = "Official school event",
            youtubeUrl = "https://www.youtube.com/watch?v=M7lc1UVf-VE",
            status = SchoolPostStatus.PUBLISHED,
            isFeatured = true,
            createdAtMillis = 1_000_000L,
            publishedAtMillis = 1_000_000L,
            updatedAtMillis = 1_000_000L
        )

        composeTestRule.setContent {
            ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                CompositionLocalProvider(LocalAppStrings provides stringsFor(currentLang)) {
                    HomeSocialFeedScreen(
                        profile = profile,
                        heroPhotos = emptyList(),
                        publishedPosts = listOf(pinnedPost),
                        publishedHighlights = emptyList(),
                        likedPostIds = emptySet(),
                        bookmarkedPostIds = emptySet(),
                        onToggleLike = {},
                        onToggleBookmark = {},
                        onNavigate = {},
                        nowMillisProvider = { 2_000_000L }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("home_section_highlights"))
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText(ta.socialHighlightsTitle).onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("home_section_social_feed"))
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText(ta.socialFeedTitle).onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText(ta.socialAllPostsFilter).onFirst().assertIsDisplayed()

        // Switch dynamically to Hindi and verify Hindi strings render cleanly
        composeTestRule.runOnIdle { currentLang = AppLanguage.HINDI }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("home_section_highlights"))
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText(hi.socialHighlightsTitle).onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("home_section_social_feed"))
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText(hi.socialFeedTitle).onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText(hi.socialAllPostsFilter).onFirst().assertIsDisplayed()
    }

    @Test
    fun task3_bookmarksEmptyState_rendersTamilAndHindiLocalization() {
        val ta = stringsFor(AppLanguage.TAMIL)
        val hi = stringsFor(AppLanguage.HINDI)
        var currentLang by mutableStateOf(AppLanguage.TAMIL)

        composeTestRule.setContent {
            ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                CompositionLocalProvider(LocalAppStrings provides stringsFor(currentLang)) {
                    BookmarksScreen(
                        profile = ChevalierDatabase.verifiedDefaultProfile(),
                        publishedPosts = emptyList(),
                        likedPostIds = emptySet(),
                        bookmarkedPostIds = emptySet(),
                        onToggleLike = {},
                        onToggleBookmark = {},
                        onExploreFeed = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(ta.socialBookmarksEmptyTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(ta.socialBrowseFeedBtn).assertIsDisplayed()

        composeTestRule.runOnIdle { currentLang = AppLanguage.HINDI }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(hi.socialBookmarksEmptyTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(hi.socialBrowseFeedBtn).assertIsDisplayed()
    }

    // ---------------------------------------------------------------------------------------------
    // TASK 4 — Highlight Timer Lifecycle Optimization
    // ---------------------------------------------------------------------------------------------

    @Test
    fun task4_highlightTimer_stopsInBackground_andRefreshesImmediatelyOnForegroundReturn() {
        val owner = TestLifecycleOwner()
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        val baseTime = 10_000_000L
        var simulatedNow = baseTime
        val clockReads = AtomicInteger(0)

        val highlight = SchoolHighlightEntity(
            id = 77L,
            youtubeShortsUrl = "https://www.youtube.com/shorts/M7lc1UVf-VE",
            title = "Morning Assembly Short",
            status = SchoolPostStatus.PUBLISHED,
            createdAtMillis = baseTime,
            publishedAtMillis = baseTime,
            expiresAtMillis = baseTime + 60_000L // expires 1 minute after baseTime
        )

        composeTestRule.setContent {
            ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                CompositionLocalProvider(
                    LocalAppStrings provides stringsFor(AppLanguage.ENGLISH),
                    LocalLifecycleOwner provides owner
                ) {
                    HomeSocialFeedScreen(
                        profile = ChevalierDatabase.verifiedDefaultProfile(),
                        heroPhotos = emptyList(),
                        publishedPosts = emptyList(),
                        publishedHighlights = listOf(highlight),
                        likedPostIds = emptySet(),
                        bookmarkedPostIds = emptySet(),
                        onToggleLike = {},
                        onToggleBookmark = {},
                        onNavigate = {},
                        nowMillisProvider = {
                            clockReads.incrementAndGet()
                            simulatedNow
                        }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // Active in foreground (scroll to Highlights section)
        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("home_section_highlights"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("highlight_card_77").assertIsDisplayed()

        // Move app to background (ON_STOP -> CREATED) and advance virtual clock past expiry
        composeTestRule.runOnIdle {
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
        composeTestRule.waitForIdle()
        val readsWhenStopped = clockReads.get()

        simulatedNow = baseTime + 120_000L // past expiry while backgrounded
        composeTestRule.mainClock.advanceTimeBy(90_000L)
        composeTestRule.waitForIdle()

        // Timer must NOT wake up while backgrounded
        assertEquals(
            "Highlight timer should not poll while backgrounded",
            readsWhenStopped,
            clockReads.get()
        )

        // Return to foreground (ON_START -> STARTED) -> immediate refresh hides expired highlight
        composeTestRule.runOnIdle {
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }
        composeTestRule.waitForIdle()
        assertTrue(
            "Highlight timer should refresh immediately on foreground return",
            clockReads.get() > readsWhenStopped
        )
        composeTestRule.onNodeWithTag("home_highlights_empty_card").assertIsDisplayed()
    }

    // ---------------------------------------------------------------------------------------------
    // TASK 5 — YouTube Metadata Cache Limit (LRU max 100 + Thread Safety)
    // ---------------------------------------------------------------------------------------------

    private fun sampleMeta(id: String, title: String) = YouTubeFetchedMetadata(
        videoId = id,
        canonicalUrl = "https://www.youtube.com/watch?v=$id",
        isShort = false,
        thumbnailUrl = "https://img.youtube.com/vi/$id/hqdefault.jpg",
        maxResThumbnailUrl = "https://img.youtube.com/vi/$id/maxresdefault.jpg",
        title = title,
        description = "Desc $id",
        metadataAvailable = true
    )

    @Test
    fun task5_publicYouTubeMetadataCache_enforcesMax100LruAndThreadSafety() {
        PublicYouTubeMetadataCache.clear()
        assertEquals(100, PublicYouTubeMetadataCache.MAX_ENTRIES)

        // Insert 120 entries -> size capped at 100, oldest 20 evicted
        for (i in 0 until 120) {
            PublicYouTubeMetadataCache.put(sampleMeta("vid_$i", "Title $i"))
        }
        assertEquals(100, PublicYouTubeMetadataCache.size())
        assertNull(PublicYouTubeMetadataCache.get("vid_0"))
        assertNull(PublicYouTubeMetadataCache.get("vid_19"))
        assertNotNull(PublicYouTubeMetadataCache.get("vid_20"))
        assertNotNull(PublicYouTubeMetadataCache.get("vid_119"))

        // Verify LRU access-order eviction: accessing oldest retained entry keeps it alive
        PublicYouTubeMetadataCache.clear()
        for (i in 0 until 100) {
            PublicYouTubeMetadataCache.put(sampleMeta("lru_$i", "LRU $i"))
        }
        // Touch lru_0 so lru_1 becomes least-recently-used
        assertEquals("LRU 0", PublicYouTubeMetadataCache.get("lru_0")?.title)
        PublicYouTubeMetadataCache.put(sampleMeta("lru_100", "LRU 100"))
        assertEquals(100, PublicYouTubeMetadataCache.size())
        assertNotNull("Recently accessed lru_0 should be kept", PublicYouTubeMetadataCache.get("lru_0"))
        assertNull("Least-recently-used lru_1 should be evicted", PublicYouTubeMetadataCache.get("lru_1"))
        assertNotNull(PublicYouTubeMetadataCache.get("lru_100"))

        // Thread-safety stress check under concurrent reads/writes
        PublicYouTubeMetadataCache.clear()
        val executor = Executors.newFixedThreadPool(8)
        try {
            val tasks = (0 until 8).map { worker ->
                Callable {
                    for (step in 0 until 200) {
                        val id = "concurrent_${(worker * 31 + step) % 150}"
                        PublicYouTubeMetadataCache.put(sampleMeta(id, "T-$id"))
                        PublicYouTubeMetadataCache.get(id)
                    }
                }
            }
            executor.invokeAll(tasks).forEach { it.get() }
        } finally {
            executor.shutdownNow()
        }
        assertTrue(
            "Cache size must never exceed MAX_ENTRIES after concurrent access",
            PublicYouTubeMetadataCache.size() <= PublicYouTubeMetadataCache.MAX_ENTRIES
        )
    }

    // ---------------------------------------------------------------------------------------------
    // TASK 6 — Backup Restore Like/Bookmark ID Reconciliation
    // ---------------------------------------------------------------------------------------------

    @Test
    fun task6_backupRestore_reconcilesLikedAndBookmarkedPostIds_andRollsBackOnFailure() = runBlocking {
        val backupManager = BackupRestoreManager(context, inMemoryDb, prefsRepo)

        // Seed 2 posts and create encrypted backup snapshot
        val postA = SchoolPostEntity(
            id = 10L,
            postType = SchoolPostType.ADMIN_CUSTOM_VIDEO,
            category = SchoolPostCategory.ACADEMICS,
            title = "Backed Up Post A",
            description = "Description A",
            youtubeUrl = "https://www.youtube.com/watch?v=M7lc1UVf-VE",
            status = SchoolPostStatus.PUBLISHED,
            isFeatured = false,
            createdAtMillis = 10_000L,
            publishedAtMillis = 10_000L,
            updatedAtMillis = 10_000L
        )
        val postB = SchoolPostEntity(
            id = 20L,
            postType = SchoolPostType.ADMIN_CUSTOM_VIDEO,
            category = SchoolPostCategory.SPORTS,
            title = "Backed Up Post B",
            description = "Description B",
            youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            status = SchoolPostStatus.PUBLISHED,
            isFeatured = false,
            createdAtMillis = 20_000L,
            publishedAtMillis = 20_000L,
            updatedAtMillis = 20_000L
        )
        inMemoryDb.schoolPostDao().upsert(postA)
        inMemoryDb.schoolPostDao().upsert(postB)

        val encryptedBytes = backupManager.createEncryptedBackupBytes("ReconcilePass123!").getOrThrow()

        // After backup, user creates a newer post (id=30) and likes/bookmarks valid + stale IDs
        inMemoryDb.schoolPostDao().upsert(
            postA.copy(id = 30L, title = "Post Created After Backup")
        )
        prefsRepo.togglePostLike(10L)
        prefsRepo.togglePostLike(30L)
        prefsRepo.togglePostLike(999L)
        prefsRepo.togglePostBookmark(20L)
        prefsRepo.togglePostBookmark(30L)
        prefsRepo.togglePostBookmark(888L)

        val beforeRestore = prefsRepo.preferencesFlow.first()
        assertEquals(setOf(10L, 30L, 999L), beforeRestore.likedPostIds)
        assertEquals(setOf(20L, 30L, 888L), beforeRestore.bookmarkedPostIds)

        // Restore backup (contains only posts 10 and 20)
        backupManager.restoreEncryptedBackupBytes(
            bytes = encryptedBytes,
            password = "ReconcilePass123!",
            actorName = "Master Admin"
        ).getOrThrow()

        // Valid IDs (10, 20) preserved; stale/missing IDs (30, 888, 999) pruned
        val afterRestore = prefsRepo.preferencesFlow.first()
        assertEquals(setOf(10L), afterRestore.likedPostIds)
        assertEquals(setOf(20L), afterRestore.bookmarkedPostIds)

        // Verify rollback on failed restore preserves pre-failure Like & Bookmark state
        prefsRepo.togglePostLike(555L)
        prefsRepo.togglePostBookmark(666L)
        val beforeFailedRestore = prefsRepo.preferencesFlow.first()
        assertEquals(setOf(10L, 555L), beforeFailedRestore.likedPostIds)
        assertEquals(setOf(20L, 666L), beforeFailedRestore.bookmarkedPostIds)

        val failedResult = backupManager.restoreEncryptedBackupBytes(
            bytes = encryptedBytes,
            password = "ReconcilePass123!",
            actorName = "Master Admin",
            simulateFailureAfterClearForTest = true
        )
        assertTrue(failedResult.isFailure)

        val afterRollback = prefsRepo.preferencesFlow.first()
        assertEquals(setOf(10L, 555L), afterRollback.likedPostIds)
        assertEquals(setOf(20L, 666L), afterRollback.bookmarkedPostIds)
        assertFalse(afterRollback.likedPostIds.isEmpty())
        assertFalse(afterRollback.bookmarkedPostIds.isEmpty())
    }
}

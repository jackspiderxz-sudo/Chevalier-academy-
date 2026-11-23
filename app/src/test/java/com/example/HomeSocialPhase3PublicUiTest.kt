package com.example

import android.content.Context
import android.os.Looper
import android.view.WindowManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.stringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.core.security.ScreenProtectionConfig
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.SchoolHighlightEntity
import com.example.data.local.entity.SchoolPostCategory
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostStatus
import com.example.data.local.entity.SchoolPostType
import com.example.data.media.MediaStudioManager
import com.example.data.media.YouTubeMetadataFetcher
import com.example.data.repository.SocialFeedRepository
import com.example.ui.navigation.AppRoute
import com.example.ui.navigation.ChevalierAppShell
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.HomeSocialFeedScreen
import com.example.ui.screens.PublicYouTubeMetadataCache
import com.example.ui.theme.ChevalierAcademyTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ManagementViewModel
import com.example.ui.viewmodel.PreferencesUiState
import com.example.ui.viewmodel.SchoolDataViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeSocialPhase3PublicUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var inMemoryDb: ChevalierDatabase
    private lateinit var socialRepo: SocialFeedRepository
    private val testViewModelStores = mutableListOf<ViewModelStore>()
    private lateinit var originalHttpProvider: (String) -> String?

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        inMemoryDb = Room.inMemoryDatabaseBuilder(context, ChevalierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        socialRepo = SocialFeedRepository.fromDatabase(inMemoryDb, MediaStudioManager(context))
        originalHttpProvider = YouTubeMetadataFetcher.httpGetProvider
        YouTubeMetadataFetcher.httpGetProvider = { null } // Offline deterministic default
        PublicYouTubeMetadataCache.clear()
    }

    @After
    fun tearDown() {
        YouTubeMetadataFetcher.httpGetProvider = originalHttpProvider
        PublicYouTubeMetadataCache.clear()
        composeTestRule.mainClock.autoAdvance = true
        testViewModelStores.forEach { it.clear() }
        testViewModelStores.clear()
        inMemoryDb.close()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    // ==================== 1. FLAG_SECURE WINDOW PROTECTION ====================

    @Test
    fun `main activity follows centralized ScreenProtectionConfig (OFF by default, ON when enabled) while keeping status bar visible`() {
        assertFalse(
            "Centralized screenshot protection must default to false",
            ScreenProtectionConfig.DEFAULT_ENABLE_SCREEN_PROTECTION
        )
        assertFalse(
            "Screen protection must be OFF by default",
            ScreenProtectionConfig.isScreenProtectionEnabled
        )

        // 1. Default OFF verification
        val defaultController = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = defaultController.get()
            val flags = activity.window.attributes.flags
            assertFalse(
                "MainActivity window must keep FLAG_SECURE OFF when centralized setting is false",
                (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
            )
            @Suppress("DEPRECATION")
            assertFalse(
                "MainActivity window must not enable FLAG_FULLSCREEN",
                (flags and WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0
            )
        } finally {
            defaultController.pause().stop().destroy()
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }

        // 2. Centralized ON verification
        ScreenProtectionConfig.setEnabledForTesting(true)
        try {
            val protectedController = Robolectric.buildActivity(MainActivity::class.java).setup()
            try {
                val activity = protectedController.get()
                val flags = activity.window.attributes.flags
                assertTrue(
                    "MainActivity window must enable FLAG_SECURE when centralized setting is true",
                    (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
                )
            } finally {
                protectedController.pause().stop().destroy()
                Shadows.shadowOf(Looper.getMainLooper()).idle()
            }
        } finally {
            ScreenProtectionConfig.setEnabledForTesting(null)
        }
    }

    // ==================== 2. PUBLISHED / UNPUBLISHED / ARCHIVED / DELETED VISIBILITY & CATEGORY FILTERING ====================

    @Test
    fun `home feed displays only published posts newest-first, filters categories, and hides unpublished archived or deleted posts`() {
        val now = 1_750_000_000_000L
        val publishedSports = SchoolPostEntity(
            id = 101L,
            postType = SchoolPostType.ADMIN_CUSTOM_VIDEO,
            category = SchoolPostCategory.SPORTS,
            title = "Annual Sports Meet Finals",
            description = "Track and field highlights",
            youtubeUrl = "https://youtu.be/dQw4w9WgXcQ",
            status = SchoolPostStatus.PUBLISHED,
            createdAtMillis = now - 2_000L,
            publishedAtMillis = now - 2_000L
        )
        val publishedEvents = SchoolPostEntity(
            id = 102L,
            postType = SchoolPostType.PHOTO,
            category = SchoolPostCategory.EVENTS,
            title = "Science Expo 2026",
            description = "Student innovations",
            photoUrisJson = SchoolPostEntity.encodePhotoUris(listOf("file:///tmp/expo1.jpg")),
            status = SchoolPostStatus.PUBLISHED,
            createdAtMillis = now,
            publishedAtMillis = now
        )
        val unpublishedDraft = SchoolPostEntity(
            id = 103L,
            postType = SchoolPostType.PHOTO,
            category = SchoolPostCategory.GENERAL,
            title = "Secret Draft Post",
            photoUrisJson = SchoolPostEntity.encodePhotoUris(listOf("file:///tmp/draft.jpg")),
            status = SchoolPostStatus.UNPUBLISHED
        )
        val archivedPost = SchoolPostEntity(
            id = 104L,
            postType = SchoolPostType.PHOTO,
            category = SchoolPostCategory.GENERAL,
            title = "Old Archived Post",
            photoUrisJson = SchoolPostEntity.encodePhotoUris(listOf("file:///tmp/old.jpg")),
            status = SchoolPostStatus.ARCHIVED
        )

        var postsState by mutableStateOf(listOf(publishedSports, publishedEvents, unpublishedDraft, archivedPost))

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    HomeSocialFeedScreen(
                        profile = ChevalierDatabase.verifiedDefaultProfile(),
                        heroPhotos = emptyList(),
                        publishedPosts = postsState,
                        publishedHighlights = emptyList(),
                        likedPostIds = emptySet(),
                        bookmarkedPostIds = emptySet(),
                        onToggleLike = {},
                        onToggleBookmark = {},
                        onNavigate = {},
                        nowMillisProvider = { now }
                    )
                }
            }
        }

        val list = composeTestRule.onNodeWithTag("home_screen_list")

        // Verify section order: Hero Banner directly followed by Highlights and Social Feed, with Welcome panel removed
        list.performScrollToNode(hasTestTag("home_section_hero"))
        composeTestRule.onNodeWithTag("home_section_hero").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_section_welcome").assertDoesNotExist()
        list.performScrollToNode(hasTestTag("home_section_highlights"))
        composeTestRule.onNodeWithTag("home_section_highlights").assertIsDisplayed()
        val heroBounds = composeTestRule.onNodeWithTag("home_section_hero").fetchSemanticsNode().boundsInRoot
        val highlightsBounds = composeTestRule.onNodeWithTag("home_section_highlights").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Highlights section should appear directly below Hero Banner",
            highlightsBounds.top >= heroBounds.bottom
        )
        list.performScrollToNode(hasTestTag("home_section_social_feed"))
        composeTestRule.onNodeWithTag("home_section_social_feed").assertIsDisplayed()

        // Only published posts appear; unpublished and archived never appear
        list.performScrollToNode(hasTestTag("social_post_card_102"))
        composeTestRule.onNodeWithTag("social_post_card_102").assertIsDisplayed()
        list.performScrollToNode(hasTestTag("social_post_card_101"))
        composeTestRule.onNodeWithTag("social_post_card_101").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("social_post_card_103").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("social_post_card_104").assertCountEquals(0)

        // Category filter: Sports -> only Sports post remains
        list.performScrollToNode(hasTestTag("home_feed_category_row"))
        composeTestRule.onNodeWithTag("home_feed_category_row")
            .performScrollToNode(hasTestTag("feed_filter_sports"))
        composeTestRule.onNodeWithTag("feed_filter_sports").performClick()
        composeTestRule.waitForIdle()

        list.performScrollToNode(hasTestTag("social_post_card_101"))
        composeTestRule.onNodeWithTag("social_post_card_101").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("social_post_card_102").assertCountEquals(0)

        // Empty category -> shows reset button -> back to All Posts
        list.performScrollToNode(hasTestTag("home_feed_category_row"))
        composeTestRule.onNodeWithTag("home_feed_category_row")
            .performScrollToNode(hasTestTag("feed_filter_academics"))
        composeTestRule.onNodeWithTag("feed_filter_academics").performClick()
        composeTestRule.waitForIdle()
        list.performScrollToNode(hasTestTag("feed_empty_reset_filter_btn"))
        composeTestRule.onNodeWithTag("feed_empty_reset_filter_btn").performClick()
        composeTestRule.waitForIdle()
        list.performScrollToNode(hasTestTag("social_post_card_102"))
        composeTestRule.onNodeWithTag("social_post_card_102").assertIsDisplayed()

        // Deleting a post removes it from public visibility
        postsState = listOf(publishedSports)
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("social_post_card_102").assertCountEquals(0)
        list.performScrollToNode(hasTestTag("social_post_card_101"))
        composeTestRule.onNodeWithTag("social_post_card_101").assertIsDisplayed()
    }

    // ==================== 3. 24-HOUR HIGHLIGHTS: 4 VERTICAL SHORTS + 5TH "MORE" CARD + EXPIRY ====================

    @Test
    fun `highlights show four vertical shorts plus fifth More card and auto-hide expired highlights without deleting db rows`() {
        val now = 1_750_000_000_000L
        runBlocking {
            // Insert 5 active highlights + 1 expired highlight into Room
            for (i in 1..5) {
                socialRepo.saveHighlight(
                    title = "Active Short #$i",
                    youtubeShortsUrl = "https://www.youtube.com/shorts/dQw4w9WgXcQ",
                    status = SchoolPostStatus.PUBLISHED,
                    nowMillis = now - (i * 10_000L)
                ).getOrThrow()
            }
            val expiredId = socialRepo.saveHighlight(
                title = "Expired Yesterday Short",
                youtubeShortsUrl = "https://www.youtube.com/shorts/9bZkp7q19f0",
                status = SchoolPostStatus.PUBLISHED,
                nowMillis = now - SchoolHighlightEntity.HIGHLIGHT_TTL_MILLIS - 60_000L
            ).getOrThrow()
            assertNotNull(inMemoryDb.schoolHighlightDao().getById(expiredId))
        }

        val allPublished = runBlocking {
            inMemoryDb.schoolHighlightDao().getAllHighlightsOnce()
                .filter { it.status == SchoolPostStatus.PUBLISHED }
        }
        assertEquals(6, allPublished.size)

        var playedUrl: String? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    HomeSocialFeedScreen(
                        profile = ChevalierDatabase.verifiedDefaultProfile(),
                        heroPhotos = emptyList(),
                        publishedPosts = emptyList(),
                        publishedHighlights = allPublished,
                        likedPostIds = emptySet(),
                        bookmarkedPostIds = emptySet(),
                        onToggleLike = {},
                        onToggleBookmark = {},
                        onNavigate = {},
                        nowMillisProvider = { now },
                        onOpenExternalYouTube = { playedUrl = it }
                    )
                }
            }
        }

        val list = composeTestRule.onNodeWithTag("home_screen_list")
        list.performScrollToNode(hasTestTag("home_highlights_row"))
        val hlRow = composeTestRule.onNodeWithTag("home_highlights_row")

        // First 4 active highlights appear in the strip + 5th "More" card
        hlRow.performScrollToNode(hasTestTag("highlight_card_1"))
        composeTestRule.onNodeWithTag("highlight_card_1").assertIsDisplayed()
        hlRow.performScrollToNode(hasTestTag("highlight_card_4"))
        composeTestRule.onNodeWithTag("highlight_card_4").assertIsDisplayed()
        // 5th active highlight is behind "More" card, and expired #6 is never shown
        composeTestRule.onAllNodesWithTag("highlight_card_5").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("highlight_card_6").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Expired Yesterday Short").assertCountEquals(0)

        // Tap 5th "More" card -> opens AllHighlightsDialog containing active #5 (still excluding expired #6)
        hlRow.performScrollToNode(hasTestTag("highlight_more_card"))
        composeTestRule.onNodeWithTag("highlight_more_card").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("all_highlights_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("all_highlights_grid_list")
            .performScrollToNode(hasTestTag("all_highlights_card_5"))
        composeTestRule.onNodeWithTag("all_highlights_card_5").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("all_highlights_card_6").assertCountEquals(0)

        // Tap highlight #5 -> opens full-screen portrait YouTube player modal -> verify embed container and close work
        composeTestRule.onNodeWithTag("all_highlights_card_5").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("youtube_player_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("youtube_embed_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("youtube_player_close_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("youtube_player_dialog").assertCountEquals(0)

        // Expired highlight row remains safely in DB (never deleted)
        assertEquals(6, runBlocking { inMemoryDb.schoolHighlightDao().getAllHighlightsOnce().size })
    }

    // ==================== 4. PHOTO LAYOUTS (1, 2-4 GRID, 5-10 PAGER) & REUSED LIGHTBOX ====================

    @Test
    fun `photo posts render single, grid, and swipeable pager layouts and open reused photo lightbox`() {
        val singlePhotoPost = SchoolPostEntity(
            id = 201L,
            postType = SchoolPostType.PHOTO,
            category = SchoolPostCategory.CAMPUS_LIFE,
            title = "Single Campus View",
            photoUrisJson = SchoolPostEntity.encodePhotoUris(listOf("file:///tmp/single.jpg")),
            status = SchoolPostStatus.PUBLISHED,
            publishedAtMillis = 3_000L
        )
        val gridPhotoPost = SchoolPostEntity(
            id = 202L,
            postType = SchoolPostType.PHOTO,
            category = SchoolPostCategory.EVENTS,
            title = "Three Photo Grid",
            photoUrisJson = SchoolPostEntity.encodePhotoUris(
                listOf("file:///tmp/g1.jpg", "file:///tmp/g2.jpg", "file:///tmp/g3.jpg")
            ),
            status = SchoolPostStatus.PUBLISHED,
            publishedAtMillis = 2_000L
        )
        val galleryPhotoPost = SchoolPostEntity(
            id = 203L,
            postType = SchoolPostType.PHOTO,
            category = SchoolPostCategory.ACHIEVEMENTS,
            title = "Six Photo Gallery",
            photoUrisJson = SchoolPostEntity.encodePhotoUris((1..6).map { "file:///tmp/p$it.jpg" }),
            status = SchoolPostStatus.PUBLISHED,
            publishedAtMillis = 1_000L
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    HomeSocialFeedScreen(
                        profile = ChevalierDatabase.verifiedDefaultProfile(),
                        heroPhotos = emptyList(),
                        publishedPosts = listOf(singlePhotoPost, gridPhotoPost, galleryPhotoPost),
                        publishedHighlights = emptyList(),
                        likedPostIds = emptySet(),
                        bookmarkedPostIds = emptySet(),
                        onToggleLike = {},
                        onToggleBookmark = {},
                        onNavigate = {}
                    )
                }
            }
        }

        val list = composeTestRule.onNodeWithTag("home_screen_list")

        // 1 image -> single layout
        list.performScrollToNode(hasTestTag("post_photo_single_201"))
        composeTestRule.onNodeWithTag("post_photo_single_201").assertIsDisplayed()

        // 3 images -> grid layout + tap opens PhotoLightboxDialog
        list.performScrollToNode(hasTestTag("post_photo_grid_202"))
        composeTestRule.onNodeWithTag("post_photo_grid_202").assertIsDisplayed()
        composeTestRule.onNodeWithTag("post_photo_item_202_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("gallery_lightbox_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 / 3").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lightbox_next_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("3 / 3").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lightbox_close_button").performClick()
        composeTestRule.waitForIdle()

        // 6 images -> swipeable pager layout
        list.performScrollToNode(hasTestTag("post_photo_pager_203"))
        composeTestRule.onNodeWithTag("post_photo_pager_203").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 / 6").assertIsDisplayed()
    }

    // ==================== 5. LIKE & BOOKMARK PERSISTENCE, NO COUNTS/COMMENTS/SHARE, BOOKMARKS SCREEN ====================

    @Test
    fun `like and bookmark persist locally without counts comments or share, and Bookmarks removes unbookmarked or unpublished posts immediately`() {
        val app = ApplicationProvider.getApplicationContext<ChevalierApplication>()
        val prefsRepo = app.container.userPreferencesRepository
        runBlocking {
            // Reset any prior toggle state cleanly
            val current = prefsRepo.preferencesFlow.first()
            current.likedPostIds.forEach { prefsRepo.togglePostLike(it) }
            current.bookmarkedPostIds.forEach { prefsRepo.togglePostBookmark(it) }
            prefsRepo.togglePostLike(301L)
            prefsRepo.togglePostBookmark(301L)
            prefsRepo.togglePostBookmark(302L) // Will be unpublished/archived -> must NOT appear in Bookmarks
        }

        val savedState = runBlocking { prefsRepo.preferencesFlow.first() }
        assertTrue(301L in savedState.likedPostIds)
        assertTrue(301L in savedState.bookmarkedPostIds)
        assertTrue(302L in savedState.bookmarkedPostIds)

        val publishedPost = SchoolPostEntity(
            id = 301L,
            postType = SchoolPostType.YOUTUBE_DIRECT,
            category = SchoolPostCategory.GENERAL,
            title = "Principal Annual Address",
            description = "Official address from YouTube",
            youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            status = SchoolPostStatus.PUBLISHED,
            publishedAtMillis = 5_000L
        )
        val archivedBookmarkedPost = SchoolPostEntity(
            id = 302L,
            postType = SchoolPostType.PHOTO,
            category = SchoolPostCategory.EVENTS,
            title = "Archived Bookmarked Item",
            photoUrisJson = SchoolPostEntity.encodePhotoUris(listOf("file:///tmp/arch.jpg")),
            status = SchoolPostStatus.ARCHIVED
        )

        var bookmarkedIds by mutableStateOf(setOf(301L, 302L))
        var likedIds by mutableStateOf(setOf(301L))

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    BookmarksScreen(
                        profile = ChevalierDatabase.verifiedDefaultProfile(),
                        publishedPosts = listOf(publishedPost, archivedBookmarkedPost),
                        likedPostIds = likedIds,
                        bookmarkedPostIds = bookmarkedIds,
                        onToggleLike = { id ->
                            likedIds = if (id in likedIds) likedIds - id else likedIds + id
                        },
                        onToggleBookmark = { id ->
                            bookmarkedIds = if (id in bookmarkedIds) bookmarkedIds - id else bookmarkedIds + id
                        },
                        onExploreFeed = {}
                    )
                }
            }
        }

        // Published bookmarked post appears; archived post does NOT appear
        composeTestRule.onNodeWithTag("social_post_card_301").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("social_post_card_302").assertCountEquals(0)

        // No Comments or Share actions exist
        composeTestRule.onAllNodesWithText("Comment").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Comments").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Share").assertCountEquals(0)

        // Unbookmark immediately removes post and shows empty state
        composeTestRule.onNodeWithTag("post_bookmark_btn_301").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("social_post_card_301").assertCountEquals(0)
        composeTestRule.onNodeWithTag("bookmarks_empty_state").assertIsDisplayed()
    }

    // ==================== 6. MENU PRESERVATION, BOOKMARKS NAVIGATION, BOTTOM NAV & BACK NAVIGATION ====================

    @Test
    @Config(sdk = [36], qualifiers = "w412dp-h915dp")
    fun `existing drawer and overflow menu options are preserved verbatim, Bookmarks opens cleanly with Back navigation, and bottom navigation is unchanged`() {
        val app = ApplicationProvider.getApplicationContext<ChevalierApplication>()
        runBlocking {
            app.container.userPreferencesRepository.restorePreferences(
                language = AppLanguage.ENGLISH,
                isOnboarded = true,
                themeMode = AppThemeMode.LIGHT
            )
            val prefs = app.container.userPreferencesRepository.preferencesFlow.first()
            prefs.bookmarkedPostIds.forEach { app.container.userPreferencesRepository.togglePostBookmark(it) }
            val postId = app.container.socialFeedRepository.saveSchoolPost(
                postType = SchoolPostType.ADMIN_CUSTOM_VIDEO,
                category = SchoolPostCategory.EVENTS,
                title = "Saved School Concert Video",
                description = "Student orchestra performance",
                youtubeUrl = "https://youtu.be/dQw4w9WgXcQ",
                status = SchoolPostStatus.PUBLISHED
            ).getOrThrow()
            app.container.userPreferencesRepository.togglePostBookmark(postId)
        }

        val store = ViewModelStore().also { testViewModelStores.add(it) }
        val mainViewModel = ViewModelProvider(
            store,
            MainViewModel.Factory(app.container.userPreferencesRepository)
        )[MainViewModel::class.java]
        val schoolDataViewModel = ViewModelProvider(
            store,
            SchoolDataViewModel.Factory(app.container.schoolRepository, app.container.socialFeedRepository)
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

        // Bottom navigation still has exact 4 items in order: Home, Academics, Parent, Contact
        val homeBounds = composeTestRule.onNodeWithTag("bottom_nav_home").assertIsDisplayed().getUnclippedBoundsInRoot()
        val academicsBounds = composeTestRule.onNodeWithTag("bottom_nav_academics").assertIsDisplayed().getUnclippedBoundsInRoot()
        val parentBounds = composeTestRule.onNodeWithTag("bottom_nav_parent").assertIsDisplayed().getUnclippedBoundsInRoot()
        val contactBounds = composeTestRule.onNodeWithTag("bottom_nav_contact").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(homeBounds.left < academicsBounds.left)
        assertTrue(academicsBounds.left < parentBounds.left)
        assertTrue(parentBounds.left < contactBounds.left)

        // Top-right overflow menu preserves Gallery AND adds Bookmarks
        composeTestRule.onNodeWithTag("top_bar_more_menu_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("top_bar_menu_gallery").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_bar_menu_bookmarks").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        // Verify Bookmarks screen title, saved post, and Back navigation to Home
        composeTestRule.onNodeWithTag("bookmarks_screen_list").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Bookmarks").onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithText("Saved School Concert Video").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_bar_back_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_screen_list").assertIsDisplayed()

        // Open Navigation Drawer -> verify ALL existing Drawer items preserved verbatim + new Bookmarks option
        composeTestRule.onNodeWithTag("top_bar_drawer_button").performClick()
        composeTestRule.waitForIdle()
        val expectedDrawerTags = listOf(
            "drawer_home",
            "drawer_parent",
            "drawer_bookmarks",
            "drawer_about",
            "drawer_academics",
            "drawer_courses",
            "drawer_facilities",
            "drawer_transport",
            "drawer_gallery",
            "drawer_news",
            "drawer_announcements",
            "drawer_contact",
            "drawer_social",
            "drawer_settings",
            "drawer_management_login"
        )
        expectedDrawerTags.forEach { tag ->
            composeTestRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }

        // Open Bookmarks via Drawer and verify Back returns to Home
        composeTestRule.onNodeWithTag("drawer_bookmarks").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("bookmarks_screen_list").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_bar_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_screen_list").assertIsDisplayed()
    }
}

package com.example

import android.os.Looper
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.stringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.data.local.entity.GalleryAlbumEntity
import com.example.data.local.entity.GalleryAlbumWithMedia
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.SchoolPostType
import com.example.data.media.YouTubeMediaHelper
import com.example.ui.components.GalleryMediaTileContent
import com.example.ui.components.PhotoLightboxDialog
import com.example.ui.components.isGalleryYouTubeMedia
import com.example.ui.components.resolveGalleryYouTubeThumbnailCandidates
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.YouTubePlayerLifecycleHooks
import com.example.ui.theme.ChevalierAcademyTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase2GalleryYouTubePlaybackTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    @After
    fun tearDown() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `resolveGalleryYouTubeThumbnailCandidates handles direct video, custom video, shorts, and invalid URL fallback`() {
        val directMedia = GalleryMediaEntity(
            id = 1L,
            albumId = 10L,
            title = "Annual Day Highlights",
            mediaUri = "https://youtu.be/M7lc1UVf-VE",
            mediaType = SchoolPostType.YOUTUBE_DIRECT
        )
        val legacyGalleryYoutube = GalleryMediaEntity(
            id = 2L,
            albumId = 10L,
            title = "Campus Short",
            mediaUri = "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            mediaType = "YOUTUBE"
        )
        val customVideo = GalleryMediaEntity(
            id = 3L,
            albumId = 10L,
            title = "Principal Address",
            mediaUri = "https://www.youtube.com/watch?v=M7lc1UVf-VE",
            mediaType = SchoolPostType.ADMIN_CUSTOM_VIDEO
        )
        val invalidYoutube = GalleryMediaEntity(
            id = 4L,
            albumId = 10L,
            title = "Broken Link",
            mediaUri = "https://example.com/not-youtube",
            mediaType = "YOUTUBE"
        )
        val regularPhoto = GalleryMediaEntity(
            id = 5L,
            albumId = 10L,
            title = "Library",
            mediaUri = "https://example.com/library.jpg",
            mediaType = "IMAGE"
        )

        assertTrue(isGalleryYouTubeMedia(directMedia))
        assertTrue(isGalleryYouTubeMedia(legacyGalleryYoutube))
        assertTrue(isGalleryYouTubeMedia(customVideo))
        assertFalse(isGalleryYouTubeMedia(regularPhoto))

        assertEquals(
            listOf(YouTubeMediaHelper.thumbnailUrl("M7lc1UVf-VE")),
            resolveGalleryYouTubeThumbnailCandidates(directMedia)
        )
        assertEquals(
            listOf(YouTubeMediaHelper.thumbnailUrl("dQw4w9WgXcQ")),
            resolveGalleryYouTubeThumbnailCandidates(legacyGalleryYoutube)
        )
        assertEquals(
            listOf(
                "file:///data/user/0/custom_thumb.jpg",
                YouTubeMediaHelper.thumbnailUrl("M7lc1UVf-VE")
            ),
            resolveGalleryYouTubeThumbnailCandidates(customVideo, "file:///data/user/0/custom_thumb.jpg")
        )
        assertTrue(resolveGalleryYouTubeThumbnailCandidates(invalidYoutube).isEmpty())
        assertTrue(resolveGalleryYouTubeThumbnailCandidates(regularPhoto).isEmpty())
    }

    @Test
    fun `GalleryMediaTileContent renders YouTube thumbnail for valid video and fallback UI for invalid URL`() {
        val validVideo = GalleryMediaEntity(
            id = 101L,
            albumId = 1L,
            title = "Science Expo Recap",
            caption = "Official school recording",
            mediaUri = "https://www.youtube.com/watch?v=M7lc1UVf-VE",
            mediaType = "YOUTUBE"
        )
        val invalidVideo = GalleryMediaEntity(
            id = 102L,
            albumId = 1L,
            title = "Unavailable Stream",
            mediaUri = "not-a-valid-url",
            mediaType = "YOUTUBE"
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    androidx.compose.foundation.layout.Column {
                        GalleryMediaTileContent(
                            media = validVideo,
                            fallbackContentDescription = "Gallery",
                            modifier = Modifier.size(180.dp)
                        )
                        GalleryMediaTileContent(
                            media = invalidVideo,
                            fallbackContentDescription = "Gallery",
                            modifier = Modifier.size(180.dp)
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("gallery_youtube_thumbnail_101").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_youtube_play_overlay_101").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_youtube_thumbnail_fallback_102").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_youtube_play_overlay_102").assertIsDisplayed()
    }

    @Test
    fun `tapping Direct Video and Custom Video in GalleryScreen opens YouTubeVideoPlayerDialog with external fallback`() {
        val externalUrls = mutableListOf<String>()
        val loadedHtmls = mutableListOf<String>()

        val directVideo = GalleryMediaEntity(
            id = 201L,
            albumId = 1L,
            title = "Direct Campus Film",
            mediaUri = "https://youtu.be/M7lc1UVf-VE",
            mediaType = SchoolPostType.YOUTUBE_DIRECT,
            isVerified = true,
            isPublished = true,
            isApproved = true
        )
        val customVideo = GalleryMediaEntity(
            id = 202L,
            albumId = 1L,
            title = "Custom Event Video",
            mediaUri = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            mediaType = SchoolPostType.ADMIN_CUSTOM_VIDEO,
            isVerified = true,
            isPublished = true,
            isApproved = true
        )
        val album = GalleryAlbumWithMedia(
            album = GalleryAlbumEntity(
                id = 1L,
                title = "Videos",
                category = "Events",
                isVerified = true,
                isPublished = true,
                isApproved = true
            ),
            mediaItems = listOf(directVideo, customVideo)
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    GalleryScreen(
                        albums = listOf(album),
                        onOpenExternalYouTube = { externalUrls += it },
                        youtubeLifecycleHooks = YouTubePlayerLifecycleHooks(
                            onInitialLoad = { _, html -> loadedHtmls += html }
                        )
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // 1. Tap Direct YouTube Video -> opens YouTubeVideoPlayerDialog directly
        composeTestRule.onNodeWithTag("gallery_photo_item_201").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("youtube_player_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_lightbox_dialog").assertDoesNotExist()
        composeTestRule.onNodeWithTag("youtube_embed_container").assertIsDisplayed()
        assertEquals(1, loadedHtmls.size)
        assertTrue(loadedHtmls.last().contains("https://www.youtube-nocookie.com/embed/M7lc1UVf-VE"))
        assertTrue(loadedHtmls.last().contains("enablejsapi=1"))

        composeTestRule.onNodeWithTag("youtube_player_external_fallback_btn")
            .performScrollTo()
            .performClick()
        assertEquals(listOf("https://www.youtube.com/watch?v=M7lc1UVf-VE"), externalUrls)

        composeTestRule.onNodeWithTag("youtube_player_close_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("youtube_player_dialog").assertDoesNotExist()

        // 2. Tap Custom Video -> opens same YouTubeVideoPlayerDialog
        composeTestRule.onNodeWithTag("gallery_photo_item_202").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("youtube_player_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("youtube_embed_container").assertIsDisplayed()
        assertEquals(2, loadedHtmls.size)
        assertTrue(loadedHtmls.last().contains("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ"))

        composeTestRule.onNodeWithTag("youtube_player_close_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("youtube_player_dialog").assertDoesNotExist()
    }

    @Test
    fun `PhotoLightboxDialog displays YouTube thumbnail, opens single YouTubeVideoPlayerDialog, and preserves external fallback`() {
        val externalUrls = mutableListOf<String>()
        var disposeCount = 0

        val photoItem = GalleryMediaEntity(
            id = 301L,
            albumId = 2L,
            title = "Front Gate",
            mediaUri = "https://example.com/gate.jpg",
            mediaType = "IMAGE"
        )
        val youtubeItem = GalleryMediaEntity(
            id = 302L,
            albumId = 2L,
            title = "Campus Documentary",
            mediaUri = "https://www.youtube.com/watch?v=M7lc1UVf-VE",
            mediaType = "YOUTUBE"
        )
        val invalidYoutubeItem = GalleryMediaEntity(
            id = 303L,
            albumId = 2L,
            title = "Invalid Link",
            mediaUri = "https://invalid.example.org/video",
            mediaType = "YOUTUBE"
        )
        val items = listOf(photoItem, youtubeItem, invalidYoutubeItem)
        var currentIndex by mutableIntStateOf(1)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    PhotoLightboxDialog(
                        photos = items,
                        currentIndex = currentIndex,
                        onIndexChange = { currentIndex = it },
                        onDismiss = {},
                        onOpenExternalYouTube = { externalUrls += it },
                        youtubeLifecycleHooks = YouTubePlayerLifecycleHooks(
                            onDisposeCleanup = { disposeCount += 1 }
                        )
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // Lightbox shows YouTube thumbnail card with Play and External fallback controls
        composeTestRule.onNodeWithTag("gallery_lightbox_youtube_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lightbox_youtube_thumbnail", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("lightbox_open_youtube_button").performScrollTo().performClick()
        assertEquals(listOf("https://www.youtube.com/watch?v=M7lc1UVf-VE"), externalUrls)

        // Tapping Play opens single YouTubeVideoPlayerDialog (replacing lightbox while active)
        composeTestRule.onNodeWithTag("lightbox_play_youtube_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("youtube_player_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_lightbox_dialog").assertDoesNotExist()

        // Closing player cleans up resources and returns cleanly to lightbox
        composeTestRule.onNodeWithTag("youtube_player_close_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, disposeCount)
        composeTestRule.onNodeWithTag("youtube_player_dialog").assertDoesNotExist()
        composeTestRule.onNodeWithTag("gallery_lightbox_youtube_card").assertIsDisplayed()

        // Stepping to invalid YouTube item shows lightbox thumbnail fallback UI
        composeTestRule.onNodeWithTag("lightbox_next_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lightbox_youtube_thumbnail_fallback", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `gallery YouTube player pauses in background, does not auto-resume on foreground, and cleans up observer on dismiss`() {
        val lifecycleOwner = TestLifecycleOwner()
        lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        var initialLoads = 0
        var pauseCount = 0
        var pauseScript: String? = null
        var resumeCount = 0
        val foregroundStaticReloadHtmls = mutableListOf<String>()
        var disposeCount = 0

        val album = GalleryAlbumWithMedia(
            album = GalleryAlbumEntity(
                id = 5L,
                title = "Sports Day",
                category = "Sports",
                isVerified = true,
                isPublished = true,
                isApproved = true
            ),
            mediaItems = listOf(
                GalleryMediaEntity(
                    id = 501L,
                    albumId = 5L,
                    title = "Relay Finals",
                    mediaUri = "https://www.youtube.com/watch?v=M7lc1UVf-VE",
                    mediaType = "YOUTUBE",
                    isVerified = true,
                    isPublished = true,
                    isApproved = true
                )
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAppStrings provides stringsFor(AppLanguage.ENGLISH),
                LocalLifecycleOwner provides lifecycleOwner
            ) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    GalleryScreen(
                        albums = listOf(album),
                        youtubeLifecycleHooks = YouTubePlayerLifecycleHooks(
                            onInitialLoad = { _, _ -> initialLoads += 1 },
                            onPause = { pauseCount += 1 },
                            onPauseScriptEvaluated = { script -> pauseScript = script },
                            onResume = { resumeCount += 1 },
                            onForegroundReloadStatic = { _, html -> foregroundStaticReloadHtmls += html },
                            onDisposeCleanup = { disposeCount += 1 }
                        )
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // Tap video in Gallery to open player
        composeTestRule.onNodeWithTag("gallery_photo_item_501").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("youtube_player_dialog").assertIsDisplayed()
        assertEquals(1, initialLoads)

        // Move app to background -> pauses video once with pauseVideo script
        composeTestRule.runOnIdle {
            lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        composeTestRule.waitForIdle()
        assertEquals(1, pauseCount)
        assertEquals(YouTubeMediaHelper.PAUSE_EMBED_PLAYER_JS, pauseScript)

        // Return to foreground -> reloads static non-autoplay embed (no auto-resume)
        composeTestRule.runOnIdle {
            lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        composeTestRule.waitForIdle()
        assertEquals(1, resumeCount)
        assertEquals(1, foregroundStaticReloadHtmls.size)
        assertFalse(
            "Foreground reload must not auto-resume playback",
            foregroundStaticReloadHtmls.single().contains("autoplay=1")
        )
        assertTrue(foregroundStaticReloadHtmls.single().contains("enablejsapi=1"))

        // Close dialog -> releases player resources and removes lifecycle observer
        composeTestRule.onNodeWithTag("youtube_player_close_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, disposeCount)
        composeTestRule.onNodeWithTag("youtube_player_dialog").assertDoesNotExist()

        // Subsequent lifecycle transitions must NOT trigger duplicate/stale observers
        composeTestRule.runOnIdle {
            lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        composeTestRule.waitForIdle()
        assertEquals(1, pauseCount)
        assertEquals(1, resumeCount)
    }

    @Test
    fun `invalid YouTube URL in Gallery shows player fallback card with working external fallback action`() {
        val externalUrls = mutableListOf<String>()
        val brokenUrl = "https://example.com/broken-video"
        val album = GalleryAlbumWithMedia(
            album = GalleryAlbumEntity(
                id = 6L,
                title = "Archive",
                category = "Events",
                isVerified = true,
                isPublished = true,
                isApproved = true
            ),
            mediaItems = listOf(
                GalleryMediaEntity(
                    id = 601L,
                    albumId = 6L,
                    title = "Legacy Recording",
                    mediaUri = brokenUrl,
                    mediaType = "YOUTUBE",
                    isVerified = true,
                    isPublished = true,
                    isApproved = true
                )
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    GalleryScreen(
                        albums = listOf(album),
                        onOpenExternalYouTube = { externalUrls += it }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("gallery_youtube_thumbnail_fallback_601", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("gallery_photo_item_601").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("youtube_player_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("youtube_embed_fallback_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("youtube_player_external_fallback_btn")
            .performScrollTo()
            .performClick()
        assertEquals(listOf(brokenUrl), externalUrls)
    }
}

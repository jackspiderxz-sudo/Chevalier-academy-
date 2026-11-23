package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.stringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.SchoolHighlightEntity
import com.example.data.local.entity.SchoolPostCategory
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostStatus
import com.example.data.local.entity.SchoolPostType
import com.example.data.media.MediaStudioManager
import com.example.data.media.YouTubeMediaHelper
import com.example.ui.screens.HomeSocialFeedScreen
import com.example.ui.screens.YouTubePlayerLifecycleHooks
import com.example.ui.theme.ChevalierAcademyTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import java.net.URLEncoder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeFeedInlineYouTubePlaybackTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `youtube parser supports live urls alongside watch, shorts, embed, and youtu_be while rejecting invalid live paths`() {
        val liveVideoId = "jfKfPfyJRdk"
        val validLiveUrls = listOf(
            "https://www.youtube.com/live/$liveVideoId",
            "https://youtube.com/live/$liveVideoId?feature=share&si=xyz123",
            "https://m.youtube.com/live/$liveVideoId"
        )
        for (url in validLiveUrls) {
            val parsed = YouTubeMediaHelper.parse(url)
            assertNotNull("Expected valid parse for live url: $url", parsed)
            assertEquals(liveVideoId, parsed!!.videoId)
            assertFalse(parsed.isShort)
            assertEquals("https://www.youtube.com/watch?v=$liveVideoId", parsed.canonicalWatchUrl)
            assertEquals("https://img.youtube.com/vi/$liveVideoId/hqdefault.jpg", parsed.thumbnailUrl)
            assertTrue(parsed.clickToPlayEmbedUrl.startsWith("https://www.youtube-nocookie.com/embed/$liveVideoId?"))
            assertTrue(MediaStudioManager.isValidYouTubeUrl(url))
            assertFalse(YouTubeMediaHelper.isValidYouTubeShortsUrl(url))
        }

        val invalidLiveUrls = listOf(
            "https://www.youtube.com/live/",
            "https://www.youtube.com/live/short"
        )
        for (url in invalidLiveUrls) {
            assertNull("Expected null parse for invalid live url: $url", YouTubeMediaHelper.parse(url))
            assertFalse(YouTubeMediaHelper.isValidYouTubeUrl(url))
        }
    }

    @Test
    fun `home feed tap plays inline inside existing post card without fullscreen dialog and closing restores thumbnail`() {
        val now = 1_750_000_000_000L
        val standardPost = SchoolPostEntity(
            id = 101L,
            postType = SchoolPostType.YOUTUBE_DIRECT,
            category = SchoolPostCategory.GENERAL,
            title = "Principal Address",
            description = "Opening assembly remarks",
            youtubeUrl = "https://www.youtube.com/watch?v=M7lc1UVf-VE",
            status = SchoolPostStatus.PUBLISHED,
            publishedAtMillis = now
        )
        val customShortsPost = SchoolPostEntity(
            id = 102L,
            postType = SchoolPostType.ADMIN_CUSTOM_VIDEO,
            category = SchoolPostCategory.EVENTS,
            title = "Campus Robotics Demo",
            description = "Custom thumbnail Shorts showcase",
            youtubeUrl = "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            customThumbnailUri = "file:///tmp/custom_thumb.jpg",
            status = SchoolPostStatus.PUBLISHED,
            publishedAtMillis = now - 1_000L
        )
        val livePost = SchoolPostEntity(
            id = 103L,
            postType = SchoolPostType.YOUTUBE_DIRECT,
            category = SchoolPostCategory.SPORTS,
            title = "Sports Day Live",
            description = "Live broadcast from track",
            youtubeUrl = "https://www.youtube.com/live/jfKfPfyJRdk?si=stream",
            status = SchoolPostStatus.PUBLISHED,
            publishedAtMillis = now - 2_000L
        )

        var externalOpenedUrl: String? = null
        var likedIds by mutableStateOf(emptySet<Long>())
        var bookmarkedIds by mutableStateOf(emptySet<Long>())
        var disposeCountPost101 = 0
        var initialHtml102: String? = null
        var initialHtml103: String? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAppStrings provides stringsFor(AppLanguage.ENGLISH)) {
                ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                    HomeSocialFeedScreen(
                        profile = ChevalierDatabase.verifiedDefaultProfile(),
                        heroPhotos = emptyList(),
                        publishedPosts = listOf(standardPost, customShortsPost, livePost),
                        publishedHighlights = emptyList(),
                        likedPostIds = likedIds,
                        bookmarkedPostIds = bookmarkedIds,
                        onToggleLike = { id -> likedIds = likedIds + id },
                        onToggleBookmark = { id -> bookmarkedIds = bookmarkedIds + id },
                        onNavigate = {},
                        nowMillisProvider = { now },
                        onOpenExternalYouTube = { externalOpenedUrl = it },
                        inlinePlayerLifecycleHooksProvider = { postId ->
                            when (postId) {
                                101L -> YouTubePlayerLifecycleHooks(onDisposeCleanup = { disposeCountPost101++ })
                                102L -> YouTubePlayerLifecycleHooks(onInitialLoad = { _, html -> initialHtml102 = html })
                                103L -> YouTubePlayerLifecycleHooks(onInitialLoad = { _, html -> initialHtml103 = html })
                                else -> null
                            }
                        }
                    )
                }
            }
        }

        val list = composeTestRule.onNodeWithTag("home_screen_list")

        // Tap standard video thumbnail -> replaces thumbnail with inline player inside same card (no dialog)
        list.performScrollToNode(hasTestTag("post_video_preview_101"))
        composeTestRule.onNodeWithTag("post_video_preview_101").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithTag("youtube_player_dialog").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("post_video_preview_101").assertCountEquals(0)
        composeTestRule.onNodeWithTag("post_video_inline_player_101").assertIsDisplayed()
        composeTestRule.onNodeWithTag("post_video_inline_embed_101").assertIsDisplayed()

        // Surrounding post header, description, Like, Bookmark, and Three-dot overflow menu remain intact
        composeTestRule.onNodeWithTag("social_post_title_101").assertIsDisplayed()
        composeTestRule.onNodeWithTag("social_post_description_101").assertIsDisplayed()
        composeTestRule.onNodeWithTag("post_like_btn_101").performClick()
        assertTrue(101L in likedIds)
        composeTestRule.onNodeWithTag("post_bookmark_btn_101").performClick()
        assertTrue(101L in bookmarkedIds)

        // Open in YouTube via header Three-dot menu
        composeTestRule.onNodeWithTag("post_overflow_menu_btn_101").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("post_video_external_btn_101").performClick()
        composeTestRule.waitForIdle()
        assertEquals("https://www.youtube.com/watch?v=M7lc1UVf-VE", externalOpenedUrl)

        // Copy Video Link via header Three-dot menu
        composeTestRule.onNodeWithTag("post_overflow_menu_btn_101").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("post_video_copy_link_btn_101").performClick()
        composeTestRule.waitForIdle()

        // Closing inline playback via header Three-dot menu disposes WebView cleanly and restores original thumbnail in card
        composeTestRule.onNodeWithTag("post_overflow_menu_btn_101").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("post_video_inline_close_btn_101").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, disposeCountPost101)
        composeTestRule.onAllNodesWithTag("post_video_inline_player_101").assertCountEquals(0)
        composeTestRule.onNodeWithTag("post_video_preview_101").assertIsDisplayed()

        // Verify Shorts post plays inline with Shorts videoId (no dialog)
        list.performScrollToNode(hasTestTag("post_video_preview_102"))
        composeTestRule.onNodeWithTag("post_video_preview_102").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("youtube_player_dialog").assertCountEquals(0)
        composeTestRule.onNodeWithTag("post_video_inline_embed_102").assertIsDisplayed()
        assertTrue(initialHtml102?.contains("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ?") == true)

        // Verify Live post plays inline with Live videoId (stops Shorts post 102 automatically)
        list.performScrollToNode(hasTestTag("post_video_preview_103"))
        composeTestRule.onNodeWithTag("post_video_preview_103").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("youtube_player_dialog").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("post_video_inline_player_102").assertCountEquals(0)
        composeTestRule.onNodeWithTag("post_video_inline_embed_103").assertIsDisplayed()
        assertTrue(initialHtml103?.contains("https://www.youtube-nocookie.com/embed/jfKfPfyJRdk?") == true)
    }

    @Test
    fun `starting another feed video or highlight stops previous inline player and preserves single observer lifecycle`() {
        val now = 1_750_000_000_000L
        val owner = TestLifecycleOwner()
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val postA = SchoolPostEntity(
            id = 201L,
            postType = SchoolPostType.YOUTUBE_DIRECT,
            category = SchoolPostCategory.GENERAL,
            title = "Video Post A",
            youtubeUrl = "https://www.youtube.com/watch?v=M7lc1UVf-VE",
            status = SchoolPostStatus.PUBLISHED,
            publishedAtMillis = now
        )
        val postB = SchoolPostEntity(
            id = 202L,
            postType = SchoolPostType.ADMIN_CUSTOM_VIDEO,
            category = SchoolPostCategory.EVENTS,
            title = "Video Post B",
            youtubeUrl = "https://youtu.be/dQw4w9WgXcQ",
            status = SchoolPostStatus.PUBLISHED,
            publishedAtMillis = now - 1_000L
        )
        val highlight = SchoolHighlightEntity(
            id = 1L,
            title = "Campus Highlight",
            youtubeShortsUrl = "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            status = SchoolPostStatus.PUBLISHED,
            publishedAtMillis = now - 60_000L,
            expiresAtMillis = now + 3_600_000L
        )

        var disposeCountA = 0
        var disposeCountB = 0
        val pauseScriptsB = mutableListOf<String>()
        var pauseCountB = 0
        var resumeCountB = 0
        val foregroundReloadsB = mutableListOf<Pair<String, String>>()

        val expectedAppOrigin = YouTubeMediaHelper.appDocumentOrigin(context.packageName)
            ?: "https://appassets.androidplatform.net"
        val expectedEncodedOrigin = URLEncoder.encode(expectedAppOrigin, "UTF-8")

        composeTestRule.setContent {
            ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                CompositionLocalProvider(
                    LocalAppStrings provides stringsFor(AppLanguage.ENGLISH),
                    LocalLifecycleOwner provides owner
                ) {
                    HomeSocialFeedScreen(
                        profile = ChevalierDatabase.verifiedDefaultProfile(),
                        heroPhotos = emptyList(),
                        publishedPosts = listOf(postA, postB),
                        publishedHighlights = listOf(highlight),
                        likedPostIds = emptySet(),
                        bookmarkedPostIds = emptySet(),
                        onToggleLike = {},
                        onToggleBookmark = {},
                        onNavigate = {},
                        nowMillisProvider = { now },
                        inlinePlayerLifecycleHooksProvider = { postId ->
                            when (postId) {
                                201L -> YouTubePlayerLifecycleHooks(onDisposeCleanup = { disposeCountA++ })
                                202L -> YouTubePlayerLifecycleHooks(
                                    onPause = { pauseCountB++ },
                                    onPauseScriptEvaluated = { pauseScriptsB.add(it) },
                                    onResume = { resumeCountB++ },
                                    onForegroundReloadStatic = { base, html ->
                                        foregroundReloadsB.add(base to html)
                                    },
                                    onDisposeCleanup = { disposeCountB++ }
                                )
                                else -> null
                            }
                        }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        val baselineObserverCount = owner.registry.observerCount
        val list = composeTestRule.onNodeWithTag("home_screen_list")

        // Play Post A inline
        list.performScrollToNode(hasTestTag("post_video_preview_201"))
        composeTestRule.onNodeWithTag("post_video_preview_201").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("post_video_inline_player_201").assertIsDisplayed()
        assertEquals(baselineObserverCount + 1, owner.registry.observerCount)

        // Start Post B inline -> stops and disposes Post A cleanly; only 1 active player observer remains
        list.performScrollToNode(hasTestTag("post_video_preview_202"))
        composeTestRule.onNodeWithTag("post_video_preview_202").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, disposeCountA)
        assertEquals(baselineObserverCount + 1, owner.registry.observerCount)
        composeTestRule.onNodeWithTag("post_video_inline_player_202").assertIsDisplayed()

        // Background (ON_PAUSE) pauses active inline player; foreground (ON_RESUME) reloads static without autoplay
        composeTestRule.runOnIdle {
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        composeTestRule.waitForIdle()
        assertEquals(1, pauseCountB)
        assertTrue(pauseScriptsB.first().contains("pauseVideo"))

        composeTestRule.runOnIdle {
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        composeTestRule.waitForIdle()
        assertEquals(1, resumeCountB)
        assertEquals(1, foregroundReloadsB.size)
        val (reloadBase, reloadHtml) = foregroundReloadsB.first()
        assertEquals(expectedAppOrigin, reloadBase)
        assertTrue(reloadHtml.contains("origin=$expectedEncodedOrigin"))
        assertFalse("Returning to foreground must not reload autoplay=1", reloadHtml.contains("autoplay=1"))

        // Opening Highlight still opens YouTubeVideoPlayerDialog AND stops active inline feed playback cleanly
        list.performScrollToNode(hasTestTag("home_highlights_row"))
        composeTestRule.onNodeWithTag("highlight_card_1").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, disposeCountB)
        composeTestRule.onNodeWithTag("youtube_player_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("youtube_player_close_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithTag("youtube_player_dialog").assertCountEquals(0)
        assertEquals(baselineObserverCount, owner.registry.observerCount)
        list.performScrollToNode(hasTestTag("social_post_card_202"))
        composeTestRule.onAllNodesWithTag("post_video_inline_player_202").assertCountEquals(0)
        composeTestRule.onNodeWithTag("post_video_preview_202").assertIsDisplayed()
    }

    @Test
    fun `scrolling active inline video post offscreen disposes player and observer cleanly and restores thumbnail when scrolled back`() {
        val now = 1_750_000_000_000L
        val owner = TestLifecycleOwner()
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val videoPost = SchoolPostEntity(
            id = 301L,
            postType = SchoolPostType.YOUTUBE_DIRECT,
            category = SchoolPostCategory.GENERAL,
            title = "Top Video Post",
            youtubeUrl = "https://www.youtube.com/watch?v=M7lc1UVf-VE",
            status = SchoolPostStatus.PUBLISHED,
            publishedAtMillis = now
        )
        val spacerPosts = (1..12).map { index ->
            SchoolPostEntity(
                id = 900L + index,
                postType = SchoolPostType.PHOTO,
                category = SchoolPostCategory.CAMPUS_LIFE,
                title = "Campus Photo #$index",
                description = "Campus moment $index with detailed description to exceed viewport height cleanly.",
                photoUrisJson = SchoolPostEntity.encodePhotoUris(listOf("file:///tmp/campus_$index.jpg")),
                status = SchoolPostStatus.PUBLISHED,
                publishedAtMillis = now - (index * 1_000L)
            )
        }

        var disposeCount = 0

        composeTestRule.setContent {
            ChevalierAcademyTheme(themeMode = AppThemeMode.LIGHT) {
                CompositionLocalProvider(
                    LocalAppStrings provides stringsFor(AppLanguage.ENGLISH),
                    LocalLifecycleOwner provides owner
                ) {
                    HomeSocialFeedScreen(
                        profile = ChevalierDatabase.verifiedDefaultProfile(),
                        heroPhotos = emptyList(),
                        publishedPosts = listOf(videoPost) + spacerPosts,
                        publishedHighlights = emptyList(),
                        likedPostIds = emptySet(),
                        bookmarkedPostIds = emptySet(),
                        onToggleLike = {},
                        onToggleBookmark = {},
                        onNavigate = {},
                        nowMillisProvider = { now },
                        inlinePlayerLifecycleHooksProvider = { postId ->
                            if (postId == 301L) {
                                YouTubePlayerLifecycleHooks(onDisposeCleanup = { disposeCount++ })
                            } else {
                                null
                            }
                        }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        val baselineObserverCount = owner.registry.observerCount
        val list = composeTestRule.onNodeWithTag("home_screen_list")

        // Play top video post inline
        list.performScrollToNode(hasTestTag("post_video_preview_301"))
        composeTestRule.onNodeWithTag("post_video_preview_301").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("post_video_inline_player_301").assertIsDisplayed()
        assertEquals(baselineObserverCount + 1, owner.registry.observerCount)

        // Scroll far down past 12 photo cards so post 301 is disposed offscreen
        list.performScrollToNode(hasTestTag("social_post_card_912"))
        composeTestRule.waitForIdle()
        assertEquals(1, disposeCount)
        assertEquals(baselineObserverCount, owner.registry.observerCount)

        // Scroll back up to post 301 -> thumbnail is restored without recreating unprompted playback
        list.performScrollToNode(hasTestTag("social_post_card_301"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("post_video_preview_301").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("post_video_inline_player_301").assertCountEquals(0)
        assertEquals(baselineObserverCount, owner.registry.observerCount)
    }
}

package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.stringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.data.media.YouTubeMediaHelper
import com.example.ui.screens.YouTubePlayerLifecycleHooks
import com.example.ui.screens.YouTubeVideoPlayerDialog
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
class HomeSocialPhase33YouTubeLifecycleTest {

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
    fun embedUrls_enableJsApi_andConfigureOriginOnlyWhenValidNonYouTubeOriginProvided() {
        val videoId = "M7lc1UVf-VE"

        // Without an explicit origin, enablejsapi=1 is present and origin is omitted (never hardcoded)
        val clickToPlayDefault = YouTubeMediaHelper.clickToPlayEmbedUrl(videoId)
        val staticDefault = YouTubeMediaHelper.staticEmbedUrl(videoId)

        assertTrue(clickToPlayDefault.startsWith("https://www.youtube-nocookie.com/embed/$videoId?"))
        assertTrue(clickToPlayDefault.contains("autoplay=1"))
        assertTrue(clickToPlayDefault.contains("enablejsapi=1"))
        assertFalse(clickToPlayDefault.contains("origin="))

        assertTrue(staticDefault.startsWith("https://www.youtube-nocookie.com/embed/$videoId?"))
        assertFalse(staticDefault.contains("autoplay=1"))
        assertTrue(staticDefault.contains("enablejsapi=1"))
        assertFalse(staticDefault.contains("origin="))

        // Valid app/site HTTPS origin is normalized and URL-encoded in origin= parameter
        val appOrigin = YouTubeMediaHelper.appDocumentOrigin("com.aistudio.chevalieracademy.school")
        assertEquals("https://com.aistudio.chevalieracademy.school", appOrigin)
        val encodedAppOrigin = URLEncoder.encode(appOrigin, "UTF-8")

        val clickToPlayWithOrigin = YouTubeMediaHelper.clickToPlayEmbedUrl(videoId, appOrigin)
        val staticWithOrigin = YouTubeMediaHelper.staticEmbedUrl(videoId, appOrigin)

        assertTrue(clickToPlayWithOrigin.contains("enablejsapi=1"))
        assertTrue(clickToPlayWithOrigin.contains("autoplay=1"))
        assertTrue(clickToPlayWithOrigin.contains("origin=$encodedAppOrigin"))

        assertTrue(staticWithOrigin.contains("enablejsapi=1"))
        assertFalse(staticWithOrigin.contains("autoplay=1"))
        assertTrue(staticWithOrigin.contains("origin=$encodedAppOrigin"))

        // Normalization strips paths/default ports and preserves non-default ports
        assertEquals(
            "https://school.example.edu",
            YouTubeMediaHelper.normalizeEmbedOrigin("https://school.example.edu:443/watch?x=1")
        )
        assertEquals(
            "https://school.example.edu:8443",
            YouTubeMediaHelper.normalizeEmbedOrigin("https://school.example.edu:8443/embed/page")
        )

        // YouTube domains and invalid/non-HTTP(S) origins are strictly rejected
        val rejectedOrigins = listOf(
            null,
            "",
            "   ",
            "about:blank",
            "file:///android_asset/player.html",
            "data:text/html,<html></html>",
            "https://localhost",
            "https://www.youtube-nocookie.com",
            "https://youtube-nocookie.com",
            "https://www.youtube.com",
            "https://m.youtube.com",
            "https://youtu.be",
            "https://embed.youtube-nocookie.com"
        )
        for (candidate in rejectedOrigins) {
            assertNull("Expected origin to be rejected: $candidate", YouTubeMediaHelper.normalizeEmbedOrigin(candidate))
            val embedUrl = YouTubeMediaHelper.clickToPlayEmbedUrl(videoId, candidate)
            assertFalse(
                "Embed URL must not emit origin for rejected candidate: $candidate",
                embedUrl.contains("origin=")
            )
            assertTrue(embedUrl.contains("enablejsapi=1"))
        }

        // Invalid package names return null from appDocumentOrigin
        assertNull(YouTubeMediaHelper.appDocumentOrigin(null))
        assertNull(YouTubeMediaHelper.appDocumentOrigin(""))
        assertNull(YouTubeMediaHelper.appDocumentOrigin("singlelabel"))
    }

    @Test
    fun embedHtmlAndPauseScript_matchPrivacyEnhancedPlayerApiRequirements() {
        val videoId = "M7lc1UVf-VE"
        val docOrigin = "https://com.aistudio.chevalieracademy.school"
        val encodedOrigin = URLEncoder.encode(docOrigin, "UTF-8")

        val autoplayHtml = YouTubeMediaHelper.buildClickToPlayEmbedHtml(
            videoId = videoId,
            autoplay = true,
            documentBaseUrl = docOrigin
        )
        assertNotNull(autoplayHtml)
        assertTrue(autoplayHtml!!.contains("id=\"yt-player\""))
        assertTrue(autoplayHtml.contains("<meta name=\"referrer\" content=\"strict-origin-when-cross-origin\" />"))
        assertTrue(autoplayHtml.contains("referrerpolicy=\"strict-origin-when-cross-origin\""))
        assertTrue(autoplayHtml.contains("https://www.youtube-nocookie.com/embed/$videoId?"))
        assertTrue(autoplayHtml.contains("autoplay=1"))
        assertTrue(autoplayHtml.contains("enablejsapi=1"))
        assertTrue(autoplayHtml.contains("origin=$encodedOrigin"))

        val staticHtml = YouTubeMediaHelper.buildClickToPlayEmbedHtml(
            videoId = videoId,
            autoplay = false,
            documentBaseUrl = docOrigin
        )
        assertNotNull(staticHtml)
        assertFalse(staticHtml!!.contains("autoplay=1"))
        assertTrue(staticHtml.contains("enablejsapi=1"))
        assertTrue(staticHtml.contains("origin=$encodedOrigin"))

        // If youtube-nocookie.com is mistakenly passed as documentBaseUrl, origin= is omitted
        val noYouTubeOriginHtml = YouTubeMediaHelper.buildClickToPlayEmbedHtml(
            videoId = videoId,
            autoplay = true,
            documentBaseUrl = "https://www.youtube-nocookie.com"
        )
        assertNotNull(noYouTubeOriginHtml)
        assertTrue(noYouTubeOriginHtml!!.contains("enablejsapi=1"))
        assertFalse(noYouTubeOriginHtml.contains("origin="))

        // Pause script targets the privacy-enhanced YouTube embed host with pauseVideo command
        assertTrue(YouTubeMediaHelper.PAUSE_EMBED_PLAYER_JS.contains("pauseVideo"))
        assertTrue(YouTubeMediaHelper.PAUSE_EMBED_PLAYER_JS.contains(YouTubeMediaHelper.PRIVACY_ENHANCED_EMBED_HOST))
    }

    @Test
    fun youtubePlayerDialog_loadsWithRealAppOrigin_pausesOnBackground_andPreventsAutoRestartOnForegroundReturn() {
        val owner = TestLifecycleOwner()
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        var showDialog by mutableStateOf(true)
        var initialBaseUrl: String? = null
        var initialHtml: String? = null
        val pauseScripts = mutableListOf<String>()
        var pauseCount = 0
        var resumeCount = 0
        val foregroundReloads = mutableListOf<Pair<String, String>>()
        var disposeCount = 0
        var externalFallbackUrl: String? = null

        val expectedAppOrigin = YouTubeMediaHelper.appDocumentOrigin(context.packageName)
            ?: "https://appassets.androidplatform.net"
        val expectedEncodedOrigin = URLEncoder.encode(expectedAppOrigin, "UTF-8")

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
                            onOpenExternal = { externalFallbackUrl = it },
                            lifecycleHooks = YouTubePlayerLifecycleHooks(
                                onInitialLoad = { baseUrl, html ->
                                    initialBaseUrl = baseUrl
                                    initialHtml = html
                                },
                                onPause = { pauseCount++ },
                                onPauseScriptEvaluated = { script -> pauseScripts.add(script) },
                                onResume = { resumeCount++ },
                                onForegroundReloadStatic = { baseUrl, html ->
                                    foregroundReloads.add(baseUrl to html)
                                },
                                onDisposeCleanup = { disposeCount++ }
                            )
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        // Initial click-to-play load uses derived app origin (not youtube-nocookie.com) + enablejsapi=1 + autoplay=1
        assertEquals(expectedAppOrigin, initialBaseUrl)
        assertFalse(initialBaseUrl!!.contains("youtube-nocookie.com"))
        assertNotNull(initialHtml)
        assertTrue(initialHtml!!.contains("https://www.youtube-nocookie.com/embed/M7lc1UVf-VE?"))
        assertTrue(initialHtml!!.contains("autoplay=1"))
        assertTrue(initialHtml!!.contains("enablejsapi=1"))
        assertTrue(initialHtml!!.contains("origin=$expectedEncodedOrigin"))
        // Initial ON_RESUME from already-resumed host must NOT trigger foreground-return reload
        assertEquals(0, foregroundReloads.size)

        // External YouTube fallback remains functional
        composeTestRule.onNodeWithTag("youtube_player_external_fallback_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals("https://www.youtube.com/watch?v=M7lc1UVf-VE", externalFallbackUrl)

        // Backgrounding the app triggers ON_PAUSE -> pause script evaluated and WebView paused
        composeTestRule.runOnIdle {
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        composeTestRule.waitForIdle()
        assertEquals(1, pauseCount)
        assertEquals(1, pauseScripts.size)
        assertTrue(pauseScripts.first().contains("pauseVideo"))
        assertTrue(pauseScripts.first().contains("https://www.youtube-nocookie.com"))
        assertEquals(0, resumeCount)
        assertEquals(0, foregroundReloads.size)

        // Returning to foreground triggers ON_RESUME -> reloads static non-autoplay embed (no automatic restart)
        composeTestRule.runOnIdle {
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        composeTestRule.waitForIdle()
        assertEquals(1, resumeCount)
        assertEquals(1, foregroundReloads.size)
        val (reloadBaseUrl, reloadHtml) = foregroundReloads.first()
        assertEquals(expectedAppOrigin, reloadBaseUrl)
        assertTrue(reloadHtml.contains("https://www.youtube-nocookie.com/embed/M7lc1UVf-VE?"))
        assertTrue(reloadHtml.contains("enablejsapi=1"))
        assertTrue(reloadHtml.contains("origin=$expectedEncodedOrigin"))
        assertFalse("Foreground return must not reload with autoplay=1", reloadHtml.contains("autoplay=1"))

        // Second background -> foreground cycle also pauses cleanly and does not auto-restart
        composeTestRule.runOnIdle {
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        composeTestRule.waitForIdle()
        assertEquals(2, pauseCount)
        assertEquals(2, pauseScripts.size)

        composeTestRule.runOnIdle {
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        composeTestRule.waitForIdle()
        assertEquals(2, resumeCount)
        assertEquals(2, foregroundReloads.size)
        assertFalse(foregroundReloads[1].second.contains("autoplay=1"))

        // Closing the dialog cleans up WebView and detaches lifecycle observer
        composeTestRule.onNodeWithTag("youtube_player_close_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, disposeCount)
        assertEquals(0, owner.registry.observerCount)

        // Further lifecycle events after dismiss do not invoke callbacks
        composeTestRule.runOnIdle {
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        composeTestRule.waitForIdle()
        assertEquals(2, pauseCount)
        assertEquals(2, resumeCount)
    }
}

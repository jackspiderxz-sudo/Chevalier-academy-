package com.example

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import com.example.core.i18n.stringsFor
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name and package match Chevalier Academy configuration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Chevalier Academy", appName)
        assertEquals("com.aistudio.chevalieracademy.school", context.packageName)
    }

    @Test
    fun `new launcher icon and splash drawables are configured while shared emblem remains`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val splashId = context.resources.getIdentifier("chevalier_splash_artwork", "drawable", context.packageName)
        val launcherLogoId = context.resources.getIdentifier("chevalier_launcher_logo", "drawable", context.packageName)
        val launcherFgId = context.resources.getIdentifier("ic_launcher_foreground", "drawable", context.packageName)
        val launcherBgId = context.resources.getIdentifier("ic_launcher_background", "drawable", context.packageName)
        val launcherMipmapId = context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)
        val launcherRoundMipmapId = context.resources.getIdentifier("ic_launcher_round", "mipmap", context.packageName)
        val emblemId = context.resources.getIdentifier("chevalier_emblem", "drawable", context.packageName)

        assertTrue(splashId != 0)
        assertTrue(launcherLogoId != 0)
        assertTrue(launcherFgId != 0)
        assertTrue(launcherBgId != 0)
        assertTrue(launcherMipmapId != 0)
        assertTrue(launcherRoundMipmapId != 0)
        assertTrue(emblemId != 0)

        val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
        assertEquals(R.mipmap.ic_launcher, appInfo.icon)
    }

    @Test
    fun `system splash theme uses royal blue surface instead of launcher icon`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val themed = ContextThemeWrapper(context, R.style.Theme_ChevalierAcademy)
        val out = TypedValue()

        assertTrue(themed.theme.resolveAttribute(android.R.attr.windowSplashScreenAnimatedIcon, out, true))
        assertEquals(R.drawable.ic_splash_system_transparent, out.resourceId)
        assertNotEquals(R.mipmap.ic_launcher, out.resourceId)

        assertTrue(themed.theme.resolveAttribute(android.R.attr.windowSplashScreenBackground, out, true))
        assertEquals(R.color.splash_royal_blue, out.resourceId)

        assertTrue(themed.theme.resolveAttribute(android.R.attr.windowSplashScreenIconBackgroundColor, out, true))
        assertEquals(R.color.splash_royal_blue, out.resourceId)

        assertTrue(themed.theme.resolveAttribute(android.R.attr.windowLayoutInDisplayCutoutMode, out, true))
        assertEquals(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES, out.data)
    }

    @Test
    fun `interface localization translates chrome while preserving school identity`() {
        val en = stringsFor(AppLanguage.ENGLISH)
        val ta = stringsFor(AppLanguage.TAMIL)
        val hi = stringsFor(AppLanguage.HINDI)

        assertEquals("Home", en.navHome)
        assertEquals("முகப்பு", ta.navHome)
        assertEquals("होम", hi.navHome)

        listOf(en, ta, hi).forEach { strings ->
            assertEquals("CHEVALIER ACADEMY MATRIC HIGHER SECONDARY SCHOOL", strings.officialSchoolName)
            assertEquals("Learn to Live & Love", strings.officialMotto)
            assertEquals("Siluvathur Road, Dindigul - 624 306", strings.officialAddress)
            assertEquals("POWERED BY JK LABS", strings.poweredByAttribution)
        }
    }

    @Test
    fun `preference enum mappings fall back safely`() {
        assertEquals(AppLanguage.TAMIL, AppLanguage.fromCode("ta"))
        assertEquals(AppLanguage.HINDI, AppLanguage.fromCode("hi"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("unknown"))

        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromKey("light"))
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromKey("dark"))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromKey(null))
    }
}

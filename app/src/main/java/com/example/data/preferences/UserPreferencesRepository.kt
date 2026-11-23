package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.chevalierDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "chevalier_academy_preferences"
)

data class AdminHeroBannerConfig(
    val titleLine1: String = DEFAULT_TITLE_LINE_1,
    val welcomeLine2: String = DEFAULT_WELCOME_LINE_2,
    val subtitleLine3: String = DEFAULT_SUBTITLE_LINE_3,
    val backgroundImageUri: String? = null,
    val updatedAtMillis: Long = 0L,
    val updatedByName: String? = null
) {
    fun resolvedWelcomeLine(adminDisplayName: String): String {
        val cleanName = adminDisplayName.trim().ifEmpty { "Admin" }
        val trimmed = welcomeLine2.trim()
        return when {
            trimmed.isEmpty() -> "Welcome Back, $cleanName"
            trimmed.contains(ADMIN_NAME_TOKEN) -> trimmed.replace(ADMIN_NAME_TOKEN, cleanName)
            else -> trimmed
        }
    }

    companion object {
        const val ADMIN_NAME_TOKEN = "{Admin Name}"
        const val DEFAULT_TITLE_LINE_1 = "Admin Dashboard"
        const val DEFAULT_WELCOME_LINE_2 = "Welcome Back, {Admin Name}"
        const val DEFAULT_SUBTITLE_LINE_3 = "School Administration Panel"
    }
}

data class AcademyHeroBannerConfig(
    val imageUri: String? = null,
    val overlayTitle: String = "",
    val overlaySubtitle: String = "",
    val isPublished: Boolean = false,
    val updatedAtMillis: Long = 0L,
    val updatedByName: String? = null
) {
    val hasImage: Boolean get() = !imageUri.isNullOrBlank()
    val hasOverlayText: Boolean get() = overlayTitle.isNotBlank() || overlaySubtitle.isNotBlank()
}

data class PostFeedVisibility(
    val showOnHomeFeed: Boolean = true,
    val showOnAcademyFeed: Boolean = false
)

data class FeedVisibilityPreferences(
    val academyFeedVisibleRemoteIds: Set<String> = emptySet(),
    val homeFeedHiddenRemoteIds: Set<String> = emptySet()
) {
    fun visibilityFor(postRemoteId: String): PostFeedVisibility {
        val normalized = postRemoteId.trim()
        if (normalized.isEmpty()) {
            return PostFeedVisibility(showOnHomeFeed = true, showOnAcademyFeed = false)
        }
        return PostFeedVisibility(
            showOnHomeFeed = normalized !in homeFeedHiddenRemoteIds,
            showOnAcademyFeed = normalized in academyFeedVisibleRemoteIds
        )
    }

    fun isVisibleOnHomeFeed(postRemoteId: String): Boolean =
        visibilityFor(postRemoteId).showOnHomeFeed

    fun isVisibleOnAcademyFeed(postRemoteId: String): Boolean =
        visibilityFor(postRemoteId).showOnAcademyFeed
}

data class UserPreferencesState(
    val selectedLanguage: AppLanguage = AppLanguage.ENGLISH,
    val isLanguageSelectedOnLaunch: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val deviceAlertsEnabled: Boolean = false,
    val likedPostIds: Set<Long> = emptySet(),
    val bookmarkedPostIds: Set<Long> = emptySet(),
    val likedPostRemoteIds: Set<String> = emptySet(),
    val bookmarkedPostRemoteIds: Set<String> = emptySet()
) {
    fun isPostLiked(localPostId: Long, remoteId: String? = null): Boolean {
        val normalizedRemoteId = remoteId?.trim()?.takeIf { it.isNotEmpty() }
        return localPostId in likedPostIds ||
            (normalizedRemoteId != null && normalizedRemoteId in likedPostRemoteIds)
    }

    fun isPostBookmarked(localPostId: Long, remoteId: String? = null): Boolean {
        val normalizedRemoteId = remoteId?.trim()?.takeIf { it.isNotEmpty() }
        return localPostId in bookmarkedPostIds ||
            (normalizedRemoteId != null && normalizedRemoteId in bookmarkedPostRemoteIds)
    }
}

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE_CODE = stringPreferencesKey("interface_language_code")
        val LANGUAGE_ONBOARDED = booleanPreferencesKey("language_selected_on_launch")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEVICE_ALERTS_ENABLED = booleanPreferencesKey("device_alerts_enabled")
        val LIKED_POST_IDS = stringSetPreferencesKey("social_liked_post_ids")
        val BOOKMARKED_POST_IDS = stringSetPreferencesKey("social_bookmarked_post_ids")
        val LIKED_POST_REMOTE_IDS = stringSetPreferencesKey("social_liked_post_remote_ids")
        val BOOKMARKED_POST_REMOTE_IDS = stringSetPreferencesKey("social_bookmarked_post_remote_ids")
        val ADMIN_HERO_LINE_1 = stringPreferencesKey("admin_hero_line_1")
        val ADMIN_HERO_LINE_2 = stringPreferencesKey("admin_hero_line_2")
        val ADMIN_HERO_LINE_3 = stringPreferencesKey("admin_hero_line_3")
        val ADMIN_HERO_IMAGE_URI = stringPreferencesKey("admin_hero_image_uri")
        val ADMIN_HERO_UPDATED_AT = longPreferencesKey("admin_hero_updated_at")
        val ADMIN_HERO_UPDATED_BY = stringPreferencesKey("admin_hero_updated_by")
        val ACADEMY_HERO_IMAGE_URI = stringPreferencesKey("academy_hero_image_uri")
        val ACADEMY_HERO_OVERLAY_TITLE = stringPreferencesKey("academy_hero_overlay_title")
        val ACADEMY_HERO_OVERLAY_SUBTITLE = stringPreferencesKey("academy_hero_overlay_subtitle")
        val ACADEMY_HERO_IS_PUBLISHED = booleanPreferencesKey("academy_hero_is_published")
        val ACADEMY_HERO_UPDATED_AT = longPreferencesKey("academy_hero_updated_at")
        val ACADEMY_HERO_UPDATED_BY = stringPreferencesKey("academy_hero_updated_by")
        val ACADEMY_FEED_VISIBLE_POST_REMOTE_IDS = stringSetPreferencesKey("academy_feed_visible_post_remote_ids")
        val HOME_FEED_HIDDEN_POST_REMOTE_IDS = stringSetPreferencesKey("home_feed_hidden_post_remote_ids")
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.chevalierDataStore.data.map { prefs ->
        val storedCode = prefs[Keys.LANGUAGE_CODE]
        val hasValidLanguage = storedCode != null && AppLanguage.entries.any { it.code == storedCode }
        UserPreferencesState(
            selectedLanguage = AppLanguage.fromCode(storedCode),
            isLanguageSelectedOnLaunch = (prefs[Keys.LANGUAGE_ONBOARDED] == true) && hasValidLanguage,
            themeMode = AppThemeMode.fromKey(prefs[Keys.THEME_MODE]),
            deviceAlertsEnabled = prefs[Keys.DEVICE_ALERTS_ENABLED] == true,
            likedPostIds = prefs[Keys.LIKED_POST_IDS]
                ?.mapNotNull { it.toLongOrNull() }
                ?.toSet()
                ?: emptySet(),
            bookmarkedPostIds = prefs[Keys.BOOKMARKED_POST_IDS]
                ?.mapNotNull { it.toLongOrNull() }
                ?.toSet()
                ?: emptySet(),
            likedPostRemoteIds = prefs[Keys.LIKED_POST_REMOTE_IDS]
                ?.asSequence()
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                ?: emptySet(),
            bookmarkedPostRemoteIds = prefs[Keys.BOOKMARKED_POST_REMOTE_IDS]
                ?.asSequence()
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                ?: emptySet()
        )
    }

    val adminHeroBannerFlow: Flow<AdminHeroBannerConfig> = context.chevalierDataStore.data.map { prefs ->
        AdminHeroBannerConfig(
            titleLine1 = prefs[Keys.ADMIN_HERO_LINE_1]
                ?.takeIf { it.isNotBlank() }
                ?: AdminHeroBannerConfig.DEFAULT_TITLE_LINE_1,
            welcomeLine2 = prefs[Keys.ADMIN_HERO_LINE_2]
                ?.takeIf { it.isNotBlank() }
                ?: AdminHeroBannerConfig.DEFAULT_WELCOME_LINE_2,
            subtitleLine3 = prefs[Keys.ADMIN_HERO_LINE_3]
                ?.takeIf { it.isNotBlank() }
                ?: AdminHeroBannerConfig.DEFAULT_SUBTITLE_LINE_3,
            backgroundImageUri = prefs[Keys.ADMIN_HERO_IMAGE_URI]?.takeIf { it.isNotBlank() },
            updatedAtMillis = prefs[Keys.ADMIN_HERO_UPDATED_AT] ?: 0L,
            updatedByName = prefs[Keys.ADMIN_HERO_UPDATED_BY]?.takeIf { it.isNotBlank() }
        )
    }

    val academyHeroBannerFlow: Flow<AcademyHeroBannerConfig> = context.chevalierDataStore.data.map { prefs ->
        AcademyHeroBannerConfig(
            imageUri = prefs[Keys.ACADEMY_HERO_IMAGE_URI]?.takeIf { it.isNotBlank() },
            overlayTitle = prefs[Keys.ACADEMY_HERO_OVERLAY_TITLE].orEmpty(),
            overlaySubtitle = prefs[Keys.ACADEMY_HERO_OVERLAY_SUBTITLE].orEmpty(),
            isPublished = prefs[Keys.ACADEMY_HERO_IS_PUBLISHED] == true,
            updatedAtMillis = prefs[Keys.ACADEMY_HERO_UPDATED_AT] ?: 0L,
            updatedByName = prefs[Keys.ACADEMY_HERO_UPDATED_BY]?.takeIf { it.isNotBlank() }
        )
    }

    val feedVisibilityFlow: Flow<FeedVisibilityPreferences> = context.chevalierDataStore.data.map { prefs ->
        FeedVisibilityPreferences(
            academyFeedVisibleRemoteIds = prefs[Keys.ACADEMY_FEED_VISIBLE_POST_REMOTE_IDS]
                ?.asSequence()
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                ?: emptySet(),
            homeFeedHiddenRemoteIds = prefs[Keys.HOME_FEED_HIDDEN_POST_REMOTE_IDS]
                ?.asSequence()
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                ?: emptySet()
        )
    }

    suspend fun saveAdminHeroBanner(config: AdminHeroBannerConfig) {
        context.chevalierDataStore.edit { prefs ->
            prefs[Keys.ADMIN_HERO_LINE_1] = config.titleLine1.trim()
                .ifEmpty { AdminHeroBannerConfig.DEFAULT_TITLE_LINE_1 }
            prefs[Keys.ADMIN_HERO_LINE_2] = config.welcomeLine2.trim()
                .ifEmpty { AdminHeroBannerConfig.DEFAULT_WELCOME_LINE_2 }
            prefs[Keys.ADMIN_HERO_LINE_3] = config.subtitleLine3.trim()
                .ifEmpty { AdminHeroBannerConfig.DEFAULT_SUBTITLE_LINE_3 }
            val cleanUri = config.backgroundImageUri?.trim()?.takeIf { it.isNotEmpty() }
            if (cleanUri != null) {
                prefs[Keys.ADMIN_HERO_IMAGE_URI] = cleanUri
            } else {
                prefs.remove(Keys.ADMIN_HERO_IMAGE_URI)
            }
            prefs[Keys.ADMIN_HERO_UPDATED_AT] = config.updatedAtMillis
            val cleanUpdatedBy = config.updatedByName?.trim()?.takeIf { it.isNotEmpty() }
            if (cleanUpdatedBy != null) {
                prefs[Keys.ADMIN_HERO_UPDATED_BY] = cleanUpdatedBy
            } else {
                prefs.remove(Keys.ADMIN_HERO_UPDATED_BY)
            }
        }
    }

    suspend fun saveAcademyHeroBanner(config: AcademyHeroBannerConfig) {
        context.chevalierDataStore.edit { prefs ->
            val cleanUri = config.imageUri?.trim()?.takeIf { it.isNotEmpty() }
            if (cleanUri != null) {
                prefs[Keys.ACADEMY_HERO_IMAGE_URI] = cleanUri
            } else {
                prefs.remove(Keys.ACADEMY_HERO_IMAGE_URI)
            }
            val cleanTitle = config.overlayTitle.trim()
            if (cleanTitle.isNotEmpty()) {
                prefs[Keys.ACADEMY_HERO_OVERLAY_TITLE] = cleanTitle
            } else {
                prefs.remove(Keys.ACADEMY_HERO_OVERLAY_TITLE)
            }
            val cleanSubtitle = config.overlaySubtitle.trim()
            if (cleanSubtitle.isNotEmpty()) {
                prefs[Keys.ACADEMY_HERO_OVERLAY_SUBTITLE] = cleanSubtitle
            } else {
                prefs.remove(Keys.ACADEMY_HERO_OVERLAY_SUBTITLE)
            }
            prefs[Keys.ACADEMY_HERO_IS_PUBLISHED] = config.isPublished && cleanUri != null
            prefs[Keys.ACADEMY_HERO_UPDATED_AT] = config.updatedAtMillis
            val cleanUpdatedBy = config.updatedByName?.trim()?.takeIf { it.isNotEmpty() }
            if (cleanUpdatedBy != null) {
                prefs[Keys.ACADEMY_HERO_UPDATED_BY] = cleanUpdatedBy
            } else {
                prefs.remove(Keys.ACADEMY_HERO_UPDATED_BY)
            }
        }
    }

    suspend fun setPostFeedVisibility(
        postRemoteId: String,
        showOnHomeFeed: Boolean,
        showOnAcademyFeed: Boolean
    ) {
        val normalized = postRemoteId.trim()
        if (normalized.isEmpty()) return
        context.chevalierDataStore.edit { prefs ->
            val academyVisible = prefs[Keys.ACADEMY_FEED_VISIBLE_POST_REMOTE_IDS]?.toMutableSet()
                ?: mutableSetOf()
            if (showOnAcademyFeed) {
                academyVisible.add(normalized)
            } else {
                academyVisible.remove(normalized)
            }
            prefs[Keys.ACADEMY_FEED_VISIBLE_POST_REMOTE_IDS] = academyVisible

            val homeHidden = prefs[Keys.HOME_FEED_HIDDEN_POST_REMOTE_IDS]?.toMutableSet()
                ?: mutableSetOf()
            if (showOnHomeFeed) {
                homeHidden.remove(normalized)
            } else {
                homeHidden.add(normalized)
            }
            prefs[Keys.HOME_FEED_HIDDEN_POST_REMOTE_IDS] = homeHidden
        }
    }

    suspend fun togglePostLike(postId: Long, postRemoteId: String? = null) {
        if (postId <= 0L) return
        val keyStr = postId.toString()
        val normalizedRemoteId = postRemoteId?.trim()?.takeIf { it.isNotEmpty() }
        context.chevalierDataStore.edit { prefs ->
            val current = prefs[Keys.LIKED_POST_IDS]?.toMutableSet() ?: mutableSetOf()
            val added = current.add(keyStr)
            if (!added) {
                current.remove(keyStr)
            }
            prefs[Keys.LIKED_POST_IDS] = current

            if (normalizedRemoteId != null) {
                val remoteSet = prefs[Keys.LIKED_POST_REMOTE_IDS]?.toMutableSet() ?: mutableSetOf()
                if (added) {
                    remoteSet.add(normalizedRemoteId)
                } else {
                    remoteSet.remove(normalizedRemoteId)
                }
                prefs[Keys.LIKED_POST_REMOTE_IDS] = remoteSet
            }
        }
    }

    suspend fun togglePostBookmark(postId: Long, postRemoteId: String? = null) {
        if (postId <= 0L) return
        val keyStr = postId.toString()
        val normalizedRemoteId = postRemoteId?.trim()?.takeIf { it.isNotEmpty() }
        context.chevalierDataStore.edit { prefs ->
            val current = prefs[Keys.BOOKMARKED_POST_IDS]?.toMutableSet() ?: mutableSetOf()
            val added = current.add(keyStr)
            if (!added) {
                current.remove(keyStr)
            }
            prefs[Keys.BOOKMARKED_POST_IDS] = current

            if (normalizedRemoteId != null) {
                val remoteSet = prefs[Keys.BOOKMARKED_POST_REMOTE_IDS]?.toMutableSet() ?: mutableSetOf()
                if (added) {
                    remoteSet.add(normalizedRemoteId)
                } else {
                    remoteSet.remove(normalizedRemoteId)
                }
                prefs[Keys.BOOKMARKED_POST_REMOTE_IDS] = remoteSet
            }
        }
    }

    /**
     * Non-breaking forward-compatibility helper: backfills stable post remoteIds from existing
     * local Long liked/bookmarked post IDs without removing or resetting local preferences.
     */
    suspend fun backfillSocialPostRemoteIds(localIdToRemoteId: Map<Long, String>) {
        if (localIdToRemoteId.isEmpty()) return
        context.chevalierDataStore.edit { prefs ->
            val localLikes = prefs[Keys.LIKED_POST_IDS]
                ?.mapNotNull { it.toLongOrNull() }
                ?.toSet()
                ?: emptySet()
            val localBookmarks = prefs[Keys.BOOKMARKED_POST_IDS]
                ?.mapNotNull { it.toLongOrNull() }
                ?.toSet()
                ?: emptySet()

            val mappedLikeRemoteIds = localLikes.mapNotNull { id ->
                localIdToRemoteId[id]?.trim()?.takeIf { it.isNotEmpty() }
            }
            val mappedBookmarkRemoteIds = localBookmarks.mapNotNull { id ->
                localIdToRemoteId[id]?.trim()?.takeIf { it.isNotEmpty() }
            }

            if (mappedLikeRemoteIds.isNotEmpty()) {
                val updatedLikes = (prefs[Keys.LIKED_POST_REMOTE_IDS] ?: emptySet()).toMutableSet()
                updatedLikes.addAll(mappedLikeRemoteIds)
                prefs[Keys.LIKED_POST_REMOTE_IDS] = updatedLikes
            }
            if (mappedBookmarkRemoteIds.isNotEmpty()) {
                val updatedBookmarks = (prefs[Keys.BOOKMARKED_POST_REMOTE_IDS] ?: emptySet()).toMutableSet()
                updatedBookmarks.addAll(mappedBookmarkRemoteIds)
                prefs[Keys.BOOKMARKED_POST_REMOTE_IDS] = updatedBookmarks
            }
        }
    }

    suspend fun setLanguage(language: AppLanguage, markSelectedOnLaunch: Boolean = false) {
        context.chevalierDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = language.code
            if (markSelectedOnLaunch) {
                prefs[Keys.LANGUAGE_ONBOARDED] = true
            }
        }
    }

    suspend fun setThemeMode(themeMode: AppThemeMode) {
        context.chevalierDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.storageKey
        }
    }

    suspend fun setDeviceAlertsEnabled(enabled: Boolean) {
        context.chevalierDataStore.edit { prefs ->
            prefs[Keys.DEVICE_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun reconcileSocialPostPreferences(
        validPostIds: Set<Long>,
        validPostRemoteIds: Set<String>? = null
    ) {
        val validKeyStrings = validPostIds.asSequence()
            .filter { it > 0L }
            .map { it.toString() }
            .toSet()
        context.chevalierDataStore.edit { prefs ->
            val currentLikes = prefs[Keys.LIKED_POST_IDS] ?: emptySet()
            val currentBookmarks = prefs[Keys.BOOKMARKED_POST_IDS] ?: emptySet()
            prefs[Keys.LIKED_POST_IDS] = currentLikes.intersect(validKeyStrings)
            prefs[Keys.BOOKMARKED_POST_IDS] = currentBookmarks.intersect(validKeyStrings)
            if (validPostRemoteIds != null) {
                val validRemoteKeys = validPostRemoteIds.asSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()
                val currentRemoteLikes = prefs[Keys.LIKED_POST_REMOTE_IDS] ?: emptySet()
                val currentRemoteBookmarks = prefs[Keys.BOOKMARKED_POST_REMOTE_IDS] ?: emptySet()
                prefs[Keys.LIKED_POST_REMOTE_IDS] = currentRemoteLikes.intersect(validRemoteKeys)
                prefs[Keys.BOOKMARKED_POST_REMOTE_IDS] = currentRemoteBookmarks.intersect(validRemoteKeys)
            }
        }
    }

    suspend fun restorePreferences(
        language: AppLanguage,
        isOnboarded: Boolean,
        themeMode: AppThemeMode,
        deviceAlertsEnabled: Boolean = false,
        likedPostIds: Set<Long>? = null,
        bookmarkedPostIds: Set<Long>? = null,
        likedPostRemoteIds: Set<String>? = null,
        bookmarkedPostRemoteIds: Set<String>? = null
    ) {
        context.chevalierDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = language.code
            prefs[Keys.LANGUAGE_ONBOARDED] = isOnboarded
            prefs[Keys.THEME_MODE] = themeMode.storageKey
            prefs[Keys.DEVICE_ALERTS_ENABLED] = deviceAlertsEnabled
            if (likedPostIds != null) {
                prefs[Keys.LIKED_POST_IDS] = likedPostIds
                    .asSequence()
                    .filter { it > 0L }
                    .map { it.toString() }
                    .toSet()
            }
            if (bookmarkedPostIds != null) {
                prefs[Keys.BOOKMARKED_POST_IDS] = bookmarkedPostIds
                    .asSequence()
                    .filter { it > 0L }
                    .map { it.toString() }
                    .toSet()
            }
            if (likedPostRemoteIds != null) {
                prefs[Keys.LIKED_POST_REMOTE_IDS] = likedPostRemoteIds
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }
            if (bookmarkedPostRemoteIds != null) {
                prefs[Keys.BOOKMARKED_POST_REMOTE_IDS] = bookmarkedPostRemoteIds
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }
        }
    }
}

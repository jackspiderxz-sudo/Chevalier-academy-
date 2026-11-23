package com.example.data.repository

import android.net.Uri
import com.example.data.local.ChevalierDatabase
import com.example.data.local.dao.SchoolHighlightDao
import com.example.data.local.dao.SchoolPostDao
import com.example.data.local.entity.SchoolHighlightEntity
import com.example.data.local.entity.SchoolPostCategory
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostStatus
import com.example.data.local.entity.SchoolPostType
import com.example.data.local.entity.SyncMetadataDefaults
import com.example.data.local.entity.SyncState
import com.example.data.media.MediaStudioManager
import com.example.data.media.ProcessedMediaResult
import com.example.data.media.YouTubeMediaHelper
import kotlinx.coroutines.flow.Flow

/**
 * Local-only repository for School Posts and 24-hour YouTube Shorts Highlights.
 * Reuses MediaStudioManager directly for image optimization and local storage.
 */
class SocialFeedRepository(
    private val schoolPostDao: SchoolPostDao,
    private val schoolHighlightDao: SchoolHighlightDao,
    private val mediaStudioManager: MediaStudioManager
) {
    val publishedPosts: Flow<List<SchoolPostEntity>> = schoolPostDao.observePublishedPosts()
    val allPostsForAdmin: Flow<List<SchoolPostEntity>> = schoolPostDao.observeAllPosts()
    val allHighlightsForAdmin: Flow<List<SchoolHighlightEntity>> = schoolHighlightDao.observeAllHighlights()

    fun observePublishedPostsByCategory(category: String): Flow<List<SchoolPostEntity>> =
        schoolPostDao.observePublishedPostsByCategory(category)

    fun observeActiveHighlights(nowMillis: Long = System.currentTimeMillis()): Flow<List<SchoolHighlightEntity>> =
        schoolHighlightDao.observeActiveHighlights(nowMillis)

    /**
     * Reuses the existing MediaStudioManager pipeline without duplicating media storage.
     */
    suspend fun importAndOptimizePostImage(sourceUri: Uri): Result<ProcessedMediaResult> =
        mediaStudioManager.importAndOptimizeImage(sourceUri)

    suspend fun saveSchoolPost(
        id: Long = 0L,
        postType: String,
        category: String = SchoolPostCategory.GENERAL,
        title: String,
        description: String = "",
        youtubeUrl: String? = null,
        customThumbnailUri: String? = null,
        photoUris: List<String> = emptyList(),
        status: String = SchoolPostStatus.UNPUBLISHED,
        isFeatured: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ): Result<Long> = runCatching {
        require(postType in SchoolPostType.all) { "Unsupported post type: $postType" }
        require(status in SchoolPostStatus.all) { "Unsupported post status: $status" }
        val cleanTitle = title.trim()
        if (postType != SchoolPostType.YOUTUBE_DIRECT) {
            require(cleanTitle.isNotEmpty()) { "Post title is required." }
        }

        val cleanCategory = category.trim().ifEmpty { SchoolPostCategory.GENERAL }
        val cleanDescription = description.trim()
        val cleanYoutubeUrl = youtubeUrl?.trim()?.takeIf { it.isNotEmpty() }
        val cleanCustomThumb = customThumbnailUri?.trim()?.takeIf { it.isNotEmpty() }
        val cleanedPhotos = photoUris.map { it.trim() }.filter { it.isNotEmpty() }

        when (postType) {
            SchoolPostType.YOUTUBE_DIRECT -> {
                require(YouTubeMediaHelper.isValidYouTubeUrl(cleanYoutubeUrl)) {
                    "A valid YouTube URL is required for YouTube Direct Posts."
                }
            }
            SchoolPostType.ADMIN_CUSTOM_VIDEO -> {
                require(YouTubeMediaHelper.isValidYouTubeUrl(cleanYoutubeUrl)) {
                    "A valid YouTube URL is required for Admin Custom Video Posts."
                }
            }
            SchoolPostType.PHOTO -> {
                require(cleanedPhotos.isNotEmpty()) {
                    "Photo Posts require at least 1 photo."
                }
                require(cleanedPhotos.size <= SchoolPostEntity.MAX_PHOTO_COUNT) {
                    "Photo Posts allow a maximum of ${SchoolPostEntity.MAX_PHOTO_COUNT} photos."
                }
            }
        }

        val existing = if (id > 0L) schoolPostDao.getById(id) else null
        val createdAt = existing?.createdAtMillis ?: nowMillis
        val publishedAt = when (status) {
            SchoolPostStatus.PUBLISHED -> existing?.publishedAtMillis ?: nowMillis
            else -> existing?.publishedAtMillis
        }

        val nextSyncState = when (existing?.syncState) {
            SyncState.SYNCED -> SyncState.PENDING_UPLOAD
            null -> SyncMetadataDefaults.DEFAULT_SYNC_STATE
            else -> existing.syncState
        }

        val entity = SchoolPostEntity(
            id = existing?.id ?: 0L,
            postType = postType,
            category = cleanCategory,
            title = cleanTitle,
            description = cleanDescription,
            youtubeUrl = if (postType == SchoolPostType.PHOTO) null else cleanYoutubeUrl,
            customThumbnailUri = if (postType == SchoolPostType.ADMIN_CUSTOM_VIDEO) cleanCustomThumb else null,
            photoUrisJson = if (postType == SchoolPostType.PHOTO) {
                SchoolPostEntity.encodePhotoUris(cleanedPhotos)
            } else {
                "[]"
            },
            status = status,
            isFeatured = isFeatured,
            createdAtMillis = createdAt,
            publishedAtMillis = publishedAt,
            updatedAtMillis = nowMillis,
            remoteId = existing?.remoteId ?: SyncMetadataDefaults.newRemoteId(),
            tenantId = existing?.tenantId ?: SyncMetadataDefaults.DEFAULT_TENANT_ID,
            syncState = nextSyncState,
            serverUpdatedAtMillis = existing?.serverUpdatedAtMillis,
            deletedAtMillis = existing?.deletedAtMillis
        )
        schoolPostDao.upsert(entity)
    }

    suspend fun publishPost(id: Long, nowMillis: Long = System.currentTimeMillis()): Result<Unit> =
        runCatching {
            val existing = schoolPostDao.getById(id)
                ?: throw IllegalArgumentException("School post #$id not found.")
            schoolPostDao.updateStatus(
                id = id,
                status = SchoolPostStatus.PUBLISHED,
                publishedAtMillis = existing.publishedAtMillis ?: nowMillis,
                updatedAtMillis = nowMillis
            )
        }

    suspend fun unpublishPost(id: Long, nowMillis: Long = System.currentTimeMillis()): Result<Unit> =
        runCatching {
            val existing = schoolPostDao.getById(id)
                ?: throw IllegalArgumentException("School post #$id not found.")
            schoolPostDao.updateStatus(
                id = id,
                status = SchoolPostStatus.UNPUBLISHED,
                publishedAtMillis = existing.publishedAtMillis,
                updatedAtMillis = nowMillis
            )
        }

    suspend fun archivePost(id: Long, nowMillis: Long = System.currentTimeMillis()): Result<Unit> =
        runCatching {
            val existing = schoolPostDao.getById(id)
                ?: throw IllegalArgumentException("School post #$id not found.")
            schoolPostDao.updateStatus(
                id = id,
                status = SchoolPostStatus.ARCHIVED,
                publishedAtMillis = existing.publishedAtMillis,
                updatedAtMillis = nowMillis
            )
        }

    suspend fun setPostFeatured(
        id: Long,
        isFeatured: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): Result<Unit> = runCatching {
        require(schoolPostDao.getById(id) != null) { "School post #$id not found." }
        schoolPostDao.updateFeatured(id, isFeatured, nowMillis)
    }

    suspend fun deletePost(id: Long) = schoolPostDao.deleteById(id)

    /**
     * Publishes or updates a 24-hour YouTube Shorts Highlight.
     * Expiry hides only the in-app Highlight entry after 24 hours without affecting YouTube.
     */
    suspend fun saveHighlight(
        id: Long = 0L,
        title: String = "",
        youtubeShortsUrl: String,
        status: String = SchoolPostStatus.PUBLISHED,
        nowMillis: Long = System.currentTimeMillis()
    ): Result<Long> = runCatching {
        require(status in SchoolPostStatus.all) { "Unsupported highlight status: $status" }
        val parsed = YouTubeMediaHelper.parse(youtubeShortsUrl)
            ?: throw IllegalArgumentException("A valid YouTube Shorts URL is required.")
        require(parsed.isShort) {
            "Highlight URL must be a YouTube Shorts link (youtube.com/shorts/...)."
        }

        val existing = if (id > 0L) schoolHighlightDao.getById(id) else null
        val createdAt = existing?.createdAtMillis ?: nowMillis
        val publishedAt = existing?.publishedAtMillis ?: nowMillis
        val expiresAt = existing?.expiresAtMillis ?: SchoolHighlightEntity.computeExpiryMillis(publishedAt)

        val nextSyncState = when (existing?.syncState) {
            SyncState.SYNCED -> SyncState.PENDING_UPLOAD
            null -> SyncMetadataDefaults.DEFAULT_SYNC_STATE
            else -> existing.syncState
        }

        val entity = SchoolHighlightEntity(
            id = existing?.id ?: 0L,
            title = title.trim(),
            youtubeShortsUrl = parsed.canonicalWatchUrl,
            status = status,
            createdAtMillis = createdAt,
            publishedAtMillis = publishedAt,
            expiresAtMillis = expiresAt,
            remoteId = existing?.remoteId ?: SyncMetadataDefaults.newRemoteId(),
            tenantId = existing?.tenantId ?: SyncMetadataDefaults.DEFAULT_TENANT_ID,
            syncState = nextSyncState,
            serverUpdatedAtMillis = existing?.serverUpdatedAtMillis,
            deletedAtMillis = existing?.deletedAtMillis
        )
        schoolHighlightDao.upsert(entity)
    }

    suspend fun updateHighlightStatus(id: Long, status: String): Result<Unit> = runCatching {
        require(status in SchoolPostStatus.all) { "Unsupported highlight status: $status" }
        require(schoolHighlightDao.getById(id) != null) { "Highlight #$id not found." }
        schoolHighlightDao.updateStatus(id, status)
    }

    suspend fun deleteHighlight(id: Long) = schoolHighlightDao.deleteById(id)

    companion object {
        fun fromDatabase(
            database: ChevalierDatabase,
            mediaStudioManager: MediaStudioManager
        ): SocialFeedRepository = SocialFeedRepository(
            schoolPostDao = database.schoolPostDao(),
            schoolHighlightDao = database.schoolHighlightDao(),
            mediaStudioManager = mediaStudioManager
        )
    }
}

package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray

object SchoolPostType {
    const val YOUTUBE_DIRECT = "YOUTUBE_DIRECT"
    const val ADMIN_CUSTOM_VIDEO = "ADMIN_CUSTOM_VIDEO"
    const val PHOTO = "PHOTO"

    val all: Set<String> = setOf(YOUTUBE_DIRECT, ADMIN_CUSTOM_VIDEO, PHOTO)
}

object SchoolPostStatus {
    const val PUBLISHED = "PUBLISHED"
    const val UNPUBLISHED = "UNPUBLISHED"
    const val ARCHIVED = "ARCHIVED"

    val all: Set<String> = setOf(PUBLISHED, UNPUBLISHED, ARCHIVED)
}

object SchoolPostCategory {
    const val GENERAL = "General"
    const val ACADEMICS = "Academics"
    const val EVENTS = "Events"
    const val SPORTS = "Sports"
    const val ACHIEVEMENTS = "Achievements"
    const val CAMPUS_LIFE = "Campus Life"
    const val ANNOUNCEMENT = "Announcement"

    val defaults: List<String> = listOf(
        GENERAL,
        ACADEMICS,
        EVENTS,
        SPORTS,
        ACHIEVEMENTS,
        CAMPUS_LIFE,
        ANNOUNCEMENT
    )
}

/**
 * Dedicated local Room entity for Chevalier Academy Home Social Network posts.
 * Supports YouTube Direct Posts, Admin Custom Video Posts, and Photo Posts (up to 10 photos).
 */
@Entity(
    tableName = "school_posts",
    indices = [
        Index(value = ["status"]),
        Index(value = ["isFeatured"]),
        Index(value = ["publishedAtMillis"]),
        Index(value = ["category"]),
        Index(value = ["remoteId"], unique = true)
    ]
)
data class SchoolPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postType: String = SchoolPostType.PHOTO,
    val category: String = SchoolPostCategory.GENERAL,
    val title: String,
    val description: String = "",
    val youtubeUrl: String? = null,
    val customThumbnailUri: String? = null,
    val photoUrisJson: String = "[]",
    val status: String = SchoolPostStatus.UNPUBLISHED,
    val isFeatured: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val publishedAtMillis: Long? = null,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val remoteId: String = SyncMetadataDefaults.newRemoteId(),
    val tenantId: String = SyncMetadataDefaults.DEFAULT_TENANT_ID,
    val syncState: String = SyncMetadataDefaults.DEFAULT_SYNC_STATE,
    val serverUpdatedAtMillis: Long? = null,
    val deletedAtMillis: Long? = null
) {
    fun photoUris(): List<String> = decodePhotoUris(photoUrisJson)

    companion object {
        const val MAX_PHOTO_COUNT: Int = 10

        fun encodePhotoUris(uris: List<String>): String {
            val cleaned = uris.map { it.trim() }.filter { it.isNotEmpty() }
            require(cleaned.size <= MAX_PHOTO_COUNT) {
                "Photo posts support a maximum of $MAX_PHOTO_COUNT photos."
            }
            return JSONArray().apply {
                cleaned.forEach { put(it) }
            }.toString()
        }

        fun decodePhotoUris(json: String?): List<String> {
            if (json.isNullOrBlank()) return emptyList()
            return runCatching {
                val arr = JSONArray(json)
                buildList(arr.length().coerceAtMost(MAX_PHOTO_COUNT)) {
                    for (i in 0 until arr.length()) {
                        val item = arr.optString(i, "").trim()
                        if (item.isNotEmpty()) {
                            add(item)
                            if (size >= MAX_PHOTO_COUNT) break
                        }
                    }
                }
            }.getOrDefault(emptyList())
        }
    }
}

/**
 * 24-hour YouTube Shorts Highlight entry for the Home Social Network.
 * Expiry hides only the in-app Highlight entry; it never deletes or modifies the YouTube video.
 */
@Entity(
    tableName = "school_highlights",
    indices = [
        Index(value = ["status"]),
        Index(value = ["expiresAtMillis"]),
        Index(value = ["publishedAtMillis"]),
        Index(value = ["remoteId"], unique = true)
    ]
)
data class SchoolHighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val youtubeShortsUrl: String,
    val status: String = SchoolPostStatus.PUBLISHED,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val publishedAtMillis: Long = System.currentTimeMillis(),
    val expiresAtMillis: Long = publishedAtMillis + HIGHLIGHT_TTL_MILLIS,
    val remoteId: String = SyncMetadataDefaults.newRemoteId(),
    val tenantId: String = SyncMetadataDefaults.DEFAULT_TENANT_ID,
    val syncState: String = SyncMetadataDefaults.DEFAULT_SYNC_STATE,
    val serverUpdatedAtMillis: Long? = null,
    val deletedAtMillis: Long? = null
) {
    fun isActiveAt(nowMillis: Long = System.currentTimeMillis()): Boolean =
        status == SchoolPostStatus.PUBLISHED && expiresAtMillis > nowMillis

    companion object {
        const val HIGHLIGHT_TTL_MILLIS: Long = 24L * 60L * 60L * 1000L

        fun computeExpiryMillis(publishedAtMillis: Long): Long =
            publishedAtMillis + HIGHLIGHT_TTL_MILLIS
    }
}

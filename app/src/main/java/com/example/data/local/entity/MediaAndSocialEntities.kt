package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "campus_hero_photos")
data class CampusHeroPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageUri: String,
    val caption: String? = null,
    val displayOrder: Int = 0,
    val isVerified: Boolean = false,
    val isPublished: Boolean = false,
    val isApproved: Boolean = false,
    val approvalStatus: String = ApprovalStatus.PENDING,
    val addedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "gallery_albums")
data class GalleryAlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = "Campus",
    val description: String? = null,
    val eventDateText: String? = null,
    val coverMediaUri: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val isVerified: Boolean = false,
    val isPublished: Boolean = false,
    val isApproved: Boolean = false,
    val approvalStatus: String = ApprovalStatus.PENDING,
    val displayOrder: Int = 0
)

@Entity(
    tableName = "gallery_media",
    foreignKeys = [
        ForeignKey(
            entity = GalleryAlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["albumId"])]
)
data class GalleryMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val albumId: Long,
    val title: String? = null,
    val caption: String? = null,
    val mediaUri: String,
    val mediaType: String = "IMAGE",
    val displayOrder: Int = 0,
    val isVerified: Boolean = false,
    val isPublished: Boolean = false,
    val isApproved: Boolean = false,
    val approvalStatus: String = ApprovalStatus.PENDING,
    val addedAtMillis: Long = System.currentTimeMillis()
)

data class GalleryAlbumWithMedia(
    @Embedded val album: GalleryAlbumEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "albumId"
    )
    val mediaItems: List<GalleryMediaEntity>
)

@Entity(tableName = "social_media_links")
data class SocialMediaLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platformName: String,
    val handleOrLabel: String,
    val url: String,
    val displayOrder: Int = 0,
    val isVerified: Boolean = false,
    val isPublished: Boolean = false,
    val isApproved: Boolean = false,
    val approvalStatus: String = ApprovalStatus.PENDING
)

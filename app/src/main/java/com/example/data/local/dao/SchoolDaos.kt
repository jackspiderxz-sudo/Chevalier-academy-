package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.AcademicInfoEntity
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.CampusHeroPhotoEntity
import com.example.data.local.entity.FacilityEntity
import com.example.data.local.entity.GalleryAlbumEntity
import com.example.data.local.entity.GalleryAlbumWithMedia
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.HigherSecondaryCourseEntity
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.local.entity.SocialMediaLinkEntity
import com.example.data.local.entity.TransportRouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolProfileDao {
    @Query("SELECT * FROM school_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<SchoolProfileEntity?>

    @Query("SELECT * FROM school_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileOnce(): SchoolProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: SchoolProfileEntity)
}

@Dao
interface CampusHeroPhotoDao {
    @Query(
        "SELECT * FROM campus_hero_photos " +
            "WHERE isVerified = 1 AND isPublished = 1 AND isApproved = 1 AND approvalStatus = 'APPROVED' " +
            "ORDER BY displayOrder ASC, id ASC"
    )
    fun observePublicPhotos(): Flow<List<CampusHeroPhotoEntity>>

    @Query("SELECT * FROM campus_hero_photos ORDER BY displayOrder ASC, id ASC")
    fun observeAllForManagement(): Flow<List<CampusHeroPhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(photo: CampusHeroPhotoEntity): Long

    @Query("DELETE FROM campus_hero_photos WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AcademicInfoDao {
    @Query(
        "SELECT * FROM academic_info " +
            "WHERE isVerified = 1 AND isPublished = 1 AND isApproved = 1 AND approvalStatus = 'APPROVED' " +
            "ORDER BY displayOrder ASC, id ASC"
    )
    fun observePublic(): Flow<List<AcademicInfoEntity>>

    @Query("SELECT * FROM academic_info ORDER BY displayOrder ASC, id ASC")
    fun observeAll(): Flow<List<AcademicInfoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AcademicInfoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: AcademicInfoEntity): Long

    @Update
    suspend fun update(item: AcademicInfoEntity)

    @Query("DELETE FROM academic_info WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface HigherSecondaryCourseDao {
    @Query(
        "SELECT * FROM higher_secondary_courses " +
            "WHERE isVerified = 1 AND isPublished = 1 AND isApproved = 1 AND approvalStatus = 'APPROVED' " +
            "ORDER BY displayOrder ASC, id ASC"
    )
    fun observePublic(): Flow<List<HigherSecondaryCourseEntity>>

    @Query("SELECT * FROM higher_secondary_courses ORDER BY displayOrder ASC, id ASC")
    fun observeAll(): Flow<List<HigherSecondaryCourseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(course: HigherSecondaryCourseEntity): Long

    @Update
    suspend fun update(course: HigherSecondaryCourseEntity)

    @Query("DELETE FROM higher_secondary_courses WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface FacilityDao {
    @Query(
        "SELECT * FROM facilities " +
            "WHERE isVerified = 1 AND isPublished = 1 AND isApproved = 1 AND approvalStatus = 'APPROVED' " +
            "ORDER BY displayOrder ASC, id ASC"
    )
    fun observePublic(): Flow<List<FacilityEntity>>

    @Query("SELECT * FROM facilities ORDER BY displayOrder ASC, id ASC")
    fun observeAll(): Flow<List<FacilityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(facility: FacilityEntity): Long

    @Update
    suspend fun update(facility: FacilityEntity)

    @Query("DELETE FROM facilities WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface TransportDao {
    @Query(
        "SELECT * FROM transport_routes " +
            "WHERE isVerified = 1 AND isPublished = 1 AND isApproved = 1 AND approvalStatus = 'APPROVED' " +
            "ORDER BY displayOrder ASC, routeNumber ASC, id ASC"
    )
    fun observePublic(): Flow<List<TransportRouteEntity>>

    @Query("SELECT * FROM transport_routes ORDER BY displayOrder ASC, routeNumber ASC, id ASC")
    fun observeAll(): Flow<List<TransportRouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(route: TransportRouteEntity): Long

    @Update
    suspend fun update(route: TransportRouteEntity)

    @Query("DELETE FROM transport_routes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface NewsDao {
    @Query(
        "SELECT * FROM news_items " +
            "WHERE isVerified = 1 AND isPublished = 1 AND isApproved = 1 AND approvalStatus = 'APPROVED' " +
            "ORDER BY isFeatured DESC, displayOrder ASC, publishedAtMillis DESC, id DESC"
    )
    fun observePublic(): Flow<List<NewsItemEntity>>

    @Query("SELECT * FROM news_items ORDER BY displayOrder ASC, publishedAtMillis DESC, id DESC")
    fun observeAll(): Flow<List<NewsItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: NewsItemEntity): Long

    @Update
    suspend fun update(item: NewsItemEntity)

    @Query("DELETE FROM news_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AnnouncementDao {
    @Query(
        "SELECT * FROM announcements " +
            "WHERE isVerified = 1 AND isPublished = 1 AND isApproved = 1 AND approvalStatus = 'APPROVED' " +
            "ORDER BY displayOrder ASC, issuedAtMillis DESC, id DESC"
    )
    fun observePublic(): Flow<List<AnnouncementEntity>>

    @Query("SELECT * FROM announcements ORDER BY displayOrder ASC, issuedAtMillis DESC, id DESC")
    fun observeAll(): Flow<List<AnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: AnnouncementEntity): Long

    @Update
    suspend fun update(item: AnnouncementEntity)

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface GalleryDao {
    @Transaction
    @Query(
        "SELECT * FROM gallery_albums " +
            "WHERE isVerified = 1 AND isPublished = 1 AND isApproved = 1 AND approvalStatus = 'APPROVED' " +
            "ORDER BY displayOrder ASC, createdAtMillis DESC, id DESC"
    )
    fun observePublicAlbumsWithMedia(): Flow<List<GalleryAlbumWithMedia>>

    @Transaction
    @Query("SELECT * FROM gallery_albums ORDER BY displayOrder ASC, createdAtMillis DESC, id DESC")
    fun observeAlbumsWithMedia(): Flow<List<GalleryAlbumWithMedia>>

    @Query(
        "SELECT * FROM gallery_media " +
            "WHERE isVerified = 1 AND isPublished = 1 AND isApproved = 1 AND approvalStatus = 'APPROVED' " +
            "ORDER BY displayOrder ASC, addedAtMillis DESC"
    )
    fun observePublicMedia(): Flow<List<GalleryMediaEntity>>

    @Query("SELECT * FROM gallery_media ORDER BY displayOrder ASC, addedAtMillis DESC")
    fun observeAllMedia(): Flow<List<GalleryMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbum(album: GalleryAlbumEntity): Long

    @Update
    suspend fun updateAlbum(album: GalleryAlbumEntity): Int

    @Transaction
    suspend fun upsertAlbum(album: GalleryAlbumEntity): Long {
        if (album.id > 0L) {
            val updated = updateAlbum(album)
            if (updated > 0) return album.id
        }
        val insertedId = insertAlbum(album)
        if (insertedId != -1L) return insertedId
        updateAlbum(album)
        return album.id
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMedia(media: GalleryMediaEntity): Long

    @Query("SELECT id FROM gallery_media WHERE albumId = :albumId")
    suspend fun getMediaIdsForAlbumOnce(albumId: Long): List<Long>

    @Query("DELETE FROM gallery_albums WHERE id = :albumId")
    suspend fun deleteAlbumById(albumId: Long)

    @Query("DELETE FROM gallery_media WHERE id = :mediaId")
    suspend fun deleteMediaById(mediaId: Long)
}

@Dao
interface SocialMediaDao {
    @Query(
        "SELECT * FROM social_media_links " +
            "WHERE isVerified = 1 AND isPublished = 1 AND isApproved = 1 AND approvalStatus = 'APPROVED' " +
            "ORDER BY displayOrder ASC, id ASC"
    )
    fun observePublic(): Flow<List<SocialMediaLinkEntity>>

    @Query("SELECT * FROM social_media_links ORDER BY displayOrder ASC, id ASC")
    fun observeAll(): Flow<List<SocialMediaLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: SocialMediaLinkEntity): Long

    @Update
    suspend fun update(link: SocialMediaLinkEntity)

    @Query("DELETE FROM social_media_links WHERE id = :id")
    suspend fun deleteById(id: Long)
}

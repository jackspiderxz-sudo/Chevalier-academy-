package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SchoolHighlightEntity
import com.example.data.local.entity.SchoolPostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolPostDao {
    @Query(
        "SELECT * FROM school_posts " +
            "WHERE status = 'PUBLISHED' " +
            "ORDER BY isFeatured DESC, COALESCE(publishedAtMillis, createdAtMillis) DESC, id DESC"
    )
    fun observePublishedPosts(): Flow<List<SchoolPostEntity>>

    @Query(
        "SELECT * FROM school_posts " +
            "WHERE status = 'PUBLISHED' AND category = :category " +
            "ORDER BY isFeatured DESC, COALESCE(publishedAtMillis, createdAtMillis) DESC, id DESC"
    )
    fun observePublishedPostsByCategory(category: String): Flow<List<SchoolPostEntity>>

    @Query(
        "SELECT * FROM school_posts " +
            "ORDER BY isFeatured DESC, updatedAtMillis DESC, id DESC"
    )
    fun observeAllPosts(): Flow<List<SchoolPostEntity>>

    @Query(
        "SELECT * FROM school_posts " +
            "ORDER BY isFeatured DESC, updatedAtMillis DESC, id DESC"
    )
    suspend fun getAllPostsOnce(): List<SchoolPostEntity>

    @Query("SELECT * FROM school_posts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SchoolPostEntity?

    @Query("SELECT * FROM school_posts WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): SchoolPostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(post: SchoolPostEntity): Long

    @Query(
        "UPDATE school_posts " +
            "SET status = :status, publishedAtMillis = :publishedAtMillis, updatedAtMillis = :updatedAtMillis " +
            "WHERE id = :id"
    )
    suspend fun updateStatus(
        id: Long,
        status: String,
        publishedAtMillis: Long?,
        updatedAtMillis: Long
    )

    @Query(
        "UPDATE school_posts " +
            "SET isFeatured = :isFeatured, updatedAtMillis = :updatedAtMillis " +
            "WHERE id = :id"
    )
    suspend fun updateFeatured(
        id: Long,
        isFeatured: Boolean,
        updatedAtMillis: Long
    )

    @Query("DELETE FROM school_posts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SchoolHighlightDao {
    /**
     * Returns only currently active (non-expired) published Highlights.
     * Expired entries remain untouched in the database and YouTube is never modified.
     */
    @Query(
        "SELECT * FROM school_highlights " +
            "WHERE status = 'PUBLISHED' AND expiresAtMillis > :nowMillis " +
            "ORDER BY publishedAtMillis DESC, id DESC"
    )
    fun observeActiveHighlights(nowMillis: Long): Flow<List<SchoolHighlightEntity>>

    @Query("SELECT * FROM school_highlights ORDER BY publishedAtMillis DESC, id DESC")
    fun observeAllHighlights(): Flow<List<SchoolHighlightEntity>>

    @Query("SELECT * FROM school_highlights ORDER BY publishedAtMillis DESC, id DESC")
    suspend fun getAllHighlightsOnce(): List<SchoolHighlightEntity>

    @Query("SELECT * FROM school_highlights WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SchoolHighlightEntity?

    @Query("SELECT * FROM school_highlights WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): SchoolHighlightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(highlight: SchoolHighlightEntity): Long

    @Query("UPDATE school_highlights SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM school_highlights WHERE id = :id")
    suspend fun deleteById(id: Long)
}

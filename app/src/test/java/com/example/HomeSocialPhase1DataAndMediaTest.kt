package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.SchoolHighlightEntity
import com.example.data.local.entity.SchoolPostCategory
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostStatus
import com.example.data.local.entity.SchoolPostType
import com.example.data.media.MediaStudioManager
import com.example.data.media.YouTubeMediaHelper
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ManagementRepository
import com.example.data.repository.SchoolRepository
import com.example.data.repository.SocialFeedRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeSocialPhase1DataAndMediaTest {

    private lateinit var context: Context
    private lateinit var database: ChevalierDatabase
    private lateinit var mediaManager: MediaStudioManager
    private lateinit var socialRepo: SocialFeedRepository
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var backupManager: BackupRestoreManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "chevalier_media").deleteRecursively()
        database = Room.inMemoryDatabaseBuilder(context, ChevalierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mediaManager = MediaStudioManager(context)
        socialRepo = SocialFeedRepository.fromDatabase(database, mediaManager)
        prefsRepo = UserPreferencesRepository(context)
        backupManager = BackupRestoreManager(context, database, prefsRepo)
    }

    @After
    fun tearDown() {
        database.close()
        File(context.filesDir, "chevalier_media").deleteRecursively()
    }

    private fun createTempImageFile(name: String, width: Int, height: Int): File {
        val file = File(context.cacheDir, name)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawColor(Color.rgb(141, 32, 41))
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        bmp.recycle()
        return file
    }

    // ==================== 1. YOUTUBE URL PARSER & HELPERS ====================

    @Test
    fun `youtube parser supports watch, youtu_be, shorts, embed and generates thumbnail, embed, and fallback urls`() {
        val videoId = "dQw4w9WgXcQ"
        val samples = listOf(
            "https://www.youtube.com/watch?v=$videoId&t=42s&feature=share" to false,
            "https://m.youtube.com/watch?v=$videoId" to false,
            "https://youtu.be/$videoId?si=abc123" to false,
            "https://www.youtube.com/shorts/$videoId?feature=share" to true,
            "https://youtube.com/embed/$videoId?rel=0" to false,
            "https://www.youtube-nocookie.com/embed/$videoId" to false
        )

        for ((url, expectShort) in samples) {
            val parsed = YouTubeMediaHelper.parse(url)
            assertNotNull("Expected valid parse for $url", parsed)
            assertEquals(videoId, parsed!!.videoId)
            assertEquals(expectShort, parsed.isShort)
            assertEquals("https://img.youtube.com/vi/$videoId/hqdefault.jpg", parsed.thumbnailUrl)
            assertEquals("https://img.youtube.com/vi/$videoId/maxresdefault.jpg", parsed.maxResThumbnailUrl)
            assertTrue(parsed.clickToPlayEmbedUrl.startsWith("https://www.youtube-nocookie.com/embed/$videoId"))
            assertTrue(parsed.clickToPlayEmbedUrl.contains("autoplay=1"))
            assertFalse(parsed.staticEmbedUrl.contains("autoplay=1"))
            assertTrue(parsed.externalFallbackUrl.contains(videoId))
            assertTrue(MediaStudioManager.isValidYouTubeUrl(url))
        }

        val embedHtml = YouTubeMediaHelper.buildClickToPlayEmbedHtml(videoId)
        assertNotNull(embedHtml)
        assertTrue(embedHtml!!.contains("https://www.youtube-nocookie.com/embed/$videoId"))
        assertTrue(embedHtml.contains("strict-origin-when-cross-origin"))
    }

    @Test
    fun `youtube parser rejects invalid urls, fake hosts, malformed video ids, and non-shorts for highlights`() {
        val invalidSamples = listOf(
            null,
            "",
            "   ",
            "not-a-url",
            "ftp://youtube.com/watch?v=dQw4w9WgXcQ",
            "https://evil-youtube.com/watch?v=dQw4w9WgXcQ",
            "https://www.youtube.com/watch?v=short",
            "https://www.youtube.com/channel/UC1234567890",
            "https://youtu.be/",
            "https://vimeo.com/123456789"
        )
        for (url in invalidSamples) {
            assertNull("Expected null parse for $url", YouTubeMediaHelper.parse(url))
            assertFalse(YouTubeMediaHelper.isValidYouTubeUrl(url))
            assertFalse(MediaStudioManager.isValidYouTubeUrl(url ?: ""))
        }

        assertFalse(YouTubeMediaHelper.isValidYouTubeShortsUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(YouTubeMediaHelper.isValidYouTubeShortsUrl("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertNull(YouTubeMediaHelper.buildClickToPlayEmbedHtml("invalid"))
    }

    // ==================== 2. ROOM MIGRATION v4 -> v5 ====================

    @Test
    fun `room migration 4 to 5 preserves all existing v4 records and enables school_posts and school_highlights`() = runTest {
        val dbName = "migration_test_v4_to_v5.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        dbFile.delete()

        // Create authentic Version 4 SQLite database matching Room v4 schema
        val v4Db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        v4Db.version = 4
        v4Db.execSQL(
            """
            CREATE TABLE school_profile (
                id INTEGER NOT NULL PRIMARY KEY,
                schoolName TEXT NOT NULL,
                shortName TEXT NOT NULL,
                motto TEXT NOT NULL,
                addressLine TEXT NOT NULL,
                emblemMark TEXT NOT NULL,
                poweredBy TEXT NOT NULL,
                welcomeIntroduction TEXT,
                mediumAndGradesNote TEXT,
                mapCoordinatesOrQuery TEXT,
                phone TEXT,
                email TEXT,
                website TEXT,
                officeHours TEXT,
                principalName TEXT,
                history TEXT,
                vision TEXT,
                mission TEXT,
                affiliationNumber TEXT,
                updatedAtMillis INTEGER NOT NULL,
                profileImageUri TEXT,
                aboutImageUri TEXT
            )
            """.trimIndent()
        )
        v4Db.execSQL("CREATE TABLE campus_hero_photos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, imageUri TEXT NOT NULL, caption TEXT, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL, addedAtMillis INTEGER NOT NULL)")
        v4Db.execSQL("CREATE TABLE academic_info (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, category TEXT NOT NULL, description TEXT NOT NULL, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL, updatedAtMillis INTEGER NOT NULL)")
        v4Db.execSQL("CREATE TABLE higher_secondary_courses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, streamTitle TEXT NOT NULL, groupCode TEXT NOT NULL, subjectsSummary TEXT NOT NULL, mediumOfInstruction TEXT NOT NULL, classLevelLabel TEXT, eligibilityNote TEXT, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v4Db.execSQL("CREATE TABLE facilities (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, description TEXT NOT NULL, capacityOrSpec TEXT, imageUri TEXT, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v4Db.execSQL("CREATE TABLE transport_routes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, routeNumber TEXT NOT NULL, routeName TEXT NOT NULL, stopsSummary TEXT NOT NULL, morningTiming TEXT, afternoonTiming TEXT, coordinatorContact TEXT, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v4Db.execSQL("CREATE TABLE news_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, summary TEXT NOT NULL, fullBody TEXT NOT NULL, publishedDateText TEXT NOT NULL, publishedAtMillis INTEGER NOT NULL, isFeatured INTEGER NOT NULL, imageUri TEXT, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v4Db.execSQL("CREATE TABLE announcements (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, message TEXT NOT NULL, priorityLabel TEXT, issuedDateText TEXT NOT NULL, issuedAtMillis INTEGER NOT NULL, imageUri TEXT, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v4Db.execSQL("CREATE TABLE gallery_albums (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, category TEXT NOT NULL, description TEXT, eventDateText TEXT, coverMediaUri TEXT, displayOrder INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v4Db.execSQL("CREATE TABLE gallery_media (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, albumId INTEGER NOT NULL, title TEXT, caption TEXT, mediaUri TEXT NOT NULL, mediaType TEXT NOT NULL, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL, addedAtMillis INTEGER NOT NULL, FOREIGN KEY(albumId) REFERENCES gallery_albums(id) ON DELETE CASCADE)")
        v4Db.execSQL("CREATE INDEX index_gallery_media_albumId ON gallery_media(albumId)")
        v4Db.execSQL("CREATE TABLE social_media_links (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, platformName TEXT NOT NULL, handleOrLabel TEXT NOT NULL, url TEXT NOT NULL, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v4Db.execSQL("CREATE TABLE management_accounts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, username TEXT NOT NULL, displayName TEXT NOT NULL, role TEXT NOT NULL, pinSaltBase64 TEXT NOT NULL, pinVerifierBase64 TEXT NOT NULL, recoverySaltBase64 TEXT, recoveryVerifierBase64 TEXT, isActive INTEGER NOT NULL, failedAttempts INTEGER NOT NULL, lockoutUntilMillis INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL, updatedAtMillis INTEGER NOT NULL)")
        v4Db.execSQL("CREATE UNIQUE INDEX index_management_accounts_username ON management_accounts (username)")
        v4Db.execSQL("CREATE TABLE staff_permissions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, accountId INTEGER NOT NULL, sectionKey TEXT NOT NULL, canView INTEGER NOT NULL, canCreate INTEGER NOT NULL, canEdit INTEGER NOT NULL, canDelete INTEGER NOT NULL, canPublish INTEGER NOT NULL, FOREIGN KEY(accountId) REFERENCES management_accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        v4Db.execSQL("CREATE INDEX index_staff_permissions_accountId ON staff_permissions (accountId)")
        v4Db.execSQL("CREATE UNIQUE INDEX index_staff_permissions_accountId_sectionKey ON staff_permissions (accountId, sectionKey)")
        v4Db.execSQL("CREATE TABLE content_approvals (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sectionKey TEXT NOT NULL, targetRecordId INTEGER NOT NULL, recordTitle TEXT NOT NULL, previousSnapshotJson TEXT, proposedPayloadJson TEXT NOT NULL, changeSummary TEXT NOT NULL, submittedByAccountId INTEGER NOT NULL, submittedByName TEXT NOT NULL, submittedAtMillis INTEGER NOT NULL, status TEXT NOT NULL, reviewedByAccountId INTEGER, reviewedByName TEXT, reviewedAtMillis INTEGER, rejectionReason TEXT)")
        v4Db.execSQL("CREATE INDEX index_content_approvals_sectionKey ON content_approvals (sectionKey)")
        v4Db.execSQL("CREATE INDEX index_content_approvals_status ON content_approvals (status)")
        v4Db.execSQL("CREATE INDEX index_content_approvals_submittedByAccountId ON content_approvals (submittedByAccountId)")
        v4Db.execSQL("CREATE TABLE version_history (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sectionKey TEXT NOT NULL, recordId INTEGER NOT NULL, versionNumber INTEGER NOT NULL, recordTitle TEXT NOT NULL, previousContentJson TEXT, contentSnapshotJson TEXT NOT NULL, changedByAccountId INTEGER NOT NULL, changedByName TEXT NOT NULL, changedAtMillis INTEGER NOT NULL, changeSummary TEXT NOT NULL, approvalStatus TEXT NOT NULL)")
        v4Db.execSQL("CREATE INDEX index_version_history_sectionKey_recordId ON version_history (sectionKey, recordId)")
        v4Db.execSQL("CREATE TABLE media_studio_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, mediaKind TEXT NOT NULL, title TEXT NOT NULL, caption TEXT, categoryOrAlbum TEXT NOT NULL, originalUri TEXT NOT NULL, optimizedUri TEXT, mimeType TEXT NOT NULL, widthPx INTEGER NOT NULL, heightPx INTEGER NOT NULL, fileSizeBytes INTEGER NOT NULL, optimizedSizeBytes INTEGER NOT NULL, assignedSectionsCsv TEXT NOT NULL, isFeatured INTEGER NOT NULL, displayOrder INTEGER NOT NULL, approvalStatus TEXT NOT NULL, createdByName TEXT NOT NULL, createdAtMillis INTEGER NOT NULL)")
        v4Db.execSQL("CREATE INDEX index_media_studio_items_mediaKind ON media_studio_items (mediaKind)")
        v4Db.execSQL("CREATE INDEX index_media_studio_items_approvalStatus ON media_studio_items (approvalStatus)")
        v4Db.execSQL("CREATE TABLE activity_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, actionType TEXT NOT NULL, actorAccountId INTEGER, actorName TEXT NOT NULL, actorRole TEXT NOT NULL, sectionKey TEXT NOT NULL, recordRef TEXT NOT NULL, details TEXT NOT NULL, timestampMillis INTEGER NOT NULL)")
        v4Db.execSQL("CREATE INDEX index_activity_logs_timestampMillis ON activity_logs (timestampMillis)")
        v4Db.execSQL("CREATE INDEX index_activity_logs_actorAccountId ON activity_logs (actorAccountId)")
        v4Db.execSQL("CREATE INDEX index_activity_logs_sectionKey ON activity_logs (sectionKey)")
        v4Db.execSQL("CREATE TABLE local_notifications (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, eventType TEXT NOT NULL, title TEXT NOT NULL, message TEXT NOT NULL, sectionKey TEXT NOT NULL, isRead INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL)")
        v4Db.execSQL("CREATE INDEX index_local_notifications_isRead ON local_notifications (isRead)")
        v4Db.execSQL("CREATE INDEX index_local_notifications_createdAtMillis ON local_notifications (createdAtMillis)")

        // Seed v4 records
        v4Db.execSQL(
            """
            INSERT INTO school_profile (id, schoolName, shortName, motto, addressLine, emblemMark, poweredBy, updatedAtMillis, profileImageUri, aboutImageUri)
            VALUES (1, 'CHEVALIER ACADEMY MATRIC HIGHER SECONDARY SCHOOL', 'Chevalier Academy', 'Learn to Live & Love', 'Siluvathur Road, Dindigul - 624 306', 'M S C', 'POWERED BY JK LABS', 9000, 'file:///p.jpg', 'file:///a.jpg')
            """.trimIndent()
        )
        v4Db.execSQL(
            """
            INSERT INTO news_items (title, summary, fullBody, publishedDateText, publishedAtMillis, isFeatured, imageUri, displayOrder, isVerified, isPublished, isApproved, approvalStatus)
            VALUES ('Annual Science Expo', 'Projects showcase', 'Full details', '15 Nov', 15000, 1, 'file:///expo.jpg', 2, 1, 1, 1, 'APPROVED')
            """.trimIndent()
        )
        v4Db.close()

        // Migrate v4 -> v6 non-destructively
        val migratedDb = Room.databaseBuilder(context, ChevalierDatabase::class.java, dbName)
            .addMigrations(
                ChevalierDatabase.MIGRATION_4_5,
                ChevalierDatabase.MIGRATION_5_6
            )
            .allowMainThreadQueries()
            .build()

        // Existing v4 records remain intact
        val profile = migratedDb.schoolProfileDao().getProfileOnce()!!
        assertEquals("Learn to Live & Love", profile.motto)
        assertEquals("file:///p.jpg", profile.profileImageUri)
        assertEquals("file:///a.jpg", profile.aboutImageUri)
        val existingNews = migratedDb.newsDao().observePublic().first().single()
        assertEquals("Annual Science Expo", existingNews.title)
        assertEquals(2, existingNews.displayOrder)

        // New v5 tables work immediately
        val migratedRepo = SocialFeedRepository.fromDatabase(migratedDb, mediaManager)
        val postId = migratedRepo.saveSchoolPost(
            postType = SchoolPostType.YOUTUBE_DIRECT,
            category = SchoolPostCategory.EVENTS,
            title = "Annual Day Highlights",
            description = "Campus celebration video",
            youtubeUrl = "https://youtu.be/dQw4w9WgXcQ",
            status = SchoolPostStatus.PUBLISHED,
            isFeatured = true,
            nowMillis = 1_000_000L
        ).getOrThrow()
        assertTrue(postId > 0L)
        assertEquals(1, migratedRepo.publishedPosts.first().size)

        val highlightId = migratedRepo.saveHighlight(
            title = "Morning Assembly",
            youtubeShortsUrl = "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            nowMillis = 1_000_000L
        ).getOrThrow()
        assertTrue(highlightId > 0L)
        assertEquals(1, migratedRepo.observeActiveHighlights(1_000_000L + 1_000L).first().size)

        migratedDb.close()
        dbFile.delete()
    }

    // ==================== 3. SCHOOL POSTS, PHOTO LIMIT & STATUS FLOWS ====================

    @Test
    fun `school posts support all 3 post types, enforce max 10 photos, status transitions, featured ordering, and reuse MediaStudioManager`() = runTest {
        // Reuse MediaStudioManager via SocialFeedRepository
        val rawImage = createTempImageFile("post_photo_large.jpg", 2200, 1400)
        val processed = socialRepo.importAndOptimizePostImage(Uri.fromFile(rawImage)).getOrThrow()
        assertTrue(mediaManager.isManagedMediaFileReadable(processed.originalUri))
        assertTrue(mediaManager.isManagedMediaFileReadable(processed.optimizedUri))

        // Photo Post validation: 0 photos rejected, 11 photos rejected, 10 photos accepted
        assertTrue(
            socialRepo.saveSchoolPost(
                postType = SchoolPostType.PHOTO,
                title = "Empty Album",
                photoUris = emptyList()
            ).isFailure
        )
        assertTrue(
            socialRepo.saveSchoolPost(
                postType = SchoolPostType.PHOTO,
                title = "Too Many Photos",
                photoUris = (1..11).map { "file:///photo_$it.jpg" }
            ).isFailure
        )
        val tenPhotos = (1..SchoolPostEntity.MAX_PHOTO_COUNT).map { "file:///photo_$it.jpg" }
        val photoPostId = socialRepo.saveSchoolPost(
            postType = SchoolPostType.PHOTO,
            category = SchoolPostCategory.SPORTS,
            title = "Sports Day Gallery",
            description = "Track and field moments",
            photoUris = tenPhotos,
            status = SchoolPostStatus.PUBLISHED,
            isFeatured = false,
            nowMillis = 20_000L
        ).getOrThrow()

        // YouTube Direct Post
        val ytDirectId = socialRepo.saveSchoolPost(
            postType = SchoolPostType.YOUTUBE_DIRECT,
            category = SchoolPostCategory.EVENTS,
            title = "Independence Day Speech",
            youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            status = SchoolPostStatus.PUBLISHED,
            isFeatured = false,
            nowMillis = 10_000L
        ).getOrThrow()

        // Admin Custom Video Post (unpublished initially, then published & featured)
        val customVideoId = socialRepo.saveSchoolPost(
            postType = SchoolPostType.ADMIN_CUSTOM_VIDEO,
            category = SchoolPostCategory.ACADEMICS,
            title = "Lab Tour",
            youtubeUrl = "https://youtu.be/M7lc1UVf-VE",
            customThumbnailUri = processed.optimizedUri,
            status = SchoolPostStatus.UNPUBLISHED,
            isFeatured = false,
            nowMillis = 5_000L
        ).getOrThrow()

        // Only PUBLISHED posts appear in public feed
        assertEquals(listOf(photoPostId, ytDirectId), socialRepo.publishedPosts.first().map { it.id })
        assertEquals(10, socialRepo.publishedPosts.first().first().photoUris().size)

        // Publish & pin older custom video -> moves to top because isFeatured DESC
        socialRepo.publishPost(customVideoId, nowMillis = 8_000L).getOrThrow()
        socialRepo.setPostFeatured(customVideoId, true, nowMillis = 9_000L).getOrThrow()
        assertEquals(
            listOf(customVideoId, photoPostId, ytDirectId),
            socialRepo.publishedPosts.first().map { it.id }
        )

        // Unpublish & archive remove from published feed while retaining record in admin view
        socialRepo.unpublishPost(ytDirectId, nowMillis = 25_000L).getOrThrow()
        socialRepo.archivePost(photoPostId, nowMillis = 26_000L).getOrThrow()
        assertEquals(listOf(customVideoId), socialRepo.publishedPosts.first().map { it.id })
        assertEquals(3, socialRepo.allPostsForAdmin.first().size)
    }

    // ==================== 4. 24-HOUR HIGHLIGHTS EXPIRY ====================

    @Test
    fun `highlights require YouTube Shorts and hide after 24h expiry without deleting stored entry`() = runTest {
        // Non-Shorts YouTube link must be rejected
        assertTrue(
            socialRepo.saveHighlight(
                title = "Regular Video",
                youtubeShortsUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                nowMillis = 1_000_000L
            ).isFailure
        )

        val publishedAt = 1_000_000L
        val id = socialRepo.saveHighlight(
            title = "Science Fair Reel",
            youtubeShortsUrl = "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            nowMillis = publishedAt
        ).getOrThrow()

        // Active 1 millisecond before 24 hours
        val beforeExpiry = publishedAt + SchoolHighlightEntity.HIGHLIGHT_TTL_MILLIS - 1L
        val activeBefore = socialRepo.observeActiveHighlights(beforeExpiry).first()
        assertEquals(1, activeBefore.size)
        assertEquals(id, activeBefore.single().id)
        assertEquals("https://www.youtube.com/shorts/dQw4w9WgXcQ", activeBefore.single().youtubeShortsUrl)

        // Hidden at and after 24 hours
        val afterExpiry = publishedAt + SchoolHighlightEntity.HIGHLIGHT_TTL_MILLIS
        val activeAfter = socialRepo.observeActiveHighlights(afterExpiry).first()
        assertTrue(activeAfter.isEmpty())

        // Underlying Highlight record is NOT deleted (expiry hides only active display, never touches YouTube)
        val allAdminHighlights = socialRepo.allHighlightsForAdmin.first()
        assertEquals(1, allAdminHighlights.size)
        assertEquals(id, allAdminHighlights.single().id)
        assertFalse(allAdminHighlights.single().isActiveAt(afterExpiry))
    }

    // ==================== 5. V5 BACKUP & RESTORE ROUNDTRIP ====================

    @Test
    fun `v5 encrypted backup preserves school posts, highlights, and remaps post media uris on restore`() = runTest {
        SchoolRepository.fromDatabase(database).ensureVerifiedProfileSeeded()
        val mgmtRepo = ManagementRepository(database, mediaManager)
        val master = mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow().account

        val imgFile = createTempImageFile("backup_post_img.jpg", 900, 600)
        val imported = socialRepo.importAndOptimizePostImage(Uri.fromFile(imgFile)).getOrThrow()

        socialRepo.saveSchoolPost(
            postType = SchoolPostType.PHOTO,
            category = SchoolPostCategory.EVENTS,
            title = "Campus Photo Story",
            description = "Backed up photo post",
            photoUris = listOf(imported.optimizedUri),
            status = SchoolPostStatus.PUBLISHED,
            isFeatured = true,
            nowMillis = 50_000L
        ).getOrThrow()
        socialRepo.saveHighlight(
            title = "Assembly Short",
            youtubeShortsUrl = "https://www.youtube.com/shorts/M7lc1UVf-VE",
            nowMillis = 60_000L
        ).getOrThrow()

        val encrypted = backupManager.createEncryptedBackupBytes("Phase1Backup!").getOrThrow()
        val preview = backupManager.inspectEncryptedBackup(encrypted, "Phase1Backup!").getOrThrow()
        assertEquals(BackupRestoreManager.CURRENT_SCHEMA_VERSION, preview.schemaVersion)
        assertTrue(preview.contentRecordCount >= 2)
        assertTrue(preview.mediaFileCount >= 1)

        database.clearAllTables()
        File(context.filesDir, "chevalier_media").deleteRecursively()

        val restored = backupManager.restoreEncryptedBackupBytes(
            bytes = encrypted,
            password = "Phase1Backup!",
            actorName = master.account.displayName
        ).getOrThrow()
        assertEquals(BackupRestoreManager.CURRENT_SCHEMA_VERSION, restored.schemaVersion)
        assertTrue(restored.contentRecordCount >= 2)

        val restoredPost = socialRepo.publishedPosts.first().single()
        assertEquals("Campus Photo Story", restoredPost.title)
        assertTrue(restoredPost.isFeatured)
        assertEquals(listOf(imported.optimizedUri), restoredPost.photoUris())
        assertTrue(mediaManager.isManagedMediaFileReadable(restoredPost.photoUris().single()))

        val restoredHighlight = socialRepo.allHighlightsForAdmin.first().single()
        assertEquals("Assembly Short", restoredHighlight.title)
        assertEquals("https://www.youtube.com/shorts/M7lc1UVf-VE", restoredHighlight.youtubeShortsUrl)
    }
}

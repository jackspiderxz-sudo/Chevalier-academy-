package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.SchoolTenantContext
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.ApprovalStatus
import com.example.data.local.entity.ContentApprovalEntity
import com.example.data.local.entity.ContentWorkflowStatus
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.MediaStudioItemEntity
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.SchoolHighlightEntity
import com.example.data.local.entity.SchoolPostCategory
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostStatus
import com.example.data.local.entity.SchoolPostType
import com.example.data.local.entity.SyncMetadataDefaults
import com.example.data.local.entity.SyncState
import com.example.data.local.entity.VersionHistoryEntity
import com.example.data.media.MediaStudioManager
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ContentSnapshotSerializer
import com.example.data.repository.ManagementContentCoordinator
import com.example.data.repository.ManagementRepository
import com.example.data.repository.SchoolRepository
import com.example.data.repository.SocialFeedRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
class Phase42ARoomSyncSchemaTest {

    private lateinit var context: Context
    private lateinit var database: ChevalierDatabase
    private lateinit var mediaManager: MediaStudioManager
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var backupManager: BackupRestoreManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ChevalierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mediaManager = MediaStudioManager(context)
        prefsRepo = UserPreferencesRepository(context)
        backupManager = BackupRestoreManager(context, database, prefsRepo)
    }

    @After
    fun tearDown() {
        database.close()
        File(context.filesDir, "chevalier_media").deleteRecursively()
        File(context.filesDir, "chevalier_media_staging").deleteRecursively()
        File(context.filesDir, "chevalier_media_rollback").deleteRecursively()
    }

    @Test
    fun `room migration 5 to 6 preserves all 20 tables, assigns deterministic remoteIds, excludes device-local tables, and never regenerates remoteIds`() = runTest {
        val dbNameA = "phase42a_v5_to_v6_a.db"
        val dbNameB = "phase42a_v5_to_v6_b.db"
        val dbFileA = createAuthenticV5Database(dbNameA)
        val dbFileB = createAuthenticV5Database(dbNameB)

        val migratedA = Room.databaseBuilder(context, ChevalierDatabase::class.java, dbNameA)
            .addMigrations(ChevalierDatabase.MIGRATION_5_6)
            .allowMainThreadQueries()
            .build()
        val migratedB = Room.databaseBuilder(context, ChevalierDatabase::class.java, dbNameB)
            .addMigrations(ChevalierDatabase.MIGRATION_5_6)
            .allowMainThreadQueries()
            .build()

        try {
            // 1. Verify existing data across all 20 tables survived intact
            val profile = migratedA.schoolProfileDao().getProfileOnce()!!
            assertEquals("Learn to Live & Love", profile.motto)
            assertEquals(SyncMetadataDefaults.deterministicRemoteId("school_profile", 1L), profile.remoteId)
            assertEquals(SchoolTenantContext.DEFAULT_SCHOOL_ID, profile.tenantId)
            assertEquals(SyncState.LOCAL_ONLY, profile.syncState)
            assertNull(profile.serverUpdatedAtMillis)

            val hero = migratedA.campusHeroPhotoDao().observeAllForManagement().first().single()
            assertEquals("Campus Gate", hero.caption)
            assertEquals(SyncMetadataDefaults.deterministicRemoteId("campus_hero_photos", hero.id), hero.remoteId)
            assertNull(hero.deletedAtMillis)

            val academic = migratedA.academicInfoDao().observeAll().first().single()
            assertEquals("Matriculation Curriculum", academic.title)
            assertEquals(SyncMetadataDefaults.deterministicRemoteId("academic_info", academic.id), academic.remoteId)

            val course = migratedA.higherSecondaryCourseDao().observeAll().first().single()
            assertEquals("Bio-Maths", course.streamTitle)

            val facility = migratedA.facilityDao().observeAll().first().single()
            assertEquals("Physics Lab", facility.name)

            val route = migratedA.transportDao().observeAll().first().single()
            assertEquals("R-01", route.routeNumber)

            val news = migratedA.newsDao().observeAll().first().single()
            assertEquals("Annual Sports Meet", news.title)

            val announcement = migratedA.announcementDao().observeAll().first().single()
            assertEquals("PTA Meeting", announcement.title)

            val albumWithMedia = migratedA.galleryDao().observeAlbumsWithMedia().first().single()
            assertEquals("Founders Day", albumWithMedia.album.title)
            assertEquals(1, albumWithMedia.mediaItems.size)
            assertEquals(albumWithMedia.album.id, albumWithMedia.mediaItems.single().albumId)

            val social = migratedA.socialMediaDao().observeAll().first().single()
            assertEquals("YouTube", social.platformName)

            val post = migratedA.schoolPostDao().getAllPostsOnce().single()
            assertEquals("Science Expo Video", post.title)
            val expectedPostRemoteId = SyncMetadataDefaults.deterministicRemoteId("school_posts", post.id)
            assertEquals(expectedPostRemoteId, post.remoteId)
            assertEquals(post, migratedA.schoolPostDao().getByRemoteId(expectedPostRemoteId))

            val highlight = migratedA.schoolHighlightDao().getAllHighlightsOnce().single()
            assertEquals("Morning Drill", highlight.title)
            assertEquals(SyncMetadataDefaults.deterministicRemoteId("school_highlights", highlight.id), highlight.remoteId)

            val approval = migratedA.contentApprovalDao().getAllOnce().single()
            assertEquals("Pending News", approval.recordTitle)
            assertEquals(SyncMetadataDefaults.deterministicRemoteId("content_approvals", approval.id), approval.remoteId)

            val version = migratedA.versionHistoryDao().getAllOnce().single()
            assertEquals("Annual Sports Meet", version.recordTitle)
            assertEquals(SyncMetadataDefaults.deterministicRemoteId("version_history", version.id), version.remoteId)

            val mediaItem = migratedA.mediaStudioDao().getAllOnce().single()
            assertEquals("Banner Photo", mediaItem.title)
            assertEquals(SyncMetadataDefaults.deterministicRemoteId("media_studio_items", mediaItem.id), mediaItem.remoteId)

            val log = migratedA.activityLogDao().getAllOnce().single()
            assertEquals("PUBLISH", log.actionType)
            assertEquals(SyncMetadataDefaults.deterministicRemoteId("activity_logs", log.id), log.remoteId)

            val accountsWithPerms = migratedA.managementAccountDao().getAllAccountsWithPermissionsOnce()
            assertEquals(1, accountsWithPerms.size)
            assertEquals("admin", accountsWithPerms.single().account.username)
            assertEquals(1, accountsWithPerms.single().permissions.size)

            val notification = migratedA.localNotificationDao().getAllOnce().single()
            assertEquals("Local Alert", notification.title)

            // 2. Verify determinism across two independently migrated v5 databases
            assertEquals(
                migratedA.schoolPostDao().getAllPostsOnce().single().remoteId,
                migratedB.schoolPostDao().getAllPostsOnce().single().remoteId
            )
            assertEquals(
                migratedA.newsDao().observeAll().first().single().remoteId,
                migratedB.newsDao().observeAll().first().single().remoteId
            )
            // Valid standard RFC-4122 UUID format
            assertNotNull(UUID.fromString(post.remoteId))

            // 3. Verify exact column classification (device-local excluded; append-only/singleton omit deletedAtMillis)
            val sqliteDb = migratedA.openHelper.readableDatabase
            val deviceLocalTables = listOf("management_accounts", "staff_permissions", "local_notifications")
            val syncColumns = setOf("remoteId", "tenantId", "syncState", "serverUpdatedAtMillis", "deletedAtMillis")
            deviceLocalTables.forEach { table ->
                val cols = readTableColumnNames(sqliteDb, table)
                assertTrue("Device-local table $table must not contain sync columns: $cols", cols.intersect(syncColumns).isEmpty())
            }
            listOf("school_profile", "version_history", "activity_logs").forEach { table ->
                val cols = readTableColumnNames(sqliteDb, table)
                assertTrue("Table $table must contain remoteId/tenantId/syncState/serverUpdatedAtMillis", cols.containsAll(setOf("remoteId", "tenantId", "syncState", "serverUpdatedAtMillis")))
                assertFalse("Table $table should omit deletedAtMillis", "deletedAtMillis" in cols)
            }
            listOf(
                "campus_hero_photos", "academic_info", "higher_secondary_courses", "facilities",
                "transport_routes", "news_items", "announcements", "gallery_albums",
                "gallery_media", "social_media_links", "school_posts", "school_highlights",
                "content_approvals", "media_studio_items"
            ).forEach { table ->
                val cols = readTableColumnNames(sqliteDb, table)
                assertTrue("Table $table must contain all 5 sync columns", cols.containsAll(syncColumns))
            }

            // 4. Verify updates NEVER regenerate remoteId (SocialFeedRepository + ManagementContentCoordinator)
            val socialRepo = SocialFeedRepository.fromDatabase(migratedA, mediaManager)
            migratedA.schoolPostDao().upsert(post.copy(syncState = SyncState.SYNCED, serverUpdatedAtMillis = 99_999L))
            socialRepo.saveSchoolPost(
                id = post.id,
                postType = post.postType,
                category = post.category,
                title = "Science Expo Video Updated",
                description = "Updated description",
                youtubeUrl = post.youtubeUrl,
                status = post.status,
                isFeatured = true
            ).getOrThrow()
            val updatedPost = migratedA.schoolPostDao().getById(post.id)!!
            assertEquals(expectedPostRemoteId, updatedPost.remoteId)
            assertEquals(SyncState.PENDING_UPLOAD, updatedPost.syncState)
            assertEquals(99_999L, updatedPost.serverUpdatedAtMillis)

            val coordinator = ManagementContentCoordinator(migratedA)
            val originalNewsRemoteId = news.remoteId
            migratedA.newsDao().upsert(news.copy(syncState = SyncState.SYNCED))
            // Simulate UI building a fresh entity (which has a newly generated default remoteId) and saving to existing ID
            val uiEditJson = ContentSnapshotSerializer.newsToJson(
                NewsItemEntity(
                    id = news.id,
                    title = "Annual Sports Meet (Edited)",
                    summary = news.summary,
                    fullBody = news.fullBody,
                    publishedDateText = news.publishedDateText
                )
            )
            coordinator.applySnapshotJsonToEntityTable(
                section = ManagementSection.NEWS,
                snapshotJson = uiEditJson,
                targetRecordId = news.id,
                publishImmediately = true
            )
            val updatedNews = migratedA.newsDao().observeAll().first().single()
            assertEquals(originalNewsRemoteId, updatedNews.remoteId)
            assertEquals(SyncState.PENDING_UPLOAD, updatedNews.syncState)
            assertEquals("Annual Sports Meet (Edited)", updatedNews.title)

            // 5. Verify FK cascade still functions properly after migration
            migratedA.galleryDao().deleteAlbumById(albumWithMedia.album.id)
            assertTrue(migratedA.galleryDao().observeAllMedia().first().isEmpty())
        } finally {
            migratedA.close()
            migratedB.close()
        }

        // 6. Reopen migrated DB -> verify remoteId is never regenerated across restarts
        val reopenedA = Room.databaseBuilder(context, ChevalierDatabase::class.java, dbNameA)
            .addMigrations(ChevalierDatabase.MIGRATION_5_6)
            .allowMainThreadQueries()
            .build()
        try {
            val postAfterReopen = reopenedA.schoolPostDao().getAllPostsOnce().single()
            assertEquals(
                SyncMetadataDefaults.deterministicRemoteId("school_posts", postAfterReopen.id),
                postAfterReopen.remoteId
            )
        } finally {
            reopenedA.close()
            dbFileA.delete()
            dbFileB.delete()
        }
    }

    @Test
    fun `v6 backup preserves sync metadata across all syncable tables and restores legacy v5 backups deterministically`() = runTest {
        SchoolRepository.fromDatabase(database).ensureVerifiedProfileSeeded()
        val mgmtRepo = ManagementRepository(database, mediaManager)
        val master = mgmtRepo.setupMasterAdmin("Principal", "admin", "246810", "246810").getOrThrow().account

        val customPostRemoteId = "11111111-2222-4333-8444-555555555555"
        val customHighlightRemoteId = "22222222-3333-4444-8555-666666666666"
        val customNewsRemoteId = "33333333-4444-4555-8666-777777777777"
        val customApprovalRemoteId = "44444444-5555-4666-8777-888888888888"
        val customVersionRemoteId = "55555555-6666-4777-8888-999999999999"
        val customMediaRemoteId = "66666666-7777-4888-8999-aaaaaaaaaaaa"
        val customLogRemoteId = "77777777-8888-4999-8aaa-bbbbbbbbbbbb"

        database.newsDao().upsert(
            NewsItemEntity(
                title = "Synced Campus News",
                summary = "Summary",
                fullBody = "Body",
                publishedDateText = "20 Nov",
                isVerified = true,
                isPublished = true,
                isApproved = true,
                approvalStatus = ApprovalStatus.APPROVED,
                remoteId = customNewsRemoteId,
                tenantId = SchoolTenantContext.DEFAULT_SCHOOL_ID,
                syncState = SyncState.SYNCED,
                serverUpdatedAtMillis = 123_456L,
                deletedAtMillis = null
            )
        )
        database.schoolPostDao().upsert(
            SchoolPostEntity(
                postType = SchoolPostType.YOUTUBE_DIRECT,
                category = SchoolPostCategory.EVENTS,
                title = "Synced Event Post",
                youtubeUrl = "https://youtu.be/dQw4w9WgXcQ",
                status = SchoolPostStatus.PUBLISHED,
                remoteId = customPostRemoteId,
                syncState = SyncState.PENDING_UPLOAD,
                serverUpdatedAtMillis = 234_567L,
                deletedAtMillis = 345_678L
            )
        )
        database.schoolHighlightDao().upsert(
            SchoolHighlightEntity(
                title = "Synced Highlight",
                youtubeShortsUrl = "https://www.youtube.com/shorts/dQw4w9WgXcQ",
                status = SchoolPostStatus.PUBLISHED,
                remoteId = customHighlightRemoteId,
                syncState = SyncState.SYNCED,
                serverUpdatedAtMillis = 456_789L
            )
        )
        database.contentApprovalDao().upsert(
            ContentApprovalEntity(
                sectionKey = ManagementSection.NEWS.key,
                targetRecordId = 1L,
                recordTitle = "Approval Item",
                proposedPayloadJson = "{}",
                changeSummary = "Edit",
                submittedByAccountId = master.account.id,
                submittedByName = master.account.displayName,
                status = ContentWorkflowStatus.PENDING_APPROVAL,
                remoteId = customApprovalRemoteId,
                syncState = SyncState.PENDING_UPLOAD,
                serverUpdatedAtMillis = 567_890L
            )
        )
        database.versionHistoryDao().insert(
            VersionHistoryEntity(
                sectionKey = ManagementSection.NEWS.key,
                recordId = 1L,
                versionNumber = 1,
                recordTitle = "Synced Campus News",
                contentSnapshotJson = "{}",
                changedByAccountId = master.account.id,
                changedByName = master.account.displayName,
                changeSummary = "Created",
                approvalStatus = ContentWorkflowStatus.PUBLISHED,
                remoteId = customVersionRemoteId,
                syncState = SyncState.SYNCED,
                serverUpdatedAtMillis = 678_901L
            )
        )
        database.mediaStudioDao().upsert(
            MediaStudioItemEntity(
                mediaKind = "YOUTUBE",
                title = "Campus Video",
                originalUri = "https://youtu.be/dQw4w9WgXcQ",
                mimeType = "video/youtube",
                createdByName = master.account.displayName,
                remoteId = customMediaRemoteId,
                syncState = SyncState.PENDING_UPLOAD,
                serverUpdatedAtMillis = 789_012L
            )
        )
        database.activityLogDao().appendLog(
            ActivityLogEntity(
                actionType = "SYNC_PREP_TEST",
                actorAccountId = master.account.id,
                actorName = master.account.displayName,
                actorRole = ManagementRole.MASTER_ADMIN,
                sectionKey = ManagementSection.NEWS.key,
                recordRef = "news#1",
                details = "Audit entry",
                remoteId = customLogRemoteId,
                syncState = SyncState.SYNCED,
                serverUpdatedAtMillis = 890_123L
            )
        )

        val v6Encrypted = backupManager.createEncryptedBackupBytes("SyncPass2026!").getOrThrow()
        val entries = decryptBackupZipEntries(v6Encrypted, "SyncPass2026!")
        val snapshotText = String(entries.getValue("database_snapshot.json"), Charsets.UTF_8)

        // Ensure no plaintext PIN, backup password, or cloud credential tokens are present
        assertFalse(snapshotText.contains("246810"))
        assertFalse(snapshotText.contains("SyncPass2026!"))
        assertFalse(snapshotText.contains("firebaseToken", ignoreCase = true))
        assertFalse(snapshotText.contains("idToken", ignoreCase = true))
        assertFalse(snapshotText.contains("refreshToken", ignoreCase = true))

        // 1. Restore v6 backup and verify exact sync metadata preserved
        database.clearAllTables()
        val v6Preview = backupManager.restoreEncryptedBackupBytes(
            bytes = v6Encrypted,
            password = "SyncPass2026!",
            actorName = master.account.displayName
        ).getOrThrow()
        assertEquals(6, v6Preview.schemaVersion)

        val restoredNews = database.newsDao().observeAll().first().single()
        assertEquals(customNewsRemoteId, restoredNews.remoteId)
        assertEquals(SyncState.SYNCED, restoredNews.syncState)
        assertEquals(123_456L, restoredNews.serverUpdatedAtMillis)

        val restoredPost = database.schoolPostDao().getAllPostsOnce().single()
        assertEquals(customPostRemoteId, restoredPost.remoteId)
        assertEquals(SyncState.PENDING_UPLOAD, restoredPost.syncState)
        assertEquals(234_567L, restoredPost.serverUpdatedAtMillis)
        assertEquals(345_678L, restoredPost.deletedAtMillis)

        val restoredHighlight = database.schoolHighlightDao().getAllHighlightsOnce().single()
        assertEquals(customHighlightRemoteId, restoredHighlight.remoteId)
        assertEquals(456_789L, restoredHighlight.serverUpdatedAtMillis)

        val restoredApproval = database.contentApprovalDao().getAllOnce().single()
        assertEquals(customApprovalRemoteId, restoredApproval.remoteId)

        val restoredVersion = database.versionHistoryDao().getAllOnce().single()
        assertEquals(customVersionRemoteId, restoredVersion.remoteId)

        val restoredMedia = database.mediaStudioDao().getAllOnce().single()
        assertEquals(customMediaRemoteId, restoredMedia.remoteId)

        val restoredLog = database.activityLogDao().getAllOnce().first { it.actionType == "SYNC_PREP_TEST" }
        assertEquals(customLogRemoteId, restoredLog.remoteId)
        assertEquals(890_123L, restoredLog.serverUpdatedAtMillis)

        // 2. Strip sync metadata to construct an authentic v5 backup and verify deterministic backfill on restore
        val v5Snapshot = JSONObject(snapshotText)
        val syncKeys = listOf("remoteId", "tenantId", "syncState", "serverUpdatedAtMillis", "deletedAtMillis")
        syncKeys.forEach { v5Snapshot.getJSONObject("schoolProfile").remove(it) }
        listOf(
            "heroPhotos", "academicInfo", "courses", "facilities", "transportRoutes",
            "newsItems", "announcements", "galleryAlbums", "galleryMedia", "socialLinks",
            "schoolPosts", "schoolHighlights", "contentApprovals", "versionHistory",
            "mediaStudioItems", "activityLogs"
        ).forEach { arrKey ->
            val arr: JSONArray? = v5Snapshot.optJSONArray(arrKey)
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    syncKeys.forEach { obj.remove(it) }
                }
            }
        }
        val v5SnapshotStr = v5Snapshot.toString()
        val v5Digest = MessageDigest.getInstance("SHA-256")
            .digest(v5SnapshotStr.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val v5Manifest = JSONObject(String(entries.getValue("manifest.json"), Charsets.UTF_8)).apply {
            put("schemaVersion", 5)
            put("snapshotSha256", v5Digest)
        }
        val v5Entries = LinkedHashMap(entries).apply {
            put("manifest.json", v5Manifest.toString().toByteArray(Charsets.UTF_8))
            put("database_snapshot.json", v5SnapshotStr.toByteArray(Charsets.UTF_8))
        }
        val v5ArchiveBytes = encryptBackupZipEntries(v5Entries, "SyncPass2026!")

        database.clearAllTables()
        val v5Restored = backupManager.restoreEncryptedBackupBytes(
            bytes = v5ArchiveBytes,
            password = "SyncPass2026!",
            actorName = master.account.displayName
        ).getOrThrow()
        assertEquals(5, v5Restored.schemaVersion)

        val v5Post = database.schoolPostDao().getAllPostsOnce().single()
        assertEquals(SyncMetadataDefaults.deterministicRemoteId("school_posts", v5Post.id), v5Post.remoteId)
        assertEquals(SchoolTenantContext.DEFAULT_SCHOOL_ID, v5Post.tenantId)
        assertEquals(SyncState.LOCAL_ONLY, v5Post.syncState)
        assertNull(v5Post.serverUpdatedAtMillis)
        assertNull(v5Post.deletedAtMillis)
    }

    @Test
    fun `datastore likes and bookmarks preserve local Long IDs while supporting remoteId compatibility backfill and resolution`() = runTest {
        // Legacy local-only toggles work identically
        prefsRepo.togglePostLike(11L)
        prefsRepo.togglePostBookmark(22L)
        val initial = prefsRepo.preferencesFlow.first()
        assertEquals(setOf(11L), initial.likedPostIds)
        assertEquals(setOf(22L), initial.bookmarkedPostIds)
        assertTrue(initial.likedPostRemoteIds.isEmpty())
        assertTrue(initial.bookmarkedPostRemoteIds.isEmpty())

        val remote11 = SyncMetadataDefaults.deterministicRemoteId("school_posts", 11L)
        val remote22 = SyncMetadataDefaults.deterministicRemoteId("school_posts", 22L)
        val remote33 = SyncMetadataDefaults.deterministicRemoteId("school_posts", 33L)

        // Backfill maps existing local IDs to stable remoteIds without losing local Long IDs
        prefsRepo.backfillSocialPostRemoteIds(mapOf(11L to remote11, 22L to remote22))
        val afterBackfill = prefsRepo.preferencesFlow.first()
        assertEquals(setOf(11L), afterBackfill.likedPostIds)
        assertEquals(setOf(22L), afterBackfill.bookmarkedPostIds)
        assertEquals(setOf(remote11), afterBackfill.likedPostRemoteIds)
        assertEquals(setOf(remote22), afterBackfill.bookmarkedPostRemoteIds)
        assertTrue(afterBackfill.isPostLiked(11L, remote11))
        assertTrue(afterBackfill.isPostLiked(999L, remote11)) // matches by stable remoteId on another device
        assertTrue(afterBackfill.isPostBookmarked(999L, remote22))

        // Toggle with remoteId updates both sets atomically
        prefsRepo.togglePostLike(33L, remote33)
        val afterToggleAdd = prefsRepo.preferencesFlow.first()
        assertEquals(setOf(11L, 33L), afterToggleAdd.likedPostIds)
        assertEquals(setOf(remote11, remote33), afterToggleAdd.likedPostRemoteIds)

        prefsRepo.togglePostLike(33L, remote33)
        val afterToggleRemove = prefsRepo.preferencesFlow.first()
        assertEquals(setOf(11L), afterToggleRemove.likedPostIds)
        assertEquals(setOf(remote11), afterToggleRemove.likedPostRemoteIds)
        assertNotEquals(remote11, remote22)

        // Reconcile prunes stale IDs across both local and remote sets
        prefsRepo.reconcileSocialPostPreferences(
            validPostIds = setOf(11L),
            validPostRemoteIds = setOf(remote11)
        )
        val afterReconcile = prefsRepo.preferencesFlow.first()
        assertEquals(setOf(11L), afterReconcile.likedPostIds)
        assertEquals(setOf(remote11), afterReconcile.likedPostRemoteIds)
        assertTrue(afterReconcile.bookmarkedPostIds.isEmpty())
        assertTrue(afterReconcile.bookmarkedPostRemoteIds.isEmpty())
    }

    @Test
    fun `exported Room v6 schema json exists and contains all 20 entities`() {
        val candidates = listOf(
            File("schemas/com.example.data.local.ChevalierDatabase/6.json"),
            File("app/schemas/com.example.data.local.ChevalierDatabase/6.json")
        )
        val schemaFile = candidates.firstOrNull { it.exists() }
        assertNotNull("Expected exported Room schema JSON at app/schemas/com.example.data.local.ChevalierDatabase/6.json", schemaFile)
        val root = JSONObject(schemaFile!!.readText(Charsets.UTF_8))
        val dbObj = root.getJSONObject("database")
        assertEquals(6, dbObj.getInt("version"))
        assertEquals(20, dbObj.getJSONArray("entities").length())
    }

    private fun createAuthenticV5Database(dbName: String): File {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        dbFile.delete()
        val v5Db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        v5Db.version = 5
        v5Db.execSQL("PRAGMA foreign_keys=ON")

        v5Db.execSQL(
            """
            CREATE TABLE school_profile (
                id INTEGER NOT NULL PRIMARY KEY,
                schoolName TEXT NOT NULL, shortName TEXT NOT NULL, motto TEXT NOT NULL,
                addressLine TEXT NOT NULL, emblemMark TEXT NOT NULL, poweredBy TEXT NOT NULL,
                welcomeIntroduction TEXT, mediumAndGradesNote TEXT, mapCoordinatesOrQuery TEXT,
                phone TEXT, email TEXT, website TEXT, officeHours TEXT, principalName TEXT,
                history TEXT, vision TEXT, mission TEXT, affiliationNumber TEXT,
                updatedAtMillis INTEGER NOT NULL, profileImageUri TEXT, aboutImageUri TEXT
            )
            """.trimIndent()
        )
        v5Db.execSQL("CREATE TABLE campus_hero_photos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, imageUri TEXT NOT NULL, caption TEXT, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL, addedAtMillis INTEGER NOT NULL)")
        v5Db.execSQL("CREATE TABLE academic_info (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, category TEXT NOT NULL, description TEXT NOT NULL, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL, updatedAtMillis INTEGER NOT NULL)")
        v5Db.execSQL("CREATE TABLE higher_secondary_courses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, streamTitle TEXT NOT NULL, groupCode TEXT NOT NULL, subjectsSummary TEXT NOT NULL, mediumOfInstruction TEXT NOT NULL, classLevelLabel TEXT, eligibilityNote TEXT, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v5Db.execSQL("CREATE TABLE facilities (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, description TEXT NOT NULL, capacityOrSpec TEXT, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL, imageUri TEXT)")
        v5Db.execSQL("CREATE TABLE transport_routes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, routeNumber TEXT NOT NULL, routeName TEXT NOT NULL, stopsSummary TEXT NOT NULL, morningTiming TEXT, afternoonTiming TEXT, coordinatorContact TEXT, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL, displayOrder INTEGER NOT NULL DEFAULT 0)")
        v5Db.execSQL("CREATE TABLE news_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, summary TEXT NOT NULL, fullBody TEXT NOT NULL, publishedDateText TEXT NOT NULL, publishedAtMillis INTEGER NOT NULL, isFeatured INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL, imageUri TEXT, displayOrder INTEGER NOT NULL DEFAULT 0)")
        v5Db.execSQL("CREATE TABLE announcements (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, message TEXT NOT NULL, priorityLabel TEXT, issuedDateText TEXT NOT NULL, issuedAtMillis INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL, imageUri TEXT, displayOrder INTEGER NOT NULL DEFAULT 0)")
        v5Db.execSQL("CREATE TABLE gallery_albums (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, category TEXT NOT NULL, description TEXT, eventDateText TEXT, coverMediaUri TEXT, createdAtMillis INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL, displayOrder INTEGER NOT NULL DEFAULT 0)")
        v5Db.execSQL("CREATE TABLE gallery_media (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, albumId INTEGER NOT NULL, title TEXT, caption TEXT, mediaUri TEXT NOT NULL, mediaType TEXT NOT NULL, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL, addedAtMillis INTEGER NOT NULL, FOREIGN KEY(albumId) REFERENCES gallery_albums(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        v5Db.execSQL("CREATE INDEX index_gallery_media_albumId ON gallery_media (albumId)")
        v5Db.execSQL("CREATE TABLE social_media_links (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, platformName TEXT NOT NULL, handleOrLabel TEXT NOT NULL, url TEXT NOT NULL, displayOrder INTEGER NOT NULL, isVerified INTEGER NOT NULL, isPublished INTEGER NOT NULL, isApproved INTEGER NOT NULL, approvalStatus TEXT NOT NULL)")
        v5Db.execSQL("CREATE TABLE management_accounts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, username TEXT NOT NULL, displayName TEXT NOT NULL, role TEXT NOT NULL, pinSaltBase64 TEXT NOT NULL, pinVerifierBase64 TEXT NOT NULL, recoverySaltBase64 TEXT, recoveryVerifierBase64 TEXT, isActive INTEGER NOT NULL, failedAttempts INTEGER NOT NULL, lockoutUntilMillis INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL, updatedAtMillis INTEGER NOT NULL)")
        v5Db.execSQL("CREATE UNIQUE INDEX index_management_accounts_username ON management_accounts (username)")
        v5Db.execSQL("CREATE TABLE staff_permissions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, accountId INTEGER NOT NULL, sectionKey TEXT NOT NULL, canView INTEGER NOT NULL, canCreate INTEGER NOT NULL, canEdit INTEGER NOT NULL, canDelete INTEGER NOT NULL, canPublish INTEGER NOT NULL, FOREIGN KEY(accountId) REFERENCES management_accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        v5Db.execSQL("CREATE INDEX index_staff_permissions_accountId ON staff_permissions (accountId)")
        v5Db.execSQL("CREATE UNIQUE INDEX index_staff_permissions_accountId_sectionKey ON staff_permissions (accountId, sectionKey)")
        v5Db.execSQL("CREATE TABLE content_approvals (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sectionKey TEXT NOT NULL, targetRecordId INTEGER NOT NULL, recordTitle TEXT NOT NULL, previousSnapshotJson TEXT, proposedPayloadJson TEXT NOT NULL, changeSummary TEXT NOT NULL, submittedByAccountId INTEGER NOT NULL, submittedByName TEXT NOT NULL, submittedAtMillis INTEGER NOT NULL, status TEXT NOT NULL, reviewedByAccountId INTEGER, reviewedByName TEXT, reviewedAtMillis INTEGER, rejectionReason TEXT)")
        v5Db.execSQL("CREATE INDEX index_content_approvals_sectionKey ON content_approvals (sectionKey)")
        v5Db.execSQL("CREATE INDEX index_content_approvals_status ON content_approvals (status)")
        v5Db.execSQL("CREATE INDEX index_content_approvals_submittedByAccountId ON content_approvals (submittedByAccountId)")
        v5Db.execSQL("CREATE TABLE version_history (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sectionKey TEXT NOT NULL, recordId INTEGER NOT NULL, versionNumber INTEGER NOT NULL, recordTitle TEXT NOT NULL, previousContentJson TEXT, contentSnapshotJson TEXT NOT NULL, changedByAccountId INTEGER NOT NULL, changedByName TEXT NOT NULL, changedAtMillis INTEGER NOT NULL, changeSummary TEXT NOT NULL, approvalStatus TEXT NOT NULL)")
        v5Db.execSQL("CREATE INDEX index_version_history_sectionKey_recordId ON version_history (sectionKey, recordId)")
        v5Db.execSQL("CREATE TABLE media_studio_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, mediaKind TEXT NOT NULL, title TEXT NOT NULL, caption TEXT, categoryOrAlbum TEXT NOT NULL, originalUri TEXT NOT NULL, optimizedUri TEXT, mimeType TEXT NOT NULL, widthPx INTEGER NOT NULL, heightPx INTEGER NOT NULL, fileSizeBytes INTEGER NOT NULL, optimizedSizeBytes INTEGER NOT NULL, assignedSectionsCsv TEXT NOT NULL, isFeatured INTEGER NOT NULL, displayOrder INTEGER NOT NULL, approvalStatus TEXT NOT NULL, createdByName TEXT NOT NULL, createdAtMillis INTEGER NOT NULL)")
        v5Db.execSQL("CREATE INDEX index_media_studio_items_mediaKind ON media_studio_items (mediaKind)")
        v5Db.execSQL("CREATE INDEX index_media_studio_items_approvalStatus ON media_studio_items (approvalStatus)")
        v5Db.execSQL("CREATE TABLE activity_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, actionType TEXT NOT NULL, actorAccountId INTEGER, actorName TEXT NOT NULL, actorRole TEXT NOT NULL, sectionKey TEXT NOT NULL, recordRef TEXT NOT NULL, details TEXT NOT NULL, timestampMillis INTEGER NOT NULL)")
        v5Db.execSQL("CREATE INDEX index_activity_logs_timestampMillis ON activity_logs (timestampMillis)")
        v5Db.execSQL("CREATE INDEX index_activity_logs_actorAccountId ON activity_logs (actorAccountId)")
        v5Db.execSQL("CREATE INDEX index_activity_logs_sectionKey ON activity_logs (sectionKey)")
        v5Db.execSQL("CREATE TABLE local_notifications (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, eventType TEXT NOT NULL, title TEXT NOT NULL, message TEXT NOT NULL, sectionKey TEXT NOT NULL, isRead INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL)")
        v5Db.execSQL("CREATE INDEX index_local_notifications_isRead ON local_notifications (isRead)")
        v5Db.execSQL("CREATE INDEX index_local_notifications_createdAtMillis ON local_notifications (createdAtMillis)")
        v5Db.execSQL("CREATE TABLE school_posts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, postType TEXT NOT NULL, category TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, youtubeUrl TEXT, customThumbnailUri TEXT, photoUrisJson TEXT NOT NULL, status TEXT NOT NULL, isFeatured INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL, publishedAtMillis INTEGER, updatedAtMillis INTEGER NOT NULL)")
        v5Db.execSQL("CREATE INDEX index_school_posts_status ON school_posts (status)")
        v5Db.execSQL("CREATE INDEX index_school_posts_isFeatured ON school_posts (isFeatured)")
        v5Db.execSQL("CREATE INDEX index_school_posts_publishedAtMillis ON school_posts (publishedAtMillis)")
        v5Db.execSQL("CREATE INDEX index_school_posts_category ON school_posts (category)")
        v5Db.execSQL("CREATE TABLE school_highlights (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, youtubeShortsUrl TEXT NOT NULL, status TEXT NOT NULL, createdAtMillis INTEGER NOT NULL, publishedAtMillis INTEGER NOT NULL, expiresAtMillis INTEGER NOT NULL)")
        v5Db.execSQL("CREATE INDEX index_school_highlights_status ON school_highlights (status)")
        v5Db.execSQL("CREATE INDEX index_school_highlights_expiresAtMillis ON school_highlights (expiresAtMillis)")
        v5Db.execSQL("CREATE INDEX index_school_highlights_publishedAtMillis ON school_highlights (publishedAtMillis)")

        // Seed 1 row in every single one of the 20 tables
        v5Db.execSQL("INSERT INTO school_profile (id, schoolName, shortName, motto, addressLine, emblemMark, poweredBy, updatedAtMillis) VALUES (1, 'CHEVALIER ACADEMY MATRIC HIGHER SECONDARY SCHOOL', 'Chevalier Academy', 'Learn to Live & Love', 'Siluvathur Road, Dindigul - 624 306', 'M S C', 'POWERED BY JK LABS', 1000)")
        v5Db.execSQL("INSERT INTO campus_hero_photos (id, imageUri, caption, displayOrder, isVerified, isPublished, isApproved, approvalStatus, addedAtMillis) VALUES (1, 'file:///gate.jpg', 'Campus Gate', 1, 1, 1, 1, 'APPROVED', 1000)")
        v5Db.execSQL("INSERT INTO academic_info (id, title, category, description, displayOrder, isVerified, isPublished, isApproved, approvalStatus, updatedAtMillis) VALUES (1, 'Matriculation Curriculum', 'Curriculum', 'TN State Board', 1, 1, 1, 1, 'APPROVED', 1000)")
        v5Db.execSQL("INSERT INTO higher_secondary_courses (id, streamTitle, groupCode, subjectsSummary, mediumOfInstruction, displayOrder, isVerified, isPublished, isApproved, approvalStatus) VALUES (1, 'Bio-Maths', 'G1', 'Physics, Chemistry, Biology, Maths', 'English', 1, 1, 1, 1, 'APPROVED')")
        v5Db.execSQL("INSERT INTO facilities (id, name, category, description, displayOrder, isVerified, isPublished, isApproved, approvalStatus) VALUES (1, 'Physics Lab', 'Labs', 'Modern equipment', 1, 1, 1, 1, 'APPROVED')")
        v5Db.execSQL("INSERT INTO transport_routes (id, routeNumber, routeName, stopsSummary, isVerified, isPublished, isApproved, approvalStatus, displayOrder) VALUES (1, 'R-01', 'Dindigul Town', 'Bus Stand', 1, 1, 1, 'APPROVED', 1)")
        v5Db.execSQL("INSERT INTO news_items (id, title, summary, fullBody, publishedDateText, publishedAtMillis, isFeatured, isVerified, isPublished, isApproved, approvalStatus, displayOrder) VALUES (1, 'Annual Sports Meet', 'Track events', 'Full report', '15 Nov', 2000, 1, 1, 1, 1, 'APPROVED', 1)")
        v5Db.execSQL("INSERT INTO announcements (id, title, message, issuedDateText, issuedAtMillis, isVerified, isPublished, isApproved, approvalStatus, displayOrder) VALUES (1, 'PTA Meeting', 'Saturday 10 AM', '16 Nov', 3000, 1, 1, 1, 'APPROVED', 1)")
        v5Db.execSQL("INSERT INTO gallery_albums (id, title, category, createdAtMillis, isVerified, isPublished, isApproved, approvalStatus, displayOrder) VALUES (1, 'Founders Day', 'Events', 4000, 1, 1, 1, 'APPROVED', 1)")
        v5Db.execSQL("INSERT INTO gallery_media (id, albumId, title, mediaUri, mediaType, displayOrder, isVerified, isPublished, isApproved, approvalStatus, addedAtMillis) VALUES (1, 1, 'Stage', 'file:///stage.jpg', 'IMAGE', 1, 1, 1, 1, 'APPROVED', 4100)")
        v5Db.execSQL("INSERT INTO social_media_links (id, platformName, handleOrLabel, url, displayOrder, isVerified, isPublished, isApproved, approvalStatus) VALUES (1, 'YouTube', '@Chevalier', 'https://youtube.com/@chevalier', 1, 1, 1, 1, 'APPROVED')")
        v5Db.execSQL("INSERT INTO management_accounts (id, username, displayName, role, pinSaltBase64, pinVerifierBase64, isActive, failedAttempts, lockoutUntilMillis, createdAtMillis, updatedAtMillis) VALUES (1, 'admin', 'Principal', 'MASTER_ADMIN', 'salt', 'verifier', 1, 0, 0, 1000, 1000)")
        v5Db.execSQL("INSERT INTO staff_permissions (id, accountId, sectionKey, canView, canCreate, canEdit, canDelete, canPublish) VALUES (1, 1, 'NEWS', 1, 1, 1, 1, 1)")
        v5Db.execSQL("INSERT INTO content_approvals (id, sectionKey, targetRecordId, recordTitle, proposedPayloadJson, changeSummary, submittedByAccountId, submittedByName, submittedAtMillis, status) VALUES (1, 'NEWS', 1, 'Pending News', '{}', 'Draft', 1, 'Principal', 5000, 'PENDING_APPROVAL')")
        v5Db.execSQL("INSERT INTO version_history (id, sectionKey, recordId, versionNumber, recordTitle, contentSnapshotJson, changedByAccountId, changedByName, changedAtMillis, changeSummary, approvalStatus) VALUES (1, 'NEWS', 1, 1, 'Annual Sports Meet', '{}', 1, 'Principal', 6000, 'Created', 'PUBLISHED')")
        v5Db.execSQL("INSERT INTO media_studio_items (id, mediaKind, title, categoryOrAlbum, originalUri, mimeType, widthPx, heightPx, fileSizeBytes, optimizedSizeBytes, assignedSectionsCsv, isFeatured, displayOrder, approvalStatus, createdByName, createdAtMillis) VALUES (1, 'IMAGE', 'Banner Photo', 'Campus', 'file:///banner.jpg', 'image/jpeg', 1200, 800, 5000, 2000, 'LOGO_BANNER', 1, 1, 'PUBLISHED', 'Principal', 7000)")
        v5Db.execSQL("INSERT INTO activity_logs (id, actionType, actorAccountId, actorName, actorRole, sectionKey, recordRef, details, timestampMillis) VALUES (1, 'PUBLISH', 1, 'Principal', 'MASTER_ADMIN', 'NEWS', 'news#1', 'Published item', 8000)")
        v5Db.execSQL("INSERT INTO local_notifications (id, eventType, title, message, sectionKey, isRead, createdAtMillis) VALUES (1, 'INFO', 'Local Alert', 'Hello', 'NEWS', 0, 9000)")
        v5Db.execSQL("INSERT INTO school_posts (id, postType, category, title, description, youtubeUrl, photoUrisJson, status, isFeatured, createdAtMillis, publishedAtMillis, updatedAtMillis) VALUES (1, 'YOUTUBE_DIRECT', 'Events', 'Science Expo Video', 'Highlights', 'https://youtu.be/dQw4w9WgXcQ', '[]', 'PUBLISHED', 1, 10000, 10000, 10000)")
        v5Db.execSQL("INSERT INTO school_highlights (id, title, youtubeShortsUrl, status, createdAtMillis, publishedAtMillis, expiresAtMillis) VALUES (1, 'Morning Drill', 'https://www.youtube.com/shorts/dQw4w9WgXcQ', 'PUBLISHED', 11000, 11000, 97400000)")

        v5Db.close()
        return dbFile
    }

    private fun readTableColumnNames(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        tableName: String
    ): Set<String> {
        val names = mutableSetOf<String>()
        db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
            val nameIdx = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                names.add(cursor.getString(nameIdx))
            }
        }
        return names
    }

    private fun decryptBackupZipEntries(encryptedBytes: ByteArray, password: String): Map<String, ByteArray> {
        val buf = ByteBuffer.wrap(encryptedBytes)
        buf.position(12)
        val salt = ByteArray(16).also { buf.get(it) }
        val iv = ByteArray(12).also { buf.get(it) }
        val ciphertext = ByteArray(buf.remaining()).also { buf.get(it) }
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        val key = SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec).encoded, "AES")
        keySpec.clearPassword()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        val zipBytes = cipher.doFinal(ciphertext)
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readBytes()
                entry = zis.nextEntry
            }
        }
        return entries
    }

    private fun encryptBackupZipEntries(entries: Map<String, ByteArray>, password: String): ByteArray {
        val zipBytes = ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zos ->
                entries.forEach { (name, bytes) ->
                    zos.putNextEntry(ZipEntry(name))
                    zos.write(bytes)
                    zos.closeEntry()
                }
            }
            baos.toByteArray()
        }
        val random = SecureRandom()
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val iv = ByteArray(12).also { random.nextBytes(it) }
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        val key = SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec).encoded, "AES")
        keySpec.clearPassword()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(zipBytes)
        return ByteBuffer.allocate(8 + 4 + 16 + 12 + encrypted.size)
            .put("CHV_BAK1".toByteArray(Charsets.US_ASCII))
            .putInt(1)
            .put(salt)
            .put(iv)
            .put(encrypted)
            .array()
    }
}

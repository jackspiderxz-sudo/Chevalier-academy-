package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AcademicInfoDao
import com.example.data.local.dao.ActivityLogDao
import com.example.data.local.dao.AnnouncementDao
import com.example.data.local.dao.CampusHeroPhotoDao
import com.example.data.local.dao.ContentApprovalDao
import com.example.data.local.dao.FacilityDao
import com.example.data.local.dao.GalleryDao
import com.example.data.local.dao.HigherSecondaryCourseDao
import com.example.data.local.dao.LocalNotificationDao
import com.example.data.local.dao.ManagementAccountDao
import com.example.data.local.dao.MediaStudioDao
import com.example.data.local.dao.NewsDao
import com.example.data.local.dao.SchoolProfileDao
import com.example.data.local.dao.SocialMediaDao
import com.example.data.local.dao.TransportDao
import com.example.data.local.dao.VersionHistoryDao
import com.example.data.local.entity.AcademicInfoEntity
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.CampusHeroPhotoEntity
import com.example.data.local.entity.ContentApprovalEntity
import com.example.data.local.entity.FacilityEntity
import com.example.data.local.entity.GalleryAlbumEntity
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.HigherSecondaryCourseEntity
import com.example.data.local.entity.LocalNotificationEntity
import com.example.data.local.entity.ManagementAccountEntity
import com.example.data.local.entity.MediaStudioItemEntity
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.local.entity.SocialMediaLinkEntity
import com.example.data.local.entity.StaffPermissionEntity
import com.example.data.local.entity.TransportRouteEntity
import com.example.data.local.entity.VersionHistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        SchoolProfileEntity::class,
        CampusHeroPhotoEntity::class,
        AcademicInfoEntity::class,
        HigherSecondaryCourseEntity::class,
        FacilityEntity::class,
        TransportRouteEntity::class,
        NewsItemEntity::class,
        AnnouncementEntity::class,
        GalleryAlbumEntity::class,
        GalleryMediaEntity::class,
        SocialMediaLinkEntity::class,
        ManagementAccountEntity::class,
        StaffPermissionEntity::class,
        ContentApprovalEntity::class,
        VersionHistoryEntity::class,
        MediaStudioItemEntity::class,
        ActivityLogEntity::class,
        LocalNotificationEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class ChevalierDatabase : RoomDatabase() {
    abstract fun schoolProfileDao(): SchoolProfileDao
    abstract fun campusHeroPhotoDao(): CampusHeroPhotoDao
    abstract fun academicInfoDao(): AcademicInfoDao
    abstract fun higherSecondaryCourseDao(): HigherSecondaryCourseDao
    abstract fun facilityDao(): FacilityDao
    abstract fun transportDao(): TransportDao
    abstract fun newsDao(): NewsDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun galleryDao(): GalleryDao
    abstract fun socialMediaDao(): SocialMediaDao

    abstract fun managementAccountDao(): ManagementAccountDao
    abstract fun contentApprovalDao(): ContentApprovalDao
    abstract fun versionHistoryDao(): VersionHistoryDao
    abstract fun mediaStudioDao(): MediaStudioDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun localNotificationDao(): LocalNotificationDao

    companion object {
        const val DATABASE_NAME = "chevalier_academy.db"

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `management_accounts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `username` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `pinSaltBase64` TEXT NOT NULL,
                        `pinVerifierBase64` TEXT NOT NULL,
                        `recoverySaltBase64` TEXT,
                        `recoveryVerifierBase64` TEXT,
                        `isActive` INTEGER NOT NULL,
                        `failedAttempts` INTEGER NOT NULL,
                        `lockoutUntilMillis` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_management_accounts_username` ON `management_accounts` (`username`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `staff_permissions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `accountId` INTEGER NOT NULL,
                        `sectionKey` TEXT NOT NULL,
                        `canView` INTEGER NOT NULL,
                        `canCreate` INTEGER NOT NULL,
                        `canEdit` INTEGER NOT NULL,
                        `canDelete` INTEGER NOT NULL,
                        `canPublish` INTEGER NOT NULL,
                        FOREIGN KEY(`accountId`) REFERENCES `management_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_staff_permissions_accountId` ON `staff_permissions` (`accountId`)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_staff_permissions_accountId_sectionKey` ON `staff_permissions` (`accountId`, `sectionKey`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `content_approvals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sectionKey` TEXT NOT NULL,
                        `targetRecordId` INTEGER NOT NULL,
                        `recordTitle` TEXT NOT NULL,
                        `previousSnapshotJson` TEXT,
                        `proposedPayloadJson` TEXT NOT NULL,
                        `changeSummary` TEXT NOT NULL,
                        `submittedByAccountId` INTEGER NOT NULL,
                        `submittedByName` TEXT NOT NULL,
                        `submittedAtMillis` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `reviewedByAccountId` INTEGER,
                        `reviewedByName` TEXT,
                        `reviewedAtMillis` INTEGER,
                        `rejectionReason` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_content_approvals_sectionKey` ON `content_approvals` (`sectionKey`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_content_approvals_status` ON `content_approvals` (`status`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_content_approvals_submittedByAccountId` ON `content_approvals` (`submittedByAccountId`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `version_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sectionKey` TEXT NOT NULL,
                        `recordId` INTEGER NOT NULL,
                        `versionNumber` INTEGER NOT NULL,
                        `recordTitle` TEXT NOT NULL,
                        `previousContentJson` TEXT,
                        `contentSnapshotJson` TEXT NOT NULL,
                        `changedByAccountId` INTEGER NOT NULL,
                        `changedByName` TEXT NOT NULL,
                        `changedAtMillis` INTEGER NOT NULL,
                        `changeSummary` TEXT NOT NULL,
                        `approvalStatus` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_version_history_sectionKey_recordId` ON `version_history` (`sectionKey`, `recordId`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `media_studio_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `mediaKind` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `caption` TEXT,
                        `categoryOrAlbum` TEXT NOT NULL,
                        `originalUri` TEXT NOT NULL,
                        `optimizedUri` TEXT,
                        `mimeType` TEXT NOT NULL,
                        `widthPx` INTEGER NOT NULL,
                        `heightPx` INTEGER NOT NULL,
                        `fileSizeBytes` INTEGER NOT NULL,
                        `optimizedSizeBytes` INTEGER NOT NULL,
                        `assignedSectionsCsv` TEXT NOT NULL,
                        `isFeatured` INTEGER NOT NULL,
                        `displayOrder` INTEGER NOT NULL,
                        `approvalStatus` TEXT NOT NULL,
                        `createdByName` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_media_studio_items_mediaKind` ON `media_studio_items` (`mediaKind`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_media_studio_items_approvalStatus` ON `media_studio_items` (`approvalStatus`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `activity_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `actionType` TEXT NOT NULL,
                        `actorAccountId` INTEGER,
                        `actorName` TEXT NOT NULL,
                        `actorRole` TEXT NOT NULL,
                        `sectionKey` TEXT NOT NULL,
                        `recordRef` TEXT NOT NULL,
                        `details` TEXT NOT NULL,
                        `timestampMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_activity_logs_timestampMillis` ON `activity_logs` (`timestampMillis`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_activity_logs_actorAccountId` ON `activity_logs` (`actorAccountId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_activity_logs_sectionKey` ON `activity_logs` (`sectionKey`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_notifications` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `sectionKey` TEXT NOT NULL,
                        `isRead` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_local_notifications_isRead` ON `local_notifications` (`isRead`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_local_notifications_createdAtMillis` ON `local_notifications` (`createdAtMillis`)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `school_profile` ADD COLUMN `profileImageUri` TEXT")
                db.execSQL("ALTER TABLE `school_profile` ADD COLUMN `aboutImageUri` TEXT")
                db.execSQL("ALTER TABLE `facilities` ADD COLUMN `imageUri` TEXT")
                db.execSQL("ALTER TABLE `news_items` ADD COLUMN `imageUri` TEXT")
                db.execSQL("ALTER TABLE `news_items` ADD COLUMN `displayOrder` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `announcements` ADD COLUMN `imageUri` TEXT")
                db.execSQL("ALTER TABLE `announcements` ADD COLUMN `displayOrder` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `transport_routes` ADD COLUMN `displayOrder` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `gallery_albums` ADD COLUMN `displayOrder` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun verifiedDefaultProfile(): SchoolProfileEntity = SchoolProfileEntity(
            id = 1,
            schoolName = "CHEVALIER ACADEMY MATRIC HIGHER SECONDARY SCHOOL",
            shortName = "Chevalier Academy",
            motto = "Learn to Live & Love",
            addressLine = "Siluvathur Road, Dindigul - 624 306",
            emblemMark = "M S C",
            poweredBy = "POWERED BY JK LABS",
            welcomeIntroduction = null,
            mediumAndGradesNote = null,
            mapCoordinatesOrQuery = "Chevalier Academy Matric Higher Secondary School, Siluvathur Road, Dindigul - 624 306",
            phone = null,
            email = null,
            website = null,
            officeHours = null,
            principalName = null,
            history = null,
            vision = null,
            mission = null,
            affiliationNumber = null
        )

        @Volatile
        private var INSTANCE: ChevalierDatabase? = null

        fun getInstance(context: Context): ChevalierDatabase {
            return INSTANCE ?: synchronized(this) {
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChevalierDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            scope.launch {
                                INSTANCE?.schoolProfileDao()?.upsertProfile(verifiedDefaultProfile())
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

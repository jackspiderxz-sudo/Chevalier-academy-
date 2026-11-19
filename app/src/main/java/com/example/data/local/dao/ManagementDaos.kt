package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.AccountWithPermissions
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.ContentApprovalEntity
import com.example.data.local.entity.LocalNotificationEntity
import com.example.data.local.entity.ManagementAccountEntity
import com.example.data.local.entity.MediaStudioItemEntity
import com.example.data.local.entity.StaffPermissionEntity
import com.example.data.local.entity.VersionHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ManagementAccountDao {
    @Transaction
    @Query("SELECT * FROM management_accounts ORDER BY role ASC, createdAtMillis ASC")
    fun observeAllAccountsWithPermissions(): Flow<List<AccountWithPermissions>>

    @Transaction
    @Query("SELECT * FROM management_accounts ORDER BY role ASC, createdAtMillis ASC")
    suspend fun getAllAccountsWithPermissionsOnce(): List<AccountWithPermissions>

    @Query("SELECT * FROM management_accounts WHERE role = 'MASTER_ADMIN' LIMIT 1")
    suspend fun getMasterAdminOnce(): ManagementAccountEntity?

    @Query("SELECT * FROM management_accounts WHERE role = 'MASTER_ADMIN' LIMIT 1")
    fun observeMasterAdmin(): Flow<ManagementAccountEntity?>

    @Query("SELECT COUNT(*) FROM management_accounts WHERE role = 'SUPER_OWNER'")
    suspend fun countSuperOwners(): Int

    @Query("SELECT * FROM management_accounts WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun findByUsername(username: String): ManagementAccountEntity?

    @Query("SELECT * FROM management_accounts WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ManagementAccountEntity?

    @Transaction
    @Query("SELECT * FROM management_accounts WHERE id = :id LIMIT 1")
    suspend fun findWithPermissionsById(id: Long): AccountWithPermissions?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: ManagementAccountEntity): Long

    @Update
    suspend fun updateAccount(account: ManagementAccountEntity)

    @Query("DELETE FROM management_accounts WHERE id = :id AND role != 'MASTER_ADMIN'")
    suspend fun deleteNonMasterAccount(id: Long)

    @Query("SELECT * FROM staff_permissions WHERE accountId = :accountId")
    suspend fun getPermissionsForAccountOnce(accountId: Long): List<StaffPermissionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPermissions(permissions: List<StaffPermissionEntity>)

    @Query("DELETE FROM staff_permissions WHERE accountId = :accountId")
    suspend fun deletePermissionsForAccount(accountId: Long)

    @Transaction
    suspend fun replacePermissions(accountId: Long, permissions: List<StaffPermissionEntity>) {
        deletePermissionsForAccount(accountId)
        if (permissions.isNotEmpty()) {
            upsertPermissions(permissions)
        }
    }
}

@Dao
interface ContentApprovalDao {
    @Query("SELECT * FROM content_approvals ORDER BY submittedAtMillis DESC, id DESC")
    fun observeAll(): Flow<List<ContentApprovalEntity>>

    @Query("SELECT * FROM content_approvals WHERE status = 'PENDING_APPROVAL' ORDER BY submittedAtMillis DESC, id DESC")
    fun observePending(): Flow<List<ContentApprovalEntity>>

    @Query("SELECT COUNT(*) FROM content_approvals WHERE status = 'PENDING_APPROVAL'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM content_approvals ORDER BY submittedAtMillis DESC, id DESC")
    suspend fun getAllOnce(): List<ContentApprovalEntity>

    @Query("SELECT * FROM content_approvals WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ContentApprovalEntity?

    @Query(
        "SELECT * FROM content_approvals " +
            "WHERE sectionKey = :sectionKey AND targetRecordId = :recordId " +
            "AND status = 'PENDING_APPROVAL' " +
            "ORDER BY submittedAtMillis DESC, id DESC LIMIT 1"
    )
    suspend fun getLatestPendingForRecord(sectionKey: String, recordId: Long): ContentApprovalEntity?

    @Query(
        "SELECT * FROM content_approvals " +
            "WHERE sectionKey = :sectionKey AND targetRecordId = :recordId " +
            "AND status = 'PENDING_APPROVAL' " +
            "ORDER BY submittedAtMillis DESC, id DESC"
    )
    suspend fun getPendingForRecordOnce(sectionKey: String, recordId: Long): List<ContentApprovalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(approval: ContentApprovalEntity): Long

    @Update
    suspend fun update(approval: ContentApprovalEntity)
}

@Dao
interface VersionHistoryDao {
    @Query("SELECT * FROM version_history ORDER BY changedAtMillis DESC, id DESC")
    fun observeAll(): Flow<List<VersionHistoryEntity>>

    @Query("SELECT * FROM version_history WHERE sectionKey = :sectionKey AND recordId = :recordId ORDER BY versionNumber DESC")
    fun observeForRecord(sectionKey: String, recordId: Long): Flow<List<VersionHistoryEntity>>

    @Query(
        "SELECT * FROM version_history " +
            "WHERE sectionKey = :sectionKey AND recordId = :recordId " +
            "ORDER BY versionNumber DESC, id DESC"
    )
    suspend fun getAllForRecordOnce(sectionKey: String, recordId: Long): List<VersionHistoryEntity>

    @Query("SELECT COALESCE(MAX(versionNumber), 0) FROM version_history WHERE sectionKey = :sectionKey AND recordId = :recordId")
    suspend fun getMaxVersionNumber(sectionKey: String, recordId: Long): Int

    @Query(
        "SELECT * FROM version_history " +
            "WHERE sectionKey = :sectionKey AND recordId = :recordId " +
            "ORDER BY versionNumber DESC, id DESC LIMIT 1"
    )
    suspend fun getLatestForRecord(sectionKey: String, recordId: Long): VersionHistoryEntity?

    @Query("SELECT * FROM version_history ORDER BY changedAtMillis DESC, id DESC")
    suspend fun getAllOnce(): List<VersionHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(version: VersionHistoryEntity): Long
}

@Dao
interface MediaStudioDao {
    @Query("SELECT * FROM media_studio_items ORDER BY displayOrder ASC, createdAtMillis DESC")
    fun observeAll(): Flow<List<MediaStudioItemEntity>>

    @Query("SELECT * FROM media_studio_items ORDER BY displayOrder ASC, createdAtMillis DESC")
    suspend fun getAllOnce(): List<MediaStudioItemEntity>

    @Query("SELECT * FROM media_studio_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MediaStudioItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MediaStudioItemEntity): Long

    @Query("DELETE FROM media_studio_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestampMillis DESC, id DESC")
    fun observeAll(): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE actorAccountId = :accountId ORDER BY timestampMillis DESC, id DESC")
    fun observeForAccount(accountId: Long): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs ORDER BY timestampMillis DESC, id DESC")
    suspend fun getAllOnce(): List<ActivityLogEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun appendLog(entry: ActivityLogEntity): Long
}

@Dao
interface LocalNotificationDao {
    @Query("SELECT * FROM local_notifications ORDER BY createdAtMillis DESC, id DESC")
    fun observeAll(): Flow<List<LocalNotificationEntity>>

    @Query("SELECT COUNT(*) FROM local_notifications WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("SELECT * FROM local_notifications ORDER BY createdAtMillis DESC, id DESC")
    suspend fun getAllOnce(): List<LocalNotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: LocalNotificationEntity): Long

    @Query("UPDATE local_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE local_notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllRead()

    @Query("DELETE FROM local_notifications")
    suspend fun clearAll()
}

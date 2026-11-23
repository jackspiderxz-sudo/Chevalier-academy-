package com.example.data.local.entity

import com.example.core.model.SchoolTenantContext
import java.util.UUID

/**
 * Local synchronization lifecycle states for cloud-synchronizable Room records.
 * Used as a forward-compatible contract for future multi-device sync phases without
 * enabling remote network synchronization in Phase 4.2A.
 */
object SyncState {
    const val LOCAL_ONLY = "LOCAL_ONLY"
    const val PENDING_UPLOAD = "PENDING_UPLOAD"
    const val SYNCED = "SYNCED"
    const val PENDING_DELETE = "PENDING_DELETE"
    const val CONFLICT = "CONFLICT"

    val all: Set<String> = setOf(
        LOCAL_ONLY,
        PENDING_UPLOAD,
        SYNCED,
        PENDING_DELETE,
        CONFLICT
    )
}

/**
 * Deterministic and runtime identifier helpers for Room sync metadata columns.
 *
 * - Newly created runtime records receive a random RFC-4122 UUID via [newRemoteId].
 * - Pre-existing local rows migrated from Room v5 -> v6 and legacy v1-v5 backup records
 *   receive a deterministic, repeatable RFC-4122 name-based UUID via [deterministicRemoteId]
 *   so repeated migrations or legacy restores produce identical identifiers without
 *   regenerating IDs on app start or record updates.
 */
object SyncMetadataDefaults {
    const val DEFAULT_TENANT_ID: String = SchoolTenantContext.DEFAULT_SCHOOL_ID
    const val DEFAULT_SYNC_STATE: String = SyncState.LOCAL_ONLY

    fun newRemoteId(): String = UUID.randomUUID().toString()

    fun deterministicRemoteId(
        tableName: String,
        localId: Long,
        tenantId: String = DEFAULT_TENANT_ID
    ): String {
        val seed = "$tenantId:$tableName:$localId"
        return UUID.nameUUIDFromBytes(seed.toByteArray(Charsets.UTF_8)).toString()
    }

    fun resolveTenantId(candidate: String?): String =
        candidate?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_TENANT_ID

    fun resolveSyncState(candidate: String?): String {
        val normalized = candidate?.trim()
        return if (normalized != null && normalized in SyncState.all) {
            normalized
        } else {
            DEFAULT_SYNC_STATE
        }
    }

    fun resolveRemoteId(
        candidate: String?,
        tableName: String,
        localId: Long,
        tenantId: String = DEFAULT_TENANT_ID
    ): String {
        val existing = candidate?.trim()?.takeIf { it.isNotEmpty() }
        if (existing != null) return existing
        return if (localId > 0L) {
            deterministicRemoteId(tableName, localId, tenantId)
        } else {
            newRemoteId()
        }
    }
}

package com.example

import com.example.data.backend.CloudSyncManager
import com.example.data.backend.OperationType
import com.example.data.local.entity.SchoolHighlightEntity
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostStatus
import com.example.data.local.entity.SchoolPostType
import com.example.data.local.entity.SyncMetadataDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSyncContractUnitTest {

    @Test
    fun cloudSyncManager_usesProvisionedTenantAndPinGateConstants() {
        assertEquals("chevalier_academy_dindigul", CloudSyncManager.TENANT_ID)
        assertEquals("pin_session_v1_verified", CloudSyncManager.PIN_GATE_DOC_ID)
        assertEquals(
            "verified-pin-gate@chevalieracademy.edu.in",
            CloudSyncManager.PIN_GATE_EMAIL
        )
        assertTrue(OperationType.entries.contains(OperationType.WRITE))
        assertTrue(OperationType.entries.contains(OperationType.LIST))
    }

    @Test
    fun deterministicRemoteIds_andSocialEntities_preserveTwoWaySyncContracts() {
        val profileRemoteId = SyncMetadataDefaults.deterministicRemoteId("school_profile", 1L)
        assertTrue(profileRemoteId.isNotBlank())

        val post = SchoolPostEntity(
            id = 42L,
            postType = SchoolPostType.YOUTUBE_DIRECT,
            category = "Events",
            title = "Annual Day Live",
            description = "Synced across Android and Website",
            youtubeUrl = "https://youtu.be/dQw4w9WgXcQ",
            status = SchoolPostStatus.PUBLISHED,
            isFeatured = true
        )
        assertEquals("chevalier_academy_dindigul", post.tenantId)
        assertTrue(post.remoteId.isNotBlank())

        val highlight = SchoolHighlightEntity(
            id = 7L,
            title = "Campus Short",
            youtubeShortsUrl = "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            status = SchoolPostStatus.PUBLISHED,
            publishedAtMillis = 1_000_000L
        )
        assertEquals(
            1_000_000L + SchoolHighlightEntity.HIGHLIGHT_TTL_MILLIS,
            highlight.expiresAtMillis
        )
        assertTrue(highlight.isActiveAt(1_000_000L + 60_000L))
    }
}

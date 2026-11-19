package com.example.data.backend

import android.content.Context
import com.example.core.model.SchoolTenantContext

enum class IntegrationState {
    ACTIVE_LOCAL,
    UNCONFIGURED
}

data class IntegrationCapabilityStatus(
    val key: String,
    val label: String,
    val state: IntegrationState,
    val summary: String
)

data class CentralBackendStatus(
    val tenantId: String,
    val persistenceMode: String,
    val backupSecuritySummary: String,
    val autoBackupHardening: String,
    val remoteCloudSync: IntegrationCapabilityStatus,
    val remoteAuth: IntegrationCapabilityStatus,
    val pushMessaging: IntegrationCapabilityStatus
) {
    val isRemoteSyncConfigured: Boolean
        get() = remoteCloudSync.state != IntegrationState.UNCONFIGURED
}

/**
 * Inspects actual compiled configuration/resources at runtime so the app never
 * misrepresents unconfigured cloud backends or push services as connected.
 */
object BackendConfigDetector {

    fun inspect(context: Context): CentralBackendStatus {
        val firebaseConfigured = hasConfiguredFirebaseAppId(context)
        val fcmConfigured = firebaseConfigured && hasConfiguredFcmSenderId(context)

        val syncStatus = if (firebaseConfigured) {
            IntegrationCapabilityStatus(
                key = "remote_sync",
                label = "Remote Cloud Database",
                state = IntegrationState.UNCONFIGURED,
                summary = "Firebase config detected, but Firestore/REST sync SDKs are not enabled in this build."
            )
        } else {
            IntegrationCapabilityStatus(
                key = "remote_sync",
                label = "Remote Cloud Database",
                state = IntegrationState.UNCONFIGURED,
                summary = "Unconfigured (no google-services.json or remote endpoint). Operating authoritatively on local Room storage."
            )
        }

        val authStatus = IntegrationCapabilityStatus(
            key = "auth_mode",
            label = "Authentication & RBAC",
            state = IntegrationState.ACTIVE_LOCAL,
            summary = "Local PBKDF2-HMAC-SHA256 (120k) multi-role PIN auth, constant-time verification & progressive lockout active."
        )

        val pushStatus = if (fcmConfigured) {
            IntegrationCapabilityStatus(
                key = "push_notifications",
                label = "Remote Push Notifications",
                state = IntegrationState.UNCONFIGURED,
                summary = "Sender resource present, but FCM client dependency is not enabled; local inbox alerts active."
            )
        } else {
            IntegrationCapabilityStatus(
                key = "push_notifications",
                label = "Remote Push Notifications",
                state = IntegrationState.UNCONFIGURED,
                summary = "Remote push (FCM) unconfigured. Local Room inbox and optional OS notification alerts active."
            )
        }

        return CentralBackendStatus(
            tenantId = SchoolTenantContext.DEFAULT_SCHOOL_ID,
            persistenceMode = "Authoritative Local Room SQLite (v4) + Jetpack DataStore + Internal Media Store",
            backupSecuritySummary = "Encrypted .chvbak archives (PBKDF2-HMAC-SHA256 + AES-256-GCM + SHA-256 manifest digest)",
            autoBackupHardening = "Android OS cloud Auto Backup disabled for local database, preferences, and auth verifiers",
            remoteCloudSync = syncStatus,
            remoteAuth = authStatus,
            pushMessaging = pushStatus
        )
    }

    private fun hasConfiguredFirebaseAppId(context: Context): Boolean {
        val id = context.resources.getIdentifier("google_app_id", "string", context.packageName)
        if (id == 0) return false
        val value = runCatching { context.getString(id) }.getOrNull()?.trim().orEmpty()
        return value.isNotEmpty() && !value.contains("placeholder", ignoreCase = true)
    }

    private fun hasConfiguredFcmSenderId(context: Context): Boolean {
        val id = context.resources.getIdentifier("gcm_defaultSenderId", "string", context.packageName)
        if (id == 0) return false
        val value = runCatching { context.getString(id) }.getOrNull()?.trim().orEmpty()
        return value.isNotEmpty() && value.all { it.isDigit() }
    }
}

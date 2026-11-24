package com.example.data.backend

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.AcademicInfoEntity
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.ApprovalStatus
import com.example.data.local.entity.CampusHeroPhotoEntity
import com.example.data.local.entity.FacilityEntity
import com.example.data.local.entity.GalleryAlbumEntity
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.HigherSecondaryCourseEntity
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.SchoolHighlightEntity
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostStatus
import com.example.data.local.entity.SchoolPostType
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.local.entity.SocialMediaLinkEntity
import com.example.data.local.entity.SyncMetadataDefaults
import com.example.data.local.entity.TransportRouteEntity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType {
    CREATE,
    UPDATE,
    DELETE,
    LIST,
    GET,
    WRITE
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    if (exception is CancellationException) return ""
    val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    val currentUser = auth?.currentUser

    val providerInfoArray = JSONArray()
    currentUser?.providerData?.forEach { profile ->
        val providerObj = JSONObject().apply {
            put("providerId", profile.providerId)
            put("displayName", profile.displayName ?: JSONObject.NULL)
            put("email", profile.email ?: JSONObject.NULL)
            put("photoUrl", profile.photoUrl?.toString() ?: JSONObject.NULL)
        }
        providerInfoArray.put(providerObj)
    }

    val authInfo = JSONObject().apply {
        put("userId", currentUser?.uid ?: JSONObject.NULL)
        put("email", currentUser?.email ?: JSONObject.NULL)
        put("emailVerified", currentUser?.isEmailVerified ?: JSONObject.NULL)
        put("isAnonymous", currentUser?.isAnonymous ?: JSONObject.NULL)
        put("tenantId", currentUser?.tenantId ?: JSONObject.NULL)
        put("providerInfo", providerInfoArray)
    }

    val errInfo = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.name.lowercase())
        put("path", path ?: JSONObject.NULL)
        put("authInfo", authInfo)
    }

    val jsonString = errInfo.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString", exception)
    return jsonString
}

data class CloudSyncRuntimeStatus(
    val isRunning: Boolean = false,
    val lastSyncedAtMillis: Long? = null,
    val lastError: String? = null,
    val activeListenersCount: Int = 0
)

/**
 * Two-Way Live Synchronization Engine connecting Android Room Database <-> Firebase Firestore.
 * Synchronizes all 13 public & admin-managed CMS collections under
 * `/schools/chevalier_academy_dindigul/{collection}/{remoteId}`.
 */
class CloudSyncManager(
    private val context: Context,
    private val database: ChevalierDatabase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val started = AtomicBoolean(false)
    private val isApplyingRemote = AtomicBoolean(false)
    private val pinSessionGateReady = AtomicBoolean(false)
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    private val syncDigests = ConcurrentHashMap<String, String>()
    private val knownLocalRemoteIds = ConcurrentHashMap<String, Set<String>>()
    private val knownRemoteIds = ConcurrentHashMap<String, Set<String>>()

    private val _status = MutableStateFlow(CloudSyncRuntimeStatus())
    val status: StateFlow<CloudSyncRuntimeStatus> = _status.asStateFlow()

    private val firestore: FirebaseFirestore? by lazy {
        runCatching {
            val dbId = context.applicationContext.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        }.onFailure { err ->
            Log.w(TAG, "Firestore initialization deferred: ${err.message}")
        }.getOrNull()
    }

    fun startIfEligible() {
        if (isRobolectricRuntime()) {
            return
        }
        start()
    }

    fun start() {
        val db = firestore ?: return
        if (!started.compareAndSet(false, true)) return

        scope.launch {
            ensurePinAdminSessionSynced(db)
            attachInboundListeners(db)
            observeOutboundChanges(db)
            _status.value = _status.value.copy(
                isRunning = true,
                activeListenersCount = synchronized(listenerRegistrations) { listenerRegistrations.size }
            )
        }
    }

    fun stop() {
        synchronized(listenerRegistrations) {
            listenerRegistrations.forEach { runCatching { it.remove() } }
            listenerRegistrations.clear()
        }
        started.set(false)
        _status.value = _status.value.copy(isRunning = false, activeListenersCount = 0)
    }

    // =========================================================================
    // Google Sign-In via Jetpack CredentialManager
    // =========================================================================

    suspend fun attemptAutoSignIn(activityContext: Context): Boolean {
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val webClientId = activityContext.getString(R.string.default_web_client_id)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential =
                    GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
                true
            } else {
                false
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "User cancelled auto sign-in", e)
            false
        } catch (e: Exception) {
            Log.d("Auth", "Auto sign-in unavailable: ${e.message}")
            false
        }
    }

    suspend fun signInWithGoogleExplicit(activityContext: Context): Boolean {
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val webClientId = activityContext.getString(R.string.default_web_client_id)
            val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential =
                    GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
                true
            } else {
                false
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "User cancelled sign-in", e)
            false
        } catch (e: Exception) {
            Log.e("Auth", "Google sign-in failed", e)
            false
        }
    }

    suspend fun signOut(activityContext: Context) {
        withContext(NonCancellable) {
            runCatching {
                FirebaseAuth.getInstance().signOut()
                CredentialManager.create(activityContext)
                    .clearCredentialState(ClearCredentialStateRequest())
            }.onFailure { e ->
                Log.w("Auth", "Sign-out cleanup warning", e)
            }
        }
    }

    // =========================================================================
    // Version 1 PIN Session Gate Document
    // =========================================================================

    private suspend fun ensurePinAdminSessionSynced(db: FirebaseFirestore) {
        if (pinSessionGateReady.get()) return
        val path = "schools/$TENANT_ID/management_accounts/$PIN_GATE_DOC_ID"
        val now = System.currentTimeMillis()
        val gatePayload = mapOf(
            "uid" to PIN_GATE_DOC_ID,
            "username" to "superowner",
            "displayName" to "Chevalier Admin",
            "email" to PIN_GATE_EMAIL,
            "role" to "SUPER_OWNER",
            "isActive" to true,
            "createdAtMillis" to now,
            "updatedAtMillis" to now,
            "tenantId" to TENANT_ID,
            "serverUpdatedAt" to FieldValue.serverTimestamp()
        )
        runCatching {
            db.document(path).set(gatePayload, SetOptions.merge()).await()
            pinSessionGateReady.set(true)
        }.onFailure { err ->
            if (err is CancellationException) throw err
            handleFirestoreError(err, OperationType.WRITE, path)
        }
    }

    // =========================================================================
    // Inbound Listeners (Firestore -> Android Room)
    // =========================================================================

    private fun attachInboundListeners(db: FirebaseFirestore) {
        val schoolRef = db.collection("schools").document(TENANT_ID)

        // 1. School Profile
        registerListener(
            schoolRef.collection("school_profile")
                .whereEqualTo("tenantId", TENANT_ID)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(
                            error,
                            OperationType.LIST,
                            "schools/$TENANT_ID/school_profile"
                        )
                        return@addSnapshotListener
                    }
                    val doc = snapshot?.documents?.firstOrNull() ?: return@addSnapshotListener
                    scope.launch { applyInboundSchoolProfile(doc.id, doc.data.orEmpty()) }
                }
        )

        // 2. Campus Hero Photos
        registerPublishedCollectionListener(db, "campus_hero_photos") { docs ->
            applyInboundHeroPhotos(docs)
        }

        // 3. Academic Info
        registerPublishedCollectionListener(db, "academic_info") { docs ->
            applyInboundAcademicInfo(docs)
        }

        // 4. Higher Secondary Courses
        registerPublishedCollectionListener(db, "higher_secondary_courses") { docs ->
            applyInboundCourses(docs)
        }

        // 5. Facilities
        registerPublishedCollectionListener(db, "facilities") { docs ->
            applyInboundFacilities(docs)
        }

        // 6. Transport Routes
        registerPublishedCollectionListener(db, "transport_routes") { docs ->
            applyInboundTransportRoutes(docs)
        }

        // 7. News Items
        registerPublishedCollectionListener(db, "news_items") { docs ->
            applyInboundNews(docs)
        }

        // 8. Announcements
        registerPublishedCollectionListener(db, "announcements") { docs ->
            applyInboundAnnouncements(docs)
        }

        // 9. Gallery Albums & 10. Gallery Media
        registerPublishedCollectionListener(db, "gallery_albums") { docs ->
            applyInboundGalleryAlbums(docs)
        }
        registerPublishedCollectionListener(db, "gallery_media") { docs ->
            applyInboundGalleryMedia(docs)
        }

        // 11. Social Media Links
        registerPublishedCollectionListener(db, "social_media_links") { docs ->
            applyInboundSocialLinks(docs)
        }

        // 12. School Posts (Social Feed)
        registerListener(
            schoolRef.collection("school_posts")
                .whereEqualTo("tenantId", TENANT_ID)
                .whereEqualTo("status", SchoolPostStatus.PUBLISHED)
                .whereEqualTo("deletedAtMillis", null)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(
                            error,
                            OperationType.LIST,
                            "schools/$TENANT_ID/school_posts"
                        )
                        return@addSnapshotListener
                    }
                    val docs = snapshot?.documents?.map { it.id to it.data.orEmpty() }.orEmpty()
                    scope.launch { applyInboundSchoolPosts(docs) }
                }
        )

        // 13. School Highlights (24h Shorts)
        registerListener(
            schoolRef.collection("school_highlights")
                .whereEqualTo("tenantId", TENANT_ID)
                .whereEqualTo("status", SchoolPostStatus.PUBLISHED)
                .whereEqualTo("deletedAtMillis", null)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(
                            error,
                            OperationType.LIST,
                            "schools/$TENANT_ID/school_highlights"
                        )
                        return@addSnapshotListener
                    }
                    val docs = snapshot?.documents?.map { it.id to it.data.orEmpty() }.orEmpty()
                    scope.launch { applyInboundSchoolHighlights(docs) }
                }
        )
    }

    private fun registerPublishedCollectionListener(
        db: FirebaseFirestore,
        collectionName: String,
        onDocs: suspend (List<Pair<String, Map<String, Any>>>) -> Unit
    ) {
        val reg = db.collection("schools")
            .document(TENANT_ID)
            .collection(collectionName)
            .whereEqualTo("tenantId", TENANT_ID)
            .whereEqualTo("isVerified", true)
            .whereEqualTo("isPublished", true)
            .whereEqualTo("isApproved", true)
            .whereEqualTo("approvalStatus", ApprovalStatus.APPROVED)
            .whereEqualTo("deletedAtMillis", null)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(
                        error,
                        OperationType.LIST,
                        "schools/$TENANT_ID/$collectionName"
                    )
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents?.map { it.id to it.data.orEmpty() }.orEmpty()
                scope.launch { onDocs(docs) }
            }
        registerListener(reg)
    }

    private fun registerListener(registration: ListenerRegistration) {
        synchronized(listenerRegistrations) {
            listenerRegistrations.add(registration)
        }
    }

    // =========================================================================
    // Inbound Appliers (Firestore Document -> Android Room Entity)
    // =========================================================================

    private suspend fun applyInboundSchoolProfile(remoteId: String, data: Map<String, Any>) {
        val digest = computeDigest("school_profile", remoteId, data)
        if (syncDigests["school_profile:$remoteId"] == digest) return

        val existing = database.schoolProfileDao().getProfileOnce()
        val remoteUpdated = (data["updatedAtMillis"] as? Number)?.toLong() ?: 0L
        if (existing != null && existing.updatedAtMillis > remoteUpdated && remoteUpdated > 0L) {
            return
        }

        val profileImg = fromPortableImageUri(
            remoteId = remoteId,
            fieldTag = "profile",
            uriStr = data["profileImageUri"] as? String
        ) ?: existing?.profileImageUri

        val aboutImg = fromPortableImageUri(
            remoteId = remoteId,
            fieldTag = "about",
            uriStr = data["aboutImageUri"] as? String
        ) ?: existing?.aboutImageUri

        val entity = SchoolProfileEntity(
            id = 1,
            schoolName = (data["schoolName"] as? String)?.takeIf { it.isNotBlank() }
                ?: existing?.schoolName
                ?: "CHEVALIER ACADEMY MATRIC HIGHER SECONDARY SCHOOL",
            shortName = (data["shortName"] as? String)?.takeIf { it.isNotBlank() }
                ?: existing?.shortName
                ?: "Chevalier Academy",
            motto = (data["motto"] as? String)?.takeIf { it.isNotBlank() }
                ?: existing?.motto
                ?: "Learn to Live & Love",
            addressLine = (data["addressLine"] as? String)?.takeIf { it.isNotBlank() }
                ?: existing?.addressLine
                ?: "Siluvathur Road, Dindigul - 624 306",
            emblemMark = (data["emblemMark"] as? String)?.takeIf { it.isNotBlank() }
                ?: existing?.emblemMark
                ?: "M S C",
            poweredBy = (data["poweredBy"] as? String)?.takeIf { it.isNotBlank() }
                ?: existing?.poweredBy
                ?: "POWERED BY JK LABS",
            welcomeIntroduction = (data["welcomeIntroduction"] as? String)
                ?: existing?.welcomeIntroduction,
            mediumAndGradesNote = (data["mediumAndGradesNote"] as? String)
                ?: existing?.mediumAndGradesNote,
            mapCoordinatesOrQuery = (data["mapCoordinatesOrQuery"] as? String)
                ?: existing?.mapCoordinatesOrQuery,
            phone = (data["phone"] as? String) ?: existing?.phone,
            email = (data["email"] as? String) ?: existing?.email,
            website = (data["website"] as? String) ?: existing?.website,
            officeHours = (data["officeHours"] as? String) ?: existing?.officeHours,
            principalName = (data["principalName"] as? String) ?: existing?.principalName,
            history = (data["history"] as? String) ?: existing?.history,
            vision = (data["vision"] as? String) ?: existing?.vision,
            mission = (data["mission"] as? String) ?: existing?.mission,
            affiliationNumber = (data["affiliationNumber"] as? String)
                ?: existing?.affiliationNumber,
            updatedAtMillis = if (remoteUpdated > 0L) remoteUpdated else System.currentTimeMillis(),
            profileImageUri = profileImg,
            aboutImageUri = aboutImg,
            remoteId = existing?.remoteId ?: remoteId,
            tenantId = TENANT_ID,
            syncState = "SYNCED",
            serverUpdatedAtMillis = System.currentTimeMillis()
        )

        syncDigests["school_profile:${entity.remoteId}"] = digest
        withRemoteGuard {
            database.schoolProfileDao().upsertProfile(entity)
        }
        markSyncCompleted()
    }

    private suspend fun applyInboundHeroPhotos(docs: List<Pair<String, Map<String, Any>>>) {
        val currentList = database.campusHeroPhotoDao().observeAllForManagement().first()
        val byRemoteId = currentList.associateBy { it.remoteId }
        val activeRemoteIds = docs.map { it.first }.toSet()
        val previouslyKnownRemote = knownRemoteIds["campus_hero_photos"].orEmpty()

        withRemoteGuard {
            for ((remoteId, data) in docs) {
                val digest = computeDigest("campus_hero_photos", remoteId, data)
                if (syncDigests["campus_hero_photos:$remoteId"] == digest) continue
                val existing = byRemoteId[remoteId]
                val localImageUri = fromPortableImageUri(
                    remoteId = remoteId,
                    fieldTag = "hero",
                    uriStr = data["imageUri"] as? String
                ) ?: existing?.imageUri ?: continue

                val entity = CampusHeroPhotoEntity(
                    id = existing?.id ?: 0L,
                    imageUri = localImageUri,
                    caption = data["caption"] as? String,
                    displayOrder = (data["displayOrder"] as? Number)?.toInt()
                        ?: existing?.displayOrder ?: 0,
                    isVerified = true,
                    isPublished = true,
                    isApproved = true,
                    approvalStatus = ApprovalStatus.APPROVED,
                    addedAtMillis = (data["addedAtMillis"] as? Number)?.toLong()
                        ?: existing?.addedAtMillis ?: System.currentTimeMillis(),
                    remoteId = remoteId,
                    tenantId = TENANT_ID,
                    syncState = "SYNCED",
                    serverUpdatedAtMillis = System.currentTimeMillis(),
                    deletedAtMillis = null
                )
                syncDigests["campus_hero_photos:$remoteId"] = digest
                database.campusHeroPhotoDao().upsert(entity)
            }

            // Remove items that were previously synced from remote and then deleted on Website
            for (removedRemoteId in (previouslyKnownRemote - activeRemoteIds)) {
                byRemoteId[removedRemoteId]?.let { toDelete ->
                    database.campusHeroPhotoDao().deleteById(toDelete.id)
                    syncDigests.remove("campus_hero_photos:$removedRemoteId")
                }
            }
        }
        knownRemoteIds["campus_hero_photos"] = activeRemoteIds
        markSyncCompleted()
    }

    private suspend fun applyInboundAcademicInfo(docs: List<Pair<String, Map<String, Any>>>) {
        val currentList = database.academicInfoDao().observeAll().first()
        val byRemoteId = currentList.associateBy { it.remoteId }
        val activeRemoteIds = docs.map { it.first }.toSet()
        val previouslyKnownRemote = knownRemoteIds["academic_info"].orEmpty()

        withRemoteGuard {
            for ((remoteId, data) in docs) {
                val digest = computeDigest("academic_info", remoteId, data)
                if (syncDigests["academic_info:$remoteId"] == digest) continue
                val existing = byRemoteId[remoteId]
                val title = (data["title"] as? String)?.trim().orEmpty()
                if (title.isEmpty()) continue

                val entity = AcademicInfoEntity(
                    id = existing?.id ?: 0L,
                    title = title,
                    category = (data["category"] as? String)?.ifBlank { "General" } ?: "General",
                    description = (data["description"] as? String).orEmpty(),
                    displayOrder = (data["displayOrder"] as? Number)?.toInt()
                        ?: existing?.displayOrder ?: 0,
                    isVerified = true,
                    isPublished = true,
                    isApproved = true,
                    approvalStatus = ApprovalStatus.APPROVED,
                    updatedAtMillis = (data["updatedAtMillis"] as? Number)?.toLong()
                        ?: System.currentTimeMillis(),
                    remoteId = remoteId,
                    tenantId = TENANT_ID,
                    syncState = "SYNCED",
                    serverUpdatedAtMillis = System.currentTimeMillis(),
                    deletedAtMillis = null
                )
                syncDigests["academic_info:$remoteId"] = digest
                database.academicInfoDao().upsert(entity)
            }
            for (removedRemoteId in (previouslyKnownRemote - activeRemoteIds)) {
                byRemoteId[removedRemoteId]?.let { toDelete ->
                    database.academicInfoDao().deleteById(toDelete.id)
                    syncDigests.remove("academic_info:$removedRemoteId")
                }
            }
        }
        knownRemoteIds["academic_info"] = activeRemoteIds
        markSyncCompleted()
    }

    private suspend fun applyInboundCourses(docs: List<Pair<String, Map<String, Any>>>) {
        val currentList = database.higherSecondaryCourseDao().observeAll().first()
        val byRemoteId = currentList.associateBy { it.remoteId }
        val byGroupCode = currentList.associateBy { it.groupCode.trim().lowercase() }
        val activeRemoteIds = docs.map { it.first }.toSet()
        val previouslyKnownRemote = knownRemoteIds["higher_secondary_courses"].orEmpty()

        withRemoteGuard {
            for ((remoteId, data) in docs) {
                val digest = computeDigest("higher_secondary_courses", remoteId, data)
                if (syncDigests["higher_secondary_courses:$remoteId"] == digest) continue
                val groupCode = (data["groupCode"] as? String)?.trim().orEmpty()
                val streamTitle = (data["streamTitle"] as? String)?.trim().orEmpty()
                if (groupCode.isEmpty() || streamTitle.isEmpty()) continue

                val existing = byRemoteId[remoteId] ?: byGroupCode[groupCode.lowercase()]
                val effectiveRemoteId = existing?.remoteId ?: remoteId

                val entity = HigherSecondaryCourseEntity(
                    id = existing?.id ?: 0L,
                    streamTitle = streamTitle,
                    groupCode = groupCode,
                    subjectsSummary = (data["subjectsSummary"] as? String).orEmpty(),
                    mediumOfInstruction = (data["mediumOfInstruction"] as? String)
                        ?.ifBlank { "English Medium" } ?: "English Medium",
                    classLevelLabel = data["classLevelLabel"] as? String
                        ?: existing?.classLevelLabel,
                    eligibilityNote = data["eligibilityNote"] as? String
                        ?: existing?.eligibilityNote,
                    displayOrder = (data["displayOrder"] as? Number)?.toInt()
                        ?: existing?.displayOrder ?: 0,
                    isVerified = true,
                    isPublished = true,
                    isApproved = true,
                    approvalStatus = ApprovalStatus.APPROVED,
                    remoteId = effectiveRemoteId,
                    tenantId = TENANT_ID,
                    syncState = "SYNCED",
                    serverUpdatedAtMillis = System.currentTimeMillis(),
                    deletedAtMillis = null
                )
                syncDigests["higher_secondary_courses:$effectiveRemoteId"] = digest
                syncDigests["higher_secondary_courses:$remoteId"] = digest
                database.higherSecondaryCourseDao().upsert(entity)
            }
            for (removedRemoteId in (previouslyKnownRemote - activeRemoteIds)) {
                byRemoteId[removedRemoteId]?.let { toDelete ->
                    database.higherSecondaryCourseDao().deleteById(toDelete.id)
                    syncDigests.remove("higher_secondary_courses:$removedRemoteId")
                }
            }
        }
        knownRemoteIds["higher_secondary_courses"] = activeRemoteIds
        markSyncCompleted()
    }

    private suspend fun applyInboundFacilities(docs: List<Pair<String, Map<String, Any>>>) {
        val currentList = database.facilityDao().observeAll().first()
        val byRemoteId = currentList.associateBy { it.remoteId }
        val activeRemoteIds = docs.map { it.first }.toSet()
        val previouslyKnownRemote = knownRemoteIds["facilities"].orEmpty()

        withRemoteGuard {
            for ((remoteId, data) in docs) {
                val digest = computeDigest("facilities", remoteId, data)
                if (syncDigests["facilities:$remoteId"] == digest) continue
                val existing = byRemoteId[remoteId]
                val name = (data["name"] as? String)?.trim().orEmpty()
                if (name.isEmpty()) continue

                val localImageUri = fromPortableImageUri(
                    remoteId = remoteId,
                    fieldTag = "facility",
                    uriStr = data["imageUri"] as? String
                ) ?: existing?.imageUri

                val entity = FacilityEntity(
                    id = existing?.id ?: 0L,
                    name = name,
                    category = (data["category"] as? String)?.ifBlank { "Campus" } ?: "Campus",
                    description = (data["description"] as? String).orEmpty(),
                    capacityOrSpec = data["capacityOrSpec"] as? String,
                    displayOrder = (data["displayOrder"] as? Number)?.toInt()
                        ?: existing?.displayOrder ?: 0,
                    isVerified = true,
                    isPublished = true,
                    isApproved = true,
                    approvalStatus = ApprovalStatus.APPROVED,
                    imageUri = localImageUri,
                    remoteId = remoteId,
                    tenantId = TENANT_ID,
                    syncState = "SYNCED",
                    serverUpdatedAtMillis = System.currentTimeMillis(),
                    deletedAtMillis = null
                )
                syncDigests["facilities:$remoteId"] = digest
                database.facilityDao().upsert(entity)
            }
            for (removedRemoteId in (previouslyKnownRemote - activeRemoteIds)) {
                byRemoteId[removedRemoteId]?.let { toDelete ->
                    database.facilityDao().deleteById(toDelete.id)
                    syncDigests.remove("facilities:$removedRemoteId")
                }
            }
        }
        knownRemoteIds["facilities"] = activeRemoteIds
        markSyncCompleted()
    }

    private suspend fun applyInboundTransportRoutes(docs: List<Pair<String, Map<String, Any>>>) {
        val currentList = database.transportDao().observeAll().first()
        val byRemoteId = currentList.associateBy { it.remoteId }
        val activeRemoteIds = docs.map { it.first }.toSet()
        val previouslyKnownRemote = knownRemoteIds["transport_routes"].orEmpty()

        withRemoteGuard {
            for ((remoteId, data) in docs) {
                val digest = computeDigest("transport_routes", remoteId, data)
                if (syncDigests["transport_routes:$remoteId"] == digest) continue
                val existing = byRemoteId[remoteId]
                val routeNumber = (data["routeNumber"] as? String)?.trim().orEmpty()
                val routeName = (data["routeName"] as? String)?.trim().orEmpty()
                if (routeNumber.isEmpty() || routeName.isEmpty()) continue

                val entity = TransportRouteEntity(
                    id = existing?.id ?: 0L,
                    routeNumber = routeNumber,
                    routeName = routeName,
                    stopsSummary = (data["stopsSummary"] as? String).orEmpty(),
                    morningTiming = data["morningTiming"] as? String,
                    afternoonTiming = data["afternoonTiming"] as? String,
                    coordinatorContact = data["coordinatorContact"] as? String,
                    isVerified = true,
                    isPublished = true,
                    isApproved = true,
                    approvalStatus = ApprovalStatus.APPROVED,
                    displayOrder = (data["displayOrder"] as? Number)?.toInt()
                        ?: existing?.displayOrder ?: 0,
                    remoteId = remoteId,
                    tenantId = TENANT_ID,
                    syncState = "SYNCED",
                    serverUpdatedAtMillis = System.currentTimeMillis(),
                    deletedAtMillis = null
                )
                syncDigests["transport_routes:$remoteId"] = digest
                database.transportDao().upsert(entity)
            }
            for (removedRemoteId in (previouslyKnownRemote - activeRemoteIds)) {
                byRemoteId[removedRemoteId]?.let { toDelete ->
                    database.transportDao().deleteById(toDelete.id)
                    syncDigests.remove("transport_routes:$removedRemoteId")
                }
            }
        }
        knownRemoteIds["transport_routes"] = activeRemoteIds
        markSyncCompleted()
    }

    private suspend fun applyInboundNews(docs: List<Pair<String, Map<String, Any>>>) {
        val currentList = database.newsDao().observeAll().first()
        val byRemoteId = currentList.associateBy { it.remoteId }
        val activeRemoteIds = docs.map { it.first }.toSet()
        val previouslyKnownRemote = knownRemoteIds["news_items"].orEmpty()

        withRemoteGuard {
            for ((remoteId, data) in docs) {
                val digest = computeDigest("news_items", remoteId, data)
                if (syncDigests["news_items:$remoteId"] == digest) continue
                val existing = byRemoteId[remoteId]
                val title = (data["title"] as? String)?.trim().orEmpty()
                if (title.isEmpty()) continue

                val localImageUri = fromPortableImageUri(
                    remoteId = remoteId,
                    fieldTag = "news",
                    uriStr = data["imageUri"] as? String
                ) ?: existing?.imageUri

                val entity = NewsItemEntity(
                    id = existing?.id ?: 0L,
                    title = title,
                    summary = (data["summary"] as? String).orEmpty(),
                    fullBody = (data["fullBody"] as? String).orEmpty(),
                    publishedDateText = (data["publishedDateText"] as? String)
                        ?.ifBlank { "Today" } ?: "Today",
                    publishedAtMillis = (data["publishedAtMillis"] as? Number)?.toLong()
                        ?: existing?.publishedAtMillis ?: System.currentTimeMillis(),
                    isFeatured = (data["isFeatured"] as? Boolean) ?: false,
                    isVerified = true,
                    isPublished = true,
                    isApproved = true,
                    approvalStatus = ApprovalStatus.APPROVED,
                    imageUri = localImageUri,
                    displayOrder = (data["displayOrder"] as? Number)?.toInt()
                        ?: existing?.displayOrder ?: 0,
                    remoteId = remoteId,
                    tenantId = TENANT_ID,
                    syncState = "SYNCED",
                    serverUpdatedAtMillis = System.currentTimeMillis(),
                    deletedAtMillis = null
                )
                syncDigests["news_items:$remoteId"] = digest
                database.newsDao().upsert(entity)
            }
            for (removedRemoteId in (previouslyKnownRemote - activeRemoteIds)) {
                byRemoteId[removedRemoteId]?.let { toDelete ->
                    database.newsDao().deleteById(toDelete.id)
                    syncDigests.remove("news_items:$removedRemoteId")
                }
            }
        }
        knownRemoteIds["news_items"] = activeRemoteIds
        markSyncCompleted()
    }

    private suspend fun applyInboundAnnouncements(docs: List<Pair<String, Map<String, Any>>>) {
        val currentList = database.announcementDao().observeAll().first()
        val byRemoteId = currentList.associateBy { it.remoteId }
        val activeRemoteIds = docs.map { it.first }.toSet()
        val previouslyKnownRemote = knownRemoteIds["announcements"].orEmpty()

        withRemoteGuard {
            for ((remoteId, data) in docs) {
                val digest = computeDigest("announcements", remoteId, data)
                if (syncDigests["announcements:$remoteId"] == digest) continue
                val existing = byRemoteId[remoteId]
                val title = (data["title"] as? String)?.trim().orEmpty()
                if (title.isEmpty()) continue

                val localImageUri = fromPortableImageUri(
                    remoteId = remoteId,
                    fieldTag = "announcement",
                    uriStr = data["imageUri"] as? String
                ) ?: existing?.imageUri

                val entity = AnnouncementEntity(
                    id = existing?.id ?: 0L,
                    title = title,
                    message = (data["message"] as? String).orEmpty(),
                    priorityLabel = data["priorityLabel"] as? String,
                    issuedDateText = (data["issuedDateText"] as? String)
                        ?.ifBlank { "Today" } ?: "Today",
                    issuedAtMillis = (data["issuedAtMillis"] as? Number)?.toLong()
                        ?: existing?.issuedAtMillis ?: System.currentTimeMillis(),
                    isVerified = true,
                    isPublished = true,
                    isApproved = true,
                    approvalStatus = ApprovalStatus.APPROVED,
                    imageUri = localImageUri,
                    displayOrder = (data["displayOrder"] as? Number)?.toInt()
                        ?: existing?.displayOrder ?: 0,
                    remoteId = remoteId,
                    tenantId = TENANT_ID,
                    syncState = "SYNCED",
                    serverUpdatedAtMillis = System.currentTimeMillis(),
                    deletedAtMillis = null
                )
                syncDigests["announcements:$remoteId"] = digest
                database.announcementDao().upsert(entity)
            }
            for (removedRemoteId in (previouslyKnownRemote - activeRemoteIds)) {
                byRemoteId[removedRemoteId]?.let { toDelete ->
                    database.announcementDao().deleteById(toDelete.id)
                    syncDigests.remove("announcements:$removedRemoteId")
                }
            }
        }
        knownRemoteIds["announcements"] = activeRemoteIds
        markSyncCompleted()
    }

    private suspend fun applyInboundGalleryAlbums(docs: List<Pair<String, Map<String, Any>>>) {
        val currentAlbums = database.galleryDao().observeAlbumsWithMedia().first().map { it.album }
        val byRemoteId = currentAlbums.associateBy { it.remoteId }
        val activeRemoteIds = docs.map { it.first }.toSet()
        val previouslyKnownRemote = knownRemoteIds["gallery_albums"].orEmpty()

        withRemoteGuard {
            for ((remoteId, data) in docs) {
                val digest = computeDigest("gallery_albums", remoteId, data)
                if (syncDigests["gallery_albums:$remoteId"] == digest) continue
                val existing = byRemoteId[remoteId]
                val title = (data["title"] as? String)?.trim().orEmpty()
                if (title.isEmpty()) continue

                val coverUri = fromPortableImageUri(
                    remoteId = remoteId,
                    fieldTag = "album_cover",
                    uriStr = data["coverMediaUri"] as? String
                ) ?: existing?.coverMediaUri

                val entity = GalleryAlbumEntity(
                    id = existing?.id ?: 0L,
                    title = title,
                    category = (data["category"] as? String)?.ifBlank { "Campus" } ?: "Campus",
                    description = data["description"] as? String,
                    eventDateText = data["eventDateText"] as? String,
                    coverMediaUri = coverUri,
                    createdAtMillis = (data["createdAtMillis"] as? Number)?.toLong()
                        ?: existing?.createdAtMillis ?: System.currentTimeMillis(),
                    isVerified = true,
                    isPublished = true,
                    isApproved = true,
                    approvalStatus = ApprovalStatus.APPROVED,
                    displayOrder = (data["displayOrder"] as? Number)?.toInt()
                        ?: existing?.displayOrder ?: 0,
                    remoteId = remoteId,
                    tenantId = TENANT_ID,
                    syncState = "SYNCED",
                    serverUpdatedAtMillis = System.currentTimeMillis(),
                    deletedAtMillis = null
                )
                syncDigests["gallery_albums:$remoteId"] = digest
                database.galleryDao().upsertAlbum(entity)
            }
            for (removedRemoteId in (previouslyKnownRemote - activeRemoteIds)) {
                byRemoteId[removedRemoteId]?.let { toDelete ->
                    database.galleryDao().deleteAlbumById(toDelete.id)
                    syncDigests.remove("gallery_albums:$removedRemoteId")
                }
            }
        }
        knownRemoteIds["gallery_albums"] = activeRemoteIds
        markSyncCompleted()
    }

    private suspend fun applyInboundGalleryMedia(docs: List<Pair<String, Map<String, Any>>>) {
        val visibleDocs = docs.filterNot { (remoteId, _) -> remoteId.startsWith(POST_PHOTO_PREFIX) }
        val currentMedia = database.galleryDao().observeAllMedia().first()
        val byRemoteId = currentMedia.associateBy { it.remoteId }
        val activeRemoteIds = visibleDocs.map { it.first }.toSet()
        val previouslyKnownRemote = knownRemoteIds["gallery_media"].orEmpty()

        withRemoteGuard {
            for ((remoteId, data) in visibleDocs) {
                val digest = computeDigest("gallery_media", remoteId, data)
                if (syncDigests["gallery_media:$remoteId"] == digest) continue
                val existing = byRemoteId[remoteId]
                val localMediaUri = fromPortableImageUri(
                    remoteId = remoteId,
                    fieldTag = "gallery_media",
                    uriStr = data["mediaUri"] as? String
                ) ?: existing?.mediaUri ?: continue

                // Resolve parent album in Room by albumRemoteId or title (create default album if needed)
                val currentAlbums =
                    database.galleryDao().observeAlbumsWithMedia().first().map { it.album }
                val incomingAlbumRemoteId = (data["albumRemoteId"] as? String)?.trim().orEmpty()
                val targetAlbumTitle = (data["title"] as? String)?.takeIf { it.isNotBlank() }
                    ?: "Campus Highlights"
                val parentAlbum = currentAlbums.firstOrNull {
                    incomingAlbumRemoteId.isNotEmpty() && it.remoteId == incomingAlbumRemoteId
                } ?: currentAlbums.firstOrNull {
                    it.title.equals(targetAlbumTitle, ignoreCase = true)
                } ?: currentAlbums.firstOrNull() ?: run {
                    val defaultAlbum = GalleryAlbumEntity(
                        title = targetAlbumTitle,
                        category = "Campus",
                        coverMediaUri = localMediaUri,
                        isVerified = true,
                        isPublished = true,
                        isApproved = true,
                        approvalStatus = ApprovalStatus.APPROVED,
                        remoteId = incomingAlbumRemoteId.ifBlank {
                            SyncMetadataDefaults.deterministicRemoteId("gallery_albums", 1L)
                        }
                    )
                    val newId = database.galleryDao().upsertAlbum(defaultAlbum)
                    defaultAlbum.copy(id = newId)
                }

                val entity = GalleryMediaEntity(
                    id = existing?.id ?: 0L,
                    albumId = existing?.albumId ?: parentAlbum.id,
                    title = data["title"] as? String,
                    caption = data["caption"] as? String,
                    mediaUri = localMediaUri,
                    mediaType = (data["mediaType"] as? String)?.ifBlank { "IMAGE" } ?: "IMAGE",
                    displayOrder = (data["displayOrder"] as? Number)?.toInt()
                        ?: existing?.displayOrder ?: 0,
                    isVerified = true,
                    isPublished = true,
                    isApproved = true,
                    approvalStatus = ApprovalStatus.APPROVED,
                    addedAtMillis = (data["addedAtMillis"] as? Number)?.toLong()
                        ?: existing?.addedAtMillis ?: System.currentTimeMillis(),
                    remoteId = remoteId,
                    tenantId = TENANT_ID,
                    syncState = "SYNCED",
                    serverUpdatedAtMillis = System.currentTimeMillis(),
                    deletedAtMillis = null
                )
                syncDigests["gallery_media:$remoteId"] = digest
                database.galleryDao().upsertMedia(entity)
            }
            for (removedRemoteId in (previouslyKnownRemote - activeRemoteIds)) {
                byRemoteId[removedRemoteId]?.let { toDelete ->
                    database.galleryDao().deleteMediaById(toDelete.id)
                    syncDigests.remove("gallery_media:$removedRemoteId")
                }
            }
        }
        knownRemoteIds["gallery_media"] = activeRemoteIds
        markSyncCompleted()
    }

    private suspend fun applyInboundSocialLinks(docs: List<Pair<String, Map<String, Any>>>) {
        val currentList = database.socialMediaDao().observeAll().first()
        val byRemoteId = currentList.associateBy { it.remoteId }
        val byPlatform = currentList.associateBy { it.platformName.trim().lowercase() }
        val activeRemoteIds = docs.map { it.first }.toSet()
        val previouslyKnownRemote = knownRemoteIds["social_media_links"].orEmpty()

        withRemoteGuard {
            for ((remoteId, data) in docs) {
                val digest = computeDigest("social_media_links", remoteId, data)
                if (syncDigests["social_media_links:$remoteId"] == digest) continue
                val platformName = (data["platformName"] as? String)?.trim().orEmpty()
                val url = (data["url"] as? String)?.trim().orEmpty()
                if (platformName.isEmpty() || url.isEmpty()) continue

                val existing = byRemoteId[remoteId] ?: byPlatform[platformName.lowercase()]
                val effectiveRemoteId = existing?.remoteId ?: remoteId

                val entity = SocialMediaLinkEntity(
                    id = existing?.id ?: 0L,
                    platformName = platformName,
                    handleOrLabel = (data["handleOrLabel"] as? String)?.ifBlank { platformName }
                        ?: platformName,
                    url = url,
                    displayOrder = (data["displayOrder"] as? Number)?.toInt()
                        ?: existing?.displayOrder ?: 0,
                    isVerified = true,
                    isPublished = true,
                    isApproved = true,
                    approvalStatus = ApprovalStatus.APPROVED,
                    remoteId = effectiveRemoteId,
                    tenantId = TENANT_ID,
                    syncState = "SYNCED",
                    serverUpdatedAtMillis = System.currentTimeMillis(),
                    deletedAtMillis = null
                )
                syncDigests["social_media_links:$effectiveRemoteId"] = digest
                syncDigests["social_media_links:$remoteId"] = digest
                database.socialMediaDao().upsert(entity)
            }
            for (removedRemoteId in (previouslyKnownRemote - activeRemoteIds)) {
                byRemoteId[removedRemoteId]?.let { toDelete ->
                    database.socialMediaDao().deleteById(toDelete.id)
                    syncDigests.remove("social_media_links:$removedRemoteId")
                }
            }
        }
        knownRemoteIds["social_media_links"] = activeRemoteIds
        markSyncCompleted()
    }

    private suspend fun applyInboundSchoolPosts(docs: List<Pair<String, Map<String, Any>>>) {
        val currentList = database.schoolPostDao().getAllPostsOnce()
        val byRemoteId = currentList.associateBy { it.remoteId }
        val activeRemoteIds = docs.map { it.first }.toSet()
        val previouslyKnownRemote = knownRemoteIds["school_posts"].orEmpty()

        withRemoteGuard {
            for ((remoteId, data) in docs) {
                val digest = computeDigest("school_posts", remoteId, data)
                if (syncDigests["school_posts:$remoteId"] == digest) continue
                val existing = byRemoteId[remoteId]
                val title = (data["title"] as? String)?.trim().orEmpty()
                if (title.isEmpty()) continue

                val customThumb = fromPortableImageUri(
                    remoteId = remoteId,
                    fieldTag = "post_thumb",
                    uriStr = data["customThumbnailUri"] as? String
                ) ?: existing?.customThumbnailUri

                val decodedPhotos: List<String> = when (val rawList = data["photoUris"]) {
                    is List<*> -> rawList.mapNotNull { (it as? String)?.trim()?.takeIf { s -> s.isNotEmpty() } }
                    else -> SchoolPostEntity.decodePhotoUris(data["photoUrisJson"] as? String)
                }
                val localizedPhotos = decodedPhotos.take(SchoolPostEntity.MAX_PHOTO_COUNT).mapIndexed { idx, uri ->
                    resolveInboundPostPhotoUri(
                        remoteId = remoteId,
                        index = idx,
                        uriStr = uri
                    ) ?: uri
                }

                val now = System.currentTimeMillis()
                val entity = SchoolPostEntity(
                    id = existing?.id ?: 0L,
                    postType = (data["postType"] as? String)?.takeIf { it in SchoolPostType.all }
                        ?: SchoolPostType.PHOTO,
                    category = (data["category"] as? String)?.ifBlank { "General" } ?: "General",
                    title = title,
                    description = (data["description"] as? String).orEmpty(),
                    youtubeUrl = data["youtubeUrl"] as? String,
                    customThumbnailUri = customThumb,
                    photoUrisJson = SchoolPostEntity.encodePhotoUris(localizedPhotos),
                    status = (data["status"] as? String)?.takeIf { it in SchoolPostStatus.all }
                        ?: SchoolPostStatus.PUBLISHED,
                    isFeatured = (data["isFeatured"] as? Boolean) ?: false,
                    createdAtMillis = (data["createdAtMillis"] as? Number)?.toLong()
                        ?: existing?.createdAtMillis ?: now,
                    publishedAtMillis = (data["publishedAtMillis"] as? Number)?.toLong()
                        ?: existing?.publishedAtMillis ?: now,
                    updatedAtMillis = (data["updatedAtMillis"] as? Number)?.toLong() ?: now,
                    remoteId = remoteId,
                    tenantId = TENANT_ID,
                    syncState = "SYNCED",
                    serverUpdatedAtMillis = now,
                    deletedAtMillis = null
                )
                syncDigests["school_posts:$remoteId"] = digest
                database.schoolPostDao().upsert(entity)
            }
            for (removedRemoteId in (previouslyKnownRemote - activeRemoteIds)) {
                byRemoteId[removedRemoteId]?.let { toDelete ->
                    database.schoolPostDao().deleteById(toDelete.id)
                    syncDigests.remove("school_posts:$removedRemoteId")
                }
            }
        }
        knownRemoteIds["school_posts"] = activeRemoteIds
        markSyncCompleted()
    }

    private suspend fun applyInboundSchoolHighlights(docs: List<Pair<String, Map<String, Any>>>) {
        val currentList = database.schoolHighlightDao().getAllHighlightsOnce()
        val byRemoteId = currentList.associateBy { it.remoteId }
        val activeRemoteIds = docs.map { it.first }.toSet()
        val previouslyKnownRemote = knownRemoteIds["school_highlights"].orEmpty()

        withRemoteGuard {
            for ((remoteId, data) in docs) {
                val digest = computeDigest("school_highlights", remoteId, data)
                if (syncDigests["school_highlights:$remoteId"] == digest) continue
                val existing = byRemoteId[remoteId]
                val youtubeShortsUrl = (data["youtubeShortsUrl"] as? String)?.trim().orEmpty()
                if (youtubeShortsUrl.isEmpty()) continue

                val now = System.currentTimeMillis()
                val publishedAt = (data["publishedAtMillis"] as? Number)?.toLong()
                    ?: existing?.publishedAtMillis ?: now
                val expiresAt = (data["expiresAtMillis"] as? Number)?.toLong()
                    ?: SchoolHighlightEntity.computeExpiryMillis(publishedAt)

                val entity = SchoolHighlightEntity(
                    id = existing?.id ?: 0L,
                    title = (data["title"] as? String).orEmpty(),
                    youtubeShortsUrl = youtubeShortsUrl,
                    status = (data["status"] as? String)?.takeIf { it in SchoolPostStatus.all }
                        ?: SchoolPostStatus.PUBLISHED,
                    createdAtMillis = (data["createdAtMillis"] as? Number)?.toLong()
                        ?: existing?.createdAtMillis ?: publishedAt,
                    publishedAtMillis = publishedAt,
                    expiresAtMillis = expiresAt,
                    remoteId = remoteId,
                    tenantId = TENANT_ID,
                    syncState = "SYNCED",
                    serverUpdatedAtMillis = now,
                    deletedAtMillis = null
                )
                syncDigests["school_highlights:$remoteId"] = digest
                database.schoolHighlightDao().upsert(entity)
            }
            for (removedRemoteId in (previouslyKnownRemote - activeRemoteIds)) {
                byRemoteId[removedRemoteId]?.let { toDelete ->
                    database.schoolHighlightDao().deleteById(toDelete.id)
                    syncDigests.remove("school_highlights:$removedRemoteId")
                }
            }
        }
        knownRemoteIds["school_highlights"] = activeRemoteIds
        markSyncCompleted()
    }

    // =========================================================================
    // Outbound Observers (Android Room -> Firestore)
    // =========================================================================

    private fun observeOutboundChanges(db: FirebaseFirestore) {
        // 1. School Profile
        scope.launch {
            database.schoolProfileDao().observeProfile().collect { profile ->
                if (profile == null || isApplyingRemote.get()) return@collect
                val remoteId = profile.remoteId.ifBlank {
                    SyncMetadataDefaults.deterministicRemoteId("school_profile", 1L)
                }
                val payload = buildCleanMap(
                    "schoolName" to profile.schoolName,
                    "shortName" to profile.shortName,
                    "motto" to profile.motto,
                    "addressLine" to profile.addressLine,
                    "emblemMark" to profile.emblemMark,
                    "poweredBy" to profile.poweredBy,
                    "welcomeIntroduction" to profile.welcomeIntroduction,
                    "mediumAndGradesNote" to profile.mediumAndGradesNote,
                    "mapCoordinatesOrQuery" to profile.mapCoordinatesOrQuery,
                    "phone" to profile.phone,
                    "email" to profile.email,
                    "website" to profile.website,
                    "officeHours" to profile.officeHours,
                    "principalName" to profile.principalName,
                    "history" to profile.history,
                    "vision" to profile.vision,
                    "mission" to profile.mission,
                    "affiliationNumber" to profile.affiliationNumber,
                    "profileImageUri" to toPortableImageUri(profile.profileImageUri),
                    "aboutImageUri" to toPortableImageUri(profile.aboutImageUri),
                    "updatedAtMillis" to profile.updatedAtMillis,
                    "remoteId" to remoteId,
                    "tenantId" to TENANT_ID,
                    "syncState" to "SYNCED"
                )
                pushDocument(db, "school_profile", remoteId, payload)
            }
        }

        // 2. Campus Hero Photos
        scope.launch {
            database.campusHeroPhotoDao().observeAllForManagement().collect { list ->
                if (isApplyingRemote.get()) return@collect
                syncCollectionList(
                    db = db,
                    collection = "campus_hero_photos",
                    items = list,
                    remoteIdSelector = { it.remoteId }
                ) { item ->
                    val portableUri = toPortableImageUri(item.imageUri) ?: item.imageUri
                    buildCleanMap(
                        "imageUri" to portableUri,
                        "caption" to item.caption,
                        "displayOrder" to item.displayOrder,
                        "isVerified" to item.isVerified,
                        "isPublished" to item.isPublished,
                        "isApproved" to item.isApproved,
                        "approvalStatus" to item.approvalStatus,
                        "addedAtMillis" to item.addedAtMillis,
                        "remoteId" to item.remoteId,
                        "tenantId" to TENANT_ID,
                        "syncState" to "SYNCED",
                        "deletedAtMillis" to item.deletedAtMillis
                    )
                }
            }
        }

        // 3. Academic Info
        scope.launch {
            database.academicInfoDao().observeAll().collect { list ->
                if (isApplyingRemote.get()) return@collect
                syncCollectionList(
                    db = db,
                    collection = "academic_info",
                    items = list,
                    remoteIdSelector = { it.remoteId }
                ) { item ->
                    buildCleanMap(
                        "title" to item.title,
                        "category" to item.category,
                        "description" to item.description,
                        "displayOrder" to item.displayOrder,
                        "isVerified" to item.isVerified,
                        "isPublished" to item.isPublished,
                        "isApproved" to item.isApproved,
                        "approvalStatus" to item.approvalStatus,
                        "updatedAtMillis" to item.updatedAtMillis,
                        "remoteId" to item.remoteId,
                        "tenantId" to TENANT_ID,
                        "syncState" to "SYNCED",
                        "deletedAtMillis" to item.deletedAtMillis
                    )
                }
            }
        }

        // 4. Higher Secondary Courses
        scope.launch {
            database.higherSecondaryCourseDao().observeAll().collect { list ->
                if (isApplyingRemote.get()) return@collect
                syncCollectionList(
                    db = db,
                    collection = "higher_secondary_courses",
                    items = list,
                    remoteIdSelector = { it.remoteId }
                ) { item ->
                    buildCleanMap(
                        "streamTitle" to item.streamTitle,
                        "groupCode" to item.groupCode,
                        "subjectsSummary" to item.subjectsSummary.ifBlank { "Official Curriculum" },
                        "mediumOfInstruction" to item.mediumOfInstruction.ifBlank { "English Medium" },
                        "classLevelLabel" to item.classLevelLabel,
                        "eligibilityNote" to item.eligibilityNote,
                        "displayOrder" to item.displayOrder,
                        "isVerified" to item.isVerified,
                        "isPublished" to item.isPublished,
                        "isApproved" to item.isApproved,
                        "approvalStatus" to item.approvalStatus,
                        "remoteId" to item.remoteId,
                        "tenantId" to TENANT_ID,
                        "syncState" to "SYNCED",
                        "deletedAtMillis" to item.deletedAtMillis
                    )
                }
            }
        }

        // 5. Facilities
        scope.launch {
            database.facilityDao().observeAll().collect { list ->
                if (isApplyingRemote.get()) return@collect
                syncCollectionList(
                    db = db,
                    collection = "facilities",
                    items = list,
                    remoteIdSelector = { it.remoteId }
                ) { item ->
                    buildCleanMap(
                        "name" to item.name,
                        "category" to item.category,
                        "description" to item.description,
                        "capacityOrSpec" to item.capacityOrSpec,
                        "displayOrder" to item.displayOrder,
                        "imageUri" to toPortableImageUri(item.imageUri),
                        "isVerified" to item.isVerified,
                        "isPublished" to item.isPublished,
                        "isApproved" to item.isApproved,
                        "approvalStatus" to item.approvalStatus,
                        "remoteId" to item.remoteId,
                        "tenantId" to TENANT_ID,
                        "syncState" to "SYNCED",
                        "deletedAtMillis" to item.deletedAtMillis
                    )
                }
            }
        }

        // 6. Transport Routes
        scope.launch {
            database.transportDao().observeAll().collect { list ->
                if (isApplyingRemote.get()) return@collect
                syncCollectionList(
                    db = db,
                    collection = "transport_routes",
                    items = list,
                    remoteIdSelector = { it.remoteId }
                ) { item ->
                    buildCleanMap(
                        "routeNumber" to item.routeNumber,
                        "routeName" to item.routeName,
                        "stopsSummary" to item.stopsSummary,
                        "morningTiming" to item.morningTiming,
                        "afternoonTiming" to item.afternoonTiming,
                        "coordinatorContact" to item.coordinatorContact,
                        "displayOrder" to item.displayOrder,
                        "isVerified" to item.isVerified,
                        "isPublished" to item.isPublished,
                        "isApproved" to item.isApproved,
                        "approvalStatus" to item.approvalStatus,
                        "remoteId" to item.remoteId,
                        "tenantId" to TENANT_ID,
                        "syncState" to "SYNCED",
                        "deletedAtMillis" to item.deletedAtMillis
                    )
                }
            }
        }

        // 7. News Items
        scope.launch {
            database.newsDao().observeAll().collect { list ->
                if (isApplyingRemote.get()) return@collect
                syncCollectionList(
                    db = db,
                    collection = "news_items",
                    items = list,
                    remoteIdSelector = { it.remoteId }
                ) { item ->
                    buildCleanMap(
                        "title" to item.title,
                        "summary" to item.summary,
                        "fullBody" to item.fullBody,
                        "publishedDateText" to item.publishedDateText,
                        "publishedAtMillis" to item.publishedAtMillis,
                        "isFeatured" to item.isFeatured,
                        "imageUri" to toPortableImageUri(item.imageUri),
                        "displayOrder" to item.displayOrder,
                        "isVerified" to item.isVerified,
                        "isPublished" to item.isPublished,
                        "isApproved" to item.isApproved,
                        "approvalStatus" to item.approvalStatus,
                        "remoteId" to item.remoteId,
                        "tenantId" to TENANT_ID,
                        "syncState" to "SYNCED",
                        "deletedAtMillis" to item.deletedAtMillis
                    )
                }
            }
        }

        // 8. Announcements
        scope.launch {
            database.announcementDao().observeAll().collect { list ->
                if (isApplyingRemote.get()) return@collect
                syncCollectionList(
                    db = db,
                    collection = "announcements",
                    items = list,
                    remoteIdSelector = { it.remoteId }
                ) { item ->
                    buildCleanMap(
                        "title" to item.title,
                        "message" to item.message,
                        "priorityLabel" to item.priorityLabel,
                        "issuedDateText" to item.issuedDateText,
                        "issuedAtMillis" to item.issuedAtMillis,
                        "imageUri" to toPortableImageUri(item.imageUri),
                        "displayOrder" to item.displayOrder,
                        "isVerified" to item.isVerified,
                        "isPublished" to item.isPublished,
                        "isApproved" to item.isApproved,
                        "approvalStatus" to item.approvalStatus,
                        "remoteId" to item.remoteId,
                        "tenantId" to TENANT_ID,
                        "syncState" to "SYNCED",
                        "deletedAtMillis" to item.deletedAtMillis
                    )
                }
            }
        }

        // 9. Gallery Albums
        scope.launch {
            database.galleryDao().observeAlbumsWithMedia().collect { list ->
                if (isApplyingRemote.get()) return@collect
                val albums = list.map { it.album }
                syncCollectionList(
                    db = db,
                    collection = "gallery_albums",
                    items = albums,
                    remoteIdSelector = { it.remoteId }
                ) { item ->
                    buildCleanMap(
                        "title" to item.title,
                        "category" to item.category,
                        "description" to item.description,
                        "eventDateText" to item.eventDateText,
                        "coverMediaUri" to toPortableImageUri(item.coverMediaUri),
                        "createdAtMillis" to item.createdAtMillis,
                        "displayOrder" to item.displayOrder,
                        "isVerified" to item.isVerified,
                        "isPublished" to item.isPublished,
                        "isApproved" to item.isApproved,
                        "approvalStatus" to item.approvalStatus,
                        "remoteId" to item.remoteId,
                        "tenantId" to TENANT_ID,
                        "syncState" to "SYNCED",
                        "deletedAtMillis" to item.deletedAtMillis
                    )
                }
            }
        }

        // 10. Gallery Media
        scope.launch {
            database.galleryDao().observeAllMedia().collect { list ->
                if (isApplyingRemote.get()) return@collect
                val albumsById = database.galleryDao().observeAlbumsWithMedia().first()
                    .associate { it.album.id to it.album.remoteId }
                syncCollectionList(
                    db = db,
                    collection = "gallery_media",
                    items = list,
                    remoteIdSelector = { it.remoteId }
                ) { item ->
                    val portableUri = toPortableImageUri(item.mediaUri) ?: item.mediaUri
                    val albumRemoteId = albumsById[item.albumId]?.takeIf { it.isNotBlank() }
                        ?: SyncMetadataDefaults.deterministicRemoteId("gallery_albums", item.albumId)
                    buildCleanMap(
                        "albumRemoteId" to albumRemoteId,
                        "title" to item.title,
                        "caption" to item.caption,
                        "mediaUri" to portableUri,
                        "mediaType" to if (item.mediaType in setOf("IMAGE", "YOUTUBE")) item.mediaType else "IMAGE",
                        "displayOrder" to item.displayOrder,
                        "addedAtMillis" to item.addedAtMillis,
                        "isVerified" to item.isVerified,
                        "isPublished" to item.isPublished,
                        "isApproved" to item.isApproved,
                        "approvalStatus" to item.approvalStatus,
                        "remoteId" to item.remoteId,
                        "tenantId" to TENANT_ID,
                        "syncState" to "SYNCED",
                        "deletedAtMillis" to item.deletedAtMillis
                    )
                }
            }
        }

        // 11. Social Media Links
        scope.launch {
            database.socialMediaDao().observeAll().collect { list ->
                if (isApplyingRemote.get()) return@collect
                syncCollectionList(
                    db = db,
                    collection = "social_media_links",
                    items = list,
                    remoteIdSelector = { it.remoteId }
                ) { item ->
                    if (item.url.isBlank()) return@syncCollectionList emptyMap()
                    buildCleanMap(
                        "platformName" to item.platformName,
                        "handleOrLabel" to item.handleOrLabel.ifBlank { item.platformName },
                        "url" to item.url,
                        "displayOrder" to item.displayOrder,
                        "isVerified" to item.isVerified,
                        "isPublished" to item.isPublished,
                        "isApproved" to item.isApproved,
                        "approvalStatus" to item.approvalStatus,
                        "remoteId" to item.remoteId,
                        "tenantId" to TENANT_ID,
                        "syncState" to "SYNCED",
                        "deletedAtMillis" to item.deletedAtMillis
                    )
                }
            }
        }

        // 12. School Posts
        scope.launch {
            database.schoolPostDao().observeAllPosts().collect { list ->
                if (isApplyingRemote.get()) return@collect
                syncCollectionList(
                    db = db,
                    collection = "school_posts",
                    items = list,
                    remoteIdSelector = { it.remoteId }
                ) { item ->
                    val offloadedPhotos = item.photoUris()
                        .take(SchoolPostEntity.MAX_PHOTO_COUNT)
                        .mapIndexedNotNull { idx, uri ->
                            uploadOrOffloadPostPhoto(db, item.remoteId, idx, uri)
                        }
                    buildCleanMap(
                        "postType" to item.postType,
                        "category" to item.category,
                        "title" to item.title,
                        "description" to item.description,
                        "youtubeUrl" to item.youtubeUrl?.takeIf { it.isNotBlank() },
                        "customThumbnailUri" to toPortableImageUri(item.customThumbnailUri),
                        "photoUris" to offloadedPhotos,
                        "status" to item.status,
                        "isFeatured" to item.isFeatured,
                        "createdAtMillis" to item.createdAtMillis,
                        "publishedAtMillis" to (item.publishedAtMillis ?: item.createdAtMillis),
                        "updatedAtMillis" to item.updatedAtMillis,
                        "remoteId" to item.remoteId,
                        "tenantId" to TENANT_ID,
                        "syncState" to "SYNCED",
                        "deletedAtMillis" to item.deletedAtMillis
                    )
                }
            }
        }

        // 13. School Highlights
        scope.launch {
            database.schoolHighlightDao().observeAllHighlights().collect { list ->
                if (isApplyingRemote.get()) return@collect
                syncCollectionList(
                    db = db,
                    collection = "school_highlights",
                    items = list,
                    remoteIdSelector = { it.remoteId }
                ) { item ->
                    buildCleanMap(
                        "title" to item.title,
                        "youtubeShortsUrl" to item.youtubeShortsUrl,
                        "status" to item.status,
                        "createdAtMillis" to item.createdAtMillis,
                        "publishedAtMillis" to item.publishedAtMillis,
                        "expiresAtMillis" to item.expiresAtMillis,
                        "remoteId" to item.remoteId,
                        "tenantId" to TENANT_ID,
                        "syncState" to "SYNCED",
                        "deletedAtMillis" to item.deletedAtMillis
                    )
                }
            }
        }
    }

        suspend fun <T> syncCollectionList(
        db: FirebaseFirestore,
        collection: String,
        items: List<T>,
        remoteIdSelector: (T) -> String,
        payloadBuilder: suspend (T) -> Map<String, Any?>
    ) {
        val currentRemoteIds = mutableSetOf<String>()
        for (item in items) {
            val remoteId = remoteIdSelector(item)
            if (remoteId.isBlank()) continue
            val payload = payloadBuilder(item)
            if (payload.isEmpty()) continue
            currentRemoteIds.add(remoteId)
            pushDocument(db, collection, remoteId, payload)
        }

        val previousIds = knownLocalRemoteIds.put(collection, currentRemoteIds).orEmpty()
        val deletedIds = previousIds - currentRemoteIds
        for (deletedRemoteId in deletedIds) {
            deleteRemoteDocument(db, collection, deletedRemoteId)
        }
    }

    private suspend fun pushDocument(
        db: FirebaseFirestore,
        collection: String,
        remoteId: String,
        payloadWithoutServerTimestamp: Map<String, Any?>
    ) {
        val digest = computeDigest(collection, remoteId, payloadWithoutServerTimestamp)
        val digestKey = "$collection:$remoteId"
        if (syncDigests[digestKey] == digest) return

        ensurePinAdminSessionSynced(db)
        val path = "schools/$TENANT_ID/$collection/$remoteId"
        val writePayload = HashMap<String, Any?>(payloadWithoutServerTimestamp).apply {
            put("serverUpdatedAt", FieldValue.serverTimestamp())
        }

        runCatching {
            db.document(path).set(writePayload).await()
            syncDigests[digestKey] = digest
            markSyncCompleted()
        }.onFailure { err ->
            if (err is CancellationException) throw err
            handleFirestoreError(err, OperationType.WRITE, path)
            _status.value = _status.value.copy(lastError = err.message)
        }
    }

    private suspend fun deleteRemoteDocument(
        db: FirebaseFirestore,
        collection: String,
        remoteId: String
    ) {
        ensurePinAdminSessionSynced(db)
        val path = "schools/$TENANT_ID/$collection/$remoteId"
        runCatching {
            db.document(path).delete().await()
            syncDigests.remove("$collection:$remoteId")
            markSyncCompleted()
        }.onFailure { err ->
            if (err is CancellationException) throw err
            handleFirestoreError(err, OperationType.DELETE, path)
        }
    }

    // =========================================================================
    // Cross-Platform Image Portability & Multi-Image Firebase Project Offload
    // Unified cap: MAX_MEDIA_URI_CHARS = 140_000 (matching firestore.rules 140000)
    // =========================================================================

    private suspend fun uploadOrOffloadPostPhoto(
        db: FirebaseFirestore,
        postRemoteId: String,
        index: Int,
        rawUri: String?
    ): String? {
        if (rawUri.isNullOrBlank()) return null
        val trimmed = rawUri.trim()
        if (trimmed.startsWith("http://") ||
            trimmed.startsWith("https://") ||
            trimmed.startsWith(FIREBASE_MEDIA_SCHEME)
        ) {
            return trimmed
        }

        val portableDataUri = toPortableImageUri(trimmed, maxBinaryBytes = MAX_MULTI_PHOTO_BINARY_BYTES)
            ?: return null

        // 1. Attempt upload to Firebase Storage bucket if provisioned
        val storageUrl = tryUploadDataUriToFirebaseStorage(
            objectPath = "schools/$TENANT_ID/posts/${postRemoteId}_$index.jpg",
            dataUri = portableDataUri
        )
        if (storageUrl != null) {
            return storageUrl
        }

        // 2. Fallback: Offload to dedicated Firebase project media document so school_posts stays < 2 KB
        val safePostId = postRemoteId.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(42)
        val mediaDocId = "${POST_PHOTO_PREFIX}${safePostId}_$index"
        val now = System.currentTimeMillis()
        val mediaPayload = buildCleanMap(
            "albumRemoteId" to SyncMetadataDefaults.deterministicRemoteId("gallery_albums", 1L),
            "title" to "Social Post Media",
            "caption" to "Post $safePostId #$index",
            "mediaUri" to portableDataUri,
            "mediaType" to "IMAGE",
            "displayOrder" to index,
            "addedAtMillis" to now,
            "isVerified" to true,
            "isPublished" to true,
            "isApproved" to true,
            "approvalStatus" to ApprovalStatus.APPROVED,
            "remoteId" to mediaDocId,
            "tenantId" to TENANT_ID,
            "syncState" to "SYNCED",
            "deletedAtMillis" to null
        )
        pushDocument(db, "gallery_media", mediaDocId, mediaPayload)
        return "$FIREBASE_MEDIA_SCHEME$mediaDocId"
    }

    private suspend fun resolveInboundPostPhotoUri(
        remoteId: String,
        index: Int,
        uriStr: String?
    ): String? {
        if (uriStr.isNullOrBlank()) return null
        val trimmed = uriStr.trim()
        if (trimmed.startsWith(FIREBASE_MEDIA_SCHEME)) {
            val mediaDocId = trimmed.removePrefix(FIREBASE_MEDIA_SCHEME).trim()
            val db = firestore ?: return null
            val resolvedDataUri = runCatching {
                val snap = db.document("schools/$TENANT_ID/gallery_media/$mediaDocId").get().await()
                snap.getString("mediaUri")
            }.getOrNull() ?: return null
            return fromPortableImageUri(
                remoteId = remoteId,
                fieldTag = "post_photo_$index",
                uriStr = resolvedDataUri
            )
        }
        return fromPortableImageUri(
            remoteId = remoteId,
            fieldTag = "post_photo_$index",
            uriStr = trimmed
        )
    }

    private suspend fun tryUploadDataUriToFirebaseStorage(
        objectPath: String,
        dataUri: String
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val commaIdx = dataUri.indexOf(',')
            if (commaIdx <= 0) return@runCatching null
            val bytes = Base64.decode(dataUri.substring(commaIdx + 1), Base64.DEFAULT)
            val encodedName = java.net.URLEncoder.encode(objectPath, "UTF-8")
            val endpoint =
                "https://firebasestorage.googleapis.com/v0/b/$STORAGE_BUCKET/o?uploadType=media&name=$encodedName"
            val conn = (java.net.URL(endpoint).openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4000
                readTimeout = 4000
                doOutput = true
                setRequestProperty("Content-Type", "image/jpeg")
            }
            conn.outputStream.use { it.write(bytes) }
            if (conn.responseCode in 200..299) {
                "https://firebasestorage.googleapis.com/v0/b/$STORAGE_BUCKET/o/$encodedName?alt=media"
            } else {
                null
            }
        }.getOrNull()
    }

    private fun toPortableImageUri(
        uriStr: String?,
        maxBinaryBytes: Int = MAX_SINGLE_IMAGE_BINARY_BYTES
    ): String? {
        if (uriStr.isNullOrBlank()) return null
        val trimmed = uriStr.trim()
        if (trimmed.startsWith("http://") ||
            trimmed.startsWith("https://")
        ) {
            return trimmed.take(MAX_MEDIA_URI_CHARS)
        }
        if (trimmed.startsWith("data:image/")) {
            return if (trimmed.length <= MAX_MEDIA_URI_CHARS) trimmed else null
        }
        return runCatching {
            val localFile = when {
                trimmed.startsWith("file://") -> Uri.parse(trimmed).path?.let { File(it) }
                trimmed.startsWith("/") -> File(trimmed)
                else -> null
            }
            if (localFile == null || !localFile.exists() || !localFile.canRead()) {
                return@runCatching null
            }
            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath) ?: return@runCatching null
            val maxDim = 720
            val ratio = minOf(
                1f,
                maxDim.toFloat() / bitmap.width.coerceAtLeast(1),
                maxDim.toFloat() / bitmap.height.coerceAtLeast(1)
            )
            val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
            val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
            val scaled = if (ratio < 1f) {
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            } else {
                bitmap
            }
            val out = ByteArrayOutputStream()
            var quality = 72
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            while (out.size() > maxBinaryBytes && quality > 25) {
                out.reset()
                quality -= 10
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            val dataUri = "data:image/jpeg;base64,$base64"
            if (dataUri.length <= MAX_MEDIA_URI_CHARS) dataUri else null
        }.getOrNull()
    }

    private fun fromPortableImageUri(
        remoteId: String,
        fieldTag: String,
        uriStr: String?
    ): String? {
        if (uriStr.isNullOrBlank()) return null
        val trimmed = uriStr.trim()
        if (!trimmed.startsWith("data:image/")) {
            return trimmed
        }
        return runCatching {
            val commaIndex = trimmed.indexOf(',')
            if (commaIndex <= 0) return@runCatching null
            val base64Part = trimmed.substring(commaIndex + 1)
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            val dir = File(context.filesDir, "chevalier_media").apply { mkdirs() }
            val safeId = remoteId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val targetFile = File(dir, "cloud_${safeId}_${fieldTag}.jpg")
            FileOutputStream(targetFile).use { fos ->
                fos.write(bytes)
                fos.flush()
            }
            Uri.fromFile(targetFile).toString()
        }.getOrDefault(trimmed)
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    private suspend fun withRemoteGuard(block: suspend () -> Unit) {
        isApplyingRemote.set(true)
        try {
            block()
        } finally {
            isApplyingRemote.set(false)
        }
    }

    private fun buildCleanMap(vararg pairs: Pair<String, Any?>): Map<String, Any?> {
        val result = LinkedHashMap<String, Any?>()
        for ((key, value) in pairs) {
            if (key == "deletedAtMillis") {
                result[key] = value
            } else if (value != null) {
                result[key] = value
            }
        }
        return result
    }

    private fun computeDigest(
        collection: String,
        remoteId: String,
        data: Map<String, Any?>
    ): String {
        val sortedKeys = data.keys.filter { it != "serverUpdatedAt" }.sorted()
        val sb = StringBuilder(collection).append(':').append(remoteId)
        for (key in sortedKeys) {
            val v = data[key]
            if (v != null) {
                sb.append('|').append(key).append('=').append(v.toString())
            }
        }
        return sb.toString().hashCode().toString()
    }

    private fun markSyncCompleted() {
        _status.value = _status.value.copy(
            lastSyncedAtMillis = System.currentTimeMillis(),
            lastError = null
        )
    }

    private fun isRobolectricRuntime(): Boolean =
        Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

    companion object {
        private const val TAG = "CloudSyncManager"
        const val TENANT_ID = SyncMetadataDefaults.DEFAULT_TENANT_ID
        const val PIN_GATE_DOC_ID = "pin_session_v1_verified"
        const val PIN_GATE_EMAIL = "verified-pin-gate@chevalieracademy.edu.in"
        const val STORAGE_BUCKET = "gen-lang-client-0028096876.firebasestorage.app"
        const val FIREBASE_MEDIA_SCHEME = "firebase-media://"
        const val POST_PHOTO_PREFIX = "post_photo_"
        const val MAX_MEDIA_URI_CHARS = 140_000
        const val MAX_SINGLE_IMAGE_BINARY_BYTES = 100_000
        const val MAX_MULTI_PHOTO_BINARY_BYTES = 65_000
    }
}

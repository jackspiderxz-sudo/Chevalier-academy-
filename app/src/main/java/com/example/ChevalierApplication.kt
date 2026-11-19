package com.example

import android.app.Application
import com.example.core.notifications.LocalAlertNotifier
import com.example.data.backend.BackendConfigDetector
import com.example.data.backend.CentralBackendStatus
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.ChevalierDatabase
import com.example.data.media.MediaStudioManager
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ManagementRepository
import com.example.data.repository.SchoolRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(application: Application) {
    val database: ChevalierDatabase by lazy { ChevalierDatabase.getInstance(application) }
    val schoolRepository: SchoolRepository by lazy { SchoolRepository.fromDatabase(database) }
    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(application)
    }
    val localAlertNotifier: LocalAlertNotifier by lazy { LocalAlertNotifier(application) }
    val centralBackendStatus: CentralBackendStatus by lazy {
        BackendConfigDetector.inspect(application)
    }
    val mediaStudioManager: MediaStudioManager by lazy { MediaStudioManager(application) }
    val managementRepository: ManagementRepository by lazy {
        ManagementRepository(
            database = database,
            mediaStudioManager = mediaStudioManager,
            preferencesRepository = userPreferencesRepository,
            localAlertNotifier = localAlertNotifier
        )
    }
    val backupRestoreManager: BackupRestoreManager by lazy {
        BackupRestoreManager(application, database, userPreferencesRepository)
    }
}

class ChevalierApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            container.schoolRepository.ensureVerifiedProfileSeeded()
        }
    }
}

package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.data.local.ChevalierDatabase
import com.example.data.local.entity.AcademicInfoEntity
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.CampusHeroPhotoEntity
import com.example.data.local.entity.FacilityEntity
import com.example.data.local.entity.GalleryAlbumWithMedia
import com.example.data.local.entity.GalleryMediaEntity
import com.example.data.local.entity.HigherSecondaryCourseEntity
import com.example.data.local.entity.NewsItemEntity
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.local.entity.SocialMediaLinkEntity
import com.example.data.local.entity.TransportRouteEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserPreferencesState
import com.example.data.repository.SchoolRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PreferencesUiState {
    data object Loading : PreferencesUiState
    data class Ready(val state: UserPreferencesState) : PreferencesUiState
}

class MainViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val preferencesUiState: StateFlow<PreferencesUiState> = preferencesRepository.preferencesFlow
        .map { PreferencesUiState.Ready(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PreferencesUiState.Loading
        )

    fun selectLanguage(language: AppLanguage, markOnboarded: Boolean = false) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language, markSelectedOnLaunch = markOnboarded)
        }
    }

    fun selectThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    class Factory(
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(preferencesRepository) as T
        }
    }
}

class SchoolDataViewModel(
    schoolRepository: SchoolRepository
) : ViewModel() {

    val schoolProfile: StateFlow<SchoolProfileEntity> = schoolRepository.schoolProfile
        .map { it ?: ChevalierDatabase.verifiedDefaultProfile() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ChevalierDatabase.verifiedDefaultProfile()
        )

    val campusHeroPhotos: StateFlow<List<CampusHeroPhotoEntity>> =
        schoolRepository.campusHeroPhotos
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val academicInfo: StateFlow<List<AcademicInfoEntity>> = schoolRepository.academicInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val higherSecondaryCourses: StateFlow<List<HigherSecondaryCourseEntity>> =
        schoolRepository.higherSecondaryCourses
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val facilities: StateFlow<List<FacilityEntity>> = schoolRepository.facilities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val transportRoutes: StateFlow<List<TransportRouteEntity>> = schoolRepository.transportRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val newsItems: StateFlow<List<NewsItemEntity>> = schoolRepository.newsItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val announcements: StateFlow<List<AnnouncementEntity>> = schoolRepository.announcements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val galleryAlbums: StateFlow<List<GalleryAlbumWithMedia>> =
        schoolRepository.galleryAlbumsWithMedia
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val galleryMedia: StateFlow<List<GalleryMediaEntity>> = schoolRepository.galleryMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val socialMediaLinks: StateFlow<List<SocialMediaLinkEntity>> = schoolRepository.socialMediaLinks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    class Factory(
        private val schoolRepository: SchoolRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SchoolDataViewModel(schoolRepository) as T
        }
    }
}

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
import com.example.data.local.entity.SchoolHighlightEntity
import com.example.data.local.entity.SchoolPostEntity
import com.example.data.local.entity.SchoolPostStatus
import com.example.data.local.entity.SchoolProfileEntity
import com.example.data.local.entity.SocialMediaLinkEntity
import com.example.data.local.entity.TransportRouteEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserPreferencesState
import com.example.data.repository.SchoolRepository
import com.example.data.repository.SocialFeedRepository
import com.example.data.v2.AcademicsV2ContentRepository
import com.example.data.v2.AcademicsV2Snapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

sealed interface PreferencesUiState {
    data object Loading : PreferencesUiState
    data class Ready(val state: UserPreferencesState) : PreferencesUiState
}

class MainViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val languageUpdateSeq = AtomicLong(0L)
    private val pendingLanguageOverride = MutableStateFlow<Triple<Long, AppLanguage, Boolean>?>(null)

    val preferencesUiState: StateFlow<PreferencesUiState> = combine(
        preferencesRepository.preferencesFlow,
        pendingLanguageOverride
    ) { prefs, pending ->
        val effective = if (pending != null) {
            prefs.copy(
                selectedLanguage = pending.second,
                isLanguageSelectedOnLaunch = prefs.isLanguageSelectedOnLaunch || pending.third
            )
        } else {
            prefs
        }
        PreferencesUiState.Ready(effective)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PreferencesUiState.Loading
    )

    fun selectLanguage(language: AppLanguage, markOnboarded: Boolean = false) {
        val seq = languageUpdateSeq.incrementAndGet()
        pendingLanguageOverride.value = Triple(seq, language, markOnboarded)
        viewModelScope.launch {
            try {
                preferencesRepository.setLanguage(language, markSelectedOnLaunch = markOnboarded)
                if (languageUpdateSeq.get() == seq) {
                    preferencesRepository.preferencesFlow.first { state ->
                        state.selectedLanguage == language &&
                            (!markOnboarded || state.isLanguageSelectedOnLaunch)
                    }
                }
            } finally {
                pendingLanguageOverride.update { current ->
                    if (current?.first == seq) null else current
                }
            }
        }
    }

    fun selectThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun togglePostLike(postId: Long, postRemoteId: String? = null) {
        viewModelScope.launch {
            preferencesRepository.togglePostLike(postId, postRemoteId)
        }
    }

    fun togglePostBookmark(postId: Long, postRemoteId: String? = null) {
        viewModelScope.launch {
            preferencesRepository.togglePostBookmark(postId, postRemoteId)
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
    schoolRepository: SchoolRepository,
    socialFeedRepository: SocialFeedRepository? = null,
    academicsV2Repository: AcademicsV2ContentRepository? = null,
    preferencesRepository: UserPreferencesRepository? = null
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

    val academyHeroBanner: StateFlow<com.example.data.preferences.AcademyHeroBannerConfig> =
        (preferencesRepository?.academyHeroBannerFlow ?: flowOf(com.example.data.preferences.AcademyHeroBannerConfig()))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = com.example.data.preferences.AcademyHeroBannerConfig()
            )

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

    private val rawPublishedPostsFlow =
        socialFeedRepository?.publishedPosts ?: schoolRepository.publishedSchoolPosts

    private val feedVisibilityFlow =
        preferencesRepository?.feedVisibilityFlow
            ?: flowOf(com.example.data.preferences.FeedVisibilityPreferences())

    val allPublishedSchoolPosts: StateFlow<List<SchoolPostEntity>> = rawPublishedPostsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val publishedSchoolPosts: StateFlow<List<SchoolPostEntity>> = combine(
        rawPublishedPostsFlow,
        feedVisibilityFlow
    ) { posts, visibility ->
        posts.filter { visibility.isVisibleOnHomeFeed(it.remoteId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val publishedAcademyFeedPosts: StateFlow<List<SchoolPostEntity>> = combine(
        rawPublishedPostsFlow,
        feedVisibilityFlow
    ) { posts, visibility ->
        posts
            .filter { visibility.isVisibleOnAcademyFeed(it.remoteId) }
            .sortedWith(
                compareByDescending<SchoolPostEntity> { it.isFeatured }
                    .thenByDescending { it.publishedAtMillis ?: it.createdAtMillis }
                    .thenByDescending { it.id }
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val publishedSchoolHighlights: StateFlow<List<SchoolHighlightEntity>> =
        (socialFeedRepository?.allHighlightsForAdmin?.map { list ->
            list.filter { it.status == SchoolPostStatus.PUBLISHED }
        } ?: schoolRepository.publishedSchoolHighlights)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val academicsV2Snapshot: StateFlow<AcademicsV2Snapshot> =
        (academicsV2Repository?.snapshot ?: flowOf(AcademicsV2Snapshot()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AcademicsV2Snapshot())

    class Factory(
        private val schoolRepository: SchoolRepository,
        private val socialFeedRepository: SocialFeedRepository? = null,
        private val academicsV2Repository: AcademicsV2ContentRepository? = null,
        private val preferencesRepository: UserPreferencesRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SchoolDataViewModel(
                schoolRepository = schoolRepository,
                socialFeedRepository = socialFeedRepository,
                academicsV2Repository = academicsV2Repository,
                preferencesRepository = preferencesRepository
            ) as T
        }
    }
}

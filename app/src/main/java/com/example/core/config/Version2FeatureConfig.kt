package com.example.core.config

import com.example.core.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central availability control for Version 2 Academics Public Page (13 cards)
 * and Admin Management Panel (8 cards + 13-category Academics Management + School Owner Control Center).
 *
 * Defaults to [RELEASE_VERSION_2_ENABLED] = false so unfinished / unreleased Version 2 capabilities
 * are never exposed in the currently released Public Version. When Version 2 is officially released,
 * flip [RELEASE_VERSION_2_ENABLED] to true (or enable via release configuration).
 */
object Version2FeatureConfig {
    const val RELEASE_VERSION_2_ENABLED: Boolean = false

    private val _isVersion2AcademicsEnabled = MutableStateFlow(RELEASE_VERSION_2_ENABLED)
    val isVersion2AcademicsEnabledFlow: StateFlow<Boolean> = _isVersion2AcademicsEnabled.asStateFlow()

    private val _isVersion2AdminPanelEnabled = MutableStateFlow(RELEASE_VERSION_2_ENABLED)
    val isVersion2AdminPanelEnabledFlow: StateFlow<Boolean> = _isVersion2AdminPanelEnabled.asStateFlow()

    val isVersion2AcademicsEnabled: Boolean
        get() = _isVersion2AcademicsEnabled.value

    val isVersion2AdminPanelEnabled: Boolean
        get() = _isVersion2AdminPanelEnabled.value

    val isVersion2Enabled: Boolean
        get() = isVersion2AcademicsEnabled && isVersion2AdminPanelEnabled

    fun setVersion2Enabled(enabled: Boolean) {
        _isVersion2AcademicsEnabled.value = enabled
        _isVersion2AdminPanelEnabled.value = enabled
    }

    fun setAcademicsV2Enabled(enabled: Boolean) {
        _isVersion2AcademicsEnabled.value = enabled
    }

    fun setAdminPanelV2Enabled(enabled: Boolean) {
        _isVersion2AdminPanelEnabled.value = enabled
    }

    fun resetToReleaseDefaults() {
        _isVersion2AcademicsEnabled.value = RELEASE_VERSION_2_ENABLED
        _isVersion2AdminPanelEnabled.value = RELEASE_VERSION_2_ENABLED
    }

    /**
     * Exact localized notice for Version 2 features that are not yet released.
     * Deliberately does not instruct users to update to Version 2 before release.
     */
    fun comingInVersion2Message(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> "Coming in Version 2"
        AppLanguage.TAMIL -> "இந்த வசதி Version 2-ல் விரைவில் கிடைக்கும்."
        AppLanguage.HINDI -> "यह सुविधा Version 2 में जल्द ही उपलब्ध होगी।"
    }
}

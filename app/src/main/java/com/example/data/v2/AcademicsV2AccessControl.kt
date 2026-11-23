package com.example.data.v2

import com.example.data.local.entity.AccountWithPermissions
import com.example.data.local.entity.ManagementRole
import com.example.data.local.entity.ManagementSection
import com.example.data.local.entity.PermissionAction

/**
 * Data-layer RBAC and School Owner authority validator for Version 2 Academics and Admin Management.
 * Always invoked at both repository/action layers and UI route guards so unauthorized staff
 * cannot bypass restrictions by opening routes directly.
 */
object AcademicsV2AccessControl {

    fun roleDisplayLabel(role: String): String = when (role) {
        ManagementRole.MASTER_ADMIN -> "School Owner / Master Admin"
        ManagementRole.SUPER_OWNER -> "Super Owner"
        else -> "Staff"
    }

    fun isSchoolOwner(actor: AccountWithPermissions): Boolean {
        if (!actor.account.isActive) return false
        return actor.account.role == ManagementRole.MASTER_ADMIN ||
            actor.account.role == ManagementRole.SUPER_OWNER
    }

    fun hasCategoryPermission(
        actor: AccountWithPermissions,
        category: AcademicsV2Category,
        action: PermissionAction
    ): Boolean {
        if (!actor.account.isActive) return false
        if (actor.account.role == ManagementRole.MASTER_ADMIN) return true

        val directMatch = actor.permissions.firstOrNull { it.sectionKey == category.permissionKey }
        if (directMatch != null && directMatch.allows(action)) return true

        val legacyMatch = category.legacySection?.let { sec ->
            actor.permissions.firstOrNull { it.sectionKey == sec.key }
        }
        if (legacyMatch != null && legacyMatch.allows(action)) return true

        // Card 06 (News & Announcements) also honors legacy ANNOUNCEMENTS permission
        if (category == AcademicsV2Category.NEWS_ANNOUNCEMENTS) {
            val annMatch = actor.permissions.firstOrNull { it.sectionKey == ManagementSection.ANNOUNCEMENTS.key }
            if (annMatch != null && annMatch.allows(action)) return true
        }

        return false
    }

    fun hasSectionPermission(
        actor: AccountWithPermissions,
        section: ManagementSection,
        action: PermissionAction
    ): Boolean {
        if (!actor.account.isActive) return false
        if (actor.account.role == ManagementRole.MASTER_ADMIN) return true
        val match = actor.permissions.firstOrNull { it.sectionKey == section.key } ?: return false
        return match.allows(action)
    }

    fun canAccessAdminCard(actor: AccountWithPermissions, card: AdminV2Card): Boolean {
        if (!actor.account.isActive) return false
        if (actor.account.role == ManagementRole.MASTER_ADMIN) return true

        return when (card) {
            AdminV2Card.HOME_PAGE -> {
                hasSectionPermission(actor, ManagementSection.LOGO_BANNER, PermissionAction.VIEW) ||
                    hasSectionPermission(actor, ManagementSection.SCHOOL_POSTS, PermissionAction.VIEW) ||
                    hasSectionPermission(actor, ManagementSection.SCHOOL_HIGHLIGHTS, PermissionAction.VIEW) ||
                    hasSectionPermission(actor, ManagementSection.MEDIA_STUDIO, PermissionAction.VIEW)
            }
            AdminV2Card.PARENT -> {
                hasSectionPermission(actor, ManagementSection.ANNOUNCEMENTS, PermissionAction.VIEW) ||
                    hasCategoryPermission(actor, AcademicsV2Category.NEWS_ANNOUNCEMENTS, PermissionAction.VIEW) ||
                    hasCategoryPermission(actor, AcademicsV2Category.CONTACT_SUPPORT_PARENT_PROXY, PermissionAction.VIEW)
            }
            AdminV2Card.ACADEMICS -> {
                AcademicsV2Category.entries.any { hasCategoryPermission(actor, it, PermissionAction.VIEW) }
            }
            AdminV2Card.CONTACT -> {
                hasSectionPermission(actor, ManagementSection.CONTACT_DETAILS, PermissionAction.VIEW)
            }
            AdminV2Card.ADMIN -> {
                hasSectionPermission(actor, ManagementSection.STAFF_MANAGEMENT, PermissionAction.VIEW)
            }
            AdminV2Card.SCHOOL_SETTINGS -> {
                hasSectionPermission(actor, ManagementSection.SCHOOL_PROFILE, PermissionAction.VIEW) ||
                    hasSectionPermission(actor, ManagementSection.SOCIAL_MEDIA, PermissionAction.VIEW) ||
                    hasSectionPermission(actor, ManagementSection.APP_SETTINGS, PermissionAction.VIEW)
            }
            AdminV2Card.PREVIEW_PUBLISH -> {
                AcademicsV2Category.entries.any {
                    hasCategoryPermission(actor, it, PermissionAction.PUBLISH) ||
                        hasCategoryPermission(actor, it, PermissionAction.EDIT)
                } || ManagementSection.entries.any {
                    hasSectionPermission(actor, it, PermissionAction.PUBLISH) ||
                        hasSectionPermission(actor, it, PermissionAction.EDIT)
                }
            }
            AdminV2Card.SCHOOL_OWNER_CONTROL_CENTER -> isSchoolOwner(actor)
        }
    }

    /**
     * Distinguishes normal Admin publishing from School Owner's final approval authority.
     * Exam Results strictly require School Owner authorization and final approval.
     */
    fun canPublishDirectly(
        actor: AccountWithPermissions,
        category: AcademicsV2Category,
        sectionKey: String
    ): Boolean {
        if (!actor.account.isActive) return false
        val section = category.findSection(sectionKey)
        if (section?.requiresOwnerAuthorizationToPublish == true) {
            return isSchoolOwner(actor)
        }
        return hasCategoryPermission(actor, category, PermissionAction.PUBLISH)
    }

    fun requireCategoryPermission(
        actor: AccountWithPermissions,
        category: AcademicsV2Category,
        action: PermissionAction
    ) {
        if (!hasCategoryPermission(actor, category, action)) {
            throw SecurityException(
                "Permission denied: ${actor.account.displayName} lacks ${action.name} permission for ${category.title}."
            )
        }
    }

    fun requireSchoolOwner(actor: AccountWithPermissions, operation: String) {
        if (!isSchoolOwner(actor)) {
            throw SecurityException(
                "School Owner authority required for: $operation."
            )
        }
    }

    private val AcademicsV2Category.Companion.CONTACT_SUPPORT_PARENT_PROXY: AcademicsV2Category
        get() = AcademicsV2Category.TRANSPORT

    private fun com.example.data.local.entity.StaffPermissionEntity.allows(action: PermissionAction): Boolean =
        when (action) {
            PermissionAction.VIEW -> canView
            PermissionAction.CREATE -> canCreate
            PermissionAction.EDIT -> canEdit
            PermissionAction.DELETE -> canDelete
            PermissionAction.PUBLISH -> canPublish
        }
}

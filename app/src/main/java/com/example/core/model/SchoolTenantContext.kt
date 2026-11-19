package com.example.core.model

import com.example.data.local.entity.SchoolProfileEntity

/**
 * Defines tenant identity metadata for Chevalier Academy while keeping the local Room
 * database ready for multi-school partitioning in future cloud-connected phases.
 */
object SchoolTenantContext {
    const val DEFAULT_SCHOOL_ID = "chevalier_academy_dindigul"
    const val DEFAULT_SCHOOL_CODE = "CAMHSS-DGL"
    const val TENANCY_SCHEMA_LEVEL = 1
}

data class SchoolTenantDescriptor(
    val schoolId: String = SchoolTenantContext.DEFAULT_SCHOOL_ID,
    val schoolCode: String = SchoolTenantContext.DEFAULT_SCHOOL_CODE,
    val officialName: String,
    val shortName: String
) {
    companion object {
        fun fromProfile(profile: SchoolProfileEntity): SchoolTenantDescriptor =
            SchoolTenantDescriptor(
                schoolId = SchoolTenantContext.DEFAULT_SCHOOL_ID,
                schoolCode = SchoolTenantContext.DEFAULT_SCHOOL_CODE,
                officialName = profile.schoolName,
                shortName = profile.shortName
            )
    }
}

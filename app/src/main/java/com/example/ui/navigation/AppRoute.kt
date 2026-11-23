package com.example.ui.navigation

object AppRoute {
    const val SPLASH = "splash"
    const val LANGUAGE_SELECTION = "language_selection"
    const val HOME = "home"
    const val PARENT = "parent"
    const val ACADEMICS = "academics"
    const val ACADEMICS_DETAILS = "academics_details"
    const val GALLERY = "gallery"
    const val CONTACT = "contact"
    const val MORE = "more"
    const val ABOUT = "about"
    const val COURSES = "courses"
    const val FACILITIES = "facilities"
    const val TRANSPORT = "transport"
    const val NEWS = "news"
    const val ANNOUNCEMENTS = "announcements"
    const val NEWS_ANNOUNCEMENTS = "news_announcements"
    const val SOCIAL_MEDIA = "social_media"
    const val BOOKMARKS = "bookmarks"
    const val SETTINGS = "settings"
    const val MANAGEMENT_ENTRY = "management_entry"
    const val PARENT_MODULE = "parent_module/{moduleId}"
    const val ACADEMY_CATEGORY = "academy_category/{categoryId}"
    const val ACADEMICS_V2_CATEGORY = "academics_v2_category/{categoryId}"
    const val FACULTY_V2_PROFILE = "faculty_v2_profile/{profileId}"

    fun parentModule(moduleId: String): String = "parent_module/$moduleId"
    fun academyCategory(categoryId: String): String = "academy_category/$categoryId"
    fun academicsV2Category(categoryId: String): String = "academics_v2_category/$categoryId"
    fun facultyV2Profile(profileId: String): String = "faculty_v2_profile/$profileId"

    val primaryTabRoutes = setOf(HOME, PARENT, ACADEMICS, CONTACT)

    val hiddenChromeRoutes = setOf(SPLASH, LANGUAGE_SELECTION, MANAGEMENT_ENTRY)
}

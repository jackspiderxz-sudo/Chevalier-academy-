package com.example.ui.navigation

object AppRoute {
    const val SPLASH = "splash"
    const val LANGUAGE_SELECTION = "language_selection"
    const val HOME = "home"
    const val PARENT = "parent"
    const val ACADEMICS = "academics"
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
    const val SETTINGS = "settings"
    const val MANAGEMENT_ENTRY = "management_entry"
    const val PARENT_MODULE = "parent_module/{moduleId}"

    fun parentModule(moduleId: String): String = "parent_module/$moduleId"

    val primaryTabRoutes = setOf(HOME, PARENT, ACADEMICS, CONTACT)

    val hiddenChromeRoutes = setOf(SPLASH, LANGUAGE_SELECTION, MANAGEMENT_ENTRY)
}

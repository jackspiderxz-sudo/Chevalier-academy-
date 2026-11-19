package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.core.i18n.LocalAppStrings
import com.example.core.model.AppLanguage
import com.example.core.model.AppThemeMode
import com.example.data.local.entity.SchoolProfileEntity
import com.example.ui.components.ChevalierTopBar
import com.example.ui.screens.AboutSchoolScreen
import com.example.ui.screens.AcademicsScreen
import com.example.ui.screens.AnnouncementsScreen
import com.example.ui.screens.ContactScreen
import com.example.ui.screens.FacilitiesScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.HigherSecondaryCoursesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LanguageSelectionScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.NewsAnnouncementsScreen
import com.example.ui.screens.NewsScreen
import com.example.ui.screens.ParentDashboardScreen
import com.example.ui.screens.ParentModuleDetailScreen
import com.example.ui.screens.ParentModuleId
import com.example.ui.screens.ParentPortalTopBar
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SocialMediaScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TransportScreen
import com.example.ui.screens.parentModuleTitle
import com.example.ui.screens.management.ManagementPortalScreen
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ManagementViewModel
import com.example.ui.viewmodel.PreferencesUiState
import com.example.ui.viewmodel.SchoolDataViewModel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class TopLevelNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private data class DrawerNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun ChevalierAppShell(
    mainViewModel: MainViewModel,
    schoolDataViewModel: SchoolDataViewModel,
    managementViewModel: ManagementViewModel,
    currentLanguage: AppLanguage,
    currentThemeMode: AppThemeMode
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppRoute.SPLASH
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var parentSearchActive by rememberSaveable { mutableStateOf(false) }
    var parentSearchQuery by rememberSaveable { mutableStateOf("") }

    val isDarkTheme = when (currentThemeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val bottomBarContainerColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.surface
    } else {
        brand.warmIvory
    }

    val schoolProfile by schoolDataViewModel.schoolProfile.collectAsStateWithLifecycle()
    val preferencesUiState by mainViewModel.preferencesUiState.collectAsStateWithLifecycle()
    val campusHeroPhotos by schoolDataViewModel.campusHeroPhotos.collectAsStateWithLifecycle()
    val academicInfo by schoolDataViewModel.academicInfo.collectAsStateWithLifecycle()
    val courses by schoolDataViewModel.higherSecondaryCourses.collectAsStateWithLifecycle()
    val facilities by schoolDataViewModel.facilities.collectAsStateWithLifecycle()
    val transportRoutes by schoolDataViewModel.transportRoutes.collectAsStateWithLifecycle()
    val newsItems by schoolDataViewModel.newsItems.collectAsStateWithLifecycle()
    val announcements by schoolDataViewModel.announcements.collectAsStateWithLifecycle()
    val galleryAlbums by schoolDataViewModel.galleryAlbums.collectAsStateWithLifecycle()
    val galleryMedia by schoolDataViewModel.galleryMedia.collectAsStateWithLifecycle()
    val socialLinks by schoolDataViewModel.socialMediaLinks.collectAsStateWithLifecycle()

    val showShellChrome = currentRoute !in AppRoute.hiddenChromeRoutes
    val isPrimaryTab = currentRoute in AppRoute.primaryTabRoutes

    val topLevelItems = listOf(
        TopLevelNavItem(
            route = AppRoute.HOME,
            label = strings.navHome,
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            testTag = "bottom_nav_home"
        ),
        TopLevelNavItem(
            route = AppRoute.PARENT,
            label = strings.navParent,
            selectedIcon = Icons.Filled.FamilyRestroom,
            unselectedIcon = Icons.Outlined.FamilyRestroom,
            testTag = "bottom_nav_parent"
        ),
        TopLevelNavItem(
            route = AppRoute.ACADEMICS,
            label = strings.navAcademics,
            selectedIcon = Icons.Filled.School,
            unselectedIcon = Icons.Outlined.School,
            testTag = "bottom_nav_academics"
        ),
        TopLevelNavItem(
            route = AppRoute.CONTACT,
            label = strings.navContact,
            selectedIcon = Icons.Filled.Phone,
            unselectedIcon = Icons.Outlined.Phone,
            testTag = "bottom_nav_contact"
        )
    )

    fun navigateToRoute(route: String) {
        if (route in AppRoute.primaryTabRoutes) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
    }

    fun navigateFromDrawer(route: String) {
        coroutineScope.launch {
            drawerState.close()
        }
        if (route != currentRoute) {
            navigateToRoute(route)
        }
    }

    // Android Back button closes drawer first when open
    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    val screenTitle = when (currentRoute) {
        AppRoute.HOME -> strings.titleHome
        AppRoute.PARENT -> strings.parentPortalTitle
        AppRoute.PARENT_MODULE -> parentModuleTitle(
            ParentModuleId.fromRouteKey(backStackEntry?.arguments?.getString("moduleId")),
            strings
        )
        AppRoute.ACADEMICS -> strings.titleAcademics
        AppRoute.GALLERY -> strings.titleGallery
        AppRoute.CONTACT -> strings.titleContact
        AppRoute.MORE -> strings.titleMore
        AppRoute.ABOUT -> strings.titleAboutSchool
        AppRoute.COURSES -> strings.titleHigherSecondaryCourses
        AppRoute.FACILITIES -> strings.titleFacilities
        AppRoute.TRANSPORT -> strings.titleTransport
        AppRoute.NEWS -> strings.titleNews
        AppRoute.ANNOUNCEMENTS -> strings.titleAnnouncements
        AppRoute.NEWS_ANNOUNCEMENTS -> strings.titleNewsAnnouncements
        AppRoute.SOCIAL_MEDIA -> strings.titleSocialMedia
        AppRoute.SETTINGS -> strings.titleSettings
        AppRoute.MANAGEMENT_ENTRY -> strings.titleManagementEntry
        else -> strings.shortSchoolName
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showShellChrome,
        drawerContent = {
            ChevalierDrawerContent(
                profile = schoolProfile,
                currentRoute = currentRoute,
                onSelectRoute = { route -> navigateFromDrawer(route) }
            )
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp && showShellChrome

            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavigationRail) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding(),
                        containerColor = bottomBarContainerColor
                    ) {
                        topLevelItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToRoute(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.weight(1f),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        if (showShellChrome) {
                            if (currentRoute == AppRoute.PARENT) {
                                ParentPortalTopBar(
                                    isSearchActive = parentSearchActive,
                                    hasNotifications = announcements.isNotEmpty(),
                                    onToggleSearch = {
                                        parentSearchActive = !parentSearchActive
                                        if (!parentSearchActive) {
                                            parentSearchQuery = ""
                                        }
                                    },
                                    onNotificationsClick = {
                                        navigateToRoute(AppRoute.ANNOUNCEMENTS)
                                    },
                                    onGalleryClick = {
                                        navigateToRoute(AppRoute.GALLERY)
                                    },
                                    onOpenDrawerClick = {
                                        coroutineScope.launch { drawerState.open() }
                                    },
                                    onSettingsClick = {
                                        navigateToRoute(AppRoute.SETTINGS)
                                    }
                                )
                            } else {
                                ChevalierTopBar(
                                    title = screenTitle,
                                    showBack = !isPrimaryTab,
                                    onBackClick = {
                                        if (navController.previousBackStackEntry != null) {
                                            navController.popBackStack()
                                        }
                                    },
                                    onOpenDrawerClick = {
                                        coroutineScope.launch { drawerState.open() }
                                    },
                                    showSettingsAction = currentRoute != AppRoute.SETTINGS,
                                    onSettingsClick = { navigateToRoute(AppRoute.SETTINGS) },
                                    onGalleryClick = {
                                        if (currentRoute != AppRoute.GALLERY) {
                                            navigateToRoute(AppRoute.GALLERY)
                                        }
                                    }
                                )
                            }
                        }
                    },
                    bottomBar = {
                        // Fixed bottom navigation remains visible across ALL public pages
                        if (showShellChrome && !useNavigationRail) {
                            NavigationBar(
                                containerColor = bottomBarContainerColor,
                                tonalElevation = 3.dp,
                                modifier = Modifier.testTag("public_bottom_navigation_bar")
                            ) {
                                topLevelItems.forEach { item ->
                                    val selected = currentRoute == item.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToRoute(item.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = item.label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.White,
                                            selectedTextColor = brand.burgundy,
                                            indicatorColor = brand.burgundy,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag(item.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = AppRoute.SPLASH,
                        modifier = if (showShellChrome) {
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        } else {
                            Modifier.fillMaxSize()
                        }
                    ) {
                        composable(AppRoute.SPLASH) {
                            SplashScreen(
                                onSplashCompleted = {
                                    coroutineScope.launch {
                                        val readyPrefs = (preferencesUiState as? PreferencesUiState.Ready)
                                            ?: mainViewModel.preferencesUiState
                                                .filterIsInstance<PreferencesUiState.Ready>()
                                                .first()
                                        val nextRoute = if (readyPrefs.state.isLanguageSelectedOnLaunch) {
                                            AppRoute.HOME
                                        } else {
                                            AppRoute.LANGUAGE_SELECTION
                                        }
                                        navController.navigate(nextRoute) {
                                            popUpTo(AppRoute.SPLASH) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        composable(AppRoute.LANGUAGE_SELECTION) {
                            val openedFromSettings =
                                navController.previousBackStackEntry?.destination?.route == AppRoute.SETTINGS
                            LanguageSelectionScreen(
                                selectedLanguage = currentLanguage,
                                onLanguagePreviewSelected = { lang ->
                                    mainViewModel.selectLanguage(lang, markOnboarded = false)
                                },
                                onConfirmLanguage = { lang ->
                                    mainViewModel.selectLanguage(lang, markOnboarded = true)
                                    if (openedFromSettings && navController.previousBackStackEntry != null) {
                                        navController.popBackStack()
                                    } else {
                                        navController.navigate(AppRoute.HOME) {
                                            popUpTo(AppRoute.LANGUAGE_SELECTION) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        composable(AppRoute.HOME) {
                            HomeScreen(
                                profile = schoolProfile,
                                heroPhotos = campusHeroPhotos,
                                newsItems = newsItems,
                                announcements = announcements,
                                galleryMedia = galleryMedia,
                                socialLinks = socialLinks,
                                onNavigate = { target -> navigateToRoute(target) }
                            )
                        }

                        composable(AppRoute.PARENT) {
                            ParentDashboardScreen(
                                announcements = announcements,
                                newsItems = newsItems,
                                isSearchActive = parentSearchActive,
                                searchQuery = parentSearchQuery,
                                onSearchQueryChange = { parentSearchQuery = it },
                                onOpenModule = { moduleId ->
                                    navigateToRoute(AppRoute.parentModule(moduleId.routeKey))
                                },
                                onOpenAllUpdates = {
                                    navigateToRoute(AppRoute.NEWS_ANNOUNCEMENTS)
                                }
                            )
                        }

                        composable(
                            route = AppRoute.PARENT_MODULE,
                            arguments = listOf(navArgument("moduleId") { type = NavType.StringType })
                        ) { entry ->
                            val moduleId = ParentModuleId.fromRouteKey(entry.arguments?.getString("moduleId"))
                            ParentModuleDetailScreen(
                                moduleId = moduleId,
                                schoolProfile = schoolProfile,
                                announcements = announcements,
                                onBackToDashboard = {
                                    if (navController.previousBackStackEntry != null) {
                                        navController.popBackStack()
                                    } else {
                                        navigateToRoute(AppRoute.PARENT)
                                    }
                                },
                                onOpenContact = { navigateToRoute(AppRoute.CONTACT) },
                                onOpenAnnouncements = { navigateToRoute(AppRoute.ANNOUNCEMENTS) }
                            )
                        }

                        composable(AppRoute.ACADEMICS) {
                            AcademicsScreen(
                                academicInfo = academicInfo,
                                onOpenCourses = { navigateToRoute(AppRoute.COURSES) }
                            )
                        }

                        composable(AppRoute.GALLERY) {
                            GalleryScreen(albums = galleryAlbums)
                        }

                        composable(AppRoute.CONTACT) {
                            ContactScreen(
                                profile = schoolProfile,
                                socialLinks = socialLinks,
                                onOpenSocialMedia = { navigateToRoute(AppRoute.SOCIAL_MEDIA) }
                            )
                        }

                        composable(AppRoute.MORE) {
                            MoreScreen(
                                onNavigate = { target -> navigateToRoute(target) }
                            )
                        }

                        composable(AppRoute.ABOUT) {
                            AboutSchoolScreen(profile = schoolProfile)
                        }

                        composable(AppRoute.COURSES) {
                            HigherSecondaryCoursesScreen(courses = courses)
                        }

                        composable(AppRoute.FACILITIES) {
                            FacilitiesScreen(facilities = facilities)
                        }

                        composable(AppRoute.TRANSPORT) {
                            TransportScreen(routes = transportRoutes)
                        }

                        composable(AppRoute.NEWS) {
                            NewsScreen(newsItems = newsItems)
                        }

                        composable(AppRoute.ANNOUNCEMENTS) {
                            AnnouncementsScreen(announcements = announcements)
                        }

                        composable(AppRoute.NEWS_ANNOUNCEMENTS) {
                            NewsAnnouncementsScreen(
                                newsItems = newsItems,
                                announcements = announcements
                            )
                        }

                        composable(AppRoute.SOCIAL_MEDIA) {
                            SocialMediaScreen(links = socialLinks)
                        }

                        composable(AppRoute.SETTINGS) {
                            SettingsScreen(
                                currentLanguage = currentLanguage,
                                currentThemeMode = currentThemeMode,
                                onLanguageSelected = { lang ->
                                    mainViewModel.selectLanguage(lang, markOnboarded = true)
                                },
                                onThemeModeSelected = { mode ->
                                    mainViewModel.selectThemeMode(mode)
                                },
                                onOpenLanguageSelection = {
                                    navController.navigate(AppRoute.LANGUAGE_SELECTION)
                                }
                            )
                        }

                        composable(AppRoute.MANAGEMENT_ENTRY) {
                            ManagementPortalScreen(
                                managementViewModel = managementViewModel,
                                mainViewModel = mainViewModel,
                                currentLanguage = currentLanguage,
                                currentThemeMode = currentThemeMode,
                                onExitToPublicApp = {
                                    if (navController.previousBackStackEntry != null) {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChevalierDrawerContent(
    profile: SchoolProfileEntity,
    currentRoute: String,
    onSelectRoute: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val brand = LocalBrandPalette.current

    ModalDrawerSheet(
        modifier = Modifier
            .width(310.dp)
            .testTag("chevalier_navigation_drawer"),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Branded Drawer Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                brand.burgundy,
                                Color(0xFF5D131A)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 22.dp)
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(2.dp, brand.royalGold),
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.chevalier_emblem),
                            contentDescription = profile.schoolName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .padding(3.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = profile.schoolName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${profile.motto}\"",
                        style = MaterialTheme.typography.labelLarge.copy(fontStyle = FontStyle.Italic),
                        color = brand.royalGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // MAIN
            DrawerGroupSection(
                groupTitle = strings.drawerGroupMain,
                items = listOf(
                    DrawerNavItem(AppRoute.HOME, strings.navHome, Icons.Outlined.Home, "drawer_home"),
                    DrawerNavItem(AppRoute.PARENT, strings.navParent, Icons.Outlined.FamilyRestroom, "drawer_parent")
                ),
                currentRoute = currentRoute,
                onSelectRoute = onSelectRoute
            )

            // SCHOOL INFORMATION
            DrawerGroupSection(
                groupTitle = strings.drawerGroupSchoolInfo,
                items = listOf(
                    DrawerNavItem(AppRoute.ABOUT, strings.titleAboutSchool, Icons.Outlined.Info, "drawer_about"),
                    DrawerNavItem(AppRoute.ACADEMICS, strings.titleAcademics, Icons.Outlined.School, "drawer_academics"),
                    DrawerNavItem(AppRoute.COURSES, strings.titleHigherSecondaryCourses, Icons.AutoMirrored.Outlined.MenuBook, "drawer_courses")
                ),
                currentRoute = currentRoute,
                onSelectRoute = onSelectRoute
            )

            // CAMPUS & ACTIVITIES
            DrawerGroupSection(
                groupTitle = strings.drawerGroupCampusActivities,
                items = listOf(
                    DrawerNavItem(AppRoute.FACILITIES, strings.titleFacilities, Icons.Outlined.Apartment, "drawer_facilities"),
                    DrawerNavItem(AppRoute.TRANSPORT, strings.titleTransport, Icons.Outlined.DirectionsBus, "drawer_transport"),
                    DrawerNavItem(AppRoute.GALLERY, strings.titleGallery, Icons.Outlined.PhotoLibrary, "drawer_gallery")
                ),
                currentRoute = currentRoute,
                onSelectRoute = onSelectRoute
            )

            // UPDATES
            DrawerGroupSection(
                groupTitle = strings.drawerGroupUpdates,
                items = listOf(
                    DrawerNavItem(AppRoute.NEWS, strings.titleNews, Icons.Outlined.Newspaper, "drawer_news"),
                    DrawerNavItem(AppRoute.ANNOUNCEMENTS, strings.titleAnnouncements, Icons.Outlined.Campaign, "drawer_announcements")
                ),
                currentRoute = currentRoute,
                onSelectRoute = onSelectRoute
            )

            // CONNECT
            DrawerGroupSection(
                groupTitle = strings.drawerGroupConnect,
                items = listOf(
                    DrawerNavItem(AppRoute.CONTACT, strings.titleContact, Icons.Outlined.Phone, "drawer_contact"),
                    DrawerNavItem(AppRoute.SOCIAL_MEDIA, strings.titleSocialMedia, Icons.Outlined.Share, "drawer_social"),
                    DrawerNavItem(AppRoute.SETTINGS, strings.titleSettings, Icons.Outlined.Settings, "drawer_settings")
                ),
                currentRoute = currentRoute,
                onSelectRoute = onSelectRoute
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // MANAGEMENT (Separated from public sections)
            DrawerGroupSection(
                groupTitle = strings.drawerGroupManagement,
                items = listOf(
                    DrawerNavItem(
                        AppRoute.MANAGEMENT_ENTRY,
                        strings.drawerManagementLogin,
                        Icons.Outlined.AdminPanelSettings,
                        "drawer_management_login"
                    )
                ),
                currentRoute = currentRoute,
                onSelectRoute = onSelectRoute
            )

            Spacer(modifier = Modifier.height(16.dp).navigationBarsPadding())
        }
    }
}

@Composable
private fun DrawerGroupSection(
    groupTitle: String,
    items: List<DrawerNavItem>,
    currentRoute: String,
    onSelectRoute: (String) -> Unit
) {
    val brand = LocalBrandPalette.current
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = groupTitle,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = brand.goldAccentText,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationDrawerItem(
                label = {
                    Text(
                        text = item.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                selected = selected,
                onClick = { onSelectRoute(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = brand.burgundy.copy(alpha = 0.14f),
                    selectedIconColor = brand.burgundy,
                    selectedTextColor = brand.burgundy
                ),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .testTag(item.testTag)
            )
        }
    }
}

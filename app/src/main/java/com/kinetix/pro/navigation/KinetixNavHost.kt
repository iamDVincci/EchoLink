package com.kinetix.pro.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.kinetix.pro.core.ui.theme.ContentMedium
import com.kinetix.pro.core.ui.theme.ObsidianCanvas
import com.kinetix.pro.core.ui.theme.ObsidianCardStroke
import com.kinetix.pro.core.ui.theme.ObsidianSurface1
import com.kinetix.pro.core.ui.theme.ObsidianSurface2
import com.kinetix.pro.core.ui.theme.PrimarySunset
import com.kinetix.pro.ui.editor.EditorScreen
import com.kinetix.pro.ui.home.HomeScreen
import com.kinetix.pro.ui.library.LibraryScreen
import com.kinetix.pro.ui.onboarding.OnboardingScreen
import com.kinetix.pro.ui.settings.SettingsScreen
import com.kinetix.pro.ui.templates.TemplatesScreen

@Composable
fun KinetixNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Download.route,
    sharedUrl: String? = null,
    onSharedUrlConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isEditorScreen = currentRoute?.startsWith("editor") == true
    val isOnboarding = currentRoute == Screen.Onboarding.route
    val showBottomBar = !isEditorScreen && !isOnboarding

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ObsidianCanvas,
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianCanvas)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    NavigationBar(
                        containerColor = ObsidianSurface1,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(66.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, ObsidianCardStroke, RoundedCornerShape(18.dp))
                    ) {
                        Screen.bottomNavItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    if (screen.icon != null) {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.title,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimarySunset,
                                    selectedTextColor = PrimarySunset,
                                    unselectedIconColor = ContentMedium,
                                    unselectedTextColor = ContentMedium,
                                    indicatorColor = ObsidianSurface2
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinishOnboarding = {
                        navController.navigate(Screen.Download.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Download.route) {
                HomeScreen(
                    sharedUrl = sharedUrl,
                    onSharedUrlConsumed = onSharedUrlConsumed,
                    onNavigateToEditor = { projectId, videoUri, title ->
                        navController.navigate(Screen.Editor.createRoute(projectId, videoUri, title))
                    }
                )
            }

            composable(Screen.Library.route) {
                LibraryScreen(
                    onNavigateToEditor = { projectId, videoUri, title ->
                        navController.navigate(Screen.Editor.createRoute(projectId, videoUri, title))
                    }
                )
            }

            composable(
                route = Screen.Editor.route,
                arguments = listOf(
                    navArgument("projectId") { type = NavType.StringType },
                    navArgument("videoUri") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    },
                    navArgument("title") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: "new_project"
                val videoUri = backStackEntry.arguments?.getString("videoUri").orEmpty()
                val title = backStackEntry.arguments?.getString("title").orEmpty()
                EditorScreen(
                    projectId = projectId,
                    initialVideoUri = videoUri,
                    initialTitle = title,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Templates.route) {
                TemplatesScreen(
                    onApplyTemplate = { templateId ->
                        navController.navigate(Screen.Editor.createRoute("template_applied_$templateId"))
                    },
                    onApplyTemplateWithVideo = { templateId, videoUri, title ->
                        navController.navigate(Screen.Editor.createRoute("template_applied_$templateId", videoUri, title))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onReplayOnboarding = {
                        navController.navigate(Screen.Onboarding.route)
                    }
                )
            }
        }
    }
}

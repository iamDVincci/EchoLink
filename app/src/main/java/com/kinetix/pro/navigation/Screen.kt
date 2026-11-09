package com.kinetix.pro.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Download : Screen("download", "Download", Icons.Default.Download)
    data object Library : Screen("library", "Library", Icons.Default.VideoLibrary)
    data object Studio : Screen("editor/studio_quick_edit?videoUri=&title=", "Studio", Icons.Default.AutoFixHigh)
    data object Editor : Screen("editor/{projectId}?videoUri={videoUri}&title={title}", "Studio", Icons.Default.Movie) {
        fun createRoute(
            projectId: String,
            videoUri: String? = null,
            title: String? = null
        ): String {
            val encodedUri = videoUri?.let { Uri.encode(it) }.orEmpty()
            val encodedTitle = title?.let { Uri.encode(it) }.orEmpty()
            return "editor/$projectId?videoUri=$encodedUri&title=$encodedTitle"
        }
    }
    data object Templates : Screen("templates", "Templates", Icons.Default.Palette)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object Onboarding : Screen("onboarding", "Onboarding")

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(Download, Library, Studio, Templates, Settings)
    }
}

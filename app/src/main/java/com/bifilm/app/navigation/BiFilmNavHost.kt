package com.bifilm.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bifilm.app.ui.capture.CaptureScreen
import com.bifilm.app.ui.compose.ComposeScreen
import com.bifilm.app.ui.export.ExportScreen
import com.bifilm.app.ui.home.HomeScreen
import com.bifilm.app.ui.settings.SettingsScreen

@Composable
fun BiFilmNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.Home) {

        composable(Routes.Home) {
            HomeScreen(
                onOpenCapture = { id -> navController.navigate(Routes.capture(id)) },
                onOpenCompose = { id -> navController.navigate(Routes.compose(id)) },
                onOpenSettings = { navController.navigate(Routes.Settings) }
            )
        }

        composable(Routes.Settings) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.Capture,
            arguments = listOf(navArgument(NavArgs.ProjectId) { type = NavType.StringType })
        ) { entry ->
            CaptureScreen(
                projectId = entry.arguments?.getString(NavArgs.ProjectId).orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.Compose,
            arguments = listOf(navArgument(NavArgs.ProjectId) { type = NavType.StringType })
        ) { entry ->
            ComposeScreen(
                projectId = entry.arguments?.getString(NavArgs.ProjectId).orEmpty(),
                onBack = { navController.popBackStack() },
                onExport = { id -> navController.navigate(Routes.export(id)) }
            )
        }

        composable(
            route = Routes.Export,
            arguments = listOf(navArgument(NavArgs.ProjectId) { type = NavType.StringType })
        ) { entry ->
            ExportScreen(
                projectId = entry.arguments?.getString(NavArgs.ProjectId).orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}

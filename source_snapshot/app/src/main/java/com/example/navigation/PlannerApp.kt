package com.example.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.FoldersScreen
import com.example.LessonPlannerScreen
import com.example.LessonPlansScreen
import com.example.PlanDetailScreen
import com.example.ui.FavoritesScreen
import com.example.ui.theme.PlannerMotion

private object Routes {
    const val Generator = "generator"
    const val Folders = "folders"
    const val Favorites = "favorites"
}

@Composable
fun PlannerApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = currentRoute == Routes.Generator,
                    onClick = {
                        navController.navigate(Routes.Generator) {
                            popUpTo(Routes.Generator) { inclusive = true }
                        }
                    },
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                    label = { Text("Создать") },
                    colors = plannerNavigationColors()
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.Folders ||
                        currentRoute?.startsWith("folder_plans") == true ||
                        currentRoute?.startsWith("plan_detail") == true,
                    onClick = { navController.navigate(Routes.Folders) { popUpTo(Routes.Generator) } },
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    label = { Text("Папки") },
                    colors = plannerNavigationColors()
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.Favorites,
                    onClick = { navController.navigate(Routes.Favorites) { popUpTo(Routes.Generator) } },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    label = { Text("Избранное") },
                    colors = plannerNavigationColors()
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Generator,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(PlannerMotion.Screen)
                ) + androidx.compose.animation.fadeIn(tween(PlannerMotion.Fade))
            },
            exitTransition = {
                androidx.compose.animation.fadeOut(tween(PlannerMotion.Fade))
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(PlannerMotion.Screen)
                ) + androidx.compose.animation.fadeIn(tween(PlannerMotion.Fade))
            },
            popExitTransition = { androidx.compose.animation.fadeOut(tween(PlannerMotion.Fade)) }
        ) {
            composable(Routes.Generator) {
                LessonPlannerScreen(navController = navController, modifier = Modifier)
            }
            composable(Routes.Folders) { FoldersScreen(navController = navController) }
            composable(Routes.Favorites) { FavoritesScreen(navController = navController) }
            composable("folder_plans/{folderId}") { entry ->
                val folderId = entry.arguments?.getString("folderId")?.toLongOrNull() ?: 0L
                LessonPlansScreen(folderId = folderId, navController = navController)
            }
            composable("plan_detail/{planId}") { entry ->
                val planId = entry.arguments?.getString("planId")?.toLongOrNull() ?: 0L
                PlanDetailScreen(planId = planId, navController = navController)
            }
        }
    }
}

@Composable
private fun plannerNavigationColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
)

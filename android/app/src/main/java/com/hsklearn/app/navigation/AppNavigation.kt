package com.hsklearn.app.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hsklearn.app.ui.components.OfflineBanner
import com.hsklearn.app.ui.home.HomeScreen
import com.hsklearn.app.ui.reading.ReadingScreen
import com.hsklearn.app.ui.speaking.SpeakingScreen
import com.hsklearn.app.ui.flashcard.FlashcardScreen
import com.hsklearn.app.ui.writing.WritingScreen
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object ReadingRoute
@Serializable data object SpeakingRoute
@Serializable data object WritingRoute
@Serializable data object FlashcardRoute

data class TopLevelRoute(
    val label: String,
    val route: Any,
    val icon: ImageVector,
)

val topLevelRoutes = listOf(
    TopLevelRoute("首页 Home", HomeRoute, Icons.Default.Home),
    TopLevelRoute("阅读 Read", ReadingRoute, Icons.Default.MenuBook),
    TopLevelRoute("口语 Speak", SpeakingRoute, Icons.Default.Mic),
    TopLevelRoute("书写 Write", WritingRoute, Icons.Default.Edit),
    TopLevelRoute("卡片 Cards", FlashcardRoute, Icons.Default.Style),
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                topLevelRoutes.forEach { route ->
                    NavigationBarItem(
                        icon = { Icon(route.icon, contentDescription = route.label) },
                        label = { Text(route.label) },
                        selected = currentDestination?.hasRoute(route.route::class) == true,
                        onClick = {
                            navController.navigate(route.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            OfflineBanner()

            NavHost(
                navController = navController,
                startDestination = HomeRoute,
            ) {
                composable<HomeRoute> {
                    HomeScreen(
                        onNavigateToReading = {
                            navController.navigate(ReadingRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToSpeaking = {
                            navController.navigate(SpeakingRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToWriting = {
                            navController.navigate(WritingRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToFlashcards = {
                            navController.navigate(FlashcardRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
                composable<ReadingRoute> { ReadingScreen() }
                composable<SpeakingRoute> { SpeakingScreen() }
                composable<WritingRoute> { WritingScreen() }
                composable<FlashcardRoute> { FlashcardScreen() }
            }
        }
    }
}

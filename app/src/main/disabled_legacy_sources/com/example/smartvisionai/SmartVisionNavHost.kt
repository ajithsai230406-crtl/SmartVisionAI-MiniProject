package com.example.smartvisionai

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.smartvisionai.ui.screens.*
import com.example.smartvisionai.ui.theme.*

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Scan : Screen("scan")
    object History : Screen("history")
    object Settings : Screen("settings")
    object ModuleDetail : Screen("module_detail/{moduleId}") {
        fun createRoute(moduleId: String) = "module_detail/$moduleId"
    }
}

@Composable
fun SmartVisionNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavRoutes = listOf(Screen.Home.route, Screen.Scan.route,
        Screen.History.route, Screen.Settings.route)
    val showBottomNav = currentRoute in bottomNavRoutes

    Column(modifier = Modifier
        .fillMaxSize()
        .background(BackgroundDark)) {

        Box(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(200)) +
                    slideInHorizontally(animationSpec = tween(200)) { it / 8 }
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(150))
                }
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onModuleClick = { moduleId ->
                            navController.navigate(Screen.ModuleDetail.createRoute(moduleId))
                        },
                        onScanClick = {
                            navController.navigate(Screen.Scan.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(Screen.Scan.route) {
                    ScanScreen()
                }
                composable(Screen.History.route) {
                    HistoryScreen()
                }
                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
                composable(Screen.ModuleDetail.route) { backStack ->
                    val moduleId = backStack.arguments?.getString("moduleId") ?: "object"
                    ModuleDetailScreen(
                        moduleId = moduleId,
                        onBack = { navController.popBackStack() },
                        onStartScan = {
                            navController.navigate(Screen.Scan.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    )
                }
            }
        }

        if (showBottomNav) {
            SmartBottomNavBar(
                currentRoute = currentRoute ?: Screen.Home.route,
                onNavClick = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun SmartBottomNavBar(currentRoute: String, onNavClick: (String) -> Unit) {
    val items = listOf(
        Triple(Screen.Home.route, "Home", "home"),
        Triple(Screen.Scan.route, "Scan", "scan"),
        Triple(Screen.History.route, "History", "history"),
        Triple(Screen.Settings.route, "Settings", "settings")
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavBarBg)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (route, label, icon) ->
            val isActive = currentRoute == route
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNavClick(route) }
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) CyanAccent.copy(alpha = 0.15f) else Color.Transparent)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NavIcon(iconKey = icon, active = isActive)
                }
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = if (isActive) CyanAccent else Color(0xFF556677)
                )
            }
        }
    }
}

@Composable
fun NavIcon(iconKey: String, active: Boolean) {
    val tint = if (active) CyanAccent else Color(0xFF556677)
    val size = 20.dp
    when (iconKey) {
        "home" -> Icon(
            painter = painterResource(android.R.drawable.ic_menu_compass),
            contentDescription = null, tint = tint,
            modifier = Modifier.size(size)
        )
        "scan" -> Icon(
            painter = painterResource(android.R.drawable.ic_menu_camera),
            contentDescription = null, tint = tint,
            modifier = Modifier.size(size)
        )
        "history" -> Icon(
            painter = painterResource(android.R.drawable.ic_menu_recent_history),
            contentDescription = null, tint = tint,
            modifier = Modifier.size(size)
        )
        "settings" -> Icon(
            painter = painterResource(android.R.drawable.ic_menu_manage),
            contentDescription = null, tint = tint,
            modifier = Modifier.size(size)
        )
        else -> {}
    }
}

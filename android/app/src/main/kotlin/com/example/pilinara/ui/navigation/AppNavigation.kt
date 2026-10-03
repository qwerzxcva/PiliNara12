package com.example.pilinara.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * App Navigation - Jetpack Compose
 * Replaces Flutter GetX router
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Dynamics : Screen("dynamics")
    object Message : Screen("message")
    object Profile : Screen("profile")
    object VideoPlayer : Screen("video/{bvid}") {
        fun createRoute(bvid: String) = "video/$bvid"
    }
    object LiveRoom : Screen("live/{roomId}") {
        fun createRoute(roomId: String) = "live/$roomId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            // TODO: Implement HomeScreen
        }
        composable(Screen.Search.route) {
            // TODO: Implement SearchScreen
        }
        composable(Screen.Dynamics.route) {
            // TODO: Implement DynamicsScreen
        }
        composable(Screen.Message.route) {
            // TODO: Implement MessageScreen
        }
        composable(Screen.Profile.route) {
            // TODO: Implement ProfileScreen
        }
        composable(Screen.VideoPlayer.route) { backStackEntry ->
            val bvid = backStackEntry.arguments?.getString("bvid") ?: ""
            // TODO: Implement VideoPlayerScreen
        }
        composable(Screen.LiveRoom.route) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            // TODO: Implement LiveRoomScreen
        }
    }
}

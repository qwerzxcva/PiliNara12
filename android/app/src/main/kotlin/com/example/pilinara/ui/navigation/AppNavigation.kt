package com.example.pilinara.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pilinara.ui.main.SearchScreen
import com.example.pilinara.ui.pages.dynamics.DynamicsScreen
import com.example.pilinara.ui.pages.home.HomeScreen
import com.example.pilinara.ui.pages.mine.MineScreen
import com.example.pilinara.playback.VideoPlayerScreen

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
            HomeScreen(
                onVideoClick = { bvid, cid ->
                    navController.navigate(Screen.VideoPlayer.createRoute(bvid))
                }
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onVideoClick = { bvid, cid ->
                    navController.navigate(Screen.VideoPlayer.createRoute(bvid))
                }
            )
        }
        composable(Screen.Dynamics.route) {
            DynamicsScreen()
        }
        composable(Screen.Message.route) {
            // TODO: Implement MessageScreen
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text("消息页面 - 待实现")
            }
        }
        composable(Screen.Profile.route) {
            MineScreen()
        }
        composable(Screen.VideoPlayer.route) { backStackEntry ->
            val bvid = backStackEntry.arguments?.getString("bvid") ?: ""
            VideoPlayerScreen(
                videoUrl = "", // TODO: Get actual video URL
                bvid = bvid,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.LiveRoom.route) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            // TODO: Implement LiveRoomScreen
        }
    }
}

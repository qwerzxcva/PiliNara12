package com.example.pilinara.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pilinara.ui.comments.CommentScreen
import com.example.pilinara.ui.live.LiveRoomScreen
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
    object Comment : Screen("comment/{bvid}") {
        fun createRoute(bvid: String) = "comment/$bvid"
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
            com.example.pilinara.ui.messages.MessageScreen()
        }
        composable(Screen.Profile.route) {
            MineScreen()
        }
        composable(Screen.VideoPlayer.route) { backStackEntry ->
            val bvid = backStackEntry.arguments?.getString("bvid") ?: ""
            VideoPlayerScreen(
                videoUrl = "", // Video URL will be set by ViewModel
                bvid = bvid,
                onBack = { navController.popBackStack() },
                onCommentClick = {
                    navController.navigate(Screen.Comment.createRoute(bvid))
                }
            )
        }
        composable(Screen.LiveRoom.route) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            LiveRoomScreen(roomId = roomId)
        }
        composable(Screen.Comment.route) { backStackEntry ->
            val bvid = backStackEntry.arguments?.getString("bvid") ?: ""
            CommentScreen(bvid = bvid)
        }
    }
}

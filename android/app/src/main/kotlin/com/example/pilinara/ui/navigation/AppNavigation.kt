package com.example.pilinara.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.pilinara.ui.comments.CommentScreen
import com.example.pilinara.ui.library.FavMediaScreen
import com.example.pilinara.ui.library.FavoritesScreen
import com.example.pilinara.ui.library.HistoryScreen
import com.example.pilinara.ui.live.LiveRoomScreen
import com.example.pilinara.ui.login.LoginScreen
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
    object Login : Screen("login")
    object Favorites : Screen("favorites/{mid}") {
        fun createRoute(mid: Long) = "favorites/$mid"
    }
    object FavMedia : Screen("favmedia/{mediaId}") {
        fun createRoute(mediaId: Long) = "favmedia/$mediaId"
    }
    object History : Screen("history")
    object Settings : Screen("settings")
    object VideoPlayer : Screen("video/{bvid}?cid={cid}") {
        const val CID_ARG = "cid"
        fun createRoute(bvid: String, cid: Long = 0L) = "video/$bvid?cid=$cid"
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
                onOpenSearch = { navController.navigate(Screen.Search.route) },
                onOpenVideo = { bvid, cid ->
                    navController.navigate(Screen.VideoPlayer.createRoute(bvid, cid))
                }
            )
        }
        composable(Screen.Search.route) {
            SearchScreen()
        }
        composable(Screen.Dynamics.route) {
            DynamicsScreen()
        }
        composable(Screen.Message.route) {
            com.example.pilinara.ui.messages.MessageScreen()
        }
        composable(Screen.Profile.route) {
            MineScreen(
                onOpenLogin = { navController.navigate(Screen.Login.route) },
                onOpenFavorites = { mid -> navController.navigate(Screen.Favorites.createRoute(mid)) },
                onOpenHistory = { navController.navigate(Screen.History.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Settings.route) {
            com.example.pilinara.ui.settings.SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onLoggedIn = { navController.popBackStack() }
            )
        }
        composable(Screen.Favorites.route) { backStackEntry ->
            val mid = backStackEntry.arguments?.getString("mid")?.toLongOrNull() ?: 0L
            FavoritesScreen(
                mid = mid,
                onBack = { navController.popBackStack() },
                onOpenFolder = { mediaId -> navController.navigate(Screen.FavMedia.createRoute(mediaId)) }
            )
        }
        composable(Screen.FavMedia.route) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId")?.toLongOrNull() ?: 0L
            FavMediaScreen(
                mediaId = mediaId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.VideoPlayer.route,
            arguments = listOf(
                navArgument("bvid") { type = NavType.StringType },
                navArgument("cid") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { backStackEntry ->
            val bvid = backStackEntry.arguments?.getString("bvid") ?: ""
            val cid = backStackEntry.arguments?.getLong("cid") ?: 0L
            VideoPlayerScreen(
                videoUrl = "",
                bvid = bvid,
                cid = cid,
                onBack = { navController.popBackStack() }
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

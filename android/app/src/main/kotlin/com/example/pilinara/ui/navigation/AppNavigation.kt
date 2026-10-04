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
    object Member : Screen("member/{mid}") {
        fun createRoute(mid: Long) = "member/$mid"
    }
    object ToView : Screen("toview")
    object FollowList : Screen("followlist/{mid}?type={type}") {
        fun createRoute(mid: Long, followers: Boolean) =
            "followlist/$mid?type=${if (followers) "1" else "0"}"
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
                onVideoClick = { bvid ->
                    navController.navigate(Screen.VideoPlayer.createRoute(bvid))
                },
                onUserClick = { mid ->
                    navController.navigate(Screen.Member.createRoute(mid))
                }
            )
        }
        composable(Screen.Dynamics.route) {
            DynamicsScreen(
                onOpenVideo = { bvid, cid ->
                    navController.navigate(Screen.VideoPlayer.createRoute(bvid, cid))
                },
                onGoLogin = { navController.navigate(Screen.Login.route) }
            )
        }
        composable(Screen.Message.route) {
            com.example.pilinara.ui.messages.MessageScreen(
                onGoLogin = { navController.navigate(Screen.Login.route) },
                onOpenVideo = { bvid, cid ->
                    navController.navigate(Screen.VideoPlayer.createRoute(bvid, cid))
                },
                onOpenUser = { mid ->
                    navController.navigate(Screen.Member.createRoute(mid))
                }
            )
        }
        composable(Screen.Profile.route) {
            MineScreen(
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onLoginClick = { navController.navigate(Screen.Login.route) },
                onHistoryClick = { navController.navigate(Screen.History.route) },
                onToViewClick = { navController.navigate(Screen.ToView.route) },
                onFavoritesClick = { mid ->
                    navController.navigate(Screen.Favorites.createRoute(mid))
                }
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
                onBack = { navController.popBackStack() },
                onOpenVideo = { bvid, cid ->
                    navController.navigate(Screen.VideoPlayer.createRoute(bvid, cid))
                }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenVideo = { bvid, cid ->
                    navController.navigate(Screen.VideoPlayer.createRoute(bvid, cid))
                }
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
                onBack = { navController.popBackStack() },
                onOpenComments = { bv -> navController.navigate(Screen.Comment.createRoute(bv)) }
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
        composable(Screen.Member.route) { backStackEntry ->
            val mid = backStackEntry.arguments?.getString("mid")?.toLongOrNull() ?: 0L
            com.example.pilinara.ui.pages.member.MemberScreen(
                mid = mid,
                onBack = { navController.popBackStack() },
                onOpenVideo = { bvid, cid ->
                    navController.navigate(Screen.VideoPlayer.createRoute(bvid, cid))
                },
                onOpenFollowList = { m, followers ->
                    navController.navigate(Screen.FollowList.createRoute(m, followers))
                }
            )
        }
        composable(Screen.ToView.route) {
            com.example.pilinara.ui.library.ToViewScreen(
                onBack = { navController.popBackStack() },
                onOpenVideo = { bvid, cid ->
                    navController.navigate(Screen.VideoPlayer.createRoute(bvid, cid))
                }
            )
        }
        composable(
            Screen.FollowList.route,
            arguments = listOf(
                navArgument("mid") { type = NavType.LongType },
                navArgument("type") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val mid = backStackEntry.arguments?.getLong("mid") ?: 0L
            val followers = backStackEntry.arguments?.getInt("type") == 1
            com.example.pilinara.ui.pages.member.FollowListScreen(
                mid = mid,
                followers = followers,
                onBack = { navController.popBackStack() },
                onOpenUser = { m -> navController.navigate(Screen.Member.createRoute(m)) }
            )
        }
    }
}

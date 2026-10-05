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
    object VideoPlayer : Screen("video/{bvid}?cid={cid}&local={local}") {
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
    object Bangumi : Screen("bangumi?seasonId={seasonId}&epId={epId}") {
        fun createRoute(seasonId: Long = 0L, epId: Long = 0L) =
            "bangumi?seasonId=$seasonId&epId=$epId"
    }
    object LiveList : Screen("livelist")
    object PgcIndex : Screen("pgcindex")
    object Rank : Screen("rank")
    object DanmakuBlock : Screen("danmakublock")
    object Sessions : Screen("sessions")

    fun chat(talkerId: Long) = "chat/$talkerId"

    object Downloads : Screen("downloads")
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
                    navController.navigate(Screen.VideoPlayer.createRoute(bvid, cid))
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                },
                onRankClick = {
                    navController.navigate(Screen.Rank.route)
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
                },
                onOpenSessions = { navController.navigate(Screen.Sessions.route) }
            )
        }
        composable(Screen.Profile.route) {
            MineScreen(
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onLoginClick = { navController.navigate(Screen.Login.route) },
                onHistoryClick = { navController.navigate(Screen.History.route) },
                onToViewClick = { navController.navigate(Screen.ToView.route) },
                onBangumiClick = { navController.navigate(Screen.Bangumi.createRoute()) },
                onFavoritesClick = { mid ->
                    navController.navigate(Screen.Favorites.createRoute(mid))
                },
                onDownloadsClick = { navController.navigate(Screen.Downloads.route) }
            )
        }
        composable(Screen.Settings.route) {
            com.example.pilinara.ui.settings.SettingsScreen(
                onBack = { navController.popBackStack() },
                onDanmakuBlockClick = { navController.navigate(Screen.DanmakuBlock.route) }
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
        composable(Screen.VideoPlayer.route,
            arguments = listOf(
                navArgument("bvid") { type = NavType.StringType },
                navArgument("cid") { type = NavType.LongType; defaultValue = 0L },
                navArgument("local") { type = NavType.StringType; defaultValue = "0" }
            )
        ) { backStackEntry ->
            val bvid = backStackEntry.arguments?.getString("bvid") ?: ""
            val cid = backStackEntry.arguments?.getLong("cid") ?: 0L
            val local = backStackEntry.arguments?.getString("local") == "1"
            // ep 请求参数（番剧模式）：video/ep123 形式
            val epId = if (bvid.startsWith("ep")) bvid.removePrefix("ep").toLongOrNull() ?: 0L else 0L
            VideoPlayerScreen(
                videoUrl = "",
                bvid = bvid,
                cid = cid,
                epId = epId,
                local = local,
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
        composable(
            Screen.Bangumi.route,
            arguments = listOf(
                navArgument("seasonId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("epId") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { backStackEntry ->
            val sid = backStackEntry.arguments?.getLong("seasonId") ?: 0L
            val epid = backStackEntry.arguments?.getLong("epId") ?: 0L
            com.example.pilinara.ui.pages.bangumi.BangumiScreen(
                seasonId = sid,
                epId = epid,
                onBack = { navController.popBackStack() },
                onOpenVideo = { bvid, cid ->
                    navController.navigate(Screen.VideoPlayer.createRoute(bvid, cid))
                },
                onOpenIndex = { navController.navigate(Screen.PgcIndex.route) }
            )
        }
        composable(Screen.LiveList.route) {
            com.example.pilinara.ui.pages.livelist.LiveListScreen(
                onBack = { navController.popBackStack() },
                onOpenRoom = { roomId -> navController.navigate(Screen.LiveRoom.createRoute(roomId)) }
            )
        }
        composable(Screen.PgcIndex.route) {
            com.example.pilinara.ui.pages.bangumi.PgcIndexScreen(
                onBack = { navController.popBackStack() },
                onOpenSeason = { sid ->
                    navController.navigate(Screen.Bangumi.createRoute(seasonId = sid))
                }
            )
        }
        composable(Screen.Rank.route) {
            com.example.pilinara.ui.pages.rank.RankScreen(
                onBack = { navController.popBackStack() },
                onOpenVideo = { bvid -> navController.navigate("video/$bvid") }
            )
        }
        composable(Screen.DanmakuBlock.route) {
            com.example.pilinara.ui.settings.DanmakuBlockScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Sessions.route) {
            com.example.pilinara.ui.messages.SessionListScreen(
                onBack = { navController.popBackStack() },
                onGoLogin = { navController.navigate(Screen.Login.route) },
                onOpenChat = { talkerId -> navController.navigate("chat/$talkerId") }
            )
        }
        composable(
            "chat/{talkerId}",
            arguments = listOf(navArgument("talkerId") { type = NavType.LongType })
        ) { backStackEntry ->
            com.example.pilinara.ui.messages.ChatScreen(
                talkerId = backStackEntry.arguments?.getLong("talkerId") ?: 0L,
                onBack = { navController.popBackStack() },
                onGoLogin = { navController.navigate(Screen.Login.route) }
            )
        }
        composable(Screen.Downloads.route) {
            com.example.pilinara.ui.download.DownloadScreen(
                onBack = { navController.popBackStack() },
                onPlayLocal = { bvid -> navController.navigate("video/$bvid?local=1") }
            )
        }
    }
}

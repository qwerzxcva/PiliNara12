package com.example.pilinara.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pilinara.ui.pages.dynamics.DynamicsScreen
import com.example.pilinara.ui.pages.home.HomeScreen
import com.example.pilinara.ui.pages.mine.MineScreen
import com.example.pilinara.ui.login.LoginScreen
import com.example.pilinara.ui.settings.SettingsScreen
import com.example.pilinara.playback.VideoPlayerScreen

@Composable
fun MainApp(viewModel: MainViewModel, onNavigateToSettings: () -> Unit = {}, onNavigateToLogin: () -> Unit = {}) {
    val selected by viewModel.selectedTab.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab,
                        onClick = { 
                            viewModel.selectTab(tab)
                            when (tab) {
                                MainTab.HOME -> navController.navigate("home") { popUpTo("home") { inclusive = true } }
                                MainTab.DYNAMICS -> navController.navigate("dynamics") { popUpTo("dynamics") { inclusive = true } }
                                MainTab.MINE -> navController.navigate("mine") { popUpTo("mine") { inclusive = true } }
                            }
                        },
                        icon = {
                            Icon(
                                if (selected == tab) tab.iconFilled else tab.iconOutlined,
                                contentDescription = stringResource(tab.labelRes),
                            )
                        },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(navController = navController, startDestination = "home") {
                composable("home") { HomeScreen(onVideoClick = { bvid, _ -> navController.navigate("video/$bvid") }) }
                composable("dynamics") { DynamicsScreen() }
                composable("mine") { MineScreen(onSettingsClick = onNavigateToSettings, onLoginClick = onNavigateToLogin) }
                composable("video/{bvid}") { backStackEntry ->
                    val bvid = backStackEntry.arguments?.getString("bvid") ?: ""
                    VideoPlayerScreen(videoUrl = "", bvid = bvid, onBack = { navController.popBackStack() })
                }
                composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
                composable("login") { LoginScreen(onBack = { navController.popBackStack() }, onLoggedIn = { navController.popBackStack() }) }
            }
        }
    }
}

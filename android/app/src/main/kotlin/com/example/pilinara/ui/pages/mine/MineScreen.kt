package com.example.pilinara.ui.pages.mine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.pilinara.data.model.NavData
import com.example.pilinara.data.repository.LoginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 「我的」页状态：登录资料 + 加载中 */
class MineViewModel(
    private val loginRepo: LoginRepository = LoginRepository()
) : ViewModel() {

    private val _nav = MutableStateFlow<NavData?>(null)
    val nav: StateFlow<NavData?> = _nav.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init { refresh() }

    /** 恢复 session 并拉取自身资料 */
    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            val loggedIn = loginRepo.restoreSession()
            if (loggedIn) {
                _nav.value = loginRepo.fetchSelfInfo().getOrNull()?.data
            } else {
                _nav.value = null
            }
            _loading.value = false
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            loginRepo.logout()
            _nav.value = null
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineScreen(
    onOpenLogin: () -> Unit = {},
    onOpenFavorites: (Long) -> Unit = {},
    onOpenHistory: () -> Unit = {},
    viewModel: MineViewModel = viewModel()
) {
    val nav by viewModel.nav.collectAsState()
    val isLoggedIn = nav != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的") },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (isLoggedIn) {
                val n = nav!!
                UserHeader(
                    uname = n.uname.ifEmpty { "用户${n.mid}" },
                    mid = n.mid,
                    face = n.face,
                    level = n.levelInfo?.currentLevel ?: 0,
                    isVip = n.vipStatus == 1
                )
                Spacer(Modifier.height(12.dp))
                QuickActions(
                    onHistory = onOpenHistory,
                    onFavorites = { onOpenFavorites(n.mid) }
                )
                Spacer(Modifier.height(12.dp))
                SectionDivider("账号")
                var showLogout by remember { mutableStateOf(false) }
                SettingRow(icon = Icons.Default.Logout, title = "退出登录") { showLogout = true }
                if (showLogout) {
                    AlertDialog(
                        onDismissRequest = { showLogout = false },
                        title = { Text("退出登录") },
                        text = { Text("确定要退出当前账号吗？") },
                        confirmButton = {
                            TextButton(onClick = {
                                showLogout = false
                                viewModel.logout { }
                            }) { Text("退出") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLogout = false }) { Text("取消") }
                        }
                    )
                }
            } else {
                LoginPrompt(onOpenLogin)
            }
        }
    }
}

@Composable
fun UserHeader(uname: String, mid: Long, face: String, level: Int, isVip: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            if (face.isNotEmpty()) {
                AsyncImage(
                    model = face,
                    contentDescription = "头像",
                    modifier = Modifier.size(64.dp)
                )
            } else {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(32.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // User info
            Column(modifier = Modifier.weight(1f)) {
                Text(uname, style = MaterialTheme.typography.titleLarge)
                Text("UID: $mid", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Badge(text = "Lv.$level")
                    if (isVip) Badge(text = "大会员")
                }
            }
        }
    }
}

@Composable
fun QuickActions(onHistory: () -> Unit = {}, onFavorites: () -> Unit = {}) {
    val actions = listOf(
        Triple("历史", Icons.Default.History, onHistory),
        Triple("收藏", Icons.Default.Favorite, onFavorites),
        Triple("离线缓存", Icons.Default.Download, {} as () -> Unit),
        Triple("稍后再看", Icons.Default.PlayArrow, {} as () -> Unit)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        actions.forEach { (label, icon, onClick) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onClick() }
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 15.sp)
    }
}

@Composable
fun SectionDivider(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun LoginPrompt(onOpenLogin: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("登录后享受更多功能", style = MaterialTheme.typography.titleMedium)
        Text(
            "同步收藏、观看历史与追番进度",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onOpenLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("扫码登录")
        }
    }
}

@Composable
fun Badge(text: String) {
    Surface(
        modifier = Modifier.padding(vertical = 2.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

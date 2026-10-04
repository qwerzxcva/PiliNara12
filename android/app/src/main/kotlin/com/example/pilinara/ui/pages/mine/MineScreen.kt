package com.example.pilinara.ui.pages.mine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/** 「我的」页状态：真实登录资料 */
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
            _nav.value = if (loggedIn) loginRepo.fetchSelfInfo().getOrNull()?.data else null
            _loading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            loginRepo.logout()
            _nav.value = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineScreen(
    onSettingsClick: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onFavoritesClick: (Long) -> Unit = {},
) {
    val nav by viewModel.nav.collectAsState()
    val isLoggedIn = nav != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        when {
            nav != null -> {
                val n = nav!!
                UserContent(
                    modifier = Modifier.padding(padding),
                    nav = n,
                    onHistoryClick = onHistoryClick,
                    onFavoritesClick = { onFavoritesClick(n.mid) },
                    onLogout = { viewModel.logout() }
                )
            }
            else -> LoginContent(modifier = Modifier.padding(padding), onLoginClick = onLoginClick)
        }
    }
}

@Composable
fun LoginContent(modifier: Modifier = Modifier, onLoginClick: () -> Unit = {}) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("PiliNara", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(8.dp))
        Text("登录后享受更多功能", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("同步收藏、观看历史与追番进度",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))

        Spacer(modifier = Modifier.height(32.dp))

        // 只保留扫码登录：B站 Web 端仅支持二维码 / 密码 / 短信，无第三方微信登录
        Button(
            onClick = onOpenLogin,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.QrCode, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("扫码登录", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("登录即表示同意《用户协议》和《隐私政策》",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
    }
}

@Composable
fun UserContent(
    modifier: Modifier = Modifier,
    nav: NavData,
    onHistoryClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { UserCard(nav = nav) }
        item { QuickActions(onHistory = onHistoryClick, onFavorites = onFavoritesClick) }
        item { HorizontalDivider() }
        item { MenuItemRow(MenuItemData("历史观看", Icons.Default.History, onHistoryClick)) }
        item { MenuItemRow(MenuItemData("我的收藏", Icons.Default.Favorite, onFavoritesClick)) }
        item { MenuItemRow(MenuItemData("设置", Icons.Default.Settings, {})) }
        item { MenuItemRow(MenuItemData("退出登录", Icons.AutoMirrored.Filled.Logout, onLogout)) }
    }
}

@Composable
fun UserCard(nav: NavData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (nav.face.isNotEmpty()) {
                    AsyncImage(
                        model = nav.face,
                        contentDescription = "头像",
                        modifier = Modifier.size(56.dp)
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(nav.uname.ifEmpty { "用户${nav.mid}" }, style = MaterialTheme.typography.titleLarge)
                    Text("UID: ${nav.mid}", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Badge(text = "Lv.${nav.levelInfo?.currentLevel ?: 0}")
                        if (nav.vipStatus == 1) Badge(text = "大会员")
                    }
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
        modifier = Modifier.fillMaxWidth(),
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
                        Icon(icon, contentDescription = label,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun MenuItemRow(item: MenuItemData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(item.icon, contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.width(16.dp))
        Text(item.title, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider()
}

data class MenuItemData(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

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

package com.example.piliai.ui.login

import androidx.compose.foundation.Image
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.piliai.data.repository.LoginRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 扫码登录界面状态 */
sealed interface QrState {
    data object Loading : QrState
    data class Ready(val content: String, val key: String) : QrState
    data object Scanned : QrState          // 已扫码待确认
    data object Expired : QrState
    data class Success(val uname: String) : QrState
    data class Error(val message: String) : QrState
}

class LoginViewModel(
    private val repo: LoginRepository = LoginRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<QrState>(QrState.Loading)
    val state: StateFlow<QrState> = _state.asStateFlow()

    private var pollJob: kotlinx.coroutines.Job? = null

    fun start() {
        pollJob?.cancel()
        _state.value = QrState.Loading
        viewModelScope.launch {
            repo.createQr()
                .onSuccess { (url, key) ->
                    _state.value = QrState.Ready(url, key)
                    poll(key)
                }
                .onFailure { _state.value = QrState.Error(it.message ?: "二维码生成失败") }
        }
    }

    private fun poll(key: String) {
        pollJob = viewModelScope.launch {
            repeat(180) {                 // 最长约 3 分钟
                delay(2000)
                val d = repo.pollOnce(key).getOrNull() ?: return@repeat
                when (d.code) {
                    86101 -> Unit                                        // 未扫码
                    86090 -> _state.value = QrState.Scanned              // 已扫码
                    86038 -> { _state.value = QrState.Expired; return@launch }
                    0 -> {
                        // 登录成功：cookie 已由 pollOnce 写入 AccountSession，拉取资料确认
                        val nav = repo.fetchSelfInfo().getOrNull()?.data
                        _state.value = QrState.Success(nav?.uname ?: "已登录")
                        return@launch
                    }
                }
            }
            _state.value = QrState.Expired
        }
    }

    fun stop() { pollJob?.cancel() }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}

/**
 * B站扫码登录页。
 * onLoggedIn: 登录成功后回调（用于刷新「我的」页面）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onBack: () -> Unit = {},
    onLoggedIn: () -> Unit = {},
    viewModel: LoginViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.start() }

    LaunchedEffect(state) {
        if (state is QrState.Success) {
            delay(600)
            onLoggedIn()
        }
    }

    // 记住最后一次成功生成二维码的内容，供「已扫码」状态做半透明底色
    var lastQrContent by remember { mutableStateOf("") }
    LaunchedEffect(state) {
        (state as? QrState.Ready)?.let { lastQrContent = it.content }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("扫码登录") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.stop(); onBack() }) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "请使用 Bilibili 手机客户端扫码登录",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                when (val s = state) {
                    is QrState.Loading -> CircularProgressIndicator()

                    is QrState.Ready -> QrCodeImage(s.content)

                    is QrState.Scanned -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            QrCodeImage(lastQrContent, alpha = 0.3f)
                            Text("已扫码，请在手机上确认", fontWeight = FontWeight.Bold)
                        }
                    }

                    is QrState.Expired -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("二维码已过期")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { viewModel.start() }) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("刷新")
                            }
                        }
                    }

                    is QrState.Success -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✅ 登录成功", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(s.uname, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    is QrState.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("出错了：${s.message}", textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { viewModel.start() }) { Text("重试") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "登录信息仅保存在本机，用于同步收藏与观看历史",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 用 ZXing 生成并渲染二维码。
 * 依赖：com.google.zxing:core（见 app/build.gradle.kts）
 */
@Composable
fun QrCodeImage(content: String, alpha: Float = 1f) {
    // 审核：Kotlin 2.2 下含 early return 的 remember 易被 lint 判为
    // RememberReturnType(Unit)；显式声明 Bitmap? 类型以明确返回值。
    val bitmap: android.graphics.Bitmap? = remember(content) {
        if (content.isEmpty()) return@remember null
        runCatching {
            val size = 480
            val hints = mapOf(
                com.google.zxing.EncodeHintType.MARGIN to 1,
                com.google.zxing.EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val matrix = com.google.zxing.qrcode.QRCodeWriter()
                .encode(content, com.google.zxing.BarcodeFormat.QR_CODE, size, size, hints)
            val bmp = createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            for (x in 0 until size) for (y in 0 until size) {
                bmp[x, y] = if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
            bmp
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "登录二维码",
            modifier = Modifier.size(220.dp),
            contentScale = ContentScale.Fit,
            alpha = alpha
        )
    } else {
        Text("二维码生成失败")
    }
}

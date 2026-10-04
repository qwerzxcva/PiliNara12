package com.example.pilinara.data.repository

import com.example.pilinara.data.model.*
import com.example.pilinara.data.remote.BiliApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 登录仓库 - 实现登录功能
 */
class LoginRepository(private val apiClient: BiliApiClient = BiliApiClient()) {
    
    data class LoginState(
        val isLogin: Boolean = false,
        val userInfo: UserInfoData? = null,
        val errorMessage: String? = null
    )
    
    private val _loginState = MutableStateFlow(LoginState())
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()
    
    /**
     * 检查登录状态
     */
    suspend fun checkLoginStatus(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiClient.getUserInfo(0L)
            
            response.onSuccess { resp ->
                if (resp.code == 0 && resp.data != null) {
                    _loginState.value = LoginState(
                        isLogin = true,
                        userInfo = resp.data
                    )
                } else {
                    _loginState.value = LoginState(isLogin = false)
                }
            }.onFailure {
                _loginState.value = LoginState(isLogin = false)
            }
            
            _loginState.value.isLogin
        }
    }
    
    /**
     * 获取用户信息
     */
    suspend fun getUserInfo(uid: Long): Result<UserInfoData> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiClient.getUserInfo(uid)
            
            response.onSuccess { resp ->
                if (resp.code == 0 && resp.data != null) {
                    _loginState.value = _loginState.value.copy(userInfo = resp.data)
                }
            }
            
            _loginState.value.userInfo ?: UserInfoData()
        }
    }
    
    /**
     * 创建二维码登录
     */
    suspend fun createQrLogin(): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        // TODO: 调用 Bilibili API
        runCatching {
            mapOf(
                "qrUrl" to "https://example.com/qr",
                "qrKey" to "temp_key"
            )
        }
    }
    
    /**
     * 轮询二维码状态
     */
    suspend fun pollQrStatus(qrKey: String): Result<Int> = withContext(Dispatchers.IO) {
        // TODO: 轮询 Bilibili API
        runCatching { 0 }
    }
    
    /**
     * 退出登录
     */
    suspend fun logout(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            _loginState.value = LoginState(isLogin = false)
            true
        }
    }
}

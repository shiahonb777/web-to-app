package com.webtoapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtoapp.core.auth.AuthRepository
import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.auth.ProStatus
import com.webtoapp.core.auth.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 认证 ViewModel — 管理登录/注册/用户状态/密码管理/账号注销
 */
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    // ─── 状态 ───

    private val _authState = MutableStateFlow<AuthState>(
        if (authRepository.isLoggedIn()) {
            val user = authRepository.getCachedUser()
            if (user != null) AuthState.LoggedIn(user) else AuthState.LoggedOut
        } else {
            AuthState.LoggedOut
        }
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _loginState = MutableStateFlow<FormState>(FormState.Idle)
    val loginState: StateFlow<FormState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<FormState>(FormState.Idle)
    val registerState: StateFlow<FormState> = _registerState.asStateFlow()

    private val _proStatus = MutableStateFlow<ProStatus?>(null)
    val proStatus: StateFlow<ProStatus?> = _proStatus.asStateFlow()

    // 密码管理状态
    private val _passwordState = MutableStateFlow<FormState>(FormState.Idle)
    val passwordState: StateFlow<FormState> = _passwordState.asStateFlow()

    // 忘记密码状态
    private val _forgotPasswordState = MutableStateFlow<FormState>(FormState.Idle)
    val forgotPasswordState: StateFlow<FormState> = _forgotPasswordState.asStateFlow()

    // 重置验证码发送状态
    private val _resetCodeState = MutableStateFlow<FormState>(FormState.Idle)
    val resetCodeState: StateFlow<FormState> = _resetCodeState.asStateFlow()

    // 账号注销状态
    private val _deleteAccountState = MutableStateFlow<FormState>(FormState.Idle)
    val deleteAccountState: StateFlow<FormState> = _deleteAccountState.asStateFlow()

    // 验证码发送状态
    private val _sendCodeState = MutableStateFlow<FormState>(FormState.Idle)
    val sendCodeState: StateFlow<FormState> = _sendCodeState.asStateFlow()

    // 头像上传状态
    private val _avatarUploadState = MutableStateFlow<FormState>(FormState.Idle)
    val avatarUploadState: StateFlow<FormState> = _avatarUploadState.asStateFlow()

    private var heartbeatJob: Job? = null

    init {
        // 已登录时刷新用户信息
        if (authRepository.isLoggedIn()) {
            viewModelScope.launch {
                when (val result = authRepository.refreshProfile()) {
                    is AuthResult.Success -> {
                        _authState.value = AuthState.LoggedIn(result.data)
                    }
                    is AuthResult.Error -> {
                        // Token 可能已过期
                        if (result.message.contains("过期") || result.message.contains("重新登录")) {
                            _authState.value = AuthState.LoggedOut
                        }
                    }
                }
            }
            startHeartbeat()
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (true) {
                delay(60_000) // 60 seconds
                if (authRepository.isLoggedIn()) {
                    authRepository.heartbeat()
                } else {
                    break
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    // ─── 登录 ───

    fun login(account: String, password: String) {
        if (account.isBlank() || password.isBlank()) {
            _loginState.value = FormState.Error("请填写账号和密码")
            return
        }
        viewModelScope.launch {
            _loginState.value = FormState.Loading
            when (val result = authRepository.login(account, password)) {
                is AuthResult.Success -> {
                    _authState.value = AuthState.LoggedIn(result.data)
                    _loginState.value = FormState.Success("登录成功")
                    loadProStatus()
                    startHeartbeat()
                }
                is AuthResult.Error -> {
                    _loginState.value = FormState.Error(result.message)
                }
            }
        }
    }

    // ─── 发送验证码 ───

    fun sendVerificationCode(email: String) {
        if (email.isBlank()) {
            _sendCodeState.value = FormState.Error("请输入邮箱")
            return
        }
        viewModelScope.launch {
            _sendCodeState.value = FormState.Loading
            when (val result = authRepository.sendVerificationCode(email)) {
                is AuthResult.Success -> {
                    _sendCodeState.value = FormState.Success(result.data)
                }
                is AuthResult.Error -> {
                    _sendCodeState.value = FormState.Error(result.message)
                }
            }
        }
    }

    // ─── 注册 ───

    fun register(email: String, username: String, password: String, confirmPassword: String, verificationCode: String) {
        if (email.isBlank() || username.isBlank() || password.isBlank()) {
            _registerState.value = FormState.Error("请填写所有字段")
            return
        }
        if (verificationCode.isBlank()) {
            _registerState.value = FormState.Error("请输入验证码")
            return
        }
        if (password != confirmPassword) {
            _registerState.value = FormState.Error("两次密码不一致")
            return
        }
        if (password.length < 6) {
            _registerState.value = FormState.Error("密码至少 6 位")
            return
        }
        if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            _registerState.value = FormState.Error("用户名只能包含字母、数字和下划线")
            return
        }
        viewModelScope.launch {
            _registerState.value = FormState.Loading
            when (val result = authRepository.register(email, username, password, verificationCode)) {
                is AuthResult.Success -> {
                    _authState.value = AuthState.LoggedIn(result.data)
                    _registerState.value = FormState.Success("注册成功")
                }
                is AuthResult.Error -> {
                    _registerState.value = FormState.Error(result.message)
                }
            }
        }
    }

    // ─── 退出登录（服务器端 Token 失效） ───

    fun logout() {
        viewModelScope.launch {
            stopHeartbeat()
            authRepository.logout()
            _authState.value = AuthState.LoggedOut
            _proStatus.value = null
        }
    }

    /**
     * 从所有设备登出
     */
    fun logoutAll() {
        viewModelScope.launch {
            authRepository.logoutAll()
            _authState.value = AuthState.LoggedOut
            _proStatus.value = null
        }
    }

    // ─── 刷新 ───

    fun refreshProfile() {
        viewModelScope.launch {
            when (val result = authRepository.refreshProfile()) {
                is AuthResult.Success -> {
                    _authState.value = AuthState.LoggedIn(result.data)
                }
                is AuthResult.Error -> { /* 静默失败 */ }
            }
        }
    }

    private fun loadProStatus() {
        viewModelScope.launch {
            when (val result = authRepository.getProStatus()) {
                is AuthResult.Success -> {
                    _proStatus.value = result.data
                }
                is AuthResult.Error -> { /* 静默失败 */ }
            }
        }
    }

    // ─── 密码管理 ───

    /**
     * 修改密码
     */
    fun changePassword(currentPassword: String, newPassword: String, confirmNewPassword: String) {
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            _passwordState.value = FormState.Error("请填写所有字段")
            return
        }
        if (newPassword != confirmNewPassword) {
            _passwordState.value = FormState.Error("两次新密码不一致")
            return
        }
        if (newPassword.length < 6) {
            _passwordState.value = FormState.Error("新密码至少 6 位")
            return
        }
        viewModelScope.launch {
            _passwordState.value = FormState.Loading
            when (val result = authRepository.changePassword(currentPassword, newPassword)) {
                is AuthResult.Success -> {
                    _passwordState.value = FormState.Success(result.data)
                    // 密码修改后需要重新登录
                    _authState.value = AuthState.LoggedOut
                    _proStatus.value = null
                }
                is AuthResult.Error -> {
                    _passwordState.value = FormState.Error(result.message)
                }
            }
        }
    }

    /**
     * 发送密码重置验证码
     */
    fun sendResetCode(email: String) {
        if (email.isBlank()) {
            _resetCodeState.value = FormState.Error("请输入邮箱")
            return
        }
        viewModelScope.launch {
            _resetCodeState.value = FormState.Loading
            when (val result = authRepository.forgotPassword(email)) {
                is AuthResult.Success -> {
                    _resetCodeState.value = FormState.Success(result.data)
                }
                is AuthResult.Error -> {
                    _resetCodeState.value = FormState.Error(result.message)
                }
            }
        }
    }

    /**
     * 重置密码（验证码方式）
     */
    fun resetPassword(email: String, code: String, newPassword: String) {
        if (email.isBlank() || code.isBlank() || newPassword.isBlank()) {
            _forgotPasswordState.value = FormState.Error("请填写所有字段")
            return
        }
        if (code.length != 6) {
            _forgotPasswordState.value = FormState.Error("验证码为 6 位数字")
            return
        }
        if (newPassword.length < 6) {
            _forgotPasswordState.value = FormState.Error("密码至少 6 位")
            return
        }
        viewModelScope.launch {
            _forgotPasswordState.value = FormState.Loading
            when (val result = authRepository.resetPassword(email, code, newPassword)) {
                is AuthResult.Success -> {
                    // Auto-login: set auth state → UI auto-transitions to ProfileScreen
                    _authState.value = AuthState.LoggedIn(result.data)
                    _forgotPasswordState.value = FormState.Success("密码已重置")
                    loadProStatus()
                    startHeartbeat()
                }
                is AuthResult.Error -> {
                    _forgotPasswordState.value = FormState.Error(result.message)
                }
            }
        }
    }

    // ─── 账号注销 ───

    /**
     * 永久删除账号
     */
    fun deleteAccount(password: String, reason: String = "") {
        if (password.isBlank()) {
            _deleteAccountState.value = FormState.Error("请输入密码确认删除")
            return
        }
        viewModelScope.launch {
            _deleteAccountState.value = FormState.Loading
            when (val result = authRepository.deleteAccount(password, reason)) {
                is AuthResult.Success -> {
                    _deleteAccountState.value = FormState.Success(result.data)
                    _authState.value = AuthState.LoggedOut
                    _proStatus.value = null
                }
                is AuthResult.Error -> {
                    _deleteAccountState.value = FormState.Error(result.message)
                }
            }
        }
    }

    // ─── 头像上传 ───

    /**
     * 上传头像
     */
    fun uploadAvatar(imageBytes: ByteArray, mimeType: String = "image/jpeg") {
        viewModelScope.launch {
            _avatarUploadState.value = FormState.Loading
            when (val result = authRepository.uploadAvatar(imageBytes, mimeType)) {
                is AuthResult.Success -> {
                    _avatarUploadState.value = FormState.Success("头像上传成功")
                    // 刷新用户信息以获取新头像 URL
                    refreshProfile()
                }
                is AuthResult.Error -> {
                    _avatarUploadState.value = FormState.Error(result.message)
                }
            }
        }
    }

    // ─── Google 登录 ───

    fun googleLogin(idToken: String) {
        viewModelScope.launch {
            _loginState.value = FormState.Loading
            when (val result = authRepository.googleLogin(idToken)) {
                is AuthResult.Success -> {
                    _authState.value = AuthState.LoggedIn(result.data)
                    _loginState.value = FormState.Success("Google 登录成功")
                    loadProStatus()
                    startHeartbeat()
                }
                is AuthResult.Error -> {
                    _loginState.value = FormState.Error(result.message)
                }
            }
        }
    }

    // ─── 重置表单状态 ───

    fun resetLoginState() {
        _loginState.value = FormState.Idle
    }

    fun resetRegisterState() {
        _registerState.value = FormState.Idle
    }

    fun resetPasswordState() {
        _passwordState.value = FormState.Idle
    }

    fun resetForgotPasswordState() {
        _forgotPasswordState.value = FormState.Idle
    }

    fun resetResetCodeState() {
        _resetCodeState.value = FormState.Idle
    }

    fun resetDeleteAccountState() {
        _deleteAccountState.value = FormState.Idle
    }

    fun resetSendCodeState() {
        _sendCodeState.value = FormState.Idle
    }

    fun resetAvatarUploadState() {
        _avatarUploadState.value = FormState.Idle
    }

    // ─── 编辑个人资料 ───

    private val _updateProfileState = MutableStateFlow<FormState>(FormState.Idle)
    val updateProfileState: StateFlow<FormState> = _updateProfileState.asStateFlow()

    fun updateProfile(username: String? = null) {
        viewModelScope.launch {
            _updateProfileState.value = FormState.Loading
            when (val result = authRepository.updateProfile(username = username)) {
                is AuthResult.Success -> {
                    _updateProfileState.value = FormState.Success("资料已更新")
                    _authState.value = AuthState.LoggedIn(result.data)
                }
                is AuthResult.Error -> {
                    _updateProfileState.value = FormState.Error(result.message)
                }
            }
        }
    }

    fun resetUpdateProfileState() {
        _updateProfileState.value = FormState.Idle
    }

    // ─── 绑定邮箱 ───

    private val _bindEmailState = MutableStateFlow<FormState>(FormState.Idle)
    val bindEmailState: StateFlow<FormState> = _bindEmailState.asStateFlow()

    fun bindEmail(email: String, verificationCode: String) {
        if (email.isBlank()) {
            _bindEmailState.value = FormState.Error("请输入邮箱")
            return
        }
        if (verificationCode.isBlank()) {
            _bindEmailState.value = FormState.Error("请输入验证码")
            return
        }
        viewModelScope.launch {
            _bindEmailState.value = FormState.Loading
            when (val result = authRepository.bindEmail(email, verificationCode)) {
                is AuthResult.Success -> {
                    _bindEmailState.value = FormState.Success(result.data)
                    refreshProfile() // 刷新用户信息
                }
                is AuthResult.Error -> {
                    _bindEmailState.value = FormState.Error(result.message)
                }
            }
        }
    }

    fun resetBindEmailState() {
        _bindEmailState.value = FormState.Idle
    }

    // ─── 刷新 Pro 状态（公开方法，外部可调用） ───

    fun refreshProStatus() {
        loadProStatus()
    }
}

// ─── 状态定义 ───

sealed class AuthState {
    data object LoggedOut : AuthState()
    data class LoggedIn(val user: UserProfile) : AuthState()
}

sealed class FormState {
    data object Idle : FormState()
    data object Loading : FormState()
    data class Success(val message: String) : FormState()
    data class Error(val message: String) : FormState()
}

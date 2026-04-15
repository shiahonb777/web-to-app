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
 * Auth ViewModel - handles login/register/user state/password/account deletion.
 */
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    // --- States ---

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

    // Password state
    private val _passwordState = MutableStateFlow<FormState>(FormState.Idle)
    val passwordState: StateFlow<FormState> = _passwordState.asStateFlow()

    // Forgot-password state
    private val _forgotPasswordState = MutableStateFlow<FormState>(FormState.Idle)
    val forgotPasswordState: StateFlow<FormState> = _forgotPasswordState.asStateFlow()

    // Reset-code send state
    private val _resetCodeState = MutableStateFlow<FormState>(FormState.Idle)
    val resetCodeState: StateFlow<FormState> = _resetCodeState.asStateFlow()

    // Account deletion state
    private val _deleteAccountState = MutableStateFlow<FormState>(FormState.Idle)
    val deleteAccountState: StateFlow<FormState> = _deleteAccountState.asStateFlow()

    // Verification-code send state
    private val _sendCodeState = MutableStateFlow<FormState>(FormState.Idle)
    val sendCodeState: StateFlow<FormState> = _sendCodeState.asStateFlow()

    // Avatar upload state
    private val _avatarUploadState = MutableStateFlow<FormState>(FormState.Idle)
    val avatarUploadState: StateFlow<FormState> = _avatarUploadState.asStateFlow()

    private var heartbeatJob: Job? = null

    init {
        // Refresh profile when already signed in
        if (authRepository.isLoggedIn()) {
            viewModelScope.launch {
                when (val result = authRepository.refreshProfile()) {
                    is AuthResult.Success -> {
                        _authState.value = AuthState.LoggedIn(result.data)
                    }
                    is AuthResult.Error -> {
                        // Token may have expired
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

    // --- Login ---

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

    // --- Send Verification Code ---

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

    // --- Register ---

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

    // --- Logout (invalidate server token) ---

    fun logout() {
        viewModelScope.launch {
            stopHeartbeat()
            authRepository.logout()
            _authState.value = AuthState.LoggedOut
            _proStatus.value = null
        }
    }

    /**
     * Log out from all devices.
     */
    fun logoutAll() {
        viewModelScope.launch {
            authRepository.logoutAll()
            _authState.value = AuthState.LoggedOut
            _proStatus.value = null
        }
    }

    // --- Refresh ---

    fun refreshProfile() {
        viewModelScope.launch {
            when (val result = authRepository.refreshProfile()) {
                is AuthResult.Success -> {
                    _authState.value = AuthState.LoggedIn(result.data)
                }
                is AuthResult.Error -> { /* Comment */ }
            }
        }
    }

    private fun loadProStatus() {
        viewModelScope.launch {
            when (val result = authRepository.getProStatus()) {
                is AuthResult.Success -> {
                    _proStatus.value = result.data
                }
                is AuthResult.Error -> { /* Comment */ }
            }
        }
    }

    // --- Password Management ---

    /**
     * Change password.
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
                    // Re-login required after password change
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
     * Send password reset code.
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
     * Reset password (verification code flow).
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

    // --- Account Deletion ---

    /**
     * Permanently delete account.
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

    // --- Avatar Upload ---

    /**
     * Upload avatar.
     */
    fun uploadAvatar(imageBytes: ByteArray, mimeType: String = "image/jpeg") {
        viewModelScope.launch {
            _avatarUploadState.value = FormState.Loading
            when (val result = authRepository.uploadAvatar(imageBytes, mimeType)) {
                is AuthResult.Success -> {
                    _avatarUploadState.value = FormState.Success("头像上传成功")
                    // Refresh profile to get new avatar URL
                    refreshProfile()
                }
                is AuthResult.Error -> {
                    _avatarUploadState.value = FormState.Error(result.message)
                }
            }
        }
    }

    // --- Google Login ---

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

    // --- Reset Form State ---

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

    // --- Edit Profile ---

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

    // --- Bind Email ---

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
                    refreshProfile() // Comment
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

    // --- Refresh Pro State (public, callable externally) ---

    fun refreshProStatus() {
        loadProStatus()
    }
}

// --- State Definitions ---

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

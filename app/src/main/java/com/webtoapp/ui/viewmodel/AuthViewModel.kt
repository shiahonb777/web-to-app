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
import com.webtoapp.core.i18n.Strings
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch




class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {



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


    private val _passwordState = MutableStateFlow<FormState>(FormState.Idle)
    val passwordState: StateFlow<FormState> = _passwordState.asStateFlow()


    private val _forgotPasswordState = MutableStateFlow<FormState>(FormState.Idle)
    val forgotPasswordState: StateFlow<FormState> = _forgotPasswordState.asStateFlow()


    private val _resetCodeState = MutableStateFlow<FormState>(FormState.Idle)
    val resetCodeState: StateFlow<FormState> = _resetCodeState.asStateFlow()


    private val _deleteAccountState = MutableStateFlow<FormState>(FormState.Idle)
    val deleteAccountState: StateFlow<FormState> = _deleteAccountState.asStateFlow()


    private val _sendCodeState = MutableStateFlow<FormState>(FormState.Idle)
    val sendCodeState: StateFlow<FormState> = _sendCodeState.asStateFlow()


    private val _avatarUploadState = MutableStateFlow<FormState>(FormState.Idle)
    val avatarUploadState: StateFlow<FormState> = _avatarUploadState.asStateFlow()

    private var heartbeatJob: Job? = null

    init {

        if (authRepository.isLoggedIn()) {
            viewModelScope.launch {
                when (val result = authRepository.refreshProfile()) {
                    is AuthResult.Success -> {
                        _authState.value = AuthState.LoggedIn(result.data)
                    }
                    is AuthResult.Error -> {

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
            while (isActive) {
                delay(60_000)
                if (!authRepository.isLoggedIn()) break
                authRepository.heartbeat()
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }



    fun login(account: String, password: String) {
        if (account.isBlank() || password.isBlank()) {
            _loginState.value = FormState.Error(Strings.authLoginFillAccountPassword)
            return
        }
        viewModelScope.launch {
            _loginState.value = FormState.Loading
            when (val result = authRepository.login(account, password)) {
                is AuthResult.Success -> {
                    _authState.value = AuthState.LoggedIn(result.data)
                    _loginState.value = FormState.Success(Strings.authLoginSuccess)
                    loadProStatus()
                    startHeartbeat()
                }
                is AuthResult.Error -> {
                    _loginState.value = FormState.Error(result.message)
                }
            }
        }
    }



    fun sendVerificationCode(email: String) {
        if (email.isBlank()) {
            _sendCodeState.value = FormState.Error(Strings.authEmailRequired)
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



    fun register(email: String, username: String, password: String, confirmPassword: String, verificationCode: String) {
        if (email.isBlank() || username.isBlank() || password.isBlank()) {
            _registerState.value = FormState.Error(Strings.authFillAllFields)
            return
        }
        if (verificationCode.isBlank()) {
            _registerState.value = FormState.Error(Strings.authCodeRequired)
            return
        }
        if (password != confirmPassword) {
            _registerState.value = FormState.Error(Strings.authPasswordMismatch)
            return
        }
        if (password.length < 6) {
            _registerState.value = FormState.Error(Strings.authPasswordMinLength)
            return
        }
        if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            _registerState.value = FormState.Error(Strings.authUsernameFormatError)
            return
        }
        viewModelScope.launch {
            _registerState.value = FormState.Loading
            when (val result = authRepository.register(email, username, password, verificationCode)) {
                is AuthResult.Success -> {
                    _authState.value = AuthState.LoggedIn(result.data)
                    _registerState.value = FormState.Success(Strings.authRegisterSuccess)
                }
                is AuthResult.Error -> {
                    _registerState.value = FormState.Error(result.message)
                }
            }
        }
    }



    fun logout() {
        viewModelScope.launch {
            stopHeartbeat()
            authRepository.logout()
            _authState.value = AuthState.LoggedOut
            _proStatus.value = null
        }
    }




    fun logoutAll() {
        viewModelScope.launch {
            authRepository.logoutAll()
            _authState.value = AuthState.LoggedOut
            _proStatus.value = null
        }
    }



    fun refreshProfile() {
        viewModelScope.launch {
            when (val result = authRepository.refreshProfile()) {
                is AuthResult.Success -> {
                    _authState.value = AuthState.LoggedIn(result.data)
                }
                is AuthResult.Error -> {  }
            }
        }
    }

    private fun loadProStatus() {
        viewModelScope.launch {
            when (val result = authRepository.getProStatus()) {
                is AuthResult.Success -> {
                    _proStatus.value = result.data
                }
                is AuthResult.Error -> {  }
            }
        }
    }






    fun changePassword(currentPassword: String, newPassword: String, confirmNewPassword: String) {
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            _passwordState.value = FormState.Error(Strings.authFillAllFields)
            return
        }
        if (newPassword != confirmNewPassword) {
            _passwordState.value = FormState.Error(Strings.authNewPasswordMismatch)
            return
        }
        if (newPassword.length < 6) {
            _passwordState.value = FormState.Error(Strings.authNewPasswordMinLength)
            return
        }
        viewModelScope.launch {
            _passwordState.value = FormState.Loading
            when (val result = authRepository.changePassword(currentPassword, newPassword)) {
                is AuthResult.Success -> {
                    _passwordState.value = FormState.Success(result.data)

                    _authState.value = AuthState.LoggedOut
                    _proStatus.value = null
                }
                is AuthResult.Error -> {
                    _passwordState.value = FormState.Error(result.message)
                }
            }
        }
    }




    fun sendResetCode(email: String) {
        if (email.isBlank()) {
            _resetCodeState.value = FormState.Error(Strings.authEmailRequired)
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




    fun resetPassword(email: String, code: String, newPassword: String) {
        if (email.isBlank() || code.isBlank() || newPassword.isBlank()) {
            _forgotPasswordState.value = FormState.Error(Strings.authFillAllFields)
            return
        }
        if (code.length != 6) {
            _forgotPasswordState.value = FormState.Error(Strings.authCodeSixDigits)
            return
        }
        if (newPassword.length < 6) {
            _forgotPasswordState.value = FormState.Error(Strings.authPasswordMinLength)
            return
        }
        viewModelScope.launch {
            _forgotPasswordState.value = FormState.Loading
            when (val result = authRepository.resetPassword(email, code, newPassword)) {
                is AuthResult.Success -> {

                    _authState.value = AuthState.LoggedIn(result.data)
                    _forgotPasswordState.value = FormState.Success(Strings.authPasswordResetDone)
                    loadProStatus()
                    startHeartbeat()
                }
                is AuthResult.Error -> {
                    _forgotPasswordState.value = FormState.Error(result.message)
                }
            }
        }
    }






    fun deleteAccount(password: String, reason: String = "") {
        if (password.isBlank()) {
            _deleteAccountState.value = FormState.Error(Strings.authDeleteAccountConfirmPassword)
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






    fun uploadAvatar(imageBytes: ByteArray, mimeType: String = "image/jpeg") {
        viewModelScope.launch {
            _avatarUploadState.value = FormState.Loading
            when (val result = authRepository.uploadAvatar(imageBytes, mimeType)) {
                is AuthResult.Success -> {
                    _avatarUploadState.value = FormState.Success(Strings.authAvatarUploadSuccess)

                    refreshProfile()
                }
                is AuthResult.Error -> {
                    _avatarUploadState.value = FormState.Error(result.message)
                }
            }
        }
    }



    fun googleLogin(idToken: String) {
        viewModelScope.launch {
            _loginState.value = FormState.Loading
            when (val result = authRepository.googleLogin(idToken)) {
                is AuthResult.Success -> {
                    _authState.value = AuthState.LoggedIn(result.data)
                    _loginState.value = FormState.Success(Strings.authGoogleLoginSuccess)
                    loadProStatus()
                    startHeartbeat()
                }
                is AuthResult.Error -> {
                    _loginState.value = FormState.Error(result.message)
                }
            }
        }
    }



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



    private val _updateProfileState = MutableStateFlow<FormState>(FormState.Idle)
    val updateProfileState: StateFlow<FormState> = _updateProfileState.asStateFlow()

    fun updateProfile(username: String? = null) {
        viewModelScope.launch {
            _updateProfileState.value = FormState.Loading
            when (val result = authRepository.updateProfile(username = username)) {
                is AuthResult.Success -> {
                    _updateProfileState.value = FormState.Success(Strings.authProfileUpdated)
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



    private val _bindEmailState = MutableStateFlow<FormState>(FormState.Idle)
    val bindEmailState: StateFlow<FormState> = _bindEmailState.asStateFlow()

    fun bindEmail(email: String, verificationCode: String) {
        if (email.isBlank()) {
            _bindEmailState.value = FormState.Error(Strings.authEmailRequired)
            return
        }
        if (verificationCode.isBlank()) {
            _bindEmailState.value = FormState.Error(Strings.authBindEmailCodeRequired)
            return
        }
        viewModelScope.launch {
            _bindEmailState.value = FormState.Loading
            when (val result = authRepository.bindEmail(email, verificationCode)) {
                is AuthResult.Success -> {
                    _bindEmailState.value = FormState.Success(result.data)
                    refreshProfile()
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



    fun refreshProStatus() {
        loadProStatus()
    }
}



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

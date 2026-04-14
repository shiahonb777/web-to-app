package com.webtoapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import com.webtoapp.R
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumOutlinedButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.core.auth.GoogleSignInHelper
import com.webtoapp.core.auth.GoogleSignInResult
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.ui.viewmodel.AuthViewModel
import com.webtoapp.ui.viewmodel.FormState
import kotlinx.coroutines.launch
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard

/**
 * & register
 *
 * , register and
 */
@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val loginState by authViewModel.loginState.collectAsStateWithLifecycle()
    val registerState by authViewModel.registerState.collectAsStateWithLifecycle()
    val forgotPasswordState by authViewModel.forgotPasswordState.collectAsStateWithLifecycle()
    val resetCodeState by authViewModel.resetCodeState.collectAsStateWithLifecycle()
    val sendCodeState by authViewModel.sendCodeState.collectAsStateWithLifecycle()

    // Tab state: 0 =, 1 = register
    var selectedTab by remember { mutableIntStateOf(0) }

    // mode
    var showForgotPassword by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var resetCode by remember { mutableStateOf("") }
    var resetNewPassword by remember { mutableStateOf("") }
    var resetCodeCountdown by remember { mutableIntStateOf(0) }

    // Note
    var loginAccount by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // register
    var regEmail by remember { mutableStateOf("") }
    var regUsername by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regConfirmPasswordVisible by remember { mutableStateOf(false) }
    var regVerificationCode by remember { mutableStateOf("") }

    // verify
    var codeCountdown by remember { mutableIntStateOf(0) }
    
    // Google loadstate
    var googleLoading by remember { mutableStateOf(false) }
    
    // registersuccessannouncementdialog
    var showAnnouncement by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // /register
    LaunchedEffect(loginState) {
        when (loginState) {
            is FormState.Success -> {
                onLoginSuccess()
                authViewModel.resetLoginState()
            }
            is FormState.Error -> {
                snackbarHostState.showSnackbar((loginState as FormState.Error).message)
                authViewModel.resetLoginState()
            }
            else -> {}
        }
    }

    LaunchedEffect(registerState) {
        when (registerState) {
            is FormState.Success -> {
                // registersuccess → display announcement
                showAnnouncement = true
                authViewModel.resetRegisterState()
            }
            is FormState.Error -> {
                snackbarHostState.showSnackbar((registerState as FormState.Error).message)
                authViewModel.resetRegisterState()
            }
            else -> {}
        }
    }
    
    // registersuccessannouncementdialog
    if (showAnnouncement) {
        AlertDialog(
            onDismissRequest = { /* close */ },
            icon = {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = AppStringsProvider.current().authRegisterSuccess,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = AppStringsProvider.current().authWelcomeMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "目前云端相关功能（云端构建、云端同步、远程管理等）仍在开发中，当前功能尚不完善，部分服务可能暂时不可用。\n\n我们正在积极开发中，敬请期待后续更新！",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                PremiumButton(
                    onClick = {
                        showAnnouncement = false
                        onLoginSuccess()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(AppStringsProvider.current().authConfirm)
                }
            }
        )
    }

    // verify
    LaunchedEffect(sendCodeState) {
        when (sendCodeState) {
            is FormState.Success -> {
                snackbarHostState.showSnackbar((sendCodeState as FormState.Success).message)
                codeCountdown = 60  // 60
                authViewModel.resetSendCodeState()
            }
            is FormState.Error -> {
                snackbarHostState.showSnackbar((sendCodeState as FormState.Error).message)
                authViewModel.resetSendCodeState()
            }
            else -> {}
        }
    }

    // Note
    LaunchedEffect(codeCountdown) {
        if (codeCountdown > 0) {
            kotlinx.coroutines.delay(1000)
            codeCountdown--
        }
    }

    // resetverify
    LaunchedEffect(resetCodeState) {
        when (resetCodeState) {
            is FormState.Success -> {
                snackbarHostState.showSnackbar((resetCodeState as FormState.Success).message)
                resetCodeCountdown = 60
                authViewModel.resetResetCodeState()
            }
            is FormState.Error -> {
                snackbarHostState.showSnackbar((resetCodeState as FormState.Error).message)
                authViewModel.resetResetCodeState()
            }
            else -> {}
        }
    }

    // resetverify
    LaunchedEffect(resetCodeCountdown) {
        if (resetCodeCountdown > 0) {
            kotlinx.coroutines.delay(1000)
            resetCodeCountdown--
        }
    }

    // reset
    LaunchedEffect(forgotPasswordState) {
        when (forgotPasswordState) {
            is FormState.Success -> {
                // resetsuccess → authState LoggedIn → ProfileScreen
                snackbarHostState.showSnackbar(AppStringsProvider.current().authPasswordResetSuccess)
                showForgotPassword = false
                resetCode = ""
                resetNewPassword = ""
                forgotPasswordEmail = ""
                onLoginSuccess()
                authViewModel.resetForgotPasswordState()
            }
            is FormState.Error -> {
                snackbarHostState.showSnackbar((forgotPasswordState as FormState.Error).message)
                authViewModel.resetForgotPasswordState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (showForgotPassword) "重置密码" else AppStringsProvider.current().authCloudService,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (showForgotPassword) {
                        IconButton(onClick = {
                            showForgotPassword = false
                            resetCode = ""
                            resetNewPassword = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = AppStringsProvider.current().back)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Logo
                Image(
                    painter = painterResource(id = R.drawable.ic_cloud_service),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                        MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (showForgotPassword) "输入邮箱，获取验证码重置密码"
                           else AppStringsProvider.current().authCloudDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Note
                if (showForgotPassword) {
                    ForgotPasswordForm(
                        authViewModel = authViewModel,
                        forgotPasswordState = forgotPasswordState,
                        resetCodeState = resetCodeState,
                        email = forgotPasswordEmail,
                        onEmailChange = { forgotPasswordEmail = it },
                        resetCode = resetCode,
                        onResetCodeChange = { if (it.length <= 6) resetCode = it.filter { c -> c.isDigit() } },
                        resetNewPassword = resetNewPassword,
                        onResetNewPasswordChange = { resetNewPassword = it },
                        resetCodeCountdown = resetCodeCountdown,
                        onBackToLogin = {
                            showForgotPassword = false
                            resetCode = ""
                            resetNewPassword = ""
                        }
                    )
                } else {
                    // / registerlabel
                    // Tab switch
                    EnhancedElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        ) {
                            listOf(AppStringsProvider.current().authLogin, AppStringsProvider.current().authRegister).forEachIndexed { index, title ->
                                val isSelected = selectedTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(weight = 1f, fill = true)
                                        .clip(RoundedCornerShape(12.dp))
                                        .then(
                                            if (isSelected)
                                                Modifier.background(MaterialTheme.colorScheme.primary)
                                            else
                                                Modifier.clickable { selectedTab = index }
                                        )
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // area
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { it } + fadeIn() togetherWith
                                        slideOutHorizontally { -it } + fadeOut()
                            } else {
                                slideInHorizontally { -it } + fadeIn() togetherWith
                                        slideOutHorizontally { it } + fadeOut()
                            }
                        },
                        label = "auth_tab"
                    ) { tab ->
                        when (tab) {
                            0 -> {
                                // Note
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    OutlinedTextField(
                                        value = loginAccount,
                                        onValueChange = { loginAccount = it },
                                        label = { Text(AppStringsProvider.current().authUsernameOrEmail) },
                                        placeholder = { Text(AppStringsProvider.current().authInputUsernameOrEmail) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Person, null, modifier = Modifier.size(20.dp))
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                        )
                                    )

                                    OutlinedTextField(
                                        value = loginPassword,
                                        onValueChange = { loginPassword = it },
                                        label = { Text(AppStringsProvider.current().authPassword) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(20.dp))
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                                Icon(
                                                    if (loginPasswordVisible) Icons.Filled.VisibilityOff
                                                    else Icons.Filled.Visibility,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        visualTransformation = if (loginPasswordVisible)
                                            VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                focusManager.clearFocus()
                                                authViewModel.login(loginAccount, loginPassword)
                                            }
                                        )
                                    )

                                    // Note
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = AppStringsProvider.current().authForgotPassword,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.clickable {
                                                forgotPasswordEmail = loginAccount
                                                showForgotPassword = true
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    PremiumButton(
                                        onClick = {
                                            focusManager.clearFocus()
                                            authViewModel.login(loginAccount, loginPassword)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = loginState !is FormState.Loading
                                    ) {
                                        if (loginState is FormState.Loading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            text = if (loginState is FormState.Loading)
                                                AppStringsProvider.current().authLoggingIn else AppStringsProvider.current().authLogin,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }

                                    // Google
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        HorizontalDivider(
                                            modifier = Modifier.weight(weight = 1f, fill = true),
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                        Text(
                                            text = AppStringsProvider.current().authOr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier.weight(weight = 1f, fill = true),
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }

                                    // Google button
                                    val context = LocalContext.current
                                    val coroutineScope = rememberCoroutineScope()

                                    PremiumOutlinedButton(
                                        onClick = {
                                            googleLoading = true
                                            coroutineScope.launch {
                                                when (val result = GoogleSignInHelper.getGoogleIdToken(context)) {
                                                    is GoogleSignInResult.Success -> {
                                                        authViewModel.googleLogin(result.idToken)
                                                        googleLoading = false
                                                    }
                                                    is GoogleSignInResult.Cancelled -> {
                                                        googleLoading = false
                                                    }
                                                    is GoogleSignInResult.Error -> {
                                                        googleLoading = false
                                                        coroutineScope.launch {
                                                            snackbarHostState.showSnackbar(result.message)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !googleLoading && loginState !is FormState.Loading,
                                        border = ButtonDefaults.outlinedButtonBorder
                                    ) {
                                        if (googleLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            text = "G",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4285F4)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (googleLoading)
                                                AppStringsProvider.current().authLoggingInWithGoogle else AppStringsProvider.current().authGoogleLogin,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }

                                    // switch register
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = AppStringsProvider.current().authNoAccount,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = AppStringsProvider.current().authRegisterNow,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { selectedTab = 1 }
                                        )
                                    }
                                }
                            }
                            1 -> {
                                // register
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    OutlinedTextField(
                                        value = regEmail,
                                        onValueChange = { regEmail = it },
                                        label = { Text(AppStringsProvider.current().authEmail) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Email, null, modifier = Modifier.size(20.dp))
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Email,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                        )
                                    )

                                    OutlinedTextField(
                                        value = regUsername,
                                        onValueChange = { regUsername = it },
                                        label = { Text(AppStringsProvider.current().authUsername) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Person, null, modifier = Modifier.size(20.dp))
                                        },
                                        supportingText = {
                                            Text(
                                                AppStringsProvider.current().authUsernameHint,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                        )
                                    )

                                    OutlinedTextField(
                                        value = regPassword,
                                        onValueChange = { regPassword = it },
                                        label = { Text(AppStringsProvider.current().authPassword) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(20.dp))
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                                Icon(
                                                    if (regPasswordVisible) Icons.Filled.VisibilityOff
                                                    else Icons.Filled.Visibility,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        },
                                        supportingText = {
                                            Text(
                                                AppStringsProvider.current().authPasswordHint,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        visualTransformation = if (regPasswordVisible)
                                            VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                        )
                                    )

                                    OutlinedTextField(
                                        value = regConfirmPassword,
                                        onValueChange = { regConfirmPassword = it },
                                        label = { Text(AppStringsProvider.current().authConfirmPassword) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(20.dp))
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { regConfirmPasswordVisible = !regConfirmPasswordVisible }) {
                                                Icon(
                                                    if (regConfirmPasswordVisible) Icons.Filled.VisibilityOff
                                                    else Icons.Filled.Visibility,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        visualTransformation = if (regConfirmPasswordVisible)
                                            VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                        ),
                                        isError = regConfirmPassword.isNotEmpty() && regPassword != regConfirmPassword
                                    )

                                    // verify
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        OutlinedTextField(
                                            value = regVerificationCode,
                                            onValueChange = { if (it.length <= 6) regVerificationCode = it.filter { c -> c.isDigit() } },
                                            label = { Text(AppStringsProvider.current().authVerificationCode) },
                                            leadingIcon = {
                                                Icon(Icons.Outlined.MarkEmailRead, null, modifier = Modifier.size(20.dp))
                                            },
                                            placeholder = { Text(AppStringsProvider.current().authCodePlaceholder) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Done
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onDone = {
                                                    focusManager.clearFocus()
                                                    authViewModel.register(regEmail, regUsername, regPassword, regConfirmPassword, regVerificationCode)
                                                }
                                            )
                                        )

                                        PremiumButton(
                                            onClick = {
                                                focusManager.clearFocus()
                                                authViewModel.sendVerificationCode(regEmail)
                                            },
                                            modifier = Modifier
                                                .height(56.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = regEmail.isNotBlank()
                                                    && codeCountdown == 0
                                                    && sendCodeState !is FormState.Loading
                                        ) {
                                            if (sendCodeState is FormState.Loading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            } else {
                                                Text(
                                                    text = if (codeCountdown > 0) "${codeCountdown}s" else "发送验证码",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    PremiumButton(
                                        onClick = {
                                            focusManager.clearFocus()
                                            authViewModel.register(regEmail, regUsername, regPassword, regConfirmPassword, regVerificationCode)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = registerState !is FormState.Loading
                                    ) {
                                        if (registerState is FormState.Loading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            text = if (registerState is FormState.Loading)
                                                AppStringsProvider.current().authRegistering else AppStringsProvider.current().authRegister,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }

                                    // switch
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = AppStringsProvider.current().authHasAccount,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = AppStringsProvider.current().authLoginNow,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { selectedTab = 0 }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Note
                if (!showForgotPassword) {
                    EnhancedElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = AppStringsProvider.current().authWhyRegister,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            FeatureRow(Icons.Outlined.Cloud, AppStringsProvider.current().authFeatureCloud)
                            FeatureRow(Icons.Outlined.BarChart, AppStringsProvider.current().authFeatureStats)
                            FeatureRow(Icons.Outlined.Share, AppStringsProvider.current().authFeatureShare)
                            FeatureRow(Icons.Outlined.Backup, AppStringsProvider.current().authFeatureBackup)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = AppStringsProvider.current().authFreeNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
        }
}

// Note

@Composable
private fun ForgotPasswordForm(
    authViewModel: AuthViewModel,
    forgotPasswordState: FormState,
    resetCodeState: FormState,
    email: String,
    onEmailChange: (String) -> Unit,
    resetCode: String,
    onResetCodeChange: (String) -> Unit,
    resetNewPassword: String,
    onResetNewPasswordChange: (String) -> Unit,
    resetCodeCountdown: Int,
    onBackToLogin: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // input
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text(AppStringsProvider.current().authRegisterEmail) },
            leadingIcon = {
                Icon(Icons.Outlined.Email, null, modifier = Modifier.size(20.dp))
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        // verify input + button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            OutlinedTextField(
                value = resetCode,
                onValueChange = onResetCodeChange,
                label = { Text(AppStringsProvider.current().authVerificationCode) },
                leadingIcon = {
                    Icon(Icons.Outlined.Pin, null, modifier = Modifier.size(20.dp))
                },
                placeholder = { Text(AppStringsProvider.current().authCodePlaceholder) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )

            PremiumButton(
                onClick = {
                    authViewModel.sendResetCode(email)
                },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = email.isNotBlank()
                        && resetCodeCountdown == 0
                        && resetCodeState !is FormState.Loading
            ) {
                if (resetCodeState is FormState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (resetCodeCountdown > 0) "${resetCodeCountdown}s" else "发送验证码",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }

        // input
        OutlinedTextField(
            value = resetNewPassword,
            onValueChange = onResetNewPasswordChange,
            label = { Text(AppStringsProvider.current().authNewPassword) },
            leadingIcon = {
                Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(20.dp))
            },
            supportingText = { Text(AppStringsProvider.current().authPasswordMinLength) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            )
        )

        PremiumButton(
            onClick = { authViewModel.resetPassword(email, resetCode, resetNewPassword) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = email.isNotBlank()
                    && resetCode.length == 6
                    && resetNewPassword.length >= 6
                    && forgotPasswordState !is FormState.Loading
        ) {
            if (forgotPasswordState is FormState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(AppStringsProvider.current().authResetPasswordBtn)
        }

        // back
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = AppStringsProvider.current().authRememberPassword,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = AppStringsProvider.current().authBackToLogin,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onBackToLogin() }
            )
        }
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

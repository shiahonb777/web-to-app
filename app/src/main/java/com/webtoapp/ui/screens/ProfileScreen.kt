package com.webtoapp.ui.screens

import androidx.compose.animation.*
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumOutlinedButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.core.auth.ProStatus
import com.webtoapp.core.auth.UserProfile
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.viewmodel.AuthState
import com.webtoapp.ui.viewmodel.AuthViewModel
import com.webtoapp.ui.viewmodel.FormState
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumTextField
import com.webtoapp.R

/**
 * 用户资料页面
 * 
 * 展示已登录用户的资料、会员状态、使用统计、密码管理、账号注销
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateDevices: () -> Unit = {},
    onNavigateActivationCode: () -> Unit = {},
    onNavigateTeams: () -> Unit = {},
    onNavigateSubscription: () -> Unit = {}
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val proStatus by authViewModel.proStatus.collectAsStateWithLifecycle()
    val passwordState by authViewModel.passwordState.collectAsStateWithLifecycle()
    val deleteAccountState by authViewModel.deleteAccountState.collectAsStateWithLifecycle()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showLogoutAllDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // 监听密码修改状态
    LaunchedEffect(passwordState) {
        when (passwordState) {
            is FormState.Success -> {
                snackbarHostState.showSnackbar((passwordState as FormState.Success).message)
                authViewModel.resetPasswordState()
                showChangePasswordDialog = false
                onLogout() // 需要重新登录
            }
            is FormState.Error -> {
                snackbarHostState.showSnackbar((passwordState as FormState.Error).message)
                authViewModel.resetPasswordState()
            }
            else -> {}
        }
    }

    // 监听账号注销状态
    LaunchedEffect(deleteAccountState) {
        when (deleteAccountState) {
            is FormState.Success -> {
                snackbarHostState.showSnackbar((deleteAccountState as FormState.Success).message)
                authViewModel.resetDeleteAccountState()
                showDeleteAccountDialog = false
                onLogout()
            }
            is FormState.Error -> {
                snackbarHostState.showSnackbar((deleteAccountState as FormState.Error).message)
                authViewModel.resetDeleteAccountState()
            }
            else -> {}
        }
    }

    // 如果未登录，返回
    val user = (authState as? AuthState.LoggedIn)?.user ?: run {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        Strings.authProfile,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { authViewModel.refreshProfile() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
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
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 头像 & 用户名
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.username.firstOrNull()?.uppercase() ?: "U",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = user.username,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 会员标签
            val planLabel = when (user.proPlan) {
                "pro_monthly", "pro_quarterly", "pro_yearly" -> "Pro"
                "pro_lifetime", "lifetime" -> "Pro ∞"
                "ultra_monthly", "ultra_quarterly", "ultra_yearly" -> "Ultra"
                "ultra_lifetime" -> "Ultra ∞"
                else -> "Free"
            }
            val planColor = when {
                user.proPlan == "ultra_lifetime" -> Color(0xFFFFD700)
                user.proPlan.startsWith("ultra") -> Color(0xFFFFD700)
                user.proPlan == "pro_lifetime" || user.proPlan == "lifetime" -> Color(0xFFE040FB)
                user.proPlan.startsWith("pro") -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            }

            AssistChip(
                onClick = { },
                label = {
                    Text(
                        text = planLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        if (user.isPro) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = planColor
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = planColor
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 激活码兑换入口
            EnhancedElevatedCard(
                onClick = onNavigateActivationCode,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.CardGiftcard,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = Strings.cloudActivationCode,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(weight = 1f, fill = true)
                    )
                    Icon(
                        Icons.Outlined.ChevronRight,
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 使用统计
            EnhancedElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        value = user.appsCreated.toString(),
                        label = Strings.authStatsAppsCreated
                    )
                    StatItem(
                        value = user.apksBuilt.toString(),
                        label = Strings.authStatsApksBuilt
                    )
                    StatItem(
                        value = user.maxDevices.toString(),
                        label = Strings.authStatsMaxDevices
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 会员状态卡片
            if (proStatus != null || user.isPro) {
                ProStatusCard(proStatus, user, onNavigateSubscription)
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // Free 用户 — 显示升级引导
                EnhancedElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateSubscription() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.WorkspacePremium,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Strings.authProInactive,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = Strings.authUpgradeDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 功能菜单
            EnhancedElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column {
                    MenuItemRow(
                        icon = Icons.Outlined.Devices,
                        title = Strings.authMenuDevices,
                        subtitle = "${Strings.authMenuDevicesMax}: ${user.maxDevices}",
                        onClick = onNavigateDevices
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    MenuItemRow(
                        icon = Icons.Outlined.Groups,
                        title = Strings.teamTitle,
                        subtitle = if (user.isPro) "${Strings.teamMembers} · RBAC" else Strings.authMenuCloudUpgrade,
                        onClick = onNavigateTeams
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    MenuItemRow(
                        icon = Icons.Outlined.Lock,
                        title = "修改密码",
                        subtitle = "更新您的登录密码",
                        onClick = { showChangePasswordDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    MenuItemRow(
                        icon = Icons.Outlined.Security,
                        title = Strings.authMenuSecurity,
                        subtitle = Strings.authMenuSecurityDesc
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 退出登录
            PremiumOutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.authLogout)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 在所有设备上登出
            TextButton(
                onClick = { showLogoutAllDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "在所有设备上登出",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 注销账号（危险操作）
            TextButton(
                onClick = { showDeleteAccountDialog = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.Outlined.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.delete_account_title),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // ─── 退出登录确认对话框 ───
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text(Strings.authLogout) },
                text = { Text(Strings.authLogoutConfirm) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            authViewModel.logout()
                            onLogout()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(Strings.authLogout)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text(Strings.cancel)
                    }
                }
            )
        }

        // ─── 所有设备登出确认对话框 ───
        if (showLogoutAllDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutAllDialog = false },
                title = { Text("在所有设备上登出") },
                text = {
                    Text("确定在所有设备上登出吗？所有设备都将需要重新登录。")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutAllDialog = false
                            authViewModel.logoutAll()
                            onLogout()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("全部登出")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutAllDialog = false }) {
                        Text(Strings.cancel)
                    }
                }
            )
        }

        // ─── 修改密码对话框 ───
        if (showChangePasswordDialog) {
            ChangePasswordDialog(
                authViewModel = authViewModel,
                onDismiss = { showChangePasswordDialog = false }
            )
        }

        // ─── 注销账号对话框 ───
        if (showDeleteAccountDialog) {
            DeleteAccountDialog(
                authViewModel = authViewModel,
                onDismiss = { showDeleteAccountDialog = false }
            )
        }
    }
}

// ─── 修改密码对话框 ───

@Composable
private fun ChangePasswordDialog(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    val passwordState by authViewModel.passwordState.collectAsStateWithLifecycle()

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("修改密码")
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PremiumTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("当前密码") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(10.dp)
                )
                PremiumTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("新密码") },
                    supportingText = { Text("至少 6 位") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(10.dp)
                )
                PremiumTextField(
                    value = confirmNewPassword,
                    onValueChange = { confirmNewPassword = it },
                    label = { Text("确认新密码") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(10.dp),
                    isError = confirmNewPassword.isNotEmpty() && newPassword != confirmNewPassword
                )
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = {
                    authViewModel.changePassword(currentPassword, newPassword, confirmNewPassword)
                },
                enabled = passwordState !is FormState.Loading
                        && currentPassword.isNotBlank()
                        && newPassword.length >= 6
                        && newPassword == confirmNewPassword
            ) {
                if (passwordState is FormState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("确认修改")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.cancel)
            }
        }
    )
}

// ─── 注销账号对话框 ───

@Composable
private fun DeleteAccountDialog(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    val deleteState by authViewModel.deleteAccountState.collectAsStateWithLifecycle()

    var password by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var confirmText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.DeleteForever, null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.delete_account_title), color = MaterialTheme.colorScheme.error)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EnhancedElevatedCard(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        stringResource(R.string.delete_account_warning),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                PremiumTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.delete_account_password_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(10.dp)
                )
                PremiumTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(stringResource(R.string.delete_account_reason_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp)
                )
                PremiumTextField(
                    value = confirmText,
                    onValueChange = { confirmText = it },
                    label = { Text(stringResource(R.string.delete_account_confirm_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    isError = confirmText.isNotEmpty() && confirmText != "DELETE"
                )
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = {
                    authViewModel.deleteAccount(password, reason)
                },
                enabled = deleteState !is FormState.Loading
                        && password.isNotBlank()
                        && confirmText == "DELETE",
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (deleteState is FormState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.delete_account_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.cancel)
            }
        }
    )
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProStatusCard(proStatus: ProStatus?, user: UserProfile, onNavigateSubscription: () -> Unit = {}) {
    val isUltra = user.proPlan.startsWith("ultra")
    val isProLifetime = user.proPlan == "pro_lifetime" || user.proPlan == "lifetime"
    val isUltraLifetime = user.proPlan == "ultra_lifetime"
    val isLifetime = isProLifetime || isUltraLifetime

    // 根据会员层级选择不同的颜色主题
    val cardColor = when {
        isUltraLifetime -> Color(0xFFFFD700).copy(alpha = 0.15f)
        isUltra -> Color(0xFFFFD700).copy(alpha = 0.15f)
        isProLifetime -> Color(0xFFE040FB).copy(alpha = 0.15f)
        user.isPro -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val iconTint = when {
        isUltraLifetime -> Color(0xFFFFD700)
        isUltra -> Color(0xFFFFD700)
        isProLifetime -> Color(0xFFE040FB)
        user.isPro -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val statusText = when {
        isUltraLifetime -> Strings.authUltraLifetimeActive
        isUltra -> Strings.authUltraActive
        isProLifetime -> Strings.authLifetimeActive
        user.isPro -> Strings.authProActive
        else -> Strings.authProInactive
    }

    EnhancedElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateSubscription() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (user.isPro) Icons.Filled.Verified else Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iconTint
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (proStatus?.daysRemaining != null && !isLifetime) {
                        Text(
                            text = "${Strings.authProRemaining}: ${proStatus.daysRemaining} ${Strings.authProDays}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isLifetime && !isUltraLifetime) {
                        Text(
                            text = "∞ ${Strings.authProDays}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isUltraLifetime) {
                        Text(
                            text = "∞ ${Strings.authProDays}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFD700).copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Pro 永久 → Ultra 永久 升级按钮
            if (isProLifetime) {
                val proLifetimePrice = 99.0
                val ultraLifetimePrice = 199.0
                val upgradeCost = ultraLifetimePrice - proLifetimePrice

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO: 触发升级流程 */ }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Strings.authUpgradeToUltra,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFD700)
                        )
                        Text(
                            text = "${Strings.authUpgradeDesc} \$${"%,.0f".format(upgradeCost)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFFD700).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "\$${"%,.0f".format(upgradeCost)}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFFFD700).copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

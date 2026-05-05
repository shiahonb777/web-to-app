package com.webtoapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.R
import com.webtoapp.core.auth.ProStatus
import com.webtoapp.core.auth.UserProfile
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumTextField
import com.webtoapp.ui.design.WtaBadge
import com.webtoapp.ui.design.WtaRadius
import com.webtoapp.ui.design.WtaRowTone
import com.webtoapp.ui.design.WtaScreen
import com.webtoapp.ui.design.WtaSection
import com.webtoapp.ui.design.WtaSectionDivider
import com.webtoapp.ui.design.WtaSettingCard
import com.webtoapp.ui.design.WtaSettingRow
import com.webtoapp.ui.design.WtaSpacing
import com.webtoapp.ui.design.WtaStatusBanner
import com.webtoapp.ui.design.WtaStatusTone
import com.webtoapp.ui.viewmodel.AuthState
import com.webtoapp.ui.viewmodel.AuthViewModel
import com.webtoapp.ui.viewmodel.FormState
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateDevices: () -> Unit = {},
    onNavigateActivationCode: () -> Unit = {},
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

    LaunchedEffect(passwordState) {
        when (passwordState) {
            is FormState.Success -> {
                snackbarHostState.showSnackbar((passwordState as FormState.Success).message)
                authViewModel.resetPasswordState()
                showChangePasswordDialog = false
                onLogout()
            }
            is FormState.Error -> {
                snackbarHostState.showSnackbar((passwordState as FormState.Error).message)
                authViewModel.resetPasswordState()
            }
            else -> Unit
        }
    }

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
            else -> Unit
        }
    }

    val user = (authState as? AuthState.LoggedIn)?.user ?: run {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    WtaScreen(
        title = Strings.authProfile,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        actions = {
            IconButton(onClick = { authViewModel.refreshProfile() }) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = WtaSpacing.ScreenHorizontal,
                    vertical = WtaSpacing.ScreenVertical
                ),
            verticalArrangement = Arrangement.spacedBy(WtaSpacing.SectionGap)
        ) {
            val planLabel = when (user.proPlan) {
                "pro_monthly", "pro_quarterly", "pro_yearly" -> "Pro"
                "pro_lifetime", "lifetime" -> "Pro ∞"
                "ultra_monthly", "ultra_quarterly", "ultra_yearly" -> "Ultra"
                "ultra_lifetime" -> "Ultra ∞"
                else -> "Free"
            }
            val planColor = when {
                user.proPlan.startsWith("ultra") -> MaterialTheme.colorScheme.tertiary
                user.proPlan == "pro_lifetime" || user.proPlan == "lifetime" -> MaterialTheme.colorScheme.secondary
                user.proPlan.startsWith("pro") -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            }

            WtaSection(title = Strings.authProfile) {
                WtaSettingCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.username.firstOrNull()?.uppercase() ?: "U",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
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
                                AssistChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            text = planLabel,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (user.isPro) Icons.Filled.Star else Icons.Outlined.WorkspacePremium,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = planColor
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        labelColor = planColor,
                                        leadingIconContentColor = planColor
                                    )
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        WtaSettingRow(
                            icon = Icons.Outlined.CardGiftcard,
                            title = Strings.cloudActivationCode,
                            subtitle = Strings.cloudActivationCode
                        ) {
                            TextButton(onClick = onNavigateActivationCode) {
                                Text(Strings.openAction)
                            }
                        }
                    }
                }
            }

            WtaSection(title = Strings.authProfile) {
                WtaSettingCard {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatBlock(value = user.appsCreated.toString(), label = Strings.authStatsAppsCreated)
                        StatBlock(value = user.apksBuilt.toString(), label = Strings.authStatsApksBuilt)
                        StatBlock(value = user.maxDevices.toString(), label = Strings.authStatsMaxDevices)
                    }
                }
            }

            WtaSection(title = "Pro") {
                if (proStatus != null || user.isPro) {
                    ProStatusCard(
                        proStatus = proStatus,
                        user = user,
                        onNavigateSubscription = onNavigateSubscription
                    )
                } else {
                    WtaSettingCard(
                        onClick = onNavigateSubscription,
                        contentPadding = PaddingValues()
                    ) {
                        WtaSettingRow(
                            icon = Icons.Outlined.WorkspacePremium,
                            title = Strings.authProInactive,
                            subtitle = Strings.authUpgradeDesc
                        ) {
                            Icon(
                                Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            WtaSection(title = Strings.authMenuSecurity) {
                WtaSettingCard {
                    WtaSettingRow(
                        icon = Icons.Outlined.Devices,
                        title = Strings.authMenuDevices,
                        subtitle = "${Strings.authMenuDevicesMax}: ${user.maxDevices}",
                        onClick = onNavigateDevices
                    ) {
                        Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp))
                    }
                    WtaSectionDivider()
                    WtaSettingRow(
                        icon = Icons.Outlined.Lock,
                        title = stringResource(R.string.change_password),
                        subtitle = stringResource(R.string.update_login_password),
                        onClick = { showChangePasswordDialog = true }
                    ) {
                        Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp))
                    }
                    WtaSectionDivider()
                    WtaSettingRow(
                        icon = Icons.Outlined.Security,
                        title = Strings.authMenuSecurity,
                        subtitle = Strings.authMenuSecurityDesc,
                        enabled = false
                    )
                }
            }

            WtaSection(title = Strings.authLogout) {
                WtaSettingCard {
                    WtaSettingRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = Strings.authLogout,
                        tone = WtaRowTone.Danger,
                        onClick = { showLogoutDialog = true }
                    ) {
                        Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp))
                    }
                    WtaSectionDivider()
                    WtaSettingRow(
                        icon = Icons.Outlined.DevicesOther,
                        title = stringResource(R.string.logout_all_devices),
                        onClick = { showLogoutAllDialog = true }
                    ) {
                        Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp))
                    }
                    WtaSectionDivider()
                    WtaSettingRow(
                        icon = Icons.Outlined.DeleteForever,
                        title = stringResource(R.string.delete_account_title),
                        tone = WtaRowTone.Danger,
                        onClick = { showDeleteAccountDialog = true }
                    ) {
                        Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

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
                        }
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

        if (showLogoutAllDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutAllDialog = false },
                title = { Text(stringResource(R.string.logout_all_confirm_title)) },
                text = { Text(stringResource(R.string.logout_all_confirm_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutAllDialog = false
                            authViewModel.logoutAll()
                            onLogout()
                        }
                    ) {
                        Text(stringResource(R.string.logout_all))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutAllDialog = false }) {
                        Text(Strings.cancel)
                    }
                }
            )
        }

        if (showChangePasswordDialog) {
            ChangePasswordDialog(
                authViewModel = authViewModel,
                onDismiss = { showChangePasswordDialog = false }
            )
        }

        if (showDeleteAccountDialog) {
            DeleteAccountDialog(
                authViewModel = authViewModel,
                onDismiss = { showDeleteAccountDialog = false }
            )
        }
    }
}

@Composable
private fun RowScope.StatBlock(value: String, label: String) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                Text(stringResource(R.string.change_password))
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text(stringResource(R.string.current_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(WtaRadius.Control)
                )
                PremiumTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.new_password)) },
                    supportingText = { Text(stringResource(R.string.password_min_length)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(WtaRadius.Control)
                )
                PremiumTextField(
                    value = confirmNewPassword,
                    onValueChange = { confirmNewPassword = it },
                    label = { Text(stringResource(R.string.confirm_new_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(WtaRadius.Control),
                    isError = confirmNewPassword.isNotEmpty() && newPassword != confirmNewPassword
                )
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = {
                    authViewModel.changePassword(currentPassword, newPassword, confirmNewPassword)
                },
                enabled = passwordState !is FormState.Loading &&
                    currentPassword.isNotBlank() &&
                    newPassword.length >= 6 &&
                    newPassword == confirmNewPassword
            ) {
                if (passwordState is FormState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.confirm_change))
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
                    Icons.Outlined.DeleteForever,
                    null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.delete_account_title), color = MaterialTheme.colorScheme.error)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WtaStatusBanner(
                    message = stringResource(R.string.delete_account_warning),
                    tone = WtaStatusTone.Warning
                )
                PremiumTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.delete_account_password_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(WtaRadius.Control)
                )
                PremiumTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(stringResource(R.string.delete_account_reason_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(WtaRadius.Control)
                )
                PremiumTextField(
                    value = confirmText,
                    onValueChange = { confirmText = it },
                    label = { Text(stringResource(R.string.delete_account_confirm_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(WtaRadius.Control),
                    isError = confirmText.isNotEmpty() && confirmText != "DELETE"
                )
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = {
                    authViewModel.deleteAccount(password, reason)
                },
                enabled = deleteState !is FormState.Loading &&
                    password.isNotBlank() &&
                    confirmText == "DELETE",
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
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
private fun ProStatusCard(
    proStatus: ProStatus?,
    user: UserProfile,
    onNavigateSubscription: () -> Unit = {}
) {
    val isUltra = user.proPlan.startsWith("ultra")
    val isProLifetime = user.proPlan == "pro_lifetime" || user.proPlan == "lifetime"
    val isUltraLifetime = user.proPlan == "ultra_lifetime"
    val isLifetime = isProLifetime || isUltraLifetime

    val accent = when {
        isUltraLifetime || isUltra -> MaterialTheme.colorScheme.tertiary
        isProLifetime -> MaterialTheme.colorScheme.secondary
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

    WtaSettingCard(onClick = onNavigateSubscription) {
        Column {
            WtaSettingRow(
                icon = if (user.isPro) Icons.Filled.Star else Icons.Outlined.WorkspacePremium,
                title = statusText,
                subtitle = when {
                    proStatus?.daysRemaining != null && !isLifetime ->
                        "${Strings.authProRemaining}: ${proStatus.daysRemaining} ${Strings.authProDays}"
                    isLifetime -> "∞ ${Strings.authProDays}"
                    else -> Strings.authUpgradeDesc
                },
                enabled = true
            ) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            if (isProLifetime) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                val upgradeCost = 199.0 - 99.0
                WtaSettingRow(
                    icon = Icons.Outlined.WorkspacePremium,
                    title = Strings.authUpgradeToUltra,
                    subtitle = "${Strings.authUpgradeDesc} $${"%,.0f".format(upgradeCost)}"
                ) {
                    WtaBadge(
                        text = "$${"%,.0f".format(upgradeCost)}",
                        containerColor = accent.copy(alpha = 0.12f),
                        contentColor = accent
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp), tint = accent)
                }
            }
        }
    }
}

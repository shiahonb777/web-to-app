package com.webtoapp.ui.navigation

import androidx.compose.runtime.Composable
import com.webtoapp.ui.viewmodel.AuthViewModel
import com.webtoapp.ui.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavigation() {
    val viewModel: MainViewModel = koinViewModel()
    val authViewModel: AuthViewModel = koinViewModel()

    AppNavigationEffects()
    AppNavigationScaffold(
        viewModel = viewModel,
        authViewModel = authViewModel
    )
}

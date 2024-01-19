package com.joinforage.android.example.ui.pos

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.joinforage.android.example.R
import com.joinforage.android.example.ui.pos.screens.ActionSelectionScreen
import com.joinforage.android.example.ui.pos.screens.BalanceInquiryScreen
import com.joinforage.android.example.ui.pos.screens.ManualPANEntryScreen
import com.joinforage.android.example.ui.pos.screens.MerchantSetupScreen

enum class POSScreen(@StringRes val title: Int) {
    MerchantSetupScreen(title = R.string.title_pos_merchant_setup),
    ActionSelectionScreen(title = R.string.title_pos_action_selection),
    BalanceInquiryScreen(title = R.string.title_pos_balance_inquiry),
    ManualPANEntryScreen(title = R.string.title_pos_manual_pan_entry)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POSComposeApp(
    viewModel: POSViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = POSScreen.valueOf(
        backStackEntry?.destination?.route ?: POSScreen.MerchantSetupScreen.name
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(currentScreen.title)) },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.pos_back_button)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val uiState by viewModel.uiState.collectAsState()

        NavHost(
            navController = navController,
            startDestination = POSScreen.MerchantSetupScreen.name,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = 72.dp, start = 16.dp, end = 16.dp)
        ) {
            composable(route = POSScreen.MerchantSetupScreen.name) {
                MerchantSetupScreen(
                    terminalId = "fake terminal ID",
                    merchantId = uiState.merchantId,
                    merchantDetailsState = uiState.merchantDetailsState,
                    onSaveButtonClicked = {
                        viewModel.setMerchantId(it, onSuccess = {
                            navController.navigate(POSScreen.ActionSelectionScreen.name)
                        })
                    }
                )
            }
            composable(route = POSScreen.ActionSelectionScreen.name) {
                ActionSelectionScreen(
                    merchantDetails = when (uiState.merchantDetailsState) {
                        is MerchantDetailsState.Success -> (uiState.merchantDetailsState as MerchantDetailsState.Success).merchant
                        is MerchantDetailsState.Loading -> null
                        is MerchantDetailsState.Idle -> null
                        is MerchantDetailsState.Error -> null
                    },
                    onBackButtonClicked = {
                        navController.popBackStack(POSScreen.MerchantSetupScreen.name, inclusive = false)
                    },
                    onBalanceButtonClicked = {
                        navController.navigate(POSScreen.BalanceInquiryScreen.name)
                    },
                    onPaymentButtonClicked = { /*TODO*/ },
                    onRefundButtonClicked = { /*TODO*/ }
                ) {
                }
            }
            composable(route = POSScreen.BalanceInquiryScreen.name) {
                BalanceInquiryScreen(
                    onManualEntryButtonClicked = { navController.navigate(POSScreen.ManualPANEntryScreen.name) },
                    onSwipeButtonClicked = { /*TODO*/ },
                    onBackButtonClicked = { navController.popBackStack(POSScreen.ActionSelectionScreen.name, inclusive = false) }
                )
            }
            composable(route = POSScreen.ManualPANEntryScreen.name) {
                ManualPANEntryScreen(
                    merchantId = uiState.merchantId,
                    onSubmitButtonClicked = { /*TODO*/ },
                    onBackButtonClicked = { navController.popBackStack(POSScreen.BalanceInquiryScreen.name, inclusive = false) }
                )
            }
        }
    }
}

@Preview
@Composable
fun PosAppPreview() {
    POSComposeApp()
}

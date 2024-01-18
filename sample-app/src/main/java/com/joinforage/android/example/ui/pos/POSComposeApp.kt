package com.joinforage.android.example.ui.pos

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.joinforage.android.example.R
import com.joinforage.android.example.ui.pos.screens.ManualPANEntryScreen
import com.joinforage.android.example.ui.pos.screens.MerchantSetupScreen
import com.joinforage.android.example.ui.pos.screens.ReceiptPreviewScreen

enum class POSScreen(@StringRes val title: Int) {
    POSHome(title = R.string.title_pos_home),
    MerchantIdEntry(title = R.string.title_pos_merchant_setup),
    ManualPANEntry(title = R.string.title_pos_card_pan),
    ReceiptPreview(title = R.string.title_pos_receipt_preview)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POSComposeApp(
    viewModel: POSViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = POSScreen.valueOf(
        backStackEntry?.destination?.route ?: POSScreen.POSHome.name
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
                                contentDescription = stringResource(R.string.back_button)
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
            startDestination = POSScreen.POSHome.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = POSScreen.POSHome.name) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    Box (
                        modifier = Modifier.background(Color.LightGray).padding(8.dp)
                    ) {
                        Column {
                            Row {
                                Text(text = "Merchant ID:")
                                Text(text = uiState.merchantId.toString())
                            }
                            Row {
                                Text(text = "Card PAN:")
                                Text(text = uiState.cardPAN.toString())
                            }
                            Text(text = uiState.merchantInfo.toString())
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { navController.navigate(POSScreen.MerchantIdEntry.name) }) {
                        Text(text = "Merchant Setup")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { navController.navigate(POSScreen.ManualPANEntry.name) }) {
                        Text(text = "Enter Card PAN")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { navController.navigate(POSScreen.ReceiptPreview.name)}) {
                        Text(text = "Preview Receipts")
                    }
                }
            }
            composable(route = POSScreen.ManualPANEntry.name) {
                ManualPANEntryScreen(
                    initialValue = uiState.cardPAN,
                    onSaveButtonClicked = {
                        viewModel.setCardPAN(it)
                        navController.popBackStack(POSScreen.POSHome.name, inclusive = false)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
            composable(route = POSScreen.MerchantIdEntry.name) {
                MerchantSetupScreen(
                    initialValue = uiState.merchantId,
                    onSaveButtonClicked = {
                        viewModel.setMerchantId(it)
                        navController.popBackStack(POSScreen.POSHome.name, inclusive = false)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
            composable(route = POSScreen.ReceiptPreview.name) {
                ReceiptPreviewScreen()
            }
        }
    }
}
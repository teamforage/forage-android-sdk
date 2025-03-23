package com.joinforage.android.example.ui.pos

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.joinforage.android.example.pos.k9sdk.K9SDK
import com.joinforage.android.example.ui.extensions.withTestId
import com.joinforage.android.example.ui.pos.data.PosPaymentRequest
import com.joinforage.android.example.ui.pos.data.RefundUIState
import com.joinforage.android.example.ui.pos.screens.ActionSelectionScreen
import com.joinforage.android.example.ui.pos.screens.MerchantSetupScreen
import com.joinforage.android.example.ui.pos.screens.balance.BalanceResultScreen
import com.joinforage.android.example.ui.pos.screens.deferred.DeferredPaymentCaptureResultScreen
import com.joinforage.android.example.ui.pos.screens.deferred.DeferredPaymentRefundResultScreen
import com.joinforage.android.example.ui.pos.screens.payment.EBTCashPurchaseScreen
import com.joinforage.android.example.ui.pos.screens.payment.EBTCashPurchaseWithCashBackScreen
import com.joinforage.android.example.ui.pos.screens.payment.EBTCashWithdrawalScreen
import com.joinforage.android.example.ui.pos.screens.payment.EBTSnapPurchaseScreen
import com.joinforage.android.example.ui.pos.screens.payment.PaymentTypeSelectionScreen
import com.joinforage.android.example.ui.pos.screens.refund.RefundDetailsScreen
import com.joinforage.android.example.ui.pos.screens.shared.MagSwipePANEntryScreen
import com.joinforage.android.example.ui.pos.screens.shared.ManualPANEntryScreen
import com.joinforage.android.example.ui.pos.screens.shared.PANMethodSelectionScreen
import com.joinforage.android.example.ui.pos.screens.shared.PINEntryScreen
import com.joinforage.forage.android.core.ui.element.ForageVaultElement
import com.joinforage.forage.android.core.ui.element.state.ElementState
import com.joinforage.forage.android.pos.ui.element.ForagePANEditText

enum class POSScreen(@StringRes val title: Int) {
    MerchantSetupScreen(title = R.string.title_pos_merchant_setup),
    ActionSelectionScreen(title = R.string.title_pos_action_selection),
    BIChoosePANMethodScreen(title = R.string.title_pos_balance_inquiry),
    BIManualPANEntryScreen(title = R.string.title_pos_bi_manual_pan_entry),
    BIMagSwipePANEntryScreen(title = R.string.title_pos_bi_mag_swipe_pan_entry),
    BIPINEntryScreen(title = R.string.title_pos_bi_pin_entry),
    BIResultScreen(title = R.string.title_pos_balance_inquiry_result),
    PAYTransactionTypeSelectionScreen(title = R.string.title_pos_payment_type_selection_screen),
    PAYChoosePANMethodScreen(title = R.string.title_pos_payment_choose_pan_method),
    PAYSnapPurchaseScreen(title = R.string.title_pos_payment_snap_purchase_screen),
    PAYEBTCashPurchaseScreen(title = R.string.title_pos_payment_ebt_cash),
    PAYEBTCashWithdrawalScreen(title = R.string.title_pos_payment_cash_withdrawal),
    PAYEBTCashPurchaseWithCashBackScreen(title = R.string.title_pos_payment_with_cashback),
    PAYManualPANEntryScreen(title = R.string.title_pos_payment_manual_pan_entry),
    PAYMagSwipePANEntryScreen(title = R.string.title_pos_payment_swipe_card_entry),
    PAYPINEntryScreen(title = R.string.title_pos_payment_pin_entry),
    PAYResultScreen(title = R.string.title_pos_payment_receipt),
    PAYErrorResultScreen(title = R.string.title_pos_payment_receipt),
    REFUNDDetailsScreen(title = R.string.title_pos_refund_details),
    REFUNDPINEntryScreen(title = R.string.title_pos_refund_pin_entry),
    REFUNDResultScreen(title = R.string.title_pos_refund_result),
    REFUNDErrorResultScreen(title = R.string.title_pos_refund_result),
    VOIDTransactionTypeSelectionScreen(title = R.string.title_pos_void_action_selection),
    VOIDPaymentScreen(title = R.string.title_pos_void_payment),
    VOIDRefundScreen(title = R.string.title_pos_void_refund),
    VOIDPaymentResultScreen(title = R.string.title_pos_void_payment_result),
    VOIDRefundResultScreen(title = R.string.title_pos_void_refund_result),
    DEFERPaymentCaptureResultScreen(title = R.string.title_pos_defer_payment_result),
    DEFERPaymentRefundResultScreen(title = R.string.title_pos_defer_refund_result)
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

    var panElement: ForagePANEditText? by rememberSaveable {
        mutableStateOf(null)
    }

    var pinElement: ForageVaultElement<ElementState>? by rememberSaveable {
        mutableStateOf(null)
    }

    val context = LocalContext.current
    val k9SDK by remember {
        mutableStateOf(K9SDK().init(context))
    }

    var pageTitle: String? by rememberSaveable {
        mutableStateOf(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pageTitle ?: stringResource(currentScreen.title)) },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = {
                            val isPayTypeScreen = navController.previousBackStackEntry?.destination?.route == POSScreen.PAYTransactionTypeSelectionScreen.name
                            if (isPayTypeScreen) {
                                pageTitle = null
                            }
                            navController.navigateUp()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.pos_back_button)
                            )
                        }
                    }
                },
                actions = {
                    val isMerchantScreen = navController.currentBackStackEntry?.destination?.route == POSScreen.MerchantSetupScreen.name
                    val isActionScreen = navController.currentBackStackEntry?.destination?.route == POSScreen.ActionSelectionScreen.name
                    if (!isMerchantScreen && !isActionScreen) {
                        IconButton(
                            onClick = {
                                navController.popBackStack(POSScreen.ActionSelectionScreen.name, inclusive = false)
                                pageTitle = null
                                viewModel.resetUiState()
                            },
                            modifier = Modifier.withTestId("pos_back_to_action_selection_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.pos_restart)
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
                    terminalId = k9SDK.terminalId,
                    merchantId = uiState.merchantId,
                    sessionToken = uiState.sessionToken,
                    onSaveButtonClicked = { merchantId, sessionToken ->
                        viewModel.setSessionToken(sessionToken)
                        viewModel.setMerchantId(merchantId, onSuccess = {
                            navController.navigate(POSScreen.ActionSelectionScreen.name)
                        })
                    }
                )
            }
            composable(route = POSScreen.ActionSelectionScreen.name) {
                ActionSelectionScreen(
                    merchantDetails = uiState.merchant,
                    onBackButtonClicked = { navController.popBackStack(POSScreen.MerchantSetupScreen.name, inclusive = false) },
                    onBalanceButtonClicked = { navController.navigate(POSScreen.BIChoosePANMethodScreen.name) },
                    onPaymentButtonClicked = { navController.navigate(POSScreen.PAYTransactionTypeSelectionScreen.name) },
                    onRefundButtonClicked = { navController.navigate(POSScreen.REFUNDDetailsScreen.name) }
                )
            }
            composable(route = POSScreen.BIChoosePANMethodScreen.name) {
                PANMethodSelectionScreen(
                    onManualEntryButtonClicked = {
                        navController.navigate(POSScreen.BIManualPANEntryScreen.name)
                    },
                    onSwipeButtonClicked = {
                        navController.navigate(POSScreen.BIMagSwipePANEntryScreen.name)
                    },
                    onBackButtonClicked = { navController.popBackStack(POSScreen.ActionSelectionScreen.name, inclusive = false) }
                )
            }
            composable(route = POSScreen.BIManualPANEntryScreen.name) {
                ManualPANEntryScreen(
                    forageConfig = uiState.forageConfig,
                    onSubmitAsManualEntry = {
                        if (panElement != null) {
                            panElement!!.clearFocus()
                            viewModel.tokenizeManualEntryEBTCard(
                                panElement as ForagePANEditText,
                                onSuccess = {
                                    viewModel.resetPinActionErrors()
                                    navController.navigate(POSScreen.BIPINEntryScreen.name)
                                }
                            )
                        }
                    },
                    onSubmitAsTrack2 = {
                        if (panElement != null) {
                            panElement!!.clearFocus()
                            viewModel.tokenizeTrack2EBTCard(
                                panElement as ForagePANEditText,
                                onSuccess = {
                                    viewModel.resetPinActionErrors()
                                    navController.navigate(POSScreen.BIPINEntryScreen.name)
                                }
                            )
                        }
                    },
                    onBackButtonClicked = { navController.popBackStack(POSScreen.BIChoosePANMethodScreen.name, inclusive = false) },
                    withPanElementReference = { panElement = it }
                )
            }
            composable(route = POSScreen.BIMagSwipePANEntryScreen.name) {
                MagSwipePANEntryScreen(
                    onLaunch = {
                        k9SDK.listenForMagneticCardSwipe { track2Data ->
                            viewModel.tokenizeEBTCard(track2Data) {
                                if (it?.ref != null) {
                                    Log.i("POSComposeApp", "Successfully tokenized EBT card with ref: $it.ref")
                                    viewModel.resetPinActionErrors()
                                    navController.navigate(POSScreen.BIPINEntryScreen.name)
                                }
                            }
                        }
                    },
                    onBackButtonClicked = { navController.popBackStack(POSScreen.BIChoosePANMethodScreen.name, inclusive = false) }
                )
            }
            composable(route = POSScreen.BIPINEntryScreen.name) {
                PINEntryScreen(
                    onSubmitButtonClicked = {
                        if (pinElement != null) {
                            pinElement!!.clearFocus()
                            viewModel.checkEBTCardBalance(
                                context,
                                pinElement!!,
                                k9SDK.terminalId,
                                onSuccess = {
                                    if (it != null) {
                                        Log.i("POSComposeApp", "Successfully checked balance of EBT card: $it")
                                        navController.navigate(POSScreen.BIResultScreen.name)
                                    }
                                }
                            )
                        }
                    },
                    onBackButtonClicked = { navController.popBackStack(POSScreen.BIManualPANEntryScreen.name, inclusive = false) },
                    last4 = uiState.last4,
                    withPinElementReference = { pinElement = it }
                )
            }
            composable(route = POSScreen.BIResultScreen.name) {
                BalanceResultScreen(
                    merchant = uiState.merchant,
                    paymentMethod = uiState.paymentMethod,
                    balanceCheckError = uiState.balanceCheckError,
                    onBackButtonClicked = { navController.popBackStack(POSScreen.BIPINEntryScreen.name, inclusive = false) },
                    onDoneButtonClicked = { navController.popBackStack(POSScreen.ActionSelectionScreen.name, inclusive = false) }
                )
            }
            composable(route = POSScreen.PAYTransactionTypeSelectionScreen.name) {
                PaymentTypeSelectionScreen(
                    onSnapPurchaseClicked = {
                        navController.navigate(POSScreen.PAYSnapPurchaseScreen.name)
                        pageTitle = "EBT SNAP Purchase"
                    },
                    onCashPurchaseClicked = {
                        navController.navigate(POSScreen.PAYEBTCashPurchaseScreen.name)
                        pageTitle = "EBT Cash Purchase"
                    },
                    onCashWithdrawalClicked = {
                        navController.navigate(POSScreen.PAYEBTCashWithdrawalScreen.name)
                        pageTitle = "EBT Cash Withdrawal"
                    },
                    onCashPurchaseCashbackClicked = {
                        navController.navigate(POSScreen.PAYEBTCashPurchaseWithCashBackScreen.name)
                        pageTitle = "EBT Cash Purchase + Cashback"
                    },
                    onCancelButtonClicked = {
                        navController.popBackStack(POSScreen.ActionSelectionScreen.name, inclusive = false)
                    }
                )
            }
            composable(route = POSScreen.PAYSnapPurchaseScreen.name) {
                EBTSnapPurchaseScreen(
                    onConfirmButtonClicked = { snapAmount ->
                        val payment = PosPaymentRequest.forSnapPayment(snapAmount, k9SDK.terminalId)
                        viewModel.setLocalPayment(payment = payment)
                        navController.navigate(POSScreen.PAYChoosePANMethodScreen.name)
                    },
                    onCancelButtonClicked = {
                        navController.popBackStack(POSScreen.PAYTransactionTypeSelectionScreen.name, inclusive = false)
                        pageTitle = null
                    }
                )
            }
            composable(route = POSScreen.PAYEBTCashPurchaseScreen.name) {
                EBTCashPurchaseScreen(
                    onConfirmButtonClicked = { ebtCashAmount ->
                        val payment =
                            PosPaymentRequest.forEbtCashPayment(ebtCashAmount, k9SDK.terminalId)
                        viewModel.setLocalPayment(payment)
                        navController.navigate(POSScreen.PAYChoosePANMethodScreen.name)
                    },
                    onCancelButtonClicked = {
                        navController.popBackStack(
                            POSScreen.PAYTransactionTypeSelectionScreen.name,
                            inclusive = false
                        )
                        pageTitle = null
                    }
                )
            }
            composable(route = POSScreen.PAYEBTCashWithdrawalScreen.name) {
                EBTCashWithdrawalScreen(
                    onConfirmButtonClicked = { ebtCashWithdrawalAmount ->
                        val payment = PosPaymentRequest.forEbtCashWithdrawal(
                            ebtCashWithdrawalAmount,
                            k9SDK.terminalId
                        )
                        viewModel.setLocalPayment(payment)
                        navController.navigate(POSScreen.PAYChoosePANMethodScreen.name)
                    },
                    onCancelButtonClicked = {
                        navController.popBackStack(
                            POSScreen.PAYTransactionTypeSelectionScreen.name,
                            inclusive = false
                        )
                        pageTitle = null
                    }
                )
            }
            composable(route = POSScreen.PAYEBTCashPurchaseWithCashBackScreen.name) {
                EBTCashPurchaseWithCashBackScreen(
                    onConfirmButtonClicked = { ebtCashAmount, cashBackAmount ->
                        val payment = PosPaymentRequest.forEbtCashPaymentWithCashBack(
                            ebtCashAmount,
                            cashBackAmount,
                            k9SDK.terminalId
                        )
                        viewModel.setLocalPayment(payment)
                        navController.navigate(POSScreen.PAYChoosePANMethodScreen.name)
                    },
                    onCancelButtonClicked = {
                        navController.popBackStack(
                            POSScreen.PAYTransactionTypeSelectionScreen.name,
                            inclusive = false
                        )
                        pageTitle = null
                    }
                )
            }
            composable(route = POSScreen.PAYChoosePANMethodScreen.name) {
                PANMethodSelectionScreen(
                    onManualEntryButtonClicked = {
                        navController.navigate(POSScreen.PAYManualPANEntryScreen.name)
                    },
                    onSwipeButtonClicked = {
                        navController.navigate(POSScreen.PAYMagSwipePANEntryScreen.name)
                    },
                    onBackButtonClicked = { navController.popBackStack(POSScreen.PAYTransactionTypeSelectionScreen.name, inclusive = false) }
                )
            }
            composable(route = POSScreen.PAYManualPANEntryScreen.name) {
                ManualPANEntryScreen(
                    forageConfig = uiState.forageConfig,
                    onSubmitAsManualEntry = {
                        Log.i("POSComposeApp", "Calling onSubmitButtonClicked in ManualPANEntryScreen in PAYChoosePANMethodScreen")
                        if (panElement != null) {
                            panElement!!.clearFocus()
                            viewModel.tokenizeManualEntryEBTCard(
                                panElement as ForagePANEditText,
                                onSuccess = {
                                    if (uiState.localPayment != null) {
                                        val payment = uiState.localPayment!!
                                        viewModel.createPayment(payment = payment, onSuccess = { serverPayment ->
                                            if (serverPayment.ref !== null) {
                                                viewModel.resetPinActionErrors()
                                                navController.navigate(POSScreen.PAYPINEntryScreen.name)
                                            }
                                        })
                                    }
                                }
                            )
                        }
                    },
                    onSubmitAsTrack2 = {
                        Log.i("POSComposeApp", "Calling onSubmitButtonClicked in ManualPANEntryScreen in PAYChoosePANMethodScreen")
                        if (panElement != null) {
                            panElement!!.clearFocus()
                            viewModel.tokenizeTrack2EBTCard(
                                panElement as ForagePANEditText,
                                onSuccess = {
                                    if (uiState.localPayment != null) {
                                        val payment = uiState.localPayment!!
                                        viewModel.createPayment(payment = payment, onSuccess = { serverPayment ->
                                            if (serverPayment.ref !== null) {
                                                viewModel.resetPinActionErrors()
                                                navController.navigate(POSScreen.PAYPINEntryScreen.name)
                                            }
                                        })
                                    }
                                }
                            )
                        }
                    },
                    onBackButtonClicked = { navController.popBackStack(POSScreen.PAYChoosePANMethodScreen.name, inclusive = false) },
                    withPanElementReference = { panElement = it },
                    errorText = uiState.createPaymentError
                )
            }
            composable(route = POSScreen.PAYMagSwipePANEntryScreen.name) {
                MagSwipePANEntryScreen(
                    onLaunch = {
                        k9SDK.listenForMagneticCardSwipe { track2Data ->
                            viewModel.tokenizeEBTCard(track2Data) { tokenizedCard ->
                                if (tokenizedCard?.ref != null) {
                                    Log.i("POSComposeApp", "Successfully tokenized EBT card with ref: $tokenizedCard.ref")
                                    val payment = uiState.localPayment!!
                                    viewModel.createPayment(payment = payment, onSuccess = { serverPayment ->
                                        if (serverPayment.ref !== null) {
                                            viewModel.resetPinActionErrors()
                                            navController.navigate(POSScreen.PAYPINEntryScreen.name)
                                        }
                                    })
                                }
                            }
                        }
                    },
                    onBackButtonClicked = { navController.popBackStack(POSScreen.PAYChoosePANMethodScreen.name, inclusive = false) },
                    errorText = uiState.createPaymentError
                )
            }
            composable(route = POSScreen.PAYPINEntryScreen.name) {
                PINEntryScreen(
                    last4 = uiState.last4,
                    onDeferButtonClicked = {
                        if (pinElement != null && uiState.createPaymentResponse?.ref != null) {
                            pinElement!!.clearFocus()
                            viewModel.deferPaymentCapture(
                                context = context,
                                forageVaultElement = pinElement!!,
                                terminalId = k9SDK.terminalId,
                                paymentRef = uiState.createPaymentResponse!!.ref!!,
                                onSuccess = {
                                    navController.navigate(POSScreen.DEFERPaymentCaptureResultScreen.name)
                                },
                                onFailure = {
                                    navController.navigate(POSScreen.PAYErrorResultScreen.name)
                                }
                            )
                        }
                    },
                    onBackButtonClicked = { navController.popBackStack(POSScreen.PAYTransactionTypeSelectionScreen.name, inclusive = false) },
                    withPinElementReference = { pinElement = it },
                    errorText = uiState.capturePaymentError
                )
            }
            composable(route = POSScreen.DEFERPaymentCaptureResultScreen.name) {
                DeferredPaymentCaptureResultScreen(
                    terminalId = k9SDK.terminalId,
                    paymentRef = uiState.createPaymentResponse!!.ref!!,
                    onBackButtonClicked = { navController.popBackStack(POSScreen.PAYPINEntryScreen.name, inclusive = false) },
                    onDoneButtonClicked = { navController.popBackStack(POSScreen.ActionSelectionScreen.name, inclusive = false) }
                )
            }
            composable(route = POSScreen.DEFERPaymentRefundResultScreen.name) {
                DeferredPaymentRefundResultScreen(
                    terminalId = k9SDK.terminalId,
                    paymentRef = uiState.localRefundState!!.paymentRef,
                    onBackButtonClicked = { navController.popBackStack(POSScreen.REFUNDPINEntryScreen.name, inclusive = false) },
                    onDoneButtonClicked = { navController.popBackStack(POSScreen.ActionSelectionScreen.name, inclusive = false) }
                )
            }
            composable(route = POSScreen.REFUNDDetailsScreen.name) {
                RefundDetailsScreen(
                    forageConfig = uiState.forageConfig,
                    withPanElementReference = { panElement = it },
                    onConfirmAsManualEntry = { paymentRef, amount, reason ->
                        val refundState = RefundUIState(
                            paymentRef = paymentRef,
                            amount = amount,
                            reason = reason
                        )
                        viewModel.setLocalRefundStateAsManualEntry(
                            refundState,
                            panElement as ForagePANEditText
                        ) {
                            viewModel.resetPinActionErrors()
                            navController.navigate(POSScreen.REFUNDPINEntryScreen.name)
                        }
                    },
                    onConfirmAsTrack2 = { paymentRef, amount, reason ->
                        val refundState = RefundUIState(
                            paymentRef = paymentRef,
                            amount = amount,
                            reason = reason
                        )
                        viewModel.setLocalRefundStateAsTrack2(
                            refundState,
                            panElement as ForagePANEditText
                        ) {
                            viewModel.resetPinActionErrors()
                            navController.navigate(POSScreen.REFUNDPINEntryScreen.name)
                        }
                    },
                    onCancelButtonClicked = { navController.popBackStack(POSScreen.ActionSelectionScreen.name, inclusive = false) }
                )
            }
            composable(route = POSScreen.REFUNDPINEntryScreen.name) {
                PINEntryScreen(
                    last4 = uiState.last4,
                    onDeferButtonClicked = {
                        if (pinElement != null && uiState.localRefundState != null) {
                            pinElement!!.clearFocus()
                            viewModel.deferPaymentRefund(
                                context = context,
                                forageVaultElement = pinElement!!,
                                terminalId = k9SDK.terminalId,
                                paymentRef = uiState.localRefundState!!.paymentRef,
                                onSuccess = {
                                    navController.navigate(POSScreen.DEFERPaymentRefundResultScreen.name)
                                },
                                onFailure = {
                                    navController.navigate(POSScreen.REFUNDErrorResultScreen.name)
                                }
                            )
                        }
                    },
                    onBackButtonClicked = { navController.popBackStack(POSScreen.REFUNDDetailsScreen.name, inclusive = false) },
                    withPinElementReference = { pinElement = it },
                    errorText = uiState.refundPaymentError
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

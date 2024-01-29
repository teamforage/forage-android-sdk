package com.joinforage.android.example.ui.pos

import android.util.Log
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
import com.joinforage.android.example.ui.pos.data.PosPaymentRequest
import com.joinforage.android.example.ui.pos.data.RefundUIState
import com.joinforage.android.example.ui.pos.screens.ActionSelectionScreen
import com.joinforage.android.example.ui.pos.screens.MerchantSetupScreen
import com.joinforage.android.example.ui.pos.screens.balance.BalanceResultScreen
import com.joinforage.android.example.ui.pos.screens.payment.EBTCashPurchaseScreen
import com.joinforage.android.example.ui.pos.screens.payment.EBTCashPurchaseWithCashBackScreen
import com.joinforage.android.example.ui.pos.screens.payment.EBTCashWithdrawalScreen
import com.joinforage.android.example.ui.pos.screens.payment.EBTSnapPurchaseScreen
import com.joinforage.android.example.ui.pos.screens.payment.PaymentResultScreen
import com.joinforage.android.example.ui.pos.screens.payment.PaymentTypeSelectionScreen
import com.joinforage.android.example.ui.pos.screens.refund.RefundDetailsScreen
import com.joinforage.android.example.ui.pos.screens.refund.RefundResultScreen
import com.joinforage.android.example.ui.pos.screens.shared.MagSwipePANEntryScreen
import com.joinforage.android.example.ui.pos.screens.shared.ManualPANEntryScreen
import com.joinforage.android.example.ui.pos.screens.shared.PANMethodSelectionScreen
import com.joinforage.android.example.ui.pos.screens.shared.PINEntryScreen
import com.joinforage.android.example.ui.pos.screens.voids.VoidPaymentScreen
import com.joinforage.android.example.ui.pos.screens.voids.VoidRefundScreen
import com.joinforage.android.example.ui.pos.screens.voids.VoidResultScreen
import com.joinforage.android.example.ui.pos.screens.voids.VoidTypeSelectionScreen
import com.joinforage.forage.android.ui.ForagePANEditText
import com.joinforage.forage.android.ui.ForagePINEditText

enum class POSScreen(@StringRes val title: Int) {
    MerchantSetupScreen(title = R.string.title_pos_merchant_setup),
    ActionSelectionScreen(title = R.string.title_pos_action_selection),
    BIChoosePANMethodScreen(title = R.string.title_pos_balance_inquiry),
    BIManualPANEntryScreen(title = R.string.title_pos_manual_pan_entry),
    BIMagSwipePANEntryScreen(title = R.string.title_pos_mag_swipe_pan_entry),
    BIPINEntryScreen(title = R.string.title_pos_pin_entry),
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
    REFUNDDetailsScreen(title = R.string.title_pos_refund_flow),
    REFUNDPINEntryScreen(title = R.string.title_pos_refund_flow),
    REFUNDResultScreen(title = R.string.title_pos_refund_flow),
    VOIDTransactionTypeSelectionScreen(title = R.string.title_pos_void_flow),
    VOIDPaymentScreen(title = R.string.title_pos_void_flow),
    VOIDRefundScreen(title = R.string.title_pos_void_flow),
    VOIDPaymentResultScreen(title = R.string.title_pos_void_flow),
    VOIDRefundResultScreen(title = R.string.title_pos_void_flow)
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

    var pinElement: ForagePINEditText? by rememberSaveable {
        mutableStateOf(null)
    }

    val context = LocalContext.current
    val k9SDK by remember {
        mutableStateOf(K9SDK().init(context))
    }

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
                    terminalId = k9SDK.terminalId,
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
                    onBackButtonClicked = { navController.popBackStack(POSScreen.MerchantSetupScreen.name, inclusive = false) },
                    onBalanceButtonClicked = { navController.navigate(POSScreen.BIChoosePANMethodScreen.name) },
                    onPaymentButtonClicked = { navController.navigate(POSScreen.PAYTransactionTypeSelectionScreen.name) },
                    onRefundButtonClicked = { navController.navigate(POSScreen.REFUNDDetailsScreen.name) },
                    onVoidButtonClicked = { navController.navigate(POSScreen.VOIDTransactionTypeSelectionScreen.name) }
                )
            }
            composable(route = POSScreen.BIChoosePANMethodScreen.name) {
                PANMethodSelectionScreen(
                    onManualEntryButtonClicked = { navController.navigate(POSScreen.BIManualPANEntryScreen.name) },
                    onSwipeButtonClicked = { navController.navigate(POSScreen.BIMagSwipePANEntryScreen.name) },
                    onBackButtonClicked = { navController.popBackStack(POSScreen.ActionSelectionScreen.name, inclusive = false) }
                )
            }
            composable(route = POSScreen.BIManualPANEntryScreen.name) {
                ManualPANEntryScreen(
                    forageConfig = uiState.forageConfig,
                    onSubmitButtonClicked = {
                        if (panElement != null) {
                            viewModel.tokenizeEBTCard(
                                panElement as ForagePANEditText,
                                k9SDK.terminalId,
                                onSuccess = {
                                    if (it?.ref != null) {
                                        Log.i("POSComposeApp", "Successfully tokenized EBT card with ref: $it.ref")
                                        navController.navigate(POSScreen.BIPINEntryScreen.name)
                                    }
                                }
                            )
                        }
                    },
                    onBackButtonClicked = { navController.popBackStack(POSScreen.BIChoosePANMethodScreen.name, inclusive = false) },
                    withPanElementReference = { panElement = it },
                    errorText = uiState.tokenizationError
                )
            }
            composable(route = POSScreen.BIMagSwipePANEntryScreen.name) {
                MagSwipePANEntryScreen(
                    onLaunch = {
                        k9SDK.listenForMagneticCardSwipe { track2Data ->
                            viewModel.tokenizeEBTCard(track2Data, k9SDK.terminalId) {
                                if (it?.ref != null) {
                                    Log.i("POSComposeApp", "Successfully tokenized EBT card with ref: $it.ref")
                                    navController.navigate(POSScreen.BIPINEntryScreen.name)
                                }
                            }
                        }
                    },
                    onBackButtonClicked = { navController.popBackStack(POSScreen.BIChoosePANMethodScreen.name, inclusive = false) },
                    errorText = uiState.tokenizationError
                )
            }
            composable(route = POSScreen.BIPINEntryScreen.name) {
                PINEntryScreen(
                    forageConfig = uiState.forageConfig,
                    paymentMethodRef = uiState.tokenizedPaymentMethod?.ref,
                    onSubmitButtonClicked = {
                        if (pinElement != null && uiState.tokenizedPaymentMethod?.ref != null) {
                            viewModel.checkEBTCardBalance(
                                pinElement as ForagePINEditText,
                                paymentMethodRef = uiState.tokenizedPaymentMethod!!.ref,
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
                    withPinElementReference = { pinElement = it },
                    errorText = uiState.balanceCheckError
                )
            }
            composable(route = POSScreen.BIResultScreen.name) {
                BalanceResultScreen(
                    balance = uiState.balance,
                    onBackButtonClicked = { navController.popBackStack(POSScreen.BIPINEntryScreen.name, inclusive = false) },
                    onDoneButtonClicked = { navController.popBackStack(POSScreen.ActionSelectionScreen.name, inclusive = false) }
                )
            }
            composable(route = POSScreen.PAYTransactionTypeSelectionScreen.name) {
                PaymentTypeSelectionScreen(
                    onSnapPurchaseClicked = { navController.navigate(POSScreen.PAYSnapPurchaseScreen.name) },
                    onCashPurchaseClicked = { navController.navigate(POSScreen.PAYEBTCashPurchaseScreen.name) },
                    onCashWithdrawalClicked = { navController.navigate(POSScreen.PAYEBTCashWithdrawalScreen.name) },
                    onCashPurchaseCashbackClicked = { navController.navigate(POSScreen.PAYEBTCashPurchaseWithCashBackScreen.name) },
                    onCancelButtonClicked = { navController.popBackStack(POSScreen.ActionSelectionScreen.name, inclusive = false) }
                )
            }
            composable(route = POSScreen.PAYSnapPurchaseScreen.name) {
                EBTSnapPurchaseScreen(
                    onConfirmButtonClicked = { snapAmount ->
                        val payment = PosPaymentRequest.forSnapPayment(snapAmount, k9SDK.terminalId)
                        viewModel.setLocalPayment(payment = payment)
                        navController.navigate(POSScreen.PAYChoosePANMethodScreen.name)
                    },
                    onCancelButtonClicked = { navController.popBackStack(POSScreen.PAYTransactionTypeSelectionScreen.name, inclusive = false) }
                )
            }
            composable(route = POSScreen.PAYEBTCashPurchaseScreen.name) {
                EBTCashPurchaseScreen(
                    onConfirmButtonClicked = { ebtCashAmount ->
                        val payment = PosPaymentRequest.forEbtCashPayment(ebtCashAmount, k9SDK.terminalId)
                        viewModel.setLocalPayment(payment)
                        navController.navigate(POSScreen.PAYChoosePANMethodScreen.name)
                    },
                    onCancelButtonClicked = { navController.popBackStack(POSScreen.PAYTransactionTypeSelectionScreen.name, inclusive = false) }
                )
            }
            composable(route = POSScreen.PAYEBTCashWithdrawalScreen.name) {
                EBTCashWithdrawalScreen(
                    onConfirmButtonClicked = { ebtCashWithdrawalAmount ->
                        val payment = PosPaymentRequest.forEbtCashWithdrawal(ebtCashWithdrawalAmount, k9SDK.terminalId)
                        viewModel.setLocalPayment(payment)
                        navController.navigate(POSScreen.PAYChoosePANMethodScreen.name)
                    },
                    onCancelButtonClicked = { navController.popBackStack(POSScreen.PAYTransactionTypeSelectionScreen.name, inclusive = false) }
                )
            }
            composable(route = POSScreen.PAYEBTCashPurchaseWithCashBackScreen.name) {
                EBTCashPurchaseWithCashBackScreen(
                    onConfirmButtonClicked = { ebtCashAmount, cashBackAmount ->
                        val payment = PosPaymentRequest.forEbtCashPaymentWithCashBack(ebtCashAmount, cashBackAmount, k9SDK.terminalId)
                        viewModel.setLocalPayment(payment)
                        navController.navigate(POSScreen.PAYChoosePANMethodScreen.name)
                    },
                    onCancelButtonClicked = { navController.popBackStack(POSScreen.PAYTransactionTypeSelectionScreen.name, inclusive = false) }
                )
            }
            composable(route = POSScreen.PAYChoosePANMethodScreen.name) {
                PANMethodSelectionScreen(
                    onManualEntryButtonClicked = { navController.navigate(POSScreen.PAYManualPANEntryScreen.name) },
                    onSwipeButtonClicked = { navController.navigate(POSScreen.PAYMagSwipePANEntryScreen.name) },
                    onBackButtonClicked = { navController.popBackStack(POSScreen.PAYTransactionTypeSelectionScreen.name, inclusive = false) }
                )
            }
            composable(route = POSScreen.PAYManualPANEntryScreen.name) {
                ManualPANEntryScreen(
                    forageConfig = uiState.forageConfig,
                    onSubmitButtonClicked = {
                        Log.i("POSComposeApp", "Calling onSubmitButtonClicked in ManualPANEntryScreen in PAYChoosePANMethodScreen")

                        if (panElement != null) {
                            viewModel.tokenizeEBTCard(
                                panElement as ForagePANEditText,
                                k9SDK.terminalId,
                                onSuccess = { tokenizedCard ->
                                    Log.i("POSComposeApp", "payment method? — $tokenizedCard")
                                    if (tokenizedCard?.ref != null && uiState.localPayment != null) {
                                        Log.i("POSComposeApp", "Successfully tokenized EBT card with ref: $tokenizedCard.ref")
                                        val payment = uiState.localPayment!!.copy(paymentMethodRef = tokenizedCard.ref)
                                        viewModel.createPayment(payment = payment, merchantId = uiState.merchantId, onSuccess = { serverPayment ->
                                            if (serverPayment.ref !== null) {
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
                    errorText = uiState.tokenizationError ?: uiState.createPaymentError
                )
            }
            composable(route = POSScreen.PAYMagSwipePANEntryScreen.name) {
                MagSwipePANEntryScreen(
                    onLaunch = {
                        k9SDK.listenForMagneticCardSwipe { track2Data ->
                            viewModel.tokenizeEBTCard(track2Data, k9SDK.terminalId) { tokenizedCard ->
                                if (tokenizedCard?.ref != null) {
                                    Log.i("POSComposeApp", "Successfully tokenized EBT card with ref: $tokenizedCard.ref")
                                    val payment = uiState.localPayment!!.copy(paymentMethodRef = tokenizedCard.ref)
                                    viewModel.createPayment(payment = payment, merchantId = uiState.merchantId, onSuccess = { serverPayment ->
                                        if (serverPayment.ref !== null) {
                                            navController.navigate(POSScreen.PAYPINEntryScreen.name)
                                        }
                                    })
                                }
                            }
                        }
                    },
                    onBackButtonClicked = { navController.popBackStack(POSScreen.PAYChoosePANMethodScreen.name, inclusive = false) },
                    errorText = uiState.tokenizationError ?: uiState.createPaymentError
                )
            }
            composable(route = POSScreen.PAYPINEntryScreen.name) {
                PINEntryScreen(
                    forageConfig = uiState.forageConfig,
                    paymentMethodRef = uiState.createPaymentResponse?.paymentMethod,
                    onSubmitButtonClicked = {
                        if (pinElement != null && uiState.createPaymentResponse?.ref != null) {
                            viewModel.capturePayment(
                                foragePinEditText = pinElement as ForagePINEditText,
                                terminalId = k9SDK.terminalId,
                                paymentRef = uiState.createPaymentResponse!!.ref!!,
                                onSuccess = {
                                    if (it?.ref != null) {
                                        navController.navigate(POSScreen.PAYResultScreen.name)
                                    }
                                }
                            )
                        }
                    },
                    onBackButtonClicked = { navController.popBackStack(POSScreen.PAYTransactionTypeSelectionScreen.name, inclusive = false) },
                    withPinElementReference = { pinElement = it },
                    errorText = uiState.capturePaymentError
                )
            }
            composable(route = POSScreen.PAYResultScreen.name) {
                PaymentResultScreen(
                    data = uiState.capturePaymentResponse.toString()
                )
            }
            composable(route = POSScreen.REFUNDDetailsScreen.name) {
                RefundDetailsScreen(
                    onConfirmButtonClicked = { paymentRef, amount, reason ->
                        val refundState = RefundUIState(
                            paymentRef = paymentRef,
                            amount = amount,
                            reason = reason
                        )
                        viewModel.setLocalRefundState(refundState)
                        navController.navigate(POSScreen.REFUNDPINEntryScreen.name)
                    },
                    onCancelButtonClicked = { navController.popBackStack(POSScreen.ActionSelectionScreen.name, inclusive = false) }
                )
            }
            composable(route = POSScreen.REFUNDPINEntryScreen.name) {
                PINEntryScreen(
                    forageConfig = uiState.forageConfig,
                    paymentMethodRef = uiState.localRefundState?.paymentRef,
                    onSubmitButtonClicked = {
                        if (pinElement != null && uiState.localRefundState != null) {
                            viewModel.refundPayment(
                                foragePinEditText = pinElement as ForagePINEditText,
                                terminalId = k9SDK.terminalId,
                                paymentRef = uiState.localRefundState!!.paymentRef,
                                amount = uiState.localRefundState!!.amount,
                                reason = uiState.localRefundState!!.reason,
                                onSuccess = {
                                    if (it != null) {
                                        navController.navigate(POSScreen.REFUNDResultScreen.name)
                                    }
                                }
                            )
                        }
                    },
                    onBackButtonClicked = { navController.popBackStack(POSScreen.REFUNDDetailsScreen.name, inclusive = false) },
                    withPinElementReference = { pinElement = it },
                    errorText = uiState.refundPaymentError
                )
            }
            composable(route = POSScreen.REFUNDResultScreen.name) {
                RefundResultScreen(data = uiState.refundPaymentResponse.toString())
            }
            composable(route = POSScreen.VOIDTransactionTypeSelectionScreen.name) {
                VoidTypeSelectionScreen(
                    onPaymentButtonClicked = { navController.navigate(POSScreen.VOIDPaymentScreen.name) },
                    onRefundButtonClicked = { navController.navigate(POSScreen.VOIDRefundScreen.name) },
                    onCancelButtonClicked = { navController.popBackStack(POSScreen.ActionSelectionScreen.name, inclusive = false) }
                )
            }
            composable(route = POSScreen.VOIDPaymentScreen.name) {
                VoidPaymentScreen(
                    onConfirmButtonClicked = { paymentRef ->
                        Log.i("POSComposeApp", "Voiding payment: $paymentRef")
                        viewModel.voidPayment(paymentRef) {
                            Log.i("POSComposeApp", "Voided payment: $it")
                            navController.navigate(POSScreen.VOIDPaymentResultScreen.name)
                        }
                    },
                    onCancelButtonClicked = {
                        navController.popBackStack(POSScreen.VOIDTransactionTypeSelectionScreen.name, inclusive = false)
                    },
                    errorText = uiState.voidPaymentError
                )
            }
            composable(route = POSScreen.VOIDRefundScreen.name) {
                VoidRefundScreen(
                    onConfirmButtonClicked = { paymentRef, refundRef ->
                        Log.i("POSComposeApp", "Voiding refund: $refundRef")
                        viewModel.voidRefund(paymentRef, refundRef) {
                            Log.i("POSComposeApp", "Voided refund: $it")
                            navController.navigate(POSScreen.VOIDRefundResultScreen.name)
                        }
                    },
                    onCancelButtonClicked = {
                        navController.popBackStack(POSScreen.VOIDTransactionTypeSelectionScreen.name, inclusive = false)
                    },
                    errorText = uiState.voidRefundError
                )
            }
            composable(route = POSScreen.VOIDPaymentResultScreen.name) {
                VoidResultScreen(data = uiState.voidPaymentResponse.toString())
            }
            composable(route = POSScreen.VOIDRefundResultScreen.name) {
                VoidResultScreen(data = uiState.voidRefundResponse.toString())
            }
        }
    }
}

@Preview
@Composable
fun PosAppPreview() {
    POSComposeApp()
}

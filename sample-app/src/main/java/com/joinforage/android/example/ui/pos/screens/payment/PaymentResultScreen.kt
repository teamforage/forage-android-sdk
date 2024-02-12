package com.joinforage.android.example.ui.pos.screens.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.joinforage.android.example.pos.receipts.templates.BaseReceiptTemplate
import com.joinforage.android.example.pos.receipts.templates.txs.CashPurchaseTxReceipt
import com.joinforage.android.example.pos.receipts.templates.txs.CashPurchaseWithCashbackTxReceipt
import com.joinforage.android.example.pos.receipts.templates.txs.CashWithdrawalTxReceipt
import com.joinforage.android.example.pos.receipts.templates.txs.SnapPurchaseTxReceipt
import com.joinforage.android.example.pos.receipts.templates.txs.TxType
import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.PosPaymentResponse
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod
import com.joinforage.android.example.ui.pos.screens.ReceiptPreviewScreen

@Composable
fun PaymentResultScreen(
    merchant: Merchant?,
    terminalId: String,
    paymentMethod: PosPaymentMethod?,
    txType: TxType?,
    paymentResponse: PosPaymentResponse?,
    onBackButtonClicked: () -> Unit,
    onDoneButtonClicked: () -> Unit,
    onReloadButtonClicked: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (txType == null) {
                Text("Unable to determine transaction type. Terminal might be offline.")
                Button(onClick = onReloadButtonClicked) {
                    Text("Re-fetch Payment")
                }
            } else if (paymentResponse == null) {
                Text("null paymentResponse")
            } else {
                var receipt: BaseReceiptTemplate? = null
                if (txType == TxType.SNAP_PAYMENT) {
                    receipt = SnapPurchaseTxReceipt(
                        merchant,
                        terminalId,
                        paymentMethod,
                        paymentResponse
                    )
                }
                if (txType == TxType.CASH_PAYMENT) {
                    receipt = CashPurchaseTxReceipt(
                        merchant,
                        terminalId,
                        paymentMethod,
                        paymentResponse
                    )
                }
                if (txType == TxType.CASH_PURCHASE_WITH_CASHBACK) {
                    receipt = CashPurchaseWithCashbackTxReceipt(
                        merchant,
                        terminalId,
                        paymentMethod,
                        paymentResponse
                    )
                }
                if (txType == TxType.CASH_WITHDRAWAL) {
                    receipt = CashWithdrawalTxReceipt(
                        merchant,
                        terminalId,
                        paymentMethod,
                        paymentResponse
                    )
                }
                Column {
                    if (paymentResponse.ref != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Payment Ref: ${paymentResponse.ref}")
                            Button(onClick = {
                                clipboardManager.setText(AnnotatedString(paymentResponse.ref!!))
                            }, colors = ButtonDefaults.elevatedButtonColors()) {
                                Text("Copy")
                            }
                        }
                    }
                    ReceiptPreviewScreen(receipt!!.getReceiptLayout())
                }
            }
        }
        if (paymentMethod?.balance == null) {
            Button(onClick = onBackButtonClicked) {
                Text("Try Again")
            }
        } else {
            Button(onClick = onDoneButtonClicked) {
                Text("Done")
            }
        }
    }
}

@Preview
@Composable
fun PaymentResultScreenPreview() {
    PaymentResultScreen(
        merchant = null,
        terminalId = "",
        paymentMethod = null,
        txType = null,
        paymentResponse = null,
        onBackButtonClicked = {},
        onDoneButtonClicked = {},
        onReloadButtonClicked = {}
    )
}

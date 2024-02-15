package com.joinforage.android.example.ui.pos.screens.refund

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
import com.joinforage.android.example.ui.pos.data.Receipt
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod
import com.joinforage.android.example.ui.pos.screens.ReceiptPreviewScreen

@Composable
fun RefundResultScreen(
    merchant: Merchant?,
    terminalId: String,
    paymentMethod: PosPaymentMethod?,
    paymentRef: String,
    refundRef: String?,
    txType: TxType?,
    receipt: Receipt?,
    fetchedPayment: PosPaymentResponse?,
    onRefundRefClicked: (paymentRef: String, refundRef: String) -> Unit,
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
                if (fetchedPayment?.ref == null) {
                    Text("Re-fetch payment to see a list of refunds on the payment.")
                    Button(onClick = onReloadButtonClicked) {
                        Text("Re-fetch Payment")
                    }
                } else {
                    Text("Select a Refund ref from payment (${fetchedPayment.ref}) to view the receipt for:")
                    fetchedPayment.refunds.forEach { refundRef ->
                        Button(onClick = { onRefundRefClicked(fetchedPayment.ref!!, refundRef) }) {
                            Text(refundRef)
                        }
                    }
                }
            } else if (receipt == null) {
                Text("null refundResponse")
            } else {
                var receiptTemplate: BaseReceiptTemplate? = null
                if (txType == TxType.REFUND_SNAP_PAYMENT) {
                    receiptTemplate = SnapPurchaseTxReceipt(
                        merchant,
                        terminalId,
                        paymentMethod,
                        receipt,
                        txType.title
                    )
                }
                if (txType == TxType.REFUND_CASH_PAYMENT) {
                    receiptTemplate = CashPurchaseTxReceipt(
                        merchant,
                        terminalId,
                        paymentMethod,
                        receipt,
                        txType.title
                    )
                }
                if (txType == TxType.REFUND_CASH_PURCHASE_WITH_CASHBACK) {
                    receiptTemplate = CashPurchaseWithCashbackTxReceipt(
                        merchant,
                        terminalId,
                        paymentMethod,
                        receipt,
                        txType.title
                    )
                }
                if (txType == TxType.REFUND_CASH_WITHDRAWAL) {
                    receiptTemplate = CashWithdrawalTxReceipt(
                        merchant,
                        terminalId,
                        paymentMethod,
                        receipt,
                        txType.title
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Payment Ref: $paymentRef")
                        Button(onClick = {
                            clipboardManager.setText(AnnotatedString(paymentRef))
                        }, colors = ButtonDefaults.elevatedButtonColors()) {
                            Text("Copy")
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Refund Ref: $refundRef")
                        if (refundRef != null) {
                            Button(onClick = {
                                clipboardManager.setText(AnnotatedString(refundRef))
                            }, colors = ButtonDefaults.elevatedButtonColors()) {
                                Text("Copy")
                            }
                        }
                    }
                    if (receiptTemplate != null) {
                        ReceiptPreviewScreen(receiptTemplate.getReceiptLayout())
                    } else {
                        Text("Couldn't find receipt template matching transaction type: ${txType.title}")
                    }
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
fun RefundResultScreenPreview() {
    RefundResultScreen(
        merchant = null,
        terminalId = "",
        paymentMethod = null,
        paymentRef = "",
        refundRef = "",
        txType = null,
        receipt = null,
        fetchedPayment = null,
        onRefundRefClicked = { _, _ -> },
        onBackButtonClicked = {},
        onDoneButtonClicked = {},
        onReloadButtonClicked = {}
    )
}

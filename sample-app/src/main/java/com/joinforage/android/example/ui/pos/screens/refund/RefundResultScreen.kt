package com.joinforage.android.example.ui.pos.screens.refund

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.joinforage.android.example.pos.receipts.templates.BaseReceiptTemplate
import com.joinforage.android.example.pos.receipts.templates.txs.CashPurchaseTxReceipt
import com.joinforage.android.example.pos.receipts.templates.txs.CashPurchaseWithCashbackTxReceipt
import com.joinforage.android.example.pos.receipts.templates.txs.CashWithdrawalTxReceipt
import com.joinforage.android.example.pos.receipts.templates.txs.SnapPurchaseTxReceipt
import com.joinforage.android.example.pos.receipts.templates.txs.TxType
import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.PosPaymentRequest
import com.joinforage.android.example.ui.pos.data.Refund
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod
import com.joinforage.android.example.ui.pos.screens.ReceiptPreviewScreen

@Composable
fun RefundResultScreen(
    merchant: Merchant?,
    terminalId: String,
    paymentMethod: PosPaymentMethod?,
    txType: TxType?,
    refundResponse: Refund?,
    onBackButtonClicked: () -> Unit,
    onDoneButtonClicked: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (txType == null) {
                Text("null paymentRequest")
            } else if (refundResponse == null) {
                Text("null refundResponse")
            } else {
                var receipt: BaseReceiptTemplate? = null
                if (txType == TxType.SNAP_PAYMENT) {
                    receipt = SnapPurchaseTxReceipt(
                        merchant,
                        terminalId,
                        paymentMethod,
                        refundResponse
                    )
                }
                if (txType == TxType.CASH_PAYMENT) {
                    receipt = CashPurchaseTxReceipt(
                        merchant,
                        terminalId,
                        paymentMethod,
                        refundResponse
                    )
                }
                if (txType == TxType.CASH_PURCHASE_WITH_CASHBACK) {
                    receipt = CashPurchaseWithCashbackTxReceipt(
                        merchant,
                        terminalId,
                        paymentMethod,
                        refundResponse
                    )
                }
                if (txType == TxType.CASH_WITHDRAWAL) {
                    receipt = CashWithdrawalTxReceipt(
                        merchant,
                        terminalId,
                        paymentMethod,
                        refundResponse
                    )
                }
                ReceiptPreviewScreen(receipt!!.getReceiptLayout())
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
        txType = null,
        refundResponse = null,
        onBackButtonClicked = {},
        onDoneButtonClicked = {}
    )
}

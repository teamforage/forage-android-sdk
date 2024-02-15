package com.joinforage.android.example.pos.receipts.templates.txs

import com.joinforage.android.example.ui.pos.data.FundingType
import com.joinforage.android.example.ui.pos.data.Receipt
import com.joinforage.android.example.ui.pos.data.TransactionType

fun isPayment(receipt: Receipt) = receipt.transactionType == "Payment"
fun isRefund(receipt: Receipt) = receipt.transactionType == "Refund"

val ZERO_TX = "0.00"
fun spentSnap(receipt: Receipt) = receipt.snapAmount != ZERO_TX
fun spentEbtCash(receipt: Receipt) = receipt.ebtCashAmount != ZERO_TX
fun withdrewEbtCash(receipt: Receipt) = receipt.cashBackAmount != ZERO_TX

enum class TxType(val title: String) {
    SNAP_PAYMENT("SNAP PAYMENT"),
    CASH_PAYMENT("CASH PAYMENT"),
    CASH_WITHDRAWAL("CASH WITHDRAWAL"),
    CASH_PURCHASE_WITH_CASHBACK("CASH PURCHASE WITH CASHBACK"),
    REFUND_SNAP_PAYMENT("REFUND SNAP PAYMENT"),
    REFUND_CASH_PAYMENT("REFUND CASH PAYMENT"),
    REFUND_CASH_WITHDRAWAL("REFUND CASH WITHDRAWAL"),
    REFUND_CASH_PURCHASE_WITH_CASHBACK("REFUND CASH PURCHASE WITH CASHBACK"),
    UNKNOWN("UNKNOWN");

    companion object {
        fun forReceipt(receipt: Receipt): TxType {
            if (isPayment(receipt)) {
                if (spentSnap(receipt)) {
                    return SNAP_PAYMENT
                }
                if (spentEbtCash(receipt) && withdrewEbtCash(receipt)) {
                    return CASH_PURCHASE_WITH_CASHBACK
                }
                if (!spentEbtCash(receipt) && withdrewEbtCash(receipt)) {
                    return CASH_WITHDRAWAL
                }
                if (spentEbtCash(receipt) && !withdrewEbtCash(receipt)) {
                    return CASH_PAYMENT
                }
            }

            if (isRefund(receipt)) {
                if (spentSnap(receipt)) {
                    return REFUND_SNAP_PAYMENT
                }
                if (spentEbtCash(receipt) && withdrewEbtCash(receipt)) {
                    return REFUND_CASH_PURCHASE_WITH_CASHBACK
                }
                if (!spentEbtCash(receipt) && withdrewEbtCash(receipt)) {
                    return REFUND_CASH_WITHDRAWAL
                }
                if (spentEbtCash(receipt) && !withdrewEbtCash(receipt)) {
                    return REFUND_CASH_PAYMENT
                }
            }

            return UNKNOWN
        }

        fun forPayment(transactionType: String?, fundingType: String): TxType {
            if (transactionType == TransactionType.Withdrawal.value) {
                return CASH_WITHDRAWAL
            }
            if (transactionType == TransactionType.PurchaseWithCashBack.value) {
                return CASH_PURCHASE_WITH_CASHBACK
            }
            if (fundingType == FundingType.EBTSnap.value) {
                return SNAP_PAYMENT
            }
            if (fundingType == FundingType.EBTCash.value) {
                return CASH_PAYMENT
            }
            return UNKNOWN
        }
    }
}

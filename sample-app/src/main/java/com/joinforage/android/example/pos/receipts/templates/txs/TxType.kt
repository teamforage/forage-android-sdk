package com.joinforage.android.example.pos.receipts.templates.txs

import com.joinforage.android.example.ui.pos.data.PosPaymentRequest
import com.joinforage.android.example.ui.pos.data.PosPaymentResponse
import com.joinforage.android.example.ui.pos.data.Receipt
import com.joinforage.android.example.ui.pos.data.TransactionType

fun isPayment(receipt: Receipt) = receipt.transactionType == "Payment"
fun isRefund(receipt: Receipt) = receipt.transactionType == "Refund"

val ZERO_TX = "0.00"
fun spentSnap(receipt: Receipt) = receipt.snapAmount != ZERO_TX
fun spentSnap(paymentRequest: PosPaymentRequest) = paymentRequest.fundingType == "ebt_snap"
fun spentSnap(paymentResponse: PosPaymentResponse) = paymentResponse.fundingType == "ebt_snap"
fun spentEbtCash(receipt: Receipt) = receipt.ebtCashAmount != ZERO_TX
fun spentEbtCash(paymentRequest: PosPaymentRequest) = paymentRequest.fundingType == "ebt_cash"
fun spentEbtCash(paymentResponse: PosPaymentResponse) = paymentResponse.fundingType == "ebt_cash"
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
        fun fromReceipt(receipt: Receipt): TxType {
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

        fun fromPaymentRequest(payment: PosPaymentRequest): TxType {
            return when {
                payment.transactionType == TransactionType.Withdrawal.value -> CASH_WITHDRAWAL
                payment.transactionType == TransactionType.PurchaseWithCashBack.value -> CASH_PURCHASE_WITH_CASHBACK
                spentSnap(payment) -> SNAP_PAYMENT
                spentEbtCash(payment) -> CASH_PAYMENT
                else -> UNKNOWN
            }
        }

        fun fromPaymentResponse(payment: PosPaymentResponse): TxType {
            return when {
                payment.transactionType == TransactionType.Withdrawal.value -> CASH_WITHDRAWAL
                payment.transactionType == TransactionType.PurchaseWithCashBack.value -> CASH_PURCHASE_WITH_CASHBACK
                spentSnap(payment) -> SNAP_PAYMENT
                spentEbtCash(payment) -> CASH_PAYMENT
                else -> UNKNOWN
            }
        }
    }
}

package com.joinforage.android.example.pos.receipts.templates.txs

import com.joinforage.android.example.pos.receipts.primitives.ReceiptLayout
import com.joinforage.android.example.pos.receipts.primitives.ReceiptLayoutLine

internal abstract class TxDataLayout {
    abstract val snapAmt: String
    abstract val cashAmt: String
    abstract val snapBal: String
    abstract val cashBal: String
    open val withdrawalAmt: String = ZERO_TX
    val header = ReceiptLayoutLine.tripleCol("", "TX AMT", "END BAL")
    val snap
        get() = ReceiptLayoutLine.tripleCol("SNAP", snapAmt, snapBal)
    val cash
        get() = ReceiptLayoutLine.tripleCol("CASH", cashAmt, cashBal)
    val withdrawal
        get() = ReceiptLayoutLine.tripleCol("CS W/D", withdrawalAmt, cashBal)

    fun getLayout() = if (withdrawalAmt == ZERO_TX) {
        ReceiptLayout(header, snap, cash)
    } else {
        ReceiptLayout(header, snap, cash, withdrawal)
    }
}

internal class SnapPaymentLayout(
    override val snapBal: String,
    override val cashBal: String,
    override val snapAmt: String
) : TxDataLayout() {
    override val cashAmt: String = ZERO_TX
}

internal class CashPaymentLayout(
    override val snapBal: String,
    override val cashBal: String,
    override val cashAmt: String
) : TxDataLayout() {
    override val snapAmt: String = ZERO_TX
}

internal class CashPurchaseWithCashbackLayout(
    override val snapBal: String,
    override val cashBal: String,
    override val cashAmt: String,
    override val withdrawalAmt: String
) : TxDataLayout() {
    override val snapAmt: String = ZERO_TX
}

internal class CashWithdrawalLayout(
    override val snapBal: String,
    override val cashBal: String,
    override val withdrawalAmt: String
) : TxDataLayout() {
    override val snapAmt: String = ZERO_TX
    override val cashAmt: String = ZERO_TX
}

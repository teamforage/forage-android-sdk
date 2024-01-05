package com.joinforage.forage.android.pos

import com.joinforage.forage.android.CapturePaymentParams
import com.joinforage.forage.android.CheckBalanceParams
import com.joinforage.forage.android.DeferPaymentCaptureParams
import com.joinforage.forage.android.ForageConfigNotSetException
import com.joinforage.forage.android.ForageSDK
import com.joinforage.forage.android.ForageSDKInterface
import com.joinforage.forage.android.TokenizeEBTCardParams
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.ui.ForagePANEditText
import com.joinforage.forage.android.ui.ForagePINEditText

data class RefundPosPaymentParams(
    val foragePinEditText: ForagePINEditText,
    val paymentRef: String,
    val amount: Float,
    val reason: String,
    val metadata: Map<String, String>? = null,
)

class ForageTerminalSDK(
    private val posTerminalId: String,
    private val forageSdk: ForageSDKInterface = ForageSDK()
) : ForageSDKInterface {

    // Use this for manual PAN entry
    /**
     * A method to securely tokenize an EBT card via ForagePANEditText
     *
     * @param foragePanEditText A ForagePANEditText  UI component. Importantly,
     * you must have called .setForageConfig() already
     * @param reusable Indicates whether the tokenized card can be reused for
     * multiple transactions.
     *
     * @return A ForageAPIResponse indicating the success or failure of the operation.
     * On success, returns a [PaymentMethod](https://docs.joinforage.app/reference/create-payment-method)
     * token which can be securely stored and used for subsequent transactions. On failure,
     * returns a detailed error response for proper handling.
     *
     * @throws ForageConfigNotSetException If the passed ForagePANEditText instance
     * hasn't had its ForageConfig set via .setForageConfig().
     */
    suspend fun tokenizeEBTCard(
        foragePanEditText: ForagePANEditText,
        reusable: Boolean = true
    ): ForageApiResponse<String> {
        // TODO: implement me
        return ForageApiResponse.Success("TODO")
    }

    // Use this for magnetic card swipe
    suspend fun tokenizeEBTCard(
        track2Data: String,
        reusable: Boolean,
    ): ForageApiResponse<String> {
        // TODO: implement me
        return ForageApiResponse.Success("TODO")
    }

    suspend fun refundPosPayment(
        params: RefundPosPaymentParams
    ): ForageApiResponse<String> {
        return ForageApiResponse.Success("TODO")
    }

    // === Identical method signatures in both SDKs ===
    override suspend fun checkBalance(
        params: CheckBalanceParams
    ): ForageApiResponse<String> {
        return this.forageSdk.checkBalance(params)
    }

    override suspend fun capturePayment(
        params: CapturePaymentParams
    ): ForageApiResponse<String> {
        return this.forageSdk.capturePayment(params)
    }

    override suspend fun deferPaymentCapture(params: DeferPaymentCaptureParams): ForageApiResponse<String> {
        return this.forageSdk.deferPaymentCapture(params)
    }

    /**
     * @throws NotImplementedError
     */
    override suspend fun tokenizeEBTCard(params: TokenizeEBTCardParams): ForageApiResponse<String> {
        throw NotImplementedError("This method is not applicable to the Forage Terminal SDK. See other tokenizeEBTCard methods with different signatures for more info.")
    }
}
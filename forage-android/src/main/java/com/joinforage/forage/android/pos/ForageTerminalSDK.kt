package com.joinforage.forage.android.pos

import com.joinforage.forage.android.CapturePaymentParams
import com.joinforage.forage.android.CheckBalanceParams
import com.joinforage.forage.android.DeferPaymentCaptureParams
import com.joinforage.forage.android.ForageConfigNotSetException
import com.joinforage.forage.android.ForageSDK
import com.joinforage.forage.android.ForageSDKInterface
import com.joinforage.forage.android.TokenizeEBTCardParams
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.ui.ForagePANEditText
import com.joinforage.forage.android.ui.ForagePINEditText

data class RefundPosPaymentParams(
    val foragePinEditText: ForagePINEditText,
    val paymentRef: String,
    val amount: Float,
    val reason: String,
    val metadata: Map<String, String>? = null
)

/**
 * Instantiating a ForageTerminalSDK starts a connection with Forage’s servers.
 * @param posTerminalId The ID of the POS terminal that is using the Forage SDK.
 */
class ForageTerminalSDK(
    private val posTerminalId: String
) : ForageSDKInterface {
    private var forageSdk: ForageSDKInterface = ForageSDK()
    private var logger: Log = Log.getInstance()

    // internal constructor facilitates testing
    internal constructor(
        posTerminalId: String,
        forageSdk: ForageSDKInterface,
        logger: Log
    ) : this(posTerminalId) {
        this.forageSdk = forageSdk
        this.logger = logger
    }

    /**
     * A method to securely tokenize an EBT card via ForagePANEditText via UI-based PAN entry
     *
     * @param foragePanEditText A ForagePANEditText  UI component. Importantly,
     * you must have called .setForageConfig() already
     * @param reusable Optional. Indicates whether the tokenized card can be reused for
     * multiple transactions. Defaults to true.
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
        logger.i(
            "[POS] Tokenizing Payment Method via UI PAN entry",
            attributes = mapOf(
                "reusable" to reusable
            )
        )

        return forageSdk.tokenizeEBTCard(
            TokenizeEBTCardParams(
                foragePanEditText = foragePanEditText,
                reusable = reusable
            )
        )
    }

    /**
     * Tokenizes an EBT card using track 2 data from a magnetic card swipe.
     *
     * @param track2Data The track 2 data obtained from the magnetic stripe of the card.
     * @param reusable Optional. Indicates whether the tokenized card can be
     * reused for multiple transactions. Defaults to true.
     *
     * @return A ForageAPIResponse indicating the success or failure of the operation.
     * On success, returns a [PaymentMethod](https://docs.joinforage.app/reference/create-payment-method)
     * token which can be securely stored and used for subsequent transactions. On failure,
     * returns a detailed error response for proper handling.
     */
    suspend fun tokenizeEBTCard(
        track2Data: String,
        reusable: Boolean = true
    ): ForageApiResponse<String> {
        logger.i(
            "[POS] Tokenizing Payment Method via magnetic card swipe",
            attributes = mapOf(
                "reusable" to reusable
            )
        )

        return ForageApiResponse.Success("TODO")
    }

    /**
     * TODO: add comment here
     */
    suspend fun refundPosPayment(
        params: RefundPosPaymentParams
    ): ForageApiResponse<String> {
        return ForageApiResponse.Success("TODO")
    }

    // ======= Same as online-only Forage SDK below =======

    /**
     * Checks the balance SNAP and EBT Cash balances of an EBT account via
     * ForagePINEditText
     *
     * @param params The parameters required for tokenization, including
     * reference to a ForagePINEditText and PaymentMethod ref
     *
     * @return A ForageAPIResponse indicating the success or failure of the operation.
     * On success, returns an object with `snap` and `cash` fields, whose values
     * indicate the balance of each tender as of now
     *
     * @throws ForageConfigNotSetException If the passed ForagePINEditText instance
     * hasn't had its ForageConfig set via .setForageConfig().
     */
    override suspend fun checkBalance(
        params: CheckBalanceParams
    ): ForageApiResponse<String> {
        return ForageApiResponse.Success("TODO")
    }

    /**
     * Captures a Forage Payment associated with an EBT card
     *
     * @param params The parameters required for payment capture, including
     * reference to a ForagePINEditText and a Payment ref
     *
     * @return A ForageAPIResponse indicating the success or failure of the
     * payment capture. On success, returns a confirmation of the transaction.
     * On failure, provides a detailed error response.
     *
     * @throws ForageConfigNotSetException If the passed ForagePINEditText instance
     * hasn't had its ForageConfig set via .setForageConfig().
     */
    override suspend fun capturePayment(
        params: CapturePaymentParams
    ): ForageApiResponse<String> {
        return forageSdk.capturePayment(params)
    }

    /**
     * Collect's a customer's PIN for an EBT payment and defers
     * the capture of the payment to the server
     *
     * @param params The parameters required for deferring the capture of an EBT payment,
     * including a reference to a ForagePINEditText and a Payment ref
     *
     * @return A ForageAPIResponse indicating the success or failure of the
     * PIN capture. On success, returns `Nothing`.
     * On failure, provides a detailed error response.
     *
     * @throws ForageConfigNotSetException If the passed ForagePINEditText instance
     * hasn't had its ForageConfig set via .setForageConfig().
     */
    override suspend fun deferPaymentCapture(params: DeferPaymentCaptureParams): ForageApiResponse<String> {
        return forageSdk.deferPaymentCapture(params)
    }

    /**
     * Use other `tokenizeEBTCard` methods with different signatures instead.
     *
     * @throws NotImplementedError
     */
    @Deprecated(
        message = "This method is not applicable to the Forage Terminal SDK. Use the other tokenizeEBTCard methods.",
        level = DeprecationLevel.ERROR
    )
    override suspend fun tokenizeEBTCard(params: TokenizeEBTCardParams): ForageApiResponse<String> {
        throw NotImplementedError(
            """
            This method is not applicable to the Forage Terminal SDK.
            Use the other tokenizeEBTCard methods.
            """.trimIndent()
        )
    }
}

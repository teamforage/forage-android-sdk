package com.joinforage.forage.android.pos

import com.joinforage.forage.android.CapturePaymentParams
import com.joinforage.forage.android.CheckBalanceParams
import com.joinforage.forage.android.DeferPaymentCaptureParams
import com.joinforage.forage.android.ForageConfigNotSetException
import com.joinforage.forage.android.ForageSDK
import com.joinforage.forage.android.ForageSDKInterface
import com.joinforage.forage.android.TokenizeEBTCardParams
import com.joinforage.forage.android.core.telemetry.CustomerPerceivedResponseMonitor
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.core.telemetry.UserAction
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.ui.ForagePANEditText

/**
 * ForageTerminalSDK is the entry point for **in-store POS Terminal** transactions.
 *
 * You need to initialize the SDK with a unique POS Terminal ID before you can perform operations like:
 *
 * * [Tokenizing card information][tokenizeCard]
 * * [Checking the balance of a card][checkBalance]
 * * [Capturing a payment][capturePayment]
 * * [Collecting a customer's PIN for a payment and deferring the capture of the payment to the server][deferPaymentCapture]
 *
 * @see [The online Forage SDK][ForageSDK] Use [ForageSDK] for processing online transactions.
 * @param posTerminalId The unique string that identifies the POS Terminal.
 */
class ForageTerminalSDK(
    private val posTerminalId: String
) : ForageSDKInterface {
    private var createServiceFactory = {
            sessionToken: String, merchantId: String, logger: Log ->
        ForageSDK.ServiceFactory(
            sessionToken = sessionToken,
            merchantId = merchantId,
            logger = logger
        )
    }

    private var forageSdk: ForageSDK = ForageSDK()
    private var createLogger: () -> Log = { Log.getInstance() }

    // internal constructor facilitates testing
    internal constructor(
        posTerminalId: String,
        forageSdk: ForageSDK,
        createLogger: () -> Log,
        createServiceFactory: ((String, String, Log) -> ForageSDK.ServiceFactory)? = null
    ) : this(posTerminalId) {
        this.forageSdk = forageSdk
        this.createLogger = createLogger
        if (createServiceFactory != null) {
            this.createServiceFactory = createServiceFactory
        }
    }

    /**
     * Securely tokenize a customer's card information
     * using a ForagePANEditText via UI-based PAN entry
     *
     * @param foragePanEditText A ForagePANEditText UI component. Importantly,
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
    suspend fun tokenizeCard(
        foragePanEditText: ForagePANEditText,
        reusable: Boolean = true
    ): ForageApiResponse<String> {
        val logger = createLogger()
        logger.i(
            "[POS] Tokenizing Payment Method via UI PAN entry",
            attributes = mapOf(
                "reusable" to reusable
            )
        )

        val tokenizationResponse = forageSdk.tokenizeEBTCard(
            TokenizeEBTCardParams(
                foragePanEditText = foragePanEditText,
                reusable = reusable
            )
        )
        if (tokenizationResponse is ForageApiResponse.Failure) {
            logger.e(
                "[POS] tokenizeCard failed on Terminal $posTerminalId: ${tokenizationResponse.errors[0]}",
                attributes = mapOf(
                    "pos_terminal_id" to posTerminalId
                )
            )
        }
        return tokenizationResponse
    }

    /**
     * Securely tokenizes a customer's card information
     * using Track 2 data from a magnetic card swipe.
     *
     * @param params The [PosTokenizeCardParams] parameters required for tokenization via magnetic card swipe.
     *
     * On success, returns a [PaymentMethod](https://docs.joinforage.app/reference/create-payment-method)
     * token which can be securely stored and used for subsequent transactions. On failure,
     * returns a detailed error response for proper handling.
     *
     * @return A [ForageAPIResponse][com.joinforage.forage.android.network.model.ForageApiResponse]
     * indicating the success or failure of the operation.
     */
    suspend fun tokenizeCard(params: PosTokenizeCardParams): ForageApiResponse<String> {
        val (forageConfig, track2Data, reusable) = params
        val logger = createLogger()
        logger.i(
            "[POS] Tokenizing Payment Method using magnetic card swipe with Track 2 data",
            attributes = mapOf(
                "merchant_ref" to forageConfig.merchantId,
                "reusable" to reusable
            )
        )

        val (merchantId, sessionToken) = forageConfig
        val serviceFactory = createServiceFactory(sessionToken, merchantId, logger)
        val tokenizeCardService = serviceFactory.createTokenizeCardService()

        return tokenizeCardService.tokenizePosCard(
            track2Data = track2Data,
            reusable = reusable
        )
    }

    /**
     * TODO: add comment here
     */
    suspend fun refundPayment(
        params: RefundPaymentParams
    ): ForageApiResponse<String> {
        return ForageApiResponse.Success("TODO")
    }

    /**
     * Checks the balance of a given PaymentMethod using a ForagePINEditText
     *
     * @param params The parameters required for balance inquiries, including
     * a reference to a ForagePINEditText and PaymentMethod ref
     *
     * @return A [ForageAPIResponse][com.joinforage.forage.android.network.model.ForageApiResponse]
     * indicating the success or failure of the operation.
     * On success, returns an object with `snap` (SNAP) and `cash` (EBT Cash) fields, whose values
     * indicate the current balance of each respective tender.
     *
     * @throws ForageConfigNotSetException If the passed ForagePINEditText instance
     * hasn't had its ForageConfig set via .setForageConfig().
     */
    override suspend fun checkBalance(
        params: CheckBalanceParams
    ): ForageApiResponse<String> {
        val logger = createLogger()
        val (foragePinEditText, paymentMethodRef) = params
        val (merchantId, sessionToken) = forageSdk._getForageConfigOrThrow(foragePinEditText)

        logger.i(
            "[POS] Called checkBalance for PaymentMethod $paymentMethodRef",
            attributes = mapOf(
                "merchant_ref" to merchantId,
                "payment_method_ref" to paymentMethodRef
            )
        )

        // This block is used for tracking Metrics!
        // ------------------------------------------------------
        val measurement = CustomerPerceivedResponseMonitor.newMeasurement(
            vault = foragePinEditText.getCollector(merchantId).getVaultType(),
            vaultAction = UserAction.BALANCE,
            logger
        )
        measurement.start()
        // ------------------------------------------------------

        val serviceFactory = createServiceFactory(sessionToken, merchantId, logger)
        val balanceCheckService = serviceFactory.createCheckBalanceRepository(foragePinEditText)
        val balanceResponse = balanceCheckService.posCheckBalance(
            paymentMethodRef = paymentMethodRef,
            posTerminalId = posTerminalId
        )
        forageSdk.processApiResponseForMetrics(balanceResponse, measurement)

        if (balanceResponse is ForageApiResponse.Failure) {
            logger.e(
                "[POS] checkBalance failed for PaymentMethod $paymentMethodRef on Terminal $posTerminalId: ${balanceResponse.errors[0]}",
                attributes = mapOf(
                    "payment_method_ref" to paymentMethodRef,
                    "pos_terminal_id" to posTerminalId
                )
            )
        }

        return balanceResponse
    }

    // ======= Same as online-only Forage SDK below =======

    /**
     * Captures a Forage Payment associated with an EBT card using a customer's EBT card PIN.
     *
     * @param params The parameters required for payment capture, including
     * reference to a ForagePINEditText and a Payment ref
     *
     * @return A [ForageAPIResponse][com.joinforage.forage.android.network.model.ForageApiResponse]
     * indicating the success or failure of the
     * payment capture. On success, returns a confirmation of the transaction.
     * On failure, provides a detailed error response.
     *
     * @throws ForageConfigNotSetException If the passed ForagePINEditText instance
     * hasn't had its ForageConfig set via .setForageConfig().
     */
    override suspend fun capturePayment(
        params: CapturePaymentParams
    ): ForageApiResponse<String> {
        val logger = createLogger()
        val (_, paymentRef) = params

        logger.i(
            "[POS] Called capturePayment for Payment $paymentRef",
            attributes = mapOf(
                "payment_method_ref" to paymentRef
            )
        )

        val captureResponse = forageSdk.capturePayment(params)

        if (captureResponse is ForageApiResponse.Failure) {
            logger.e(
                "[POS] capturePayment failed for payment $paymentRef on Terminal $posTerminalId: ${captureResponse.errors[0]}",
                attributes = mapOf(
                    "payment" to paymentRef,
                    "pos_terminal_id" to posTerminalId
                )
            )
        }
        return captureResponse
    }

    /**
     * Collect's a customer's PIN for an EBT payment and defers
     * the capture of the payment to the server.
     *
     * @param params The parameters required for deferring the capture of an EBT payment,
     * including a reference to a ForagePINEditText and a Payment ref
     *
     * @return A [ForageAPIResponse][com.joinforage.forage.android.network.model.ForageApiResponse]
     * indicating the success or failure of the
     * PIN capture. On success, returns `Nothing`.
     * On failure, provides a detailed error response.
     *
     * @throws ForageConfigNotSetException If the passed ForagePINEditText instance
     * hasn't had its ForageConfig set via .setForageConfig().
     */
    override suspend fun deferPaymentCapture(params: DeferPaymentCaptureParams): ForageApiResponse<String> {
        val logger = createLogger()
        val (_, paymentRef) = params
        logger.i(
            "[POS] Called deferPaymentCapture for Payment $paymentRef",
            attributes = mapOf(
                "payment_ref" to paymentRef
            )
        )

        val deferCaptureResponse = forageSdk.deferPaymentCapture(params)

        if (deferCaptureResponse is ForageApiResponse.Failure) {
            logger.e(
                "[POS] deferPaymentCapture failed for Payment $paymentRef on Terminal $posTerminalId: ${deferCaptureResponse.errors[0]}",
                attributes = mapOf(
                    "payment_ref" to paymentRef,
                    "pos_terminal_id" to posTerminalId
                )
            )
        }
        return deferCaptureResponse
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

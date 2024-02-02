package com.joinforage.forage.android.pos

import com.joinforage.forage.android.CapturePaymentParams
import com.joinforage.forage.android.CheckBalanceParams
import com.joinforage.forage.android.DeferPaymentCaptureParams
import com.joinforage.forage.android.ForageConfigNotSetException
import com.joinforage.forage.android.ForageSDK
import com.joinforage.forage.android.ForageSDKInterface
import com.joinforage.forage.android.TokenizeEBTCardParams
import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.core.telemetry.CustomerPerceivedResponseMonitor
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.core.telemetry.UserAction
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.ui.ForagePANEditText
import com.joinforage.forage.android.ui.ForagePINEditText

/**
 * ForageTerminalSDK is the entry point for **in-store POS Terminal** transactions.
 *
 * You need to initialize the SDK with a unique POS Terminal ID before you can perform operations like:
 *
 * * [Tokenizing card information][tokenizeCard]
 * * [Checking the balance of a card][checkBalance]
 * * [Capturing a payment][capturePayment]
 * * [Refunding a payment][refundPayment]
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
    private var createLogger: () -> Log = { Log.getInstance().addAttribute("pos_terminal_id", posTerminalId) }

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
        logger.addAttribute("reusable", reusable)
        logger.i("[POS] Tokenizing Payment Method via UI PAN entry on Terminal $posTerminalId")

        val tokenizationResponse = forageSdk.tokenizeEBTCard(
            TokenizeEBTCardParams(
                foragePanEditText = foragePanEditText,
                reusable = reusable
            )
        )
        if (tokenizationResponse is ForageApiResponse.Failure) {
            logger.e("[POS] tokenizeCard failed on Terminal $posTerminalId: ${tokenizationResponse.errors[0]}")
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
        val (posForageConfig, track2Data, reusable) = params
        val logger = createLogger()
        logger.addAttribute("reusable", reusable)
            .addAttribute("merchant_ref", posForageConfig.merchantId)

        logger.i("[POS] Tokenizing Payment Method using magnetic card swipe with Track 2 data on Terminal $posTerminalId")

        val (merchantId, sessionToken) = posForageConfig
        val serviceFactory = createServiceFactory(sessionToken, merchantId, logger)
        val tokenizeCardService = serviceFactory.createTokenizeCardService()

        return tokenizeCardService.tokenizePosCard(
            track2Data = track2Data,
            reusable = reusable
        )
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

        val illegalVaultException = isIllegalVaultExceptionOrNull(foragePinEditText, logger)
        if (illegalVaultException != null) {
            return illegalVaultException
        }

        val (merchantId, sessionToken) = forageSdk._getForageConfigOrThrow(foragePinEditText)

        logger.addAttribute("merchant_ref", merchantId)
            .addAttribute("payment_method_ref", paymentMethodRef)

        logger.i("[POS] Called checkBalance for PaymentMethod $paymentMethodRef on Terminal $posTerminalId")

        // This block is used for tracking Metrics!
        // ------------------------------------------------------
        val measurement = CustomerPerceivedResponseMonitor.newMeasurement(
            vault = foragePinEditText.getVaultType(),
            vaultAction = UserAction.BALANCE,
            logger
        )
        measurement.start()
        // ------------------------------------------------------

        val serviceFactory = createServiceFactory(sessionToken, merchantId, logger)
        val balanceCheckService = serviceFactory.createCheckBalanceRepository(foragePinEditText)
        val balanceResponse = balanceCheckService.posCheckBalance(
            merchantId = merchantId,
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
        val (_, paymentRef) = params

        val logger = createLogger().addAttribute("payment_ref", paymentRef)
        logger.i("[POS] Called capturePayment for Payment $paymentRef")

        val captureResponse = forageSdk.capturePayment(params)

        if (captureResponse is ForageApiResponse.Failure) {
            logger.e("[POS] capturePayment failed for payment $paymentRef on Terminal $posTerminalId: ${captureResponse.errors[0]}")
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
        val (_, paymentRef) = params

        val logger = createLogger().addAttribute("payment_ref", paymentRef)
        logger.i("[POS] Called deferPaymentCapture for Payment $paymentRef")

        val deferCaptureResponse = forageSdk.deferPaymentCapture(params)

        if (deferCaptureResponse is ForageApiResponse.Failure) {
            logger.e("[POS] deferPaymentCapture failed for Payment $paymentRef on Terminal $posTerminalId: ${deferCaptureResponse.errors[0]}")
        }
        return deferCaptureResponse
    }

    /**
     * Refund a Forage Payment using a ForagePINEditText

     * * On Success, the response includes a Forage
     * [`Refund`]https://docs.joinforage.app/reference/create-payment-refund) object.
     * * On failure, the response includes a list of
     * * [ForageError][com.joinforage.forage.android.network.model.ForageError] objects that you can
     * * unpack to troubleshoot the issue.
     *
     * @param params The [PosRefundPaymentParams] parameters required for refunding a Payment.
     * @return A [ForageAPIResponse][com.joinforage.forage.android.network.model.ForageApiResponse]
     * @throws ForageConfigNotSetException If the passed ForagePINEditText instance
     * hasn't had its ForageConfig set via .setForageConfig().
     */
    suspend fun refundPayment(params: PosRefundPaymentParams): ForageApiResponse<String> {
        val logger = createLogger()
        val (foragePinEditText, paymentRef, amount, reason) = params
        val (merchantId, sessionToken) = forageSdk._getForageConfigOrThrow(foragePinEditText)

        val illegalVaultException = isIllegalVaultExceptionOrNull(foragePinEditText, logger)
        if (illegalVaultException != null) {
            return illegalVaultException
        }

        logger
            .addAttribute("payment_ref", paymentRef)
            .addAttribute("merchant_ref", merchantId)
        logger.i(
            """
            [POS] Called refundPayment for Payment $paymentRef
            with amount: $amount
            for reason: $reason
            on Terminal: $posTerminalId
            """.trimIndent()
        )

        // This block is used for tracking Metrics!
        // ------------------------------------------------------
        val measurement = CustomerPerceivedResponseMonitor.newMeasurement(
            vault = foragePinEditText.getVaultType(),
            vaultAction = UserAction.REFUND,
            logger
        )
        measurement.start()
        // ------------------------------------------------------

        val serviceFactory = createServiceFactory(sessionToken, merchantId, logger)
        val refundService = serviceFactory.createRefundPaymentRepository(foragePinEditText)
        val refund = refundService.refundPayment(
            merchantId = merchantId,
            posTerminalId = posTerminalId,
            refundParams = params
        )
        forageSdk.processApiResponseForMetrics(refund, measurement)

        if (refund is ForageApiResponse.Failure) {
            logger.e("[POS] refundPayment failed for Payment $paymentRef on Terminal $posTerminalId: ${refund.errors[0]}")
        }

        return refund
    }

    private fun isIllegalVaultExceptionOrNull(
        foragePinEditText: ForagePINEditText,
        logger: Log
    ): ForageApiResponse<String>? {
        if (foragePinEditText.getVaultType() != VaultType.VGS_VAULT_TYPE) {
            logger.e("[POS] checkBalance failed on Terminal $posTerminalId because the vault type is not VGS")
            return ForageApiResponse.Failure.fromError(
                ForageError(
                    code = "invalid_input_data",
                    message = "IllegalStateException: Use ForageElement.setPosForageConfig, instead of ForageElement.setForageConfig.",
                    httpStatusCode = 400
                )
            )
        }
        return null
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

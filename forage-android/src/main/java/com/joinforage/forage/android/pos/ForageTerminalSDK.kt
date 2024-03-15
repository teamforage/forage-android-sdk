package com.joinforage.forage.android.pos

import android.os.Build
import androidx.annotation.RequiresApi
import com.joinforage.forage.android.CapturePaymentParams
import com.joinforage.forage.android.CheckBalanceParams
import com.joinforage.forage.android.DeferPaymentCaptureParams
import com.joinforage.forage.android.ForageConfigNotSetException
import com.joinforage.forage.android.ForageSDK
import com.joinforage.forage.android.ForageSDKInterface
import com.joinforage.forage.android.PosDeferPaymentRefundParams
import com.joinforage.forage.android.TokenizeEBTCardParams
import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.core.telemetry.CustomerPerceivedResponseMonitor
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.core.telemetry.UserAction
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.network.model.UnknownErrorApiResponse
import com.joinforage.forage.android.pos.encryption.KsnManager
import com.joinforage.forage.android.ui.ForagePANEditText
import com.joinforage.forage.android.ui.ForagePINEditText

/**
 * The entry point for **in-store POS Terminal** transactions.
 *
 * A [ForageTerminalSDK] instance interacts with the Forage API.
 * Provide a unique POS Terminal ID, the `posTerminalId` parameter, to perform operations like:
 *
 * * [Tokenizing card information][tokenizeCard]
 * * [Checking the balance of a card][checkBalance]
 * * [Collecting a card PIN for a payment and
 * deferring the capture of the payment to the server][deferPaymentCapture]
 * * [Capturing a payment immediately][capturePayment]
 * * [Collecting a customer's card PIN for a refund and defer the completion of the refund to the
 * server][deferPaymentRefund]
 * * [Refunding a payment immediately][refundPayment]
 *
 * @param posTerminalId **Required**. A string that uniquely identifies the POS Terminal
 * used for a transaction. The max length of the string is 255 characters.
 * @see * [Forage guide to Terminal POS integrations](https://docs.joinforage.app/docs/forage-terminal-android)
 * * [ForageSDK] to process online-only transactions
 */
class ForageTerminalSDK(
    private val posTerminalId: String
) : ForageSDKInterface {
    private var calledInit = false
    private var initSucceeded = false

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
        createServiceFactory: ((String, String, Log) -> ForageSDK.ServiceFactory)? = null,
        initSucceeded: Boolean = false
    ) : this(posTerminalId) {
        this.forageSdk = forageSdk
        this.createLogger = createLogger
        if (createServiceFactory != null) {
            this.createServiceFactory = createServiceFactory
        }

        if (initSucceeded) {
            // STOPGAP to allow testing without depending on the init method.
            this.initSucceeded = initSucceeded
            this.calledInit = initSucceeded
        }
    }

    /**
     * The [ForageTerminalSDK] may perform some long running initialization
     * operations in certain circumstances. The operations typically
     * last less than 10 seconds and only occur infrequently. It is
     * required to call [ForageTerminalSDK.init] ahead of calling any other methods on
     * a [ForageTerminalSDK] instance.
     *
     * @throws Exception
     *
     * @param merchantId A unique Merchant ID that Forage provides during onboarding
     *  * preceded by "mid/".
     *  * For example, `mid/123ab45c67`. The Merchant ID can be found in the Forage
     *  * [Sandbox](https://dashboard.sandbox.joinforage.app/login/)
     *  * or [Production](https://dashboard.joinforage.app/login/) Dashboard.
     *
     * @param sessionToken A short-lived token that authenticates front-end requests to Forage.
     *  * To create one, send a server-side `POST` request from your backend to the
     *  * [`/session_token/`](https://docs.joinforage.app/reference/create-session-token) endpoint.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun init(
        merchantId: String,
        sessionToken: String
    ): ForageTerminalSDK {
        calledInit = true
        val logger = createLogger()
        try {
            val logSuffix = getLogSuffix(merchantId)
            logger.addAttribute("merchant_ref", merchantId)
            logger.i("[POS] Executing ForageTerminalSDK.init() initialization sequence $logSuffix")

            val initializer = PosTerminalInitializer(
                logger = createLogger(),
                ksnManager = KsnManager.forJavaRuntime()
            )

            initializer.execute(
                merchantId = merchantId,
                sessionToken = sessionToken
            )
            initSucceeded = true

            logger.i("[POS] Initialized ForageTerminalSDK using the init() method $logSuffix")
        } catch (e: Exception) {
            logger.e("[POS] Failed to initialize ForageTerminalSDK using the init() method.", e)
            // TODO: to throw or not to throw, that is the question!
//            throw e
        }

        return this
    }

    /**
     * Tokenizes a card via a [ForagePANEdit
     * Text][com.joinforage.forage.android.ui.ForagePANEditText] Element.
     * * On success, the object includes a `ref` token that represents an instance of a Forage
     * [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods). You can store
     * the token in your database and reference it for future transactions, like to call
     * [checkBalance] or to [create a Payment](https://docs.joinforage.app/reference/create-a-payment)
     * in Forage's database.
     * * On failure, for example in the case of [`unsupported_bin`](https://docs.joinforage.app/reference/errors#unsupported_bin),
     * the response includes a list of [ForageError][com.joinforage.forage.android.network.model.ForageError]
     * objects that you can unpack to programmatically handle the error and display the appropriate
     * customer-facing message based on the `ForageError.code`.
     *
     * @param foragePanEditText **Required**. A reference to a [ForagePANEditText] instance that
     * collects the customer's card number.
     * [setPosForageConfig][com.joinforage.forage.android.ui.ForageElement.setPosForageConfig] must
     * have been called on the instance before it can be passed.
     * @param reusable Optional. A boolean that indicates whether the same card can be used to create
     * multiple payments. Defaults to true.
     * @throws ForageConfigNotSetException If the [PosForageConfig] is not set for the provided
     * [ForagePANEditText] instance.
     *
     * @return A [ForageApiResponse] object.
     */
    suspend fun tokenizeCard(
        foragePanEditText: ForagePANEditText,
        reusable: Boolean = true
    ): ForageApiResponse<String> {
        val logger = createLogger()
        logger.addAttribute("reusable", reusable)
        logger.i("[POS] Tokenizing Payment Method via UI PAN entry on Terminal $posTerminalId")

        val initializationException = isInitializationExceptionOrNull(logger, "tokenizeCard")
        if (initializationException != null) {
            return initializationException
        }

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
     * Tokenizes a card via a magnetic swipe from a physical POS Terminal.
     * * On success, the object includes a `ref` token that represents an instance of a Forage
     * [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods). You can store
     * the token for future transactions, like to call [checkBalance] or to
     * [create a Payment](https://docs.joinforage.app/reference/create-a-payment) in Forage's database.
     * * On failure, for example in the case of [`unsupported_bin`](https://docs.joinforage.app/reference/errors#unsupported_bin),
     * the response includes a list of [ForageError][com.joinforage.forage.android.network.model.ForageError]
     * objects that you can unpack to programmatically handle the error and display the appropriate
     *      * customer-facing message based on the `ForageError.code`.
     *
     * @param params **Required**. A [PosTokenizeCardParams] model that passes the [PosForageConfig], the card's
     * `track2Data`, and a `reusable` boolean that Forage uses to tokenize the card.
     *
     * @throws ForageConfigNotSetException If the [PosForageConfig] is not set for the provided
     * [ForagePANEditText] instance.
     *
     * @return A [ForageAPIResponse][com.joinforage.forage.android.network.model.ForageApiResponse]
     * object.
     */
    suspend fun tokenizeCard(params: PosTokenizeCardParams): ForageApiResponse<String> {
        val (posForageConfig, track2Data, reusable) = params
        val logger = createLogger()
        logger.addAttribute("reusable", reusable)
            .addAttribute("merchant_ref", posForageConfig.merchantId)

        logger.i("[POS] Tokenizing Payment Method using magnetic card swipe with Track 2 data on Terminal $posTerminalId")

        val initializationException = isInitializationExceptionOrNull(logger, "tokenizeCard")
        if (initializationException != null) {
            return initializationException
        }

        val (merchantId, sessionToken) = posForageConfig
        val serviceFactory = createServiceFactory(sessionToken, merchantId, logger)
        val tokenizeCardService = serviceFactory.createTokenizeCardService()

        return tokenizeCardService.tokenizePosCard(
            track2Data = track2Data,
            reusable = reusable
        )
    }

    /**
     * Checks the balance of a previously created
     * [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods)
     * via a [ForagePINEditText][com.joinforage.forage.android.ui.ForagePINEditText] Element.
     *
     * ⚠️ _FNS prohibits balance inquiries on sites and apps that offer guest checkout. Skip this
     * method if your customers can opt for guest checkout. If guest checkout is not an option, then
     * it's up to you whether or not to add a balance inquiry feature. No FNS regulations apply._
     * * On success, the response object includes `snap` and `cash` fields that indicate
     * the EBT Card's current SNAP and EBT Cash balances.
     * * On failure, for example in the case of
     * [`ebt_error_14`](https://docs.joinforage.app/reference/errors#ebt_error_14),
     * the response includes a list of
     * [ForageError][com.joinforage.forage.android.network.model.ForageError] objects that you can
     * unpack to programmatically handle the error and display the appropriate
     * customer-facing message based on the `ForageError.code`.
     * @param params A [CheckBalanceParams] model that passes
     * a [`foragePinEditText`][com.joinforage.forage.android.ui.ForagePINEditText] instance and a
     * `paymentMethodRef`, found in the response from a call to [tokenizeEBTCard] or the
     * [Create a `PaymentMethod`](https://docs.joinforage.app/reference/create-payment-method)
     * endpoint, that Forage uses to check the payment method's balance.
     *
     * @throws [ForageConfigNotSetException] If the [PosForageConfig] is not set for the provided
     * `foragePinEditText`.
     * @see * [SDK errors](https://docs.joinforage.app/reference/errors#sdk-errors) for more
     * information on error handling.
     * * [Test EBT Cards](https://docs.joinforage.app/docs/test-ebt-cards#balance-inquiry-exceptions)
     * to trigger balance inquiry exceptions during testing.
     * @return A [ForageApiResponse] object.
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

        val initializationException = isInitializationExceptionOrNull(logger, "checkBalance")
        if (initializationException != null) {
            return initializationException
        }

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
            posTerminalId = posTerminalId,
            sessionToken = sessionToken
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

    /**
     * Immediately captures a payment via a
     * [ForagePINEditText][com.joinforage.forage.android.ui.ForagePINEditText] Element.
     *
     * * On success, the object confirms the transaction. The response includes a Forage
     * [`Payment`](https://docs.joinforage.app/reference/payments) object.
     * * On failure, for example in the case of
     * [`card_not_reusable`](https://docs.joinforage.app/reference/errors#card_not_reusable) or
     * [`ebt_error_51`](https://docs.joinforage.app/reference/errors#ebt_error_51) errors, the
     * response includes a list of
     * [ForageError][com.joinforage.forage.android.network.model.ForageError] objects that you can
     * unpack to programmatically handle the error and display the appropriate
     * customer-facing message based on the `ForageError.code`.
     *
     * @param params A [CapturePaymentParams] model that passes a
     * [`foragePinEditText`][com.joinforage.forage.android.ui.ForagePINEditText]
     * instance and a `paymentRef`, returned by the
     * [Create a Payment](https://docs.joinforage.app/reference/create-a-payment) endpoint, that
     * Forage uses to capture a payment.
     *
     * @throws [ForageConfigNotSetException] If the [PosForageConfig] is not set for the provided
     * `foragePinEditText`.
     * @see
     * * [SDK errors](https://docs.joinforage.app/reference/errors#sdk-errors) for more information
     * on error handling.
     * * [Test EBT Cards](https://docs.joinforage.app/docs/test-ebt-cards#payment-capture-exceptions)
     * to trigger payment capture exceptions during testing.
     * @return A [ForageApiResponse] object.
     */
    override suspend fun capturePayment(
        params: CapturePaymentParams
    ): ForageApiResponse<String> {
        val (foragePinEditText, paymentRef) = params
        val logger = createLogger().addAttribute("payment_ref", paymentRef)
        logger.i("[POS] Called capturePayment for Payment $paymentRef")

        val illegalVaultException = isIllegalVaultExceptionOrNull(foragePinEditText, logger)
        if (illegalVaultException != null) {
            return illegalVaultException
        }
        val initializationException = isInitializationExceptionOrNull(logger, "capturePayment")
        if (initializationException != null) {
            return initializationException
        }

        val captureResponse = forageSdk.capturePayment(params)

        if (captureResponse is ForageApiResponse.Failure) {
            logger.e("[POS] capturePayment failed for payment $paymentRef on Terminal $posTerminalId: ${captureResponse.errors[0]}")
        }
        return captureResponse
    }

    /**
     * Submits a card PIN via a
     * [ForagePINEditText][com.joinforage.forage.android.ui.ForagePINEditText] Element and defers
     * payment capture to the server.
     *
     * * On success, the `data` property of the [ForageApiResponse.Success] object resolves with an empty string.
     * * On failure, for example in the case of [`expired_session_token`](https://docs.joinforage.app/reference/errors#expired_session_token) errors, the
     * response includes a list of
     * [ForageError][com.joinforage.forage.android.network.model.ForageError] objects that you can
     * unpack to programmatically handle the error and display the appropriate
     * customer-facing message based on the `ForageError.code`.
     *
     * @param params A [DeferPaymentCaptureParams] model that passes a
     * [`foragePinEditText`][com.joinforage.forage.android.ui.ForagePINEditText] instance and a
     * `paymentRef`, returned by the
     * [Create a Payment](https://docs.joinforage.app/reference/create-a-payment) endpoint, as the
     * DeferPaymentCaptureParams.
     *
     * @throws [ForageConfigNotSetException] If the [PosForageConfig] is not set for the provided
     * `foragePinEditText`.
     * @see * [Defer EBT payment capture and refund completion to the server](https://docs.joinforage.app/docs/capture-ebt-payments-server-side)
     * for the related step-by-step guide.
     * * [Capture an EBT Payment](https://docs.joinforage.app/reference/capture-a-payment)
     * for the API endpoint to call after [deferPaymentCapture].
     * * [SDK errors](https://docs.joinforage.app/reference/errors#sdk-errors) for more information
     * on error handling.
     * @return A [ForageApiResponse] object.
     */
    override suspend fun deferPaymentCapture(params: DeferPaymentCaptureParams): ForageApiResponse<String> {
        val (foragePinEditText, paymentRef) = params
        val logger = createLogger().addAttribute("payment_ref", paymentRef)
        logger.i("[POS] Called deferPaymentCapture for Payment $paymentRef")

        val illegalVaultException = isIllegalVaultExceptionOrNull(foragePinEditText, logger)
        if (illegalVaultException != null) {
            return illegalVaultException
        }

        val deferCaptureResponse = forageSdk.deferPaymentCapture(params)

        if (deferCaptureResponse is ForageApiResponse.Failure) {
            logger.e("[POS] deferPaymentCapture failed for Payment $paymentRef on Terminal $posTerminalId: ${deferCaptureResponse.errors[0]}")
        }
        return deferCaptureResponse
    }

    /**
     * Refunds a Payment via a [ForagePinEditText][com.joinforage.forage.android.ui.ForagePINEditText]
     * Element. This method is only available for POS Terminal transactions.
     * You must use [ForageTerminalSDK].
     *
     * * On success, the response includes a Forage
     * [`PaymentRefund`](https://docs.joinforage.app/reference/create-payment-refund) object.
     * * On failure, for example in the case of
     * [`ebt_error_61`](https://docs.joinforage.app/reference/errors#ebt_error_61), the response
     * includes a list of [ForageError] objects. You can unpack the list to programmatically handle
     * the error and display the appropriate customer-facing message based on the `ForageError.code`.
     *
     * @param params A [PosRefundPaymentParams] model that passes a
     * [`foragePinEditText`][com.joinforage.forage.android.ui.ForagePINEditText] instance, a
     * `paymentRef`, returned by the [Create a Payment](https://docs.joinforage.app/reference/create-a-payment)
     * endpoint, an `amount`, and a `reason` as the PosRefundPaymentParams.
     * @throws ForageConfigNotSetException If the [PosForageConfig] is not set for the provided
     * `foragePinEditText`.
     * @see * [SDK errors](https://docs.joinforage.app/reference/errors#sdk-errors) for more information
     * on error handling.
     * @return A [ForageApiResponse] object.
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

        val initializationException = isInitializationExceptionOrNull(logger, "refundPayment")
        if (initializationException != null) {
            return initializationException
        }

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
            refundParams = params,
            sessionToken = sessionToken
        )
        forageSdk.processApiResponseForMetrics(refund, measurement)

        if (refund is ForageApiResponse.Failure) {
            logger.e("[POS] refundPayment failed for Payment $paymentRef on Terminal $posTerminalId: ${refund.errors[0]}")
        }

        return refund
    }

    /**
     * Collects a card PIN for an EBT payment and defers
     * the refund of the payment to the server.
     *
     * @param params The [PosRefundPaymentParams] parameters required for refunding a Payment.
     * @return A [ForageAPIResponse][com.joinforage.forage.android.network.model.ForageApiResponse]
     * indicating the success or failure of the
     * PIN capture. On success, returns `Nothing`.
     * On failure, the response includes a list of
     * [ForageError][com.joinforage.forage.android.network.model.ForageError] objects that you can
     * unpack to troubleshoot the issue.
     * @see * [Defer EBT payment capture and refund completion to the server](https://docs.joinforage.app/docs/capture-ebt-payments-server-side)
     * for the related step-by-step guide.
     * @throws ForageConfigNotSetException If the passed ForagePINEditText instance
     * hasn't had its ForageConfig set via .setForageConfig().
     */
    suspend fun deferPaymentRefund(params: PosDeferPaymentRefundParams): ForageApiResponse<String> {
        val logger = createLogger()
        val (foragePinEditText, paymentRef) = params
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
            [POS] Called deferPaymentRefund for Payment $paymentRef
            on Terminal: $posTerminalId
            """.trimIndent()
        )

        val initializationException = isInitializationExceptionOrNull(logger, "deferPaymentRefund")
        if (initializationException != null) {
            return initializationException
        }

        // This block is used for tracking Metrics!
        // ------------------------------------------------------
        val measurement = CustomerPerceivedResponseMonitor.newMeasurement(
            vault = foragePinEditText.getVaultType(),
            vaultAction = UserAction.DEFER_REFUND,
            logger
        )
        measurement.start()
        // ------------------------------------------------------

        val serviceFactory = createServiceFactory(sessionToken, merchantId, logger)
        val refundService = serviceFactory.createDeferPaymentRefundRepository(foragePinEditText)
        val refund = refundService.deferPaymentRefund(
            merchantId = merchantId,
            paymentRef = paymentRef,
            sessionToken = sessionToken
        )
        forageSdk.processApiResponseForMetrics(refund, measurement)

        if (refund is ForageApiResponse.Failure) {
            logger.e("[POS] deferPaymentRefund failed for Payment $paymentRef on Terminal $posTerminalId: ${refund.errors[0]}")
        }

        return refund
    }

    private fun isIllegalVaultExceptionOrNull(
        foragePinEditText: ForagePINEditText,
        logger: Log
    ): ForageApiResponse<String>? {
        if (foragePinEditText.getVaultType() != VaultType.FORAGE_VAULT_TYPE) {
            logger.e("[POS] checkBalance failed on Terminal $posTerminalId because the vault type is not forage")
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

    private fun isInitializationExceptionOrNull(logger: Log, methodName: String): ForageApiResponse<String>? {
        if (!calledInit) {
            val errorMessage = """
                    This instance of `ForageTerminalSDK` has not been pre-initialized 
                    and will run the initialization process now instead. Consider 
                    calling `.init(config: PosForageConfig)` ahead of calling $methodName
                    to ensure any long-running init operations are completed beforehand.
            """.trimIndent()

            throw IllegalStateException(errorMessage)
        }

        if (!initSucceeded) {
            logger.e("[POS] $methodName failed because the ForageTerminalSDK init method failed.")
            return UnknownErrorApiResponse
        }
        return null
    }

    private fun getLogSuffix(merchantId: String): String = "on Terminal $posTerminalId for Merchant $merchantId"

    /**
     * Use one of the [tokenizeCard] options instead.
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

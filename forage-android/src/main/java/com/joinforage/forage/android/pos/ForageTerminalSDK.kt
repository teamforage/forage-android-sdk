package com.joinforage.forage.android.pos.services

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.ForageConfigNotSetException
import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeyService
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.ForageError
import com.joinforage.forage.android.core.services.forageapi.network.OkHttpClientBuilder
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.core.services.forageapi.payment.PaymentService
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.core.services.forageapi.polling.MessageStatusService
import com.joinforage.forage.android.core.services.forageapi.polling.PollingService
import com.joinforage.forage.android.core.services.telemetry.CustomerPerceivedResponseMonitor
import com.joinforage.forage.android.core.services.telemetry.EventOutcome
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.vault.CapturePaymentRepository
import com.joinforage.forage.android.core.services.vault.DeferPaymentCaptureRepository
import com.joinforage.forage.android.core.services.vault.TokenizeCardService
import com.joinforage.forage.android.core.ui.element.ForageConfig
import com.joinforage.forage.android.core.ui.element.ForagePanElement
import com.joinforage.forage.android.core.ui.element.ForagePinElement
import com.joinforage.forage.android.pos.services.vault.rosetta.PosTerminalInitializer
import com.joinforage.forage.android.pos.services.encryption.storage.KsnFileManager
import com.joinforage.forage.android.pos.services.forageapi.paymentmethod.PosDeferPaymentRefundParams
import com.joinforage.forage.android.pos.services.forageapi.paymentmethod.PosForageConfig
import com.joinforage.forage.android.pos.services.forageapi.paymentmethod.PosRefundPaymentParams
import com.joinforage.forage.android.pos.services.forageapi.refund.PosRefundPaymentRepository
import com.joinforage.forage.android.pos.services.forageapi.paymentmethod.PosTokenizeCardParams
import com.joinforage.forage.android.pos.services.forageapi.refund.PosRefundService
import com.joinforage.forage.android.pos.services.vault.DeferPaymentRefundRepository
import com.joinforage.forage.android.pos.ui.element.ForagePANEditText
import com.joinforage.forage.android.pos.services.vault.rosetta.ForagePinSubmitter
import com.joinforage.forage.android.pos.services.vault.PosCheckBalanceRepository
import com.joinforage.forage.android.pos.services.vault.PosTokenizeCardService

/**
 * The entry point for **in-store POS Terminal** transactions.
 *
 * A [ForageTerminalSDK] instance interacts with the Forage API.
 *
 * **You need to call [`ForageTerminalSDK.init`][init] to initialize the SDK.**
 * Then you can perform operations like:
 *
 * <br><br>
 *
 * **You need to call [`ForageTerminalSDK.init`][init] to initialize the SDK.**
 * Then you can perform operations like:
 * <br><br>
 * * [Tokenizing card information][tokenizeCard]
 * * [Checking the balance of a card][checkBalance]
 * * [Collecting a card PIN for a payment and
 * deferring the capture of the payment to the server][deferPaymentCapture]
 * * [Capturing a payment immediately][capturePayment]
 * * [Collecting a customer's card PIN for a refund and defer the completion of the refund to the
 * server][deferPaymentRefund]
 * * [Refunding a payment immediately][refundPayment]
 * <br><br>
 *
 *```kotlin
 * // Example: Initialize the Forage Terminal SDK
 * val forageTerminalSdk = ForageTerminalSDK.init(
 *     context = androidContext,
 *     posTerminalId = "<id-that-uniquely-identifies-the-pos-terminal>",
 *     posForageConfig = PosForageConfig(
 *         merchantId = "mid/123ab45c67",
 *         sessionToken = "sandbox_ey123..."
 *     )
 * )
 * ```
 *
 * @see * [Forage guide to Terminal POS integrations](https://docs.joinforage.app/docs/forage-terminal-android)
 * * [ForageSDK] to process online-only transactions
 */
class ForageTerminalSDK internal constructor(
    private val posTerminalId: String,
    private val forageConfig: ForageConfig,
) {

    companion object {
        private var calledInit = false
        private var initSucceeded = false

        /**
         * A method that initializes the [ForageTerminalSDK].
         *
         * **You must call [init] ahead of calling
         * any other methods on a ForageTerminalSDK instance.**
         *
         * Forage may perform some long running initialization operations in
         * certain circumstances. The operations typically last less than 10 seconds and only occur
         * infrequently.
         *
         * ⚠️The [ForageTerminalSDK.init] method is only available in the private
         * distribution of the Forage Terminal SDK.
         *
         *```kotlin
         * // Example: Initialize the Forage Terminal SDK
         * try {
         *     val forageTerminalSdk = ForageTerminalSDK.init(
         *         context = androidContext,
         *         posTerminalId = "<id-that-uniquely-identifies-the-pos-terminal>",
         *         posForageConfig = PosForageConfig(
         *             merchantId = "mid/123ab45c67",
         *             sessionToken = "sandbox_ey123..."
         *         )
         *     )
         *
         *     // Use the forageTerminalSdk to call other methods
         *     // (e.g. tokenizeCard, checkBalance, etc.)
         * } catch (e: Exception) {
         *     // handle initialization error
         * }
         * ```
         *
         * @throws Exception If the initialization fails.
         *
         * @param context **Required**. The Android application context.
         * @param posTerminalId **Required**. A string that uniquely identifies the POS Terminal
         * used for a transaction. The max length of the string is 255 characters.
         * @param posForageConfig **Required**. A [PosForageConfig] instance that specifies a
         * `merchantId` and `sessionToken`.
         */
        @RequiresApi(Build.VERSION_CODES.M)
        @Throws(Exception::class)
        suspend fun init(
            context: Context,
            posTerminalId: String,
            forageConfig: ForageConfig
        ): ForageTerminalSDK {
            calledInit = true
            val logger = createLogger(posTerminalId)
            val (merchantId, sessionToken) = forageConfig

            try {
                val logSuffix = getLogSuffix(posTerminalId, merchantId)
                logger.addAttribute("merchant_ref", merchantId)
                logger.i("[POS] Executing ForageTerminalSDK.init() initialization sequence $logSuffix")

                val ksnFileManager = KsnFileManager.byFile(context)
                // STOPGAP to feed `context` to ForagePinSubmitter
                ForagePinSubmitter.ksnFileManager = ksnFileManager

                val initializer = PosTerminalInitializer(
                    logger = logger,
                    ksnManager = ksnFileManager
                )

                initializer.execute(
                    posTerminalId = posTerminalId,
                    merchantId = merchantId,
                    sessionToken = sessionToken
                )
                initSucceeded = true

                logger.i("[POS] Initialized ForageTerminalSDK using the init() method $logSuffix")
            } catch (e: Exception) {
                // clear file!

                logger.e("[POS] Failed to initialize ForageTerminalSDK using the init() method.", e)
                throw Exception("Failed to initialize the ForageTerminalSDK")
            }

            return ForageTerminalSDK(posTerminalId, forageConfig)
        }

        private var createLogger: (posTerminalId: String) -> Log = { posTerminalId ->
            Log.getInstance().addAttribute("pos_terminal_id", posTerminalId)
        }

        private fun getLogSuffix(
            posTerminalId: String,
            merchantId: String
        ): String = "on Terminal $posTerminalId for Merchant $merchantId"
    }

    // internal constructor facilitates testing
    internal constructor(
        posTerminalId: String,
        forageConfig: ForageConfig,
        createLogger: (String) -> Log,
        initSucceeded: Boolean = false
    ) : this(posTerminalId, forageConfig) {
        Companion.createLogger = createLogger
        if (initSucceeded) {
            // STOPGAP to allow testing without depending on the init method.
            Companion.initSucceeded = initSucceeded
            calledInit = initSucceeded
        }
    }

    /**
     * Tokenizes a card via a [ForagePANEdit Text]
     * [com.joinforage.forage.android.ui.ForagePANEditText] Element.
     * * On success, the object includes a `ref` token that represents an instance of a Forage
     * [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods). You can store
     * the token in your database and reference it for future transactions, like to call
     * [checkBalance] or to [create a Payment](https://docs.joinforage.app/reference/create-a-payment)
     * in Forage's database. *(Example [PosPaymentMethod](https://github.com/teamforage/forage-android-sdk/blob/229a0c7d38dcae751070aed45ff2f7e7ea2a5abb/sample-app/src/main/java/com/joinforage/android/example/ui/pos/data/tokenize/PosPaymentMethod.kt#L7) class)*
     * * On failure, for example in the case of [`unsupported_bin`](https://docs.joinforage.app/reference/errors#unsupported_bin),
     * the response includes a list of [ForageError][com.joinforage.forage.android.network.model.ForageError]
     * objects that you can unpack to programmatically handle the error and display the appropriate
     * customer-facing message based on the `ForageError.code`.
     * ```kotlin
     * // Example tokenizeCard call in a TokenizeViewModel.kt
     * class TokenizeViewMode : ViewModel() {
     *     val merchantId = "mid/<merchant_id>"
     *     val sessionToken = "<session_token>"
     *
     *     fun tokenizeCard(foragePanEditText: ForagePANEditText) = viewModelScope.launch {
     *         val response = forageTerminalSdk.tokenizeCard(
     *           foragePanEditText = foragePanEditText,
     *           reusable = true
     *         )
     *
     *         when (response) {
     *             is ForageApiResponse.Success -> {
     *                 // parse response.data for the PaymentMethod object
     *             }
     *             is ForageApiResponse.Failure -> {
     *                 // do something with error text (i.e. response.message)
     *             }
     *         }
     *     }
     * }
     * ```
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
        val logger = createLogger(posTerminalId)
        logger.addAttribute("reusable", reusable)
        logger.i("[POS] Tokenizing Payment Method via UI PAN entry on Terminal $posTerminalId")

        val initializationException = isInitializationExceptionOrNull(logger, "tokenizeCard")
        if (initializationException != null) {
            return initializationException
        }

        // ====== Begin Duplicate Code ======

        val (merchantId, sessionToken) = getForageConfigOrThrow(foragePanEditText.getForageConfig())
        val config = EnvConfig.fromSessionToken(sessionToken)

        // TODO: replace Log.getInstance() with Log() in future PR
        logger.addAttribute("merchant_ref", merchantId)
        logger.i("[POS] Tokenizing Payment Method")

        val okHttpClient =  OkHttpClientBuilder.provideOkHttpClient(
            sessionToken = sessionToken,
            merchantId = merchantId,
            traceId = logger.getTraceIdValue()
        )

        // ====== End Duplicate Code ======

        val tokenizationResponse = TokenizeCardService(
            config.apiBaseUrl,
            okHttpClient,
            logger
        ).tokenizeCard(
            cardNumber = foragePanEditText.getPanNumber(),
            reusable = reusable ?: true
        )

        if (tokenizationResponse is ForageApiResponse.Failure) {
            logger.e(
                "[POS] tokenizeCard failed on Terminal $posTerminalId: ${tokenizationResponse.errors[0]}"
            )
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
     * customer-facing message based on the `ForageError.code`.
     * ```kotlin
     * // Example tokenizeCard(PosTokenizeCardParams) call in a TokenizePosViewModel.kt
     * class TokenizePosViewModel : ViewModel() {
     *     val merchantId = "mid/<merchant_id>"
     *     val sessionToken = "<session_token>"
     *
     *     fun tokenizePosCard(foragePinEditText: ForagePINEditText) = viewModelScope.launch {
     *         val response = forageTerminalSdk.tokenizeCard(
     *           PosTokenizeCardParams(
     *             forageConfig = ForageConfig(
     *               merchantId = merchantId,
     *               sessionToken = sessionToken
     *             ),
     *             track2Data = "<read_track_2_data>" // "123456789123456789=123456789123",
     *           // reusable = true
     *           )
     *         )
     *
     *         when (response) {
     *             is ForageApiResponse.Success -> {
     *                 // parse response.data for the PaymentMethod object
     *             }
     *             is ForageApiResponse.Failure -> {
     *                 // do something with error text (i.e. response.message)
     *             }
     *         }
     *     }
     * }
     * ```
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
        val logger = createLogger(posTerminalId)
        logger.addAttribute("reusable", reusable)
            .addAttribute("merchant_ref", posForageConfig.merchantId)

        logger.i(
            "[POS] Tokenizing Payment Method using magnetic card swipe with Track 2 data on Terminal $posTerminalId"
        )

        val initializationException = isInitializationExceptionOrNull(logger, "tokenizeCard")
        if (initializationException != null) {
            return initializationException
        }

        val (merchantId, sessionToken) = posForageConfig

        // ====== Begin Duplicate Code ======

        val config = EnvConfig.fromSessionToken(sessionToken)

        // TODO: replace Log.getInstance() with Log() in future PR
        logger.addAttribute("merchant_ref", merchantId)
        logger.i("[POS] Tokenizing Payment Method")

        val okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
            sessionToken = sessionToken,
            merchantId = merchantId,
            traceId = logger.getTraceIdValue()
        )

        // ====== End Duplicate Code ======

        return PosTokenizeCardService(
            config.apiBaseUrl,
            okHttpClient,
            logger
        ).tokenizePosCard(track2Data = track2Data, reusable = reusable)
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
     * the EBT Card's current SNAP and EBT Cash balances. *(Example [BalanceCheck](https://github.com/Forage-PCI-CDE/android-pos-terminal-sdk/blob/main/sample-app/src/main/java/com/joinforage/android/example/ui/pos/data/BalanceCheck.kt) class)*
     * * On failure, for example in the case of
     * [`ebt_error_14`](https://docs.joinforage.app/reference/errors#ebt_error_14),
     * the response includes a list of
     * [ForageError][com.joinforage.forage.android.network.model.ForageError] objects that you can
     * unpack to programmatically handle the error and display the appropriate
     * customer-facing message based on the `ForageError.code`.
     * ```kotlin
     * // Example checkBalance call in a BalanceCheckViewModel.kt
     * class BalanceCheckViewModel : ViewModel() {
     *     val paymentMethodRef = "020xlaldfh"
     *
     *     fun checkBalance(foragePinEditText: ForagePINEditText) = viewModelScope.launch {
     *         val response = forageTerminalSdk.checkBalance(
     *             CheckBalanceParams(
     *                 foragePinEditText = foragePinEditText,
     *                 paymentMethodRef = paymentMethodRef
     *             )
     *         )
     *
     *         when (response) {
     *             is ForageApiResponse.Success -> {
     *                 // response.data will have a .snap and a .cash value
     *             }
     *             is ForageApiResponse.Failure -> {
     *                 // do something with error text (i.e. response.message)
     *             }
     *         }
     *     }
     * }
     * ```
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
    suspend fun checkBalance(
        params: CheckBalanceParams
    ): ForageApiResponse<String> {
        val logger = createLogger(posTerminalId)
        val (foragePinEditText, paymentMethodRef) = params

        logger.addAttribute("merchant_ref", forageConfig.merchantId)
            .addAttribute("payment_method_ref", paymentMethodRef)

        logger.i(
            "[POS] Called checkBalance for PaymentMethod $paymentMethodRef on Terminal $posTerminalId"
        )

        val initializationException = isInitializationExceptionOrNull(logger, "checkBalance")
        if (initializationException != null) {
            return initializationException
        }

        // This block is used for tracking Metrics!
        // ------------------------------------------------------
        val measurement =
            CustomerPerceivedResponseMonitor.newMeasurement(
                vault = foragePinEditText.getVaultType(),
                vaultAction = UserAction.BALANCE,
                logger
            )
        measurement.start()
        // ------------------------------------------------------
        val config = forageConfig.envConfig
        val okHttpClient =  OkHttpClientBuilder.provideOkHttpClient(
            sessionToken = forageConfig.sessionToken,
            merchantId = forageConfig.merchantId,
            traceId = logger.getTraceIdValue()
        )
        val balanceResponse =
            PosCheckBalanceRepository(
                vaultSubmitter = foragePinEditText.vault.getVaultSubmitter(config, logger),
                encryptionKeyService = EncryptionKeyService(config.apiBaseUrl, okHttpClient, logger),
                paymentMethodService = PaymentMethodService(config.apiBaseUrl, okHttpClient, logger),
                pollingService = PollingService(
                    messageStatusService = MessageStatusService(config.apiBaseUrl, okHttpClient, logger),
                    logger = logger
                ),
                logger = logger
            ).posCheckBalance(
                merchantId = forageConfig.merchantId,
                paymentMethodRef = paymentMethodRef,
                posTerminalId = posTerminalId,
                sessionToken = forageConfig.sessionToken
            )
        processApiResponseForMetrics(balanceResponse, measurement)

        if (balanceResponse is ForageApiResponse.Failure) {
            logger.e(
                "[POS] checkBalance failed for PaymentMethod $paymentMethodRef on Terminal $posTerminalId: ${balanceResponse.errors[0]}",
                attributes =
                mapOf(
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
     * [`Payment`](https://docs.joinforage.app/reference/payments) object. *(Example [PosPaymentResponse](https://github.com/Forage-PCI-CDE/android-pos-terminal-sdk/blob/main/sample-app/src/main/java/com/joinforage/android/example/ui/pos/data/PosPaymentResponse.kt#L8) and [Receipt](https://github.com/Forage-PCI-CDE/android-pos-terminal-sdk/blob/main/sample-app/src/main/java/com/joinforage/android/example/ui/pos/data/PosReceipt.kt) class)*
     * * On failure, for example in the case of
     * [`card_not_reusable`](https://docs.joinforage.app/reference/errors#card_not_reusable) or
     * [`ebt_error_51`](https://docs.joinforage.app/reference/errors#ebt_error_51) errors, the
     * response includes a list of
     * [ForageError][com.joinforage.forage.android.network.model.ForageError] objects that you can
     * unpack to programmatically handle the error and display the appropriate
     * customer-facing message based on the `ForageError.code`.
     * ```kotlin
     * // Example capturePayment call in a PaymentCaptureViewModel.kt
     * class PaymentCaptureViewModel : ViewModel() {
     *     val snapPaymentRef = "s0alzle0fal"
     *     val merchantId = "mid/<merchant_id>"
     *     val sessionToken = "<session_token>"
     *
     *     fun capturePayment(foragePinEditText: ForagePINEditText, paymentRef: String) =
     *         viewModelScope.launch {
     *             val response = forageTerminalSdk.capturePayment(
     *                 CapturePaymentParams(
     *                     foragePinEditText = foragePinEditText,
     *                     paymentRef = snapPaymentRef
     *                 )
     *             )
     *
     *             when (response) {
     *                 is ForageApiResponse.Success -> {
     *                     // handle successful capture
     *                 }
     *                 is ForageApiResponse.Failure -> {
     *                     val error = response.errors[0]
     *
     *                     // handle Insufficient Funds error
     *                     if (error.code == "ebt_error_51") {
     *                         val details = error.details as ForageErrorDetails.EbtError51Details
     *                         val (snapBalance, cashBalance) = details
     *
     *                         // do something with balances ...
     *                     }
     *                 }
     *             }
     *         }
     * }
     *```
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
    suspend fun capturePayment(params: CapturePaymentParams): ForageApiResponse<String> {
        val (foragePinEditText, paymentRef) = params
        val logger = createLogger(posTerminalId).addAttribute("payment_ref", paymentRef)
        logger.i("[POS] Called capturePayment for Payment $paymentRef")

        val initializationException = isInitializationExceptionOrNull(logger, "capturePayment")
        if (initializationException != null) {
            return initializationException
        }

        // TODO: replace Log.getInstance() with Log() in future PR
        logger
            .addAttribute("merchant_ref", forageConfig.merchantId)
            .addAttribute("payment_ref", paymentRef)

        // This block is used for Metrics Tracking!
        // ------------------------------------------------------
        val measurement = CustomerPerceivedResponseMonitor.newMeasurement(
            vault = foragePinEditText.getVaultType(),
            vaultAction = UserAction.CAPTURE,
            logger
        )
        measurement.start()
        // ------------------------------------------------------
        val config = forageConfig.envConfig
        val okHttpClient =  OkHttpClientBuilder.provideOkHttpClient(
            sessionToken = forageConfig.sessionToken,
            merchantId = forageConfig.merchantId,
            traceId = logger.getTraceIdValue()
        )
        val captureResponse = CapturePaymentRepository(
            vaultSubmitter = foragePinEditText.vault.getVaultSubmitter(config, logger),
            encryptionKeyService = EncryptionKeyService(config.apiBaseUrl, okHttpClient, logger),
            paymentMethodService = PaymentMethodService(config.apiBaseUrl, okHttpClient, logger),
            paymentService = PaymentService(config.apiBaseUrl, okHttpClient, logger),
            pollingService = PollingService(
                messageStatusService = MessageStatusService(config.apiBaseUrl, okHttpClient, logger),
                logger = logger
            ),
            logger = logger
        ).capturePayment(
            merchantId = forageConfig.merchantId,
            paymentRef = paymentRef,
            sessionToken = forageConfig.sessionToken
        )

        processApiResponseForMetrics(captureResponse, measurement)

        if (captureResponse is ForageApiResponse.Failure) {
            logger.e(
                "[POS] capturePayment failed for payment $paymentRef on Terminal $posTerminalId: ${captureResponse.errors[0]}"
            )
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
     * ```kotlin
     * // Example deferPaymentCapture call in a DeferPaymentCaptureViewModel.kt
     * class DeferPaymentCaptureViewModel  : ViewModel() {
     *     val snapPaymentRef = "s0alzle0fal"
     *     val merchantId = "mid/<merchant_id>"
     *     val sessionToken = "<session_token>"
     *
     *     fun deferPaymentCapture(foragePinEditText: ForagePINEditText, paymentRef: String) =
     *         viewModelScope.launch {
     *             val response = forageTerminalSdk.deferPaymentCapture(
     *                 DeferPaymentCaptureParams(
     *                     foragePinEditText = foragePinEditText,
     *                     paymentRef = snapPaymentRef
     *                 )
     *             )
     *
     *             when (response) {
     *                 is ForageApiResponse.Success -> {
     *                     // there will be no financial affects upon success
     *                     // you need to capture from the server to formally
     *                     // capture the payment
     *                 }
     *                 is ForageApiResponse.Failure -> {
     *                     // handle an error response here
     *                 }
     *             }
     *         }
     * }
     * ```
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
    suspend fun deferPaymentCapture(
        params: DeferPaymentCaptureParams
    ): ForageApiResponse<String> {
        val (foragePinEditText, paymentRef) = params
        val logger = createLogger(posTerminalId).addAttribute("payment_ref", paymentRef)
        logger.i("[POS] Called deferPaymentCapture for Payment $paymentRef")

        val config = forageConfig.envConfig
        val okHttpClient =  OkHttpClientBuilder.provideOkHttpClient(
            sessionToken = forageConfig.sessionToken,
            merchantId = forageConfig.merchantId,
            traceId = logger.getTraceIdValue()
        )

        // TODO: replace Log.getInstance() with Log() in future PR
        logger
            .addAttribute("merchant_ref", forageConfig.merchantId)
            .addAttribute("payment_ref", paymentRef)

        val response = DeferPaymentCaptureRepository(
            vaultSubmitter = foragePinEditText.vault.getVaultSubmitter(config, logger),
            encryptionKeyService = EncryptionKeyService(config.apiBaseUrl, okHttpClient, logger),
            paymentService = PaymentService(config.apiBaseUrl, okHttpClient, logger),
            paymentMethodService = PaymentMethodService(config.apiBaseUrl, okHttpClient, logger),
        ).deferPaymentCapture(
            merchantId = forageConfig.merchantId,
            paymentRef = paymentRef,
            sessionToken = forageConfig.sessionToken
        )

        return if (response is ForageApiResponse.Failure) {
            logger.e(
                "[POS] deferPaymentCapture failed for Payment $paymentRef on Terminal $posTerminalId: ${response.errors[0]}"
            )
            response
        } else {
            logger.i("[POS] Successfully deferred payment capture for Payment $paymentRef")
            ForageApiResponse.Success("")
        }
    }

    /**
     * Refunds a Payment via a [ForagePinEditText][com.joinforage.forage.android.ui.ForagePINEditText]
     * Element. This method is only available for POS Terminal transactions.
     * You must use [ForageTerminalSDK].
     *
     * * On success, the response includes a Forage
     * [`PaymentRefund`](https://docs.joinforage.app/reference/create-payment-refund) object. *(Example [Refund](https://github.com/Forage-PCI-CDE/android-pos-terminal-sdk/blob/0d845ea57d901bbca13775f4f2de4d4ed6f74791/sample-app/src/main/java/com/joinforage/android/example/ui/pos/data/Refund.kt#L7-L23) and [Receipt](https://github.com/Forage-PCI-CDE/android-pos-terminal-sdk/blob/main/sample-app/src/main/java/com/joinforage/android/example/ui/pos/data/PosReceipt.kt) class)*
     * * On failure, for example in the case of
     * [`ebt_error_61`](https://docs.joinforage.app/reference/errors#ebt_error_61), the response
     * includes a list of [ForageError] objects. You can unpack the list to programmatically handle
     * the error and display the appropriate customer-facing message based on the `ForageError.code`.
     * ```kotlin
     * // Example refundPayment call in a PosRefundViewModel.kt
     * class PosRefundViewModel : ViewModel() {
     *   var paymentRef: String  = ""
     *   var amount: Float = 0.0
     *   var reason: String = ""
     *   var metadata: HashMap? = null
     *
     *   fun refundPayment(foragePinEditText: ForagePINEditText) = viewModelScope.launch {
     *     val forageTerminalSdk = ForageTerminalSDK.init(...) // may throw!
     *     val refundParams = PosRefundPaymentParams(
     *       foragePinEditText,
     *       paymentRef,
     *       amount,
     *       reason,
     *       metadata,
     *     )
     *     val response = forage.refundPayment(refundParams)
     *
     *     when (response) {
     *       is ForageApiResponse.Success -> {
     *         // do something with response.data
     *       }
     *       is ForageApiResponse.Failure -> {
     *         // do something with response.errors
     *       }
     *     }
     *   }
     * }
     * ```
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
        val logger = createLogger(posTerminalId)
        val (foragePinEditText, paymentRef, amount, reason) = params
        logger.addAttribute("payment_ref", paymentRef).addAttribute("merchant_ref", forageConfig.merchantId)
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
        val measurement =
            CustomerPerceivedResponseMonitor.newMeasurement(
                vault = foragePinEditText.getVaultType(),
                vaultAction = UserAction.REFUND,
                logger
            )
        measurement.start()
        // ------------------------------------------------------
        val config = forageConfig.envConfig
        val okHttpClient =  OkHttpClientBuilder.provideOkHttpClient(
            sessionToken = forageConfig.sessionToken,
            merchantId = forageConfig.merchantId,
            traceId = logger.getTraceIdValue()
        )

        val refund =
            PosRefundPaymentRepository(
                vaultSubmitter = foragePinEditText.vault.getVaultSubmitter(config, logger),
                encryptionKeyService = EncryptionKeyService(config.apiBaseUrl, okHttpClient, logger),
                paymentMethodService = PaymentMethodService(config.apiBaseUrl, okHttpClient, logger),
                paymentService = PaymentService(config.apiBaseUrl, okHttpClient, logger),
                pollingService = PollingService(
                    messageStatusService = MessageStatusService(config.apiBaseUrl, okHttpClient, logger),
                    logger = logger
                ),
                logger = logger,
                refundService = PosRefundService(config.apiBaseUrl, logger, okHttpClient)
            ).refundPayment(
                merchantId = forageConfig.merchantId,
                posTerminalId = posTerminalId,
                refundParams = params,
                sessionToken = forageConfig.sessionToken
            )
        processApiResponseForMetrics(refund, measurement)

        if (refund is ForageApiResponse.Failure) {
            logger.e(
                "[POS] refundPayment failed for Payment $paymentRef on Terminal $posTerminalId: ${refund.errors[0]}"
            )
        }

        return refund
    }

    /**
     * Collects a card PIN for an EBT payment and defers
     * the refund of the payment to the server.
     * * On success, the `data` property of the [ForageApiResponse.Success] object resolves with an empty string.
     * * On failure, the response includes a list of
     * [ForageError][com.joinforage.forage.android.network.model.ForageError] objects that you can
     * unpack to troubleshoot the issue.
     * ```kotlin
     * // Example deferPaymentRefund call in a PosDeferPaymentRefundViewModel.kt
     * class PosDeferPaymentRefundViewModel : ViewModel() {
     *   var paymentRef: String  = ""
     *
     *   fun deferPaymentRefund(foragePinEditText: ForagePINEditText) = viewModelScope.launch {
     *     val forageTerminalSdk = ForageTerminalSDK.init(...) // may throw!
     *     val deferPaymentRefundParams = PosDeferPaymentRefundParams(
     *       foragePinEditText,
     *       paymentRef
     *     )
     *     val response = forage.deferPaymentRefund(deferPaymentRefundParams)
     *
     *     when (response) {
     *       is ForageApiResponse.Success -> {
     *         // do something with response.data
     *       }
     *       is ForageApiResponse.Failure -> {
     *         // do something with response.errors
     *       }
     *     }
     *   }
     * }
     * ```
     * @param params The [PosRefundPaymentParams] parameters required for refunding a Payment.
     * @return A [ForageAPIResponse][com.joinforage.forage.android.network.model.ForageApiResponse]
     * indicating the success or failure of the secure PIN submission. On success, returns `Nothing`. On
     * failure, the response includes a list of [ForageError]
     * [com.joinforage.forage.android.network.model.ForageError] objects that you can unpack to
     * troubleshoot the issue.
     * @see
     * *
     * [Defer EBT payment capture and refund completion to the server](https://docs.joinforage.app/docs/capture-ebt-payments-server-side)
     * for the related step-by-step guide.
     * @throws ForageConfigNotSetException If the passed ForagePINEditText instance
     * hasn't had its ForageConfig set via .setForageConfig().
     */
    suspend fun deferPaymentRefund(params: PosDeferPaymentRefundParams): ForageApiResponse<String> {
        val logger = createLogger(posTerminalId)
        val (foragePinEditText, paymentRef) = params

        logger.addAttribute("payment_ref", paymentRef).addAttribute("merchant_ref", forageConfig.merchantId)
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
        val measurement =
            CustomerPerceivedResponseMonitor.newMeasurement(
                vault = foragePinEditText.getVaultType(),
                vaultAction = UserAction.DEFER_REFUND,
                logger
            )
        measurement.start()
        // ------------------------------------------------------
        val config = forageConfig.envConfig
        val okHttpClient =  OkHttpClientBuilder.provideOkHttpClient(
            sessionToken = forageConfig.sessionToken,
            merchantId = forageConfig.merchantId,
            traceId = logger.getTraceIdValue()
        )
        val refund =
            DeferPaymentRefundRepository(
                vaultSubmitter = foragePinEditText.vault.getVaultSubmitter(config, logger),
                encryptionKeyService = EncryptionKeyService(config.apiBaseUrl, okHttpClient, logger),
                paymentService = PaymentService(config.apiBaseUrl, okHttpClient, logger),
                paymentMethodService = PaymentMethodService(config.apiBaseUrl, okHttpClient, logger),
            ).deferPaymentRefund(
                merchantId = forageConfig.merchantId,
                paymentRef = paymentRef,
                sessionToken = forageConfig.sessionToken
            )
        processApiResponseForMetrics(refund, measurement)

        if (refund is ForageApiResponse.Failure) {
            logger.e(
                "[POS] deferPaymentRefund failed for Payment $paymentRef on Terminal $posTerminalId: ${refund.errors[0]}"
            )
        }

        return refund
    }

    private fun isInitializationExceptionOrNull(
        logger: Log,
        methodName: String
    ): ForageApiResponse<String>? {
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
}



/**
 * Retrieves the ForageConfig for a given ForageElement, or throws an exception if the
 * ForageConfig is not set.
 *
 * @param element A ForageElement instance
 * @return The ForageConfig associated with the ForageElement
 * @throws ForageConfigNotSetException If the ForageConfig is not set for the ForageElement
 */
internal fun getForageConfigOrThrow(forageConfig: ForageConfig?): ForageConfig {
    return forageConfig ?: throw ForageConfigNotSetException(
        """
    The ForageElement you passed did not have a ForageConfig. In order to submit
    a request via Forage SDK, your ForageElement MUST have a ForageConfig.
    Make sure to call myForageElement.setForageConfig(forageConfig: ForageConfig) 
    immediately on your ForageElement 
            """.trimIndent()
    )
}

/**
 * Determines the outcome of a Forage API response,
 * to report the measurement to the Telemetry service.
 *
 * This involves stopping the measurement timer,
 * marking the Metrics event as a success or failure,
 * and if the event is a failure, setting the Forage error code.
 */
internal fun processApiResponseForMetrics(
    apiResponse: ForageApiResponse<String>,
    measurement: CustomerPerceivedResponseMonitor
) {
    measurement.end()
    val outcome = if (apiResponse is ForageApiResponse.Failure) {
        if (apiResponse.errors.isNotEmpty()) {
            measurement.setForageErrorCode(apiResponse.errors[0].code)
        }
        EventOutcome.FAILURE
    } else {
        EventOutcome.SUCCESS
    }
    measurement.setEventOutcome(outcome).logResult()
}

/**
 * A model that represents the parameters that Forage requires to check a card's balance.
 * [CheckBalanceParams] are passed to the
 * [checkBalance][com.joinforage.forage.android.ForageSDK.checkBalance] method.
 *
 * @property foragePinEditText A reference to a [ForagePINEditText] instance.
 * [setForageConfig][com.joinforage.forage.android.ui.ForageElement.setForageConfig] must
 * be called on the instance before it can be passed.
 * @property paymentMethodRef A unique string identifier for a previously created
 * [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods) in Forage's database,
 * found in the response from a call to
 * [tokenizeEBTCard][com.joinforage.forage.android.ForageSDK.tokenizeEBTCard] (online-only),
 * [tokenizeCard][com.joinforage.forage.android.pos.ForageTerminalSDK.tokenizeCard] (POS),
 * or the [Create a `PaymentMethod`](https://docs.joinforage.app/reference/create-payment-method)
 * endpoint.
 */
data class CheckBalanceParams(
    val foragePinEditText: ForagePinElement,
    val paymentMethodRef: String
)

/**
 * A model that represents the parameters that Forage requires to capture a payment.
 * [CapturePaymentParams] are passed to the
 * [capturePayment][com.joinforage.forage.android.ForageSDK.capturePayment] method.
 *
 * @property foragePinEditText A reference to a [ForagePINEditText] instance.
 * [setForageConfig][com.joinforage.forage.android.ui.ForageElement.setForageConfig] must
 * be called on the instance before it can be passed.
 * @property paymentRef A unique string identifier for a previously created
 * [`Payment`](https://docs.joinforage.app/reference/payments) in Forage's
 * database, returned by the
 * [Create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) endpoint.
 */
data class CapturePaymentParams(
    val foragePinEditText: ForagePinElement,
    val paymentRef: String
)

/**
 * A model that represents the parameters that Forage requires to collect a card PIN and defer
 * the capture of the payment to the server.
 * [DeferPaymentCaptureParams] are passed to the
 * [deferPaymentCapture][com.joinforage.forage.android.ForageSDK.deferPaymentCapture] method.
 *
 * @see * [Defer EBT payment capture to the server](https://docs.joinforage.app/docs/capture-ebt-payments-server-side)
 * for the related step-by-step guide.
 * * [Capture an EBT Payment](https://docs.joinforage.app/reference/capture-a-payment)
 * for the API endpoint to call after
 * [deferPaymentCapture][com.joinforage.forage.android.ForageSDK.deferPaymentCapture].
 *
 * @property foragePinEditText A reference to a [ForagePINEditText] instance.
 * [setForageConfig][com.joinforage.forage.android.ui.ForageElement.setForageConfig] must
 * be called on the instance before it can be passed.
 * @property paymentRef A unique string identifier for a previously created
 * [`Payment`](https://docs.joinforage.app/reference/payments) in Forage's
 * database, returned by the
 * [Create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) endpoint.
 */
data class DeferPaymentCaptureParams(
    val foragePinEditText: ForagePinElement,
    val paymentRef: String
)

/**
 * A model that represents the parameters that Forage requires to collect a card PIN and defer
 * the refund of the payment to the server.
 * [PosDeferPaymentRefundParams] are passed to the
 * [deferPaymentRefund][com.joinforage.forage.android.pos.ForageTerminalSDK.deferPaymentRefund] method.
 *
 * @property foragePinEditText A reference to a [ForagePINEditText] instance.
 * [setForageConfig][com.joinforage.forage.android.ui.ForageElement.setForageConfig] must
 * be called on the instance before it can be passed.
 * @property paymentRef A unique string identifier for a previously created
 * [`Payment`](https://docs.joinforage.app/reference/payments) in Forage's
 * database, returned by the
 * [Create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) endpoint.
 */
data class PosDeferPaymentRefundParams(
    val foragePinEditText: ForagePinElement,
    val paymentRef: String
)
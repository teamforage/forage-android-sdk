package com.joinforage.forage.android.pos.services

import android.content.Context
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.ForageConfigNotSetException
import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeyService
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.OkHttpClientBuilder
import com.joinforage.forage.android.core.services.forageapi.payment.PaymentService
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.core.services.telemetry.CustomerPerceivedResponseMonitor
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.ui.element.ForageVaultElement
import com.joinforage.forage.android.core.ui.element.state.ElementState
import com.joinforage.forage.android.pos.services.encryption.storage.KsnFileManager
import com.joinforage.forage.android.pos.services.forageapi.refund.PosRefundService
import com.joinforage.forage.android.pos.services.vault.DeferPaymentRefundRepository
import com.joinforage.forage.android.pos.services.vault.PosCapturePaymentRepository
import com.joinforage.forage.android.pos.services.vault.PosCheckBalanceRepository
import com.joinforage.forage.android.pos.services.vault.PosDeferPaymentCaptureRepository
import com.joinforage.forage.android.pos.services.vault.PosRefundPaymentRepository
import com.joinforage.forage.android.pos.services.vault.PosTokenizeCardService
import com.joinforage.forage.android.pos.services.vault.rosetta.RosettaPinSubmitter
import com.joinforage.forage.android.pos.ui.element.ForagePANEditText
import java.io.File

/**
 * The entry point for **in-store POS Terminal** transactions.
 *
 * A [ForageTerminalSDK] instance interacts with the Forage API.
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
 *     forageConfig = ForageConfig(
 *         merchantId = "123ab45c67",
 *         sessionToken = "sandbox_ey123..."
 *     )
 * )
 * ```
 *
 * @see * [Forage guide to Terminal POS integrations](https://docs.joinforage.app/docs/forage-terminal-android)
 * * [Forage Android online-only SDK](https://android.joinforage.app/) to process online-only transactions
 */
class ForageTerminalSDK internal constructor(
    private val posTerminalId: String,
    private val forageConfig: ForageConfig
) {

    companion object {
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
         *         forageConfig = ForageConfig(
         *             merchantId = "123ab45c67",
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
         * @param ForageConfig **Required**. A [ForageConfig][com.joinforage.forage.android.core.services.ForageConfig] instance that specifies a
         * `merchantId` and `sessionToken`.
         * @param ksnDir **Optional**. A File instance representing a directory where the KSN file
         * for DUKPT should reside. If no directory is supplied, the KSN file will live in the
         * root of the application's files directory.
         */
        @Throws(Exception::class)
        suspend fun init(
            context: Context,
            posTerminalId: String,
            forageConfig: ForageConfig,
            ksnDir: File = context.filesDir
        ): ForageTerminalSDK {
            val (merchantId, sessionToken) = forageConfig
            val logSuffix = "on Terminal $posTerminalId for Merchant $merchantId"
            val logger = Log.getInstance()
            logger.initializeDD(context, forageConfig)
            logger
                .addAttribute("pos_terminal_id", posTerminalId)
                .addAttribute("merchant_ref", merchantId)
                .i("[POS] Executing ForageTerminalSDK.init() initialization sequence $logSuffix")

            try {
                val ksnFileManager = KsnFileManager.byDir(ksnDir)
                // STOPGAP to feed `context` to ForagePinSubmitter
                RosettaPinSubmitter.ksnFileManager = ksnFileManager

                val initializer = PosTerminalInitializer(
                    logger = logger,
                    ksnManager = ksnFileManager
                )

                initializer.execute(
                    posTerminalId = posTerminalId,
                    merchantId = merchantId,
                    sessionToken = sessionToken
                )

                logger.i("[POS] Initialized ForageTerminalSDK using the init() method $logSuffix")
            } catch (e: Exception) {
                // clear file!

                logger.e("[POS] Failed to initialize ForageTerminalSDK using the init() method.", e)
                throw Exception("Failed to initialize the ForageTerminalSDK")
            }

            return ForageTerminalSDK(posTerminalId, forageConfig)
        }
    }

    /**
     * Tokenizes a card via manual entry into a [ForagePANEdit Text]
     * [com.joinforage.forage.android.pos.ui.element.ForagePANEditText] Element.
     * * On success, the object includes a `ref` token that represents an instance of a Forage
     * [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods). You can store
     * the token in your database and reference it for future transactions, like to call
     * [checkBalance] or to [create a Payment](https://docs.joinforage.app/reference/create-a-payment)
     * in Forage's database. *(Example [PosPaymentMethod](https://github.com/teamforage/forage-android-sdk/blob/229a0c7d38dcae751070aed45ff2f7e7ea2a5abb/sample-app/src/main/java/com/joinforage/android/example/ui/pos/data/tokenize/PosPaymentMethod.kt#L7) class)*
     * * On failure, for example in the case of [`unsupported_bin`](https://docs.joinforage.app/reference/errors#unsupported_bin),
     * the response includes a list of [ForageError][com.joinforage.forage.android.core.services.forageapi.network.ForageError]
     * objects that you can unpack to programmatically handle the error and display the appropriate
     * customer-facing message based on the `ForageError.code`.
     * ```kotlin
     * // Example tokenizeCard call in a TokenizeViewModel.kt
     * class TokenizeViewMode : ViewModel() {
     *     val merchantId = "<merchant_id>"
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
     * @param params **Required**. A [TokenizeManualEntryParams] model that passes a reference to a
     * [ForagePANEditText] instance that collects the customer's card number and an optional
     * `reusable` boolean that Forage uses to tokenize the card.
     * [setForageConfig][com.joinforage.forage.android.core.ui.element.ForagePanElement.setForageConfig]
     * must have been called on the instance before it can be passed.
     * @throws ForageConfigNotSetException If the [ForageConfig] is not set for the provided
     * [ForagePANEditText] instance.
     *
     * @return A [ForageApiResponse] object.
     */
    suspend fun tokenizeCard(
        params: TokenizeManualEntryParams
    ): ForageApiResponse<String> {
        val (foragePanEditText, reusable) = params
        val (merchantId, sessionToken) = forageConfig
        val config = EnvConfig.fromSessionToken(sessionToken)
        val logger = Log.getInstance()
            .addAttribute("pos_terminal_id", posTerminalId)
            .addAttribute("reusable", reusable)
            .addAttribute("merchant_ref", merchantId)
            .i("[POS] Tokenizing Payment Method via UI PAN entry on Terminal $posTerminalId")

        val okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
            sessionToken = sessionToken,
            merchantId = merchantId,
            traceId = logger.getTraceIdValue()
        )

        return PosTokenizeCardService(
            config.apiBaseUrl,
            okHttpClient,
            logger
        ).tokenizeCard(
            cardNumber = foragePanEditText.getPanNumber(),
            reusable = reusable
        )
    }

    /**
     * Tokenizes a card via a magnetic swipe from a physical POS Terminal.
     * * On success, the object includes a `ref` token that represents an instance of a Forage
     * [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods). You can store
     * the token for future transactions, like to call [checkBalance] or to
     * [create a Payment](https://docs.joinforage.app/reference/create-a-payment) in Forage's database.
     * * On failure, for example in the case of [`unsupported_bin`](https://docs.joinforage.app/reference/errors#unsupported_bin),
     * the response includes a list of [ForageError][com.joinforage.forage.android.core.services.forageapi.network.ForageError]
     * objects that you can unpack to programmatically handle the error and display the appropriate
     * customer-facing message based on the `ForageError.code`.
     * ```kotlin
     * // Example tokenizeCard(TokenizeMagSwipeParams) call in a TokenizePosViewModel.kt
     * class TokenizePosViewModel : ViewModel() {
     *     val merchantId = "<merchant_id>"
     *     val sessionToken = "<session_token>"
     *
     *     fun tokenizePosCard(foragePinEditText: ForagePINEditText) = viewModelScope.launch {
     *         val response = forageTerminalSdk.tokenizeCard(
     *           TokenizeMagSwipeParams(
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
     * @param params **Required**. A [TokenizeMagSwipeParams] model that passes the [ForageConfig],
     * the card's `track2Data`, and a `reusable` boolean that Forage uses to tokenize the card.
     *
     * @throws ForageConfigNotSetException If the [ForageConfig] is not set for the provided
     * [ForagePANEditText] instance.
     *
     * @return A [ForageAPIResponse][com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse]
     * object.
     */
    suspend fun tokenizeCard(params: TokenizeMagSwipeParams): ForageApiResponse<String> {
        val (track2Data, reusable) = params
        val (merchantId, sessionToken) = forageConfig
        val config = EnvConfig.fromSessionToken(sessionToken)
        val logger = Log.getInstance()
            .addAttribute("pos_terminal_id", posTerminalId)
            .addAttribute("reusable", reusable)
            .addAttribute("merchant_ref", merchantId)
            .i(
                "[POS] Tokenizing Payment Method using magnetic card swipe with Track 2 data on Terminal $posTerminalId"
            )
        val okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
            sessionToken = sessionToken,
            merchantId = merchantId,
            traceId = logger.getTraceIdValue()
        )
        return PosTokenizeCardService(
            config.apiBaseUrl,
            okHttpClient,
            logger
        ).tokenizeMagSwipeCard(track2Data = track2Data, reusable = reusable)
    }

    /**
     * Checks the balance of a previously created
     * [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods)
     * via a [ForageVaultElement][com.joinforage.forage.android.core.ui.element.ForageVaultElement]
     * (either a [ForagePINEditText][com.joinforage.forage.android.pos.ui.element.ForagePINEditText]
     * or a [ForagePinPad][com.joinforage.forage.android.pos.ui.element.ForagePinPad]).
     *
     * ⚠️ _FNS prohibits balance inquiries on sites and apps that offer guest checkout. Skip this
     * method if your customers can opt for guest checkout. If guest checkout is not an option, then
     * it's up to you whether or not to add a balance inquiry feature. No FNS regulations apply._
     * * On success, the response object includes `snap` and `cash` fields that indicate
     * the EBT Card's current SNAP and EBT Cash balances. *(Example [BalanceCheck](https://github.com/Forage-PCI-CDE/android-pos-terminal-sdk/blob/main/sample-app/src/main/java/com/joinforage/android/example/ui/pos/data/BalanceCheck.kt) class)*
     * * On failure, for example in the case of
     * [`ebt_error_14`](https://docs.joinforage.app/reference/errors#ebt_error_14),
     * the response includes a list of
     * [ForageError][com.joinforage.forage.android.core.services.forageapi.network.ForageError] objects that you can
     * unpack to programmatically handle the error and display the appropriate
     * customer-facing message based on the `ForageError.code`.
     * ```kotlin
     * // Example checkBalance call in a BalanceCheckViewModel.kt
     * class BalanceCheckViewModel : ViewModel() {
     *     val paymentMethodRef = "020xlaldfh"
     *
     *     fun checkBalance(forageVaultElement: ForagePINEditText) = viewModelScope.launch {
     *         val response = forageTerminalSdk.checkBalance(
     *             CheckBalanceParams(
     *                 forageVaultElement = foragePinEditText,
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
     * a [ForageVaultElement][com.joinforage.forage.android.core.ui.element.ForageVaultElement]
     * (either a [ForagePINEditText][com.joinforage.forage.android.pos.ui.element.ForagePINEditText]
     * or a [ForagePinPad][com.joinforage.forage.android.pos.ui.element.ForagePinPad]) and a
     * `paymentMethodRef`, found in the response from a call to [tokenizeCard] or the
     * [Create a `PaymentMethod`](https://docs.joinforage.app/reference/create-payment-method)
     * endpoint, that Forage uses to check the payment method's balance.
     *
     * @see * [SDK errors](https://docs.joinforage.app/reference/errors#sdk-errors) for more
     * information on error handling.
     * * [Test EBT Cards](https://docs.joinforage.app/docs/test-ebt-cards#balance-inquiry-exceptions)
     * to trigger balance inquiry exceptions during testing.
     * @return A [ForageApiResponse] object.
     */
    suspend fun checkBalance(params: CheckBalanceParams): ForageApiResponse<String> {
        val (forageVaultElement, paymentMethodRef) = params
        val config = forageConfig.envConfig
        val logger = Log.getInstance()
            .addAttribute("pos_terminal_id", posTerminalId)
            .addAttribute("merchant_ref", forageConfig.merchantId)
            .addAttribute("payment_method_ref", paymentMethodRef)
            .i(
                "[POS] Called checkBalance for PaymentMethod $paymentMethodRef on Terminal $posTerminalId"
            )
        val measurement = CustomerPerceivedResponseMonitor(
            VaultType.FORAGE_VAULT_TYPE,
            UserAction.BALANCE,
            logger
        )
        val okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
            sessionToken = forageConfig.sessionToken,
            merchantId = forageConfig.merchantId,
            traceId = logger.getTraceIdValue()
        )
        val balanceResponse =
            PosCheckBalanceRepository(
                vaultSubmitter = forageVaultElement.getVaultSubmitter(config, logger),
                encryptionKeyService = EncryptionKeyService(config.apiBaseUrl, okHttpClient, logger),
                paymentMethodService = PaymentMethodService(config.apiBaseUrl, okHttpClient, logger),
                logger = logger
            ).posCheckBalance(
                merchantId = forageConfig.merchantId,
                paymentMethodRef = paymentMethodRef,
                posTerminalId = posTerminalId,
                sessionToken = forageConfig.sessionToken
            )
        measurement.setEventOutcome(balanceResponse).logResult()
        return balanceResponse
    }

    /**
     * Immediately captures a payment
     * via a [ForageVaultElement][com.joinforage.forage.android.core.ui.element.ForageVaultElement]
     * (either a [ForagePINEditText][com.joinforage.forage.android.pos.ui.element.ForagePINEditText]
     * or a [ForagePinPad][com.joinforage.forage.android.pos.ui.element.ForagePinPad]).
     *
     * * On success, the object confirms the transaction. The response includes a Forage
     * [`Payment`](https://docs.joinforage.app/reference/payments) object. *(Example [PosPaymentResponse](https://github.com/Forage-PCI-CDE/android-pos-terminal-sdk/blob/main/sample-app/src/main/java/com/joinforage/android/example/ui/pos/data/PosPaymentResponse.kt#L8) and [Receipt](https://github.com/Forage-PCI-CDE/android-pos-terminal-sdk/blob/main/sample-app/src/main/java/com/joinforage/android/example/ui/pos/data/PosReceipt.kt) class)*
     * * On failure, for example in the case of
     * [`card_not_reusable`](https://docs.joinforage.app/reference/errors#card_not_reusable) or
     * [`ebt_error_51`](https://docs.joinforage.app/reference/errors#ebt_error_51) errors, the
     * response includes a list of
     * [ForageError][com.joinforage.forage.android.core.services.forageapi.network.ForageError] objects that you can
     * unpack to programmatically handle the error and display the appropriate
     * customer-facing message based on the `ForageError.code`.
     * ```kotlin
     * // Example capturePayment call in a PaymentCaptureViewModel.kt
     * class PaymentCaptureViewModel : ViewModel() {
     *     val snapPaymentRef = "s0alzle0fal"
     *     val merchantId = "<merchant_id>"
     *     val sessionToken = "<session_token>"
     *
     *     fun capturePayment(forageVaultElement: ForagePINEditText, paymentRef: String) =
     *         viewModelScope.launch {
     *             val response = forageTerminalSdk.capturePayment(
     *                 CapturePaymentParams(
     *                     forageVaultElement = foragePinEditText,
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
     * a [ForageVaultElement][com.joinforage.forage.android.core.ui.element.ForageVaultElement]
     * (either a [ForagePINEditText][com.joinforage.forage.android.pos.ui.element.ForagePINEditText]
     * or a [ForagePinPad][com.joinforage.forage.android.pos.ui.element.ForagePinPad])
     * instance and a `paymentRef`, returned by the
     * [Create a Payment](https://docs.joinforage.app/reference/create-a-payment) endpoint, that
     * Forage uses to capture a payment.
     *
     * @see
     * * [SDK errors](https://docs.joinforage.app/reference/errors#sdk-errors) for more information
     * on error handling.
     * * [Test EBT Cards](https://docs.joinforage.app/docs/test-ebt-cards#payment-capture-exceptions)
     * to trigger payment capture exceptions during testing.
     * @return A [ForageApiResponse] object.
     */
    suspend fun capturePayment(params: CapturePaymentParams): ForageApiResponse<String> {
        val (forageVaultElement, paymentRef) = params
        val config = forageConfig.envConfig
        val logger = Log.getInstance()
            .addAttribute("pos_terminal_id", posTerminalId)
            .addAttribute("payment_ref", paymentRef)
            .addAttribute("merchant_ref", forageConfig.merchantId)
            .i("[POS] Called capturePayment for Payment $paymentRef")
        val measurement = CustomerPerceivedResponseMonitor(
            VaultType.FORAGE_VAULT_TYPE,
            UserAction.CAPTURE,
            logger
        )
        val okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
            sessionToken = forageConfig.sessionToken,
            merchantId = forageConfig.merchantId,
            traceId = logger.getTraceIdValue()
        )
        val captureResponse = PosCapturePaymentRepository(
            vaultSubmitter = forageVaultElement.getVaultSubmitter(config, logger),
            encryptionKeyService = EncryptionKeyService(config.apiBaseUrl, okHttpClient, logger),
            paymentMethodService = PaymentMethodService(config.apiBaseUrl, okHttpClient, logger),
            paymentService = PaymentService(config.apiBaseUrl, okHttpClient, logger),
            logger = logger
        ).capturePosPayment(
            merchantId = forageConfig.merchantId,
            paymentRef = paymentRef,
            sessionToken = forageConfig.sessionToken,
            posTerminalId = posTerminalId
        )

        measurement.setEventOutcome(captureResponse).logResult()

        return captureResponse
    }

    /**
     * Submits a card PIN via a
     * via a [ForageVaultElement][com.joinforage.forage.android.core.ui.element.ForageVaultElement]
     * (either a [ForagePINEditText][com.joinforage.forage.android.pos.ui.element.ForagePINEditText]
     * or a [ForagePinPad][com.joinforage.forage.android.pos.ui.element.ForagePinPad])
     * and defers payment capture to the server.
     *
     * * On success, the `data` property of the [ForageApiResponse.Success] object resolves with an empty string.
     * * On failure, for example in the case of [`expired_session_token`](https://docs.joinforage.app/reference/errors#expired_session_token) errors, the
     * response includes a list of
     * [ForageError][com.joinforage.forage.android.core.services.forageapi.network.ForageError] objects that you can
     * unpack to programmatically handle the error and display the appropriate
     * customer-facing message based on the `ForageError.code`.
     * ```kotlin
     * // Example deferPaymentCapture call in a DeferPaymentCaptureViewModel.kt
     * class DeferPaymentCaptureViewModel  : ViewModel() {
     *     val snapPaymentRef = "s0alzle0fal"
     *     val merchantId = "<merchant_id>"
     *     val sessionToken = "<session_token>"
     *
     *     fun deferPaymentCapture(forageVaultElement: ForagePINEditText, paymentRef: String) =
     *         viewModelScope.launch {
     *             val response = forageTerminalSdk.deferPaymentCapture(
     *                 DeferPaymentCaptureParams(
     *                     forageVaultElement = forageVaultElement,
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
     * [ForageVaultElement][com.joinforage.forage.android.core.ui.element.ForageVaultElement]
     * (either a [ForagePINEditText][com.joinforage.forage.android.pos.ui.element.ForagePINEditText]
     * or a [ForagePinPad][com.joinforage.forage.android.pos.ui.element.ForagePinPad]) instance
     * and a `paymentRef`, returned by the
     * [Create a Payment](https://docs.joinforage.app/reference/create-a-payment) endpoint, as the
     * DeferPaymentCaptureParams.
     *
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
        val (forageVaultElement, paymentRef) = params
        val config = forageConfig.envConfig
        val logger = Log.getInstance()
            .addAttribute("pos_terminal_id", posTerminalId)
            .addAttribute("payment_ref", paymentRef)
            .addAttribute("merchant_ref", forageConfig.merchantId)
            .i("[POS] Called deferPaymentCapture for Payment $paymentRef")
        val okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
            sessionToken = forageConfig.sessionToken,
            merchantId = forageConfig.merchantId,
            traceId = logger.getTraceIdValue()
        )
        return PosDeferPaymentCaptureRepository(
            vaultSubmitter = forageVaultElement.getVaultSubmitter(config, logger),
            encryptionKeyService = EncryptionKeyService(config.apiBaseUrl, okHttpClient, logger),
            paymentService = PaymentService(config.apiBaseUrl, okHttpClient, logger),
            paymentMethodService = PaymentMethodService(config.apiBaseUrl, okHttpClient, logger),
            logger = logger
        ).deferPosPaymentCapture(
            merchantId = forageConfig.merchantId,
            paymentRef = paymentRef,
            sessionToken = forageConfig.sessionToken,
            posTerminalId
        )
    }

    /**
     * Refunds a Payment
     * via a [ForageVaultElement][com.joinforage.forage.android.core.ui.element.ForageVaultElement]
     * (either a [ForagePINEditText][com.joinforage.forage.android.pos.ui.element.ForagePINEditText]
     * or a [ForagePinPad][com.joinforage.forage.android.pos.ui.element.ForagePinPad]).
     * This method is only available for POS Terminal transactions.
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
     *   fun refundPayment(forageVaultElement: ForagePINEditText) = viewModelScope.launch {
     *     val forageTerminalSdk = ForageTerminalSDK.init(...) // may throw!
     *     val refundParams = RefundPaymentParams(
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
     * @param params A [RefundPaymentParams] model that passes a
     * [ForageVaultElement][com.joinforage.forage.android.core.ui.element.ForageVaultElement]
     * (either a [ForagePINEditText][com.joinforage.forage.android.pos.ui.element.ForagePINEditText]
     * or a [ForagePinPad][com.joinforage.forage.android.pos.ui.element.ForagePinPad]) instance and
     * a `paymentRef`, returned by the [Create a Payment](https://docs.joinforage.app/reference/create-a-payment)
     * endpoint, an `amount`, and a `reason` as the RefundPaymentParams.
     * @see * [SDK errors](https://docs.joinforage.app/reference/errors#sdk-errors) for more information
     * on error handling.
     * @return A [ForageApiResponse] object.
     */
    suspend fun refundPayment(params: RefundPaymentParams): ForageApiResponse<String> {
        val (forageVaultElement, paymentRef, amount, reason) = params
        val config = forageConfig.envConfig
        val logger = Log.getInstance()
            .addAttribute("pos_terminal_id", posTerminalId)
            .addAttribute("payment_ref", paymentRef)
            .addAttribute("merchant_ref", forageConfig.merchantId)
            .i(
                """
                [POS] Called refundPayment for Payment $paymentRef
                with amount: $amount
                for reason: $reason
                on Terminal: $posTerminalId
                """.trimIndent()
            )
        val measurement = CustomerPerceivedResponseMonitor(
            VaultType.FORAGE_VAULT_TYPE,
            UserAction.REFUND,
            logger
        )
        val okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
            sessionToken = forageConfig.sessionToken,
            merchantId = forageConfig.merchantId,
            traceId = logger.getTraceIdValue()
        )

        val refund =
            PosRefundPaymentRepository(
                vaultSubmitter = forageVaultElement.getVaultSubmitter(config, logger),
                encryptionKeyService = EncryptionKeyService(config.apiBaseUrl, okHttpClient, logger),
                paymentMethodService = PaymentMethodService(config.apiBaseUrl, okHttpClient, logger),
                paymentService = PaymentService(config.apiBaseUrl, okHttpClient, logger),
                logger = logger,
                refundService = PosRefundService(config.apiBaseUrl, logger, okHttpClient)
            ).refundPayment(
                merchantId = forageConfig.merchantId,
                posTerminalId = posTerminalId,
                refundParams = params,
                sessionToken = forageConfig.sessionToken
            )
        measurement.setEventOutcome(refund).logResult()
        return refund
    }

    /**
     * Collects a card PIN for an EBT payment
     * via a [ForageVaultElement][com.joinforage.forage.android.core.ui.element.ForageVaultElement]
     * (either a [ForagePINEditText][com.joinforage.forage.android.pos.ui.element.ForagePINEditText]
     * or a [ForagePinPad][com.joinforage.forage.android.pos.ui.element.ForagePinPad])
     * and defers the refund of the payment to the server.
     * * On success, the `data` property of the [ForageApiResponse.Success] object resolves with an empty string.
     * * On failure, the response includes a list of
     * [ForageError][com.joinforage.forage.android.core.services.forageapi.network.ForageError] objects that you can
     * unpack to troubleshoot the issue.
     * ```kotlin
     * // Example deferPaymentRefund call in a PosDeferPaymentRefundViewModel.kt
     * class PosDeferPaymentRefundViewModel : ViewModel() {
     *   var paymentRef: String  = ""
     *
     *   fun deferPaymentRefund(forageVaultElement: ForagePINEditText) = viewModelScope.launch {
     *     val forageTerminalSdk = ForageTerminalSDK.init(...) // may throw!
     *     val deferPaymentRefundParams = DeferPaymentRefundParams(
     *       forageVaultElement = forageVaultElement,
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
     * @param params The [DeferPaymentRefundParams] parameters required for refunding a Payment.
     * @return A [ForageAPIResponse][com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse]
     * indicating the success or failure of the secure PIN submission. On success, returns `Nothing`. On
     * failure, the response includes a list of [ForageError]
     * [com.joinforage.forage.android.core.services.forageapi.network.ForageError] objects that you can unpack to
     * troubleshoot the issue.
     * @see
     * *
     * [Defer EBT payment capture and refund completion to the server](https://docs.joinforage.app/docs/capture-ebt-payments-server-side)
     * for the related step-by-step guide.
     */
    suspend fun deferPaymentRefund(params: DeferPaymentRefundParams): ForageApiResponse<String> {
        val (forageVaultElement, paymentRef) = params
        val config = forageConfig.envConfig
        val logger = Log.getInstance()
            .addAttribute("pos_terminal_id", posTerminalId)
            .addAttribute("payment_ref", paymentRef)
            .addAttribute("merchant_ref", forageConfig.merchantId)
            .i(
                """
                [POS] Called deferPaymentRefund for Payment $paymentRef
                on Terminal: $posTerminalId
                """.trimIndent()
            )
        val measurement = CustomerPerceivedResponseMonitor(
            VaultType.FORAGE_VAULT_TYPE,
            UserAction.DEFER_REFUND,
            logger
        )
        val okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
            sessionToken = forageConfig.sessionToken,
            merchantId = forageConfig.merchantId,
            traceId = logger.getTraceIdValue()
        )
        val refund =
            DeferPaymentRefundRepository(
                vaultSubmitter = forageVaultElement.getVaultSubmitter(config, logger),
                encryptionKeyService = EncryptionKeyService(config.apiBaseUrl, okHttpClient, logger),
                paymentService = PaymentService(config.apiBaseUrl, okHttpClient, logger),
                paymentMethodService = PaymentMethodService(config.apiBaseUrl, okHttpClient, logger),
                logger = logger
            ).deferPaymentRefund(
                merchantId = forageConfig.merchantId,
                paymentRef = paymentRef,
                sessionToken = forageConfig.sessionToken,
                posTerminalId
            )
        measurement.setEventOutcome(refund).logResult()

        return refund
    }
}

/**
 * A model that represents the parameters that [ForageTerminalSDK] requires to tokenize a card by
 * entering the card number into a [ForagePANEditText].
 * This data class is not supported for online-only transactions.
 * [TokenizeManualEntryParams] are passed to the
 * [tokenizeCard][com.joinforage.forage.android.pos.services.ForageTerminalSDK.tokenizeCard] method.
 *
 * @property foragePanEditText **Required**. A reference to a [ForagePANEditText] instance that
 * collects the customer's card number.
 * @property reusable Optional. A boolean that indicates whether the same card can be used to create
 * multiple payments. Defaults to true.
 */
data class TokenizeManualEntryParams(
    val foragePanEditText: ForagePANEditText,
    val reusable: Boolean = true
)

/**
 * A model that represents the parameters that [ForageTerminalSDK] requires to tokenize a card via
 * a magnetic swipe from a physical POS Terminal.
 * This data class is not supported for online-only transactions.
 * [TokenizeMagSwipeParams] are passed to the
 * [tokenizeCard][com.joinforage.forage.android.pos.services.ForageTerminalSDK.tokenizeCard] method.
 *
 * @property track2Data **Required**. The information encoded on Track 2 of the card’s magnetic
 * stripe, excluding the start and stop sentinels and any LRC characters. _Example value_:
 * `"123456789123456789=123456789123"`
 * @property reusable Optional. A boolean that indicates whether the same card can be used to create
 * multiple payments. Defaults to true.
 */
data class TokenizeMagSwipeParams(
    val track2Data: String,
    val reusable: Boolean = true
)

/**
 * A model that represents the parameters that Forage requires to check a card's balance.
 * [CheckBalanceParams] are passed to the
 * [checkBalance][com.joinforage.forage.android.pos.services.ForageTerminalSDK.checkBalance] method.
 *
 * @property forageVaultElement A reference to a [ForageVaultElement][com.joinforage.forage.android.core.ui.element.ForageVaultElement]
 * instance (either a [ForagePINEditText][com.joinforage.forage.android.pos.ui.element.ForagePINEditText]
 * or a [ForagePinPad][com.joinforage.forage.android.pos.ui.element.ForagePinPad]).
 * @property paymentMethodRef A unique string identifier for a previously created
 * [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods) in Forage's database,
 * found in the response from a call to
 * [tokenizeCard][com.joinforage.forage.android.pos.services.ForageTerminalSDK.tokenizeCard]
 * or the [Create a `PaymentMethod`](https://docs.joinforage.app/reference/create-payment-method)
 * endpoint.
 */
data class CheckBalanceParams(
    val forageVaultElement: ForageVaultElement<ElementState>,
    val paymentMethodRef: String
)

/**
 * A model that represents the parameters that Forage requires to capture a payment.
 * [CapturePaymentParams] are passed to the
 * [capturePayment][com.joinforage.forage.android.pos.services.ForageTerminalSDK.capturePayment] method.
 *
 * @property forageVaultElement A reference to a [ForageVaultElement][com.joinforage.forage.android.core.ui.element.ForageVaultElement]
 * instance (either a [ForagePINEditText][com.joinforage.forage.android.pos.ui.element.ForagePINEditText]
 * or a [ForagePinPad][com.joinforage.forage.android.pos.ui.element.ForagePinPad]).
 * @property paymentRef A unique string identifier for a previously created
 * [`Payment`](https://docs.joinforage.app/reference/payments) in Forage's
 * database, returned by the
 * [Create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) endpoint.
 */
data class CapturePaymentParams(
    val forageVaultElement: ForageVaultElement<ElementState>,
    val paymentRef: String
)

/**
 * A model that represents the parameters that Forage requires to collect a card PIN and defer
 * the capture of the payment to the server.
 * [DeferPaymentCaptureParams] are passed to the
 * [deferPaymentCapture][com.joinforage.forage.android.pos.services.ForageTerminalSDK.deferPaymentCapture] method.
 *
 * @see * [Defer EBT payment capture to the server](https://docs.joinforage.app/docs/capture-ebt-payments-server-side)
 * for the related step-by-step guide.
 * * [Capture an EBT Payment](https://docs.joinforage.app/reference/capture-a-payment)
 * for the API endpoint to call after
 * [deferPaymentCapture][com.joinforage.forage.android.pos.services.ForageTerminalSDK.deferPaymentCapture].
 *
 * @property forageVaultElement A reference to a [ForageVaultElement][com.joinforage.forage.android.core.ui.element.ForageVaultElement]
 * instance (either a [ForagePINEditText][com.joinforage.forage.android.pos.ui.element.ForagePINEditText]
 * or a [ForagePinPad][com.joinforage.forage.android.pos.ui.element.ForagePinPad]).
 * @property paymentRef A unique string identifier for a previously created
 * [`Payment`](https://docs.joinforage.app/reference/payments) in Forage's
 * database, returned by the
 * [Create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) endpoint.
 */
data class DeferPaymentCaptureParams(
    val forageVaultElement: ForageVaultElement<ElementState>,
    val paymentRef: String
)

/**
 * A model that represents the parameters that [ForageTerminalSDK] requires to refund a Payment.
 * [RefundPaymentParams] are passed to the
 * [refundPayment][com.joinforage.forage.android.pos.services.ForageTerminalSDK.refundPayment] method.
 *
 * @property forageVaultElement A reference to a [ForageVaultElement][com.joinforage.forage.android.core.ui.element.ForageVaultElement]
 * instance (either a [ForagePINEditText][com.joinforage.forage.android.pos.ui.element.ForagePINEditText]
 * or a [ForagePinPad][com.joinforage.forage.android.pos.ui.element.ForagePinPad])..
 * @property paymentRef **Required**. A unique string identifier for a previously created
 * [`Payment`](https://docs.joinforage.app/reference/payments) in Forage's database, returned by the
 *  [Create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) endpoint.
 * @property amount **Required**. A positive decimal number that represents how much of the original
 * payment to refund in USD. Precision to the penny is supported.
 * The minimum amount that can be refunded is `0.01`.
 * @property reason **Required**. A string that describes why the payment is to be refunded.
 * @property metadata Optional. A map of merchant-defined key-value pairs. For example, some
 * merchants attach their credit card processor’s ID for the customer making the refund.
 */
data class RefundPaymentParams(
    val forageVaultElement: ForageVaultElement<ElementState>,
    val paymentRef: String,
    val amount: Float,
    val reason: String,
    val metadata: Map<String, String>? = null
)

/**
 * A model that represents the parameters that Forage requires to collect a card PIN and defer
 * the refund of the payment to the server.
 * [DeferPaymentRefundParams] are passed to the
 * [deferPaymentRefund][com.joinforage.forage.android.pos.services.ForageTerminalSDK.deferPaymentRefund] method.
 *
 * @property forageVaultElement A reference to a [ForageVaultElement][com.joinforage.forage.android.core.ui.element.ForageVaultElement]
 * instance (either a [ForagePINEditText][com.joinforage.forage.android.pos.ui.element.ForagePINEditText]
 * or a [ForagePinPad][com.joinforage.forage.android.pos.ui.element.ForagePinPad]).
 * @property paymentRef A unique string identifier for a previously created
 * [`Payment`](https://docs.joinforage.app/reference/payments) in Forage's
 * database, returned by the
 * [Create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) endpoint.
 */
data class DeferPaymentRefundParams(
    val forageVaultElement: ForageVaultElement<ElementState>,
    val paymentRef: String
)

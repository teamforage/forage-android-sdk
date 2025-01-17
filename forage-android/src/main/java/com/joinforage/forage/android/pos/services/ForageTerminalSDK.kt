package com.joinforage.forage.android.pos.services

import android.content.Context
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.engine.OkHttpEngine
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.payment.PaymentService
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.core.services.telemetry.DatadogLogger
import com.joinforage.forage.android.core.ui.element.ForageVaultElement
import com.joinforage.forage.android.core.ui.element.state.ElementState
import com.joinforage.forage.android.pos.services.emvchip.CardholderInteraction
import com.joinforage.forage.android.pos.services.emvchip.TerminalCapabilities
import com.joinforage.forage.android.pos.services.encryption.certificate.RsaKeyManager
import com.joinforage.forage.android.pos.services.encryption.dukpt.DukptService
import com.joinforage.forage.android.pos.services.encryption.storage.AndroidKeyStoreKeyRegisters
import com.joinforage.forage.android.pos.services.encryption.storage.FileKsnManager
import com.joinforage.forage.android.pos.services.encryption.storage.KsnFileManager
import com.joinforage.forage.android.pos.services.init.AndroidBase64Util
import com.joinforage.forage.android.pos.services.init.PosTerminalInitializer
import com.joinforage.forage.android.pos.services.init.RosettaInitService
import com.joinforage.forage.android.pos.services.vault.submission.PosBalanceCheckSubmission
import com.joinforage.forage.android.pos.services.vault.submission.PosCapturePaymentSubmission
import com.joinforage.forage.android.pos.services.vault.submission.PosDeferCapturePaymentSubmission
import com.joinforage.forage.android.pos.services.vault.submission.PosDeferRefundPaymentSubmission
import com.joinforage.forage.android.pos.services.vault.submission.PosRefundPaymentSubmission
import java.io.File

/**
 * The entry point for **in-store POS Terminal** transactions.
 *
 * A [ForageTerminalSDK] instance interacts with the Forage API.
 *
 * **You need to call [`ForageTerminalSDK.init`][init] to initialize the SDK.**
 * Then you can perform operations like:
 * <br><br>
 * - [Checking the balance of a card][checkBalance]
 * - [Collecting a card PIN for a payment and
 * deferring the capture of the payment to the server][deferPaymentCapture]
 * - [Capturing a payment immediately][capturePayment]
 * - [Collecting a customer's card PIN for a refund and defer the completion of the refund to the
 * server][deferPaymentRefund]
 * - [Refunding a payment immediately][refundPayment]
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
    private val forageConfig: ForageConfig,
    private val ksnFileManager: KsnFileManager,
    private val capabilities: TerminalCapabilities,
    private val _logger: DatadogLogger
) {
    private val traceId = _logger.traceId
    internal val httpEngine = OkHttpEngine()
    internal val paymentMethodService =
        PaymentMethodService(
            forageConfig,
            traceId,
            httpEngine
        )
    internal val paymentService =
        PaymentService(
            forageConfig,
            traceId,
            httpEngine
        )

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
     *     // (e.g. checkBalance, etc.)
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
     * @param ksnDir **Optional**. Specifies the directory where the SDK initializes and stores state for DUKPT
     *   (Derived Unique Key Per Transaction) key generation. By default, this is set to the app's `filesDir`.
     *   Choose a directory that the host application won't frequently modify or delete
     *   to prevent unnecessary reinitialization of the SDK's state.
     * @param capabilities **Optional**. Defines the terminal's capabilities for processing transactions, such as supporting card swiping,
     *   tapping, or inserting. This information is used to report terminal features for compliance purposes.
     *   Most applications can use the default value without modification.
     */
    companion object {
        @Throws(Exception::class)
        suspend fun init(
            context: Context,
            posTerminalId: String,
            forageConfig: ForageConfig,
            ksnDir: File = context.filesDir,
            capabilities: TerminalCapabilities = TerminalCapabilities.TapAndInsert
        ): ForageTerminalSDK {
            val dd = DatadogLogger.getPosDatadogInstance(context, forageConfig)
            val logger = DatadogLogger.forPos(dd, forageConfig, posTerminalId)
            val httpEngine = OkHttpEngine()
            val ksnFileManager = FileKsnManager(ksnDir)
            val rosetta = RosettaInitService(
                forageConfig,
                logger.traceId,
                posTerminalId,
                httpEngine
            )
            val base64Util = AndroidBase64Util()
            val rsaKeyManager = RsaKeyManager(base64Util)
            val keyRegisters = AndroidKeyStoreKeyRegisters()

            PosTerminalInitializer(
                ksnFileManager,
                logger,
                rosetta,
                keyRegisters,
                base64Util,
                rsaKeyManager
            ) { ksn -> DukptService(ksn, keyRegisters) }.safeInit()

            return ForageTerminalSDK(
                posTerminalId,
                forageConfig,
                ksnFileManager,
                capabilities,
                logger
            )
        }
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
     * `paymentMethodRef`, found in the response from a call to the
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
        val (forageVaultElement, paymentMethodRef, interaction) = params
        return PosBalanceCheckSubmission(
            paymentMethodRef = paymentMethodRef,
            vaultSubmitter = forageVaultElement.getVaultSubmitter(forageConfig.envConfig),
            paymentMethodService = paymentMethodService,
            ksnFileManager = ksnFileManager,
            keystoreRegisters = AndroidKeyStoreKeyRegisters(),
            interaction = interaction,
            capabilities = capabilities,
            forageConfig = forageConfig,
            posTerminalId = posTerminalId,
            logLogger = DatadogLogger.forPos(_logger.dd, forageConfig, posTerminalId, _logger.traceId)
        ).submit()
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
        val (forageVaultElement, paymentRef, interaction) = params
        return PosCapturePaymentSubmission(
            paymentRef = paymentRef,
            vaultSubmitter = forageVaultElement.getVaultSubmitter(forageConfig.envConfig),
            paymentMethodService = paymentMethodService,
            paymentService = paymentService,
            ksnFileManager = ksnFileManager,
            keystoreRegisters = AndroidKeyStoreKeyRegisters(),
            interaction = interaction,
            capabilities = capabilities,
            forageConfig = forageConfig,
            posTerminalId = posTerminalId,
            logLogger = DatadogLogger.forPos(_logger.dd, forageConfig, posTerminalId, _logger.traceId)
        ).submit()
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
        val (forageVaultElement, paymentRef, interaction) = params
        return PosDeferCapturePaymentSubmission(
            paymentRef = paymentRef,
            vaultSubmitter = forageVaultElement.getVaultSubmitter(forageConfig.envConfig),
            paymentMethodService = paymentMethodService,
            paymentService = paymentService,
            ksnFileManager = ksnFileManager,
            keystoreRegisters = AndroidKeyStoreKeyRegisters(),
            interaction = interaction,
            capabilities = capabilities,
            forageConfig = forageConfig,
            posTerminalId = posTerminalId,
            logLogger = DatadogLogger.forPos(_logger.dd, forageConfig, posTerminalId, _logger.traceId)
        ).submit()
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
        val (forageVaultElement, paymentRef, amount, reason, metadata, interaction) = params
        return PosRefundPaymentSubmission(
            paymentRef = paymentRef,
            vaultSubmitter = forageVaultElement.getVaultSubmitter(forageConfig.envConfig),
            paymentMethodService = paymentMethodService,
            paymentService = paymentService,
            ksnFileManager = ksnFileManager,
            keystoreRegisters = AndroidKeyStoreKeyRegisters(),
            interaction = interaction,
            capabilities = capabilities,
            forageConfig = forageConfig,
            amount = amount,
            reason = reason,
            metadata = metadata,
            posTerminalId = posTerminalId,
            logLogger = DatadogLogger.forPos(_logger.dd, forageConfig, posTerminalId, _logger.traceId)
        ).submit()
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
        val (forageVaultElement, paymentRef, interaction) = params
        return PosDeferRefundPaymentSubmission(
            paymentRef = paymentRef,
            vaultSubmitter = forageVaultElement.getVaultSubmitter(forageConfig.envConfig),
            paymentMethodService = paymentMethodService,
            paymentService = paymentService,
            ksnFileManager = ksnFileManager,
            keystoreRegisters = AndroidKeyStoreKeyRegisters(),
            interaction = interaction,
            capabilities = capabilities,
            forageConfig = forageConfig,
            posTerminalId = posTerminalId,
            logLogger = DatadogLogger.forPos(_logger.dd, forageConfig, posTerminalId, _logger.traceId)
        ).submit()
    }
}

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
 * found in the response from a call to the
 * [Create a `PaymentMethod`](https://docs.joinforage.app/reference/create-payment-method)
 * endpoint.
 * @property interaction Represents the method of interaction between the cardholder and the terminal.
 * This includes key card details such as PAN (Primary Account Number) and Track 2 data.
 * Use the appropriate implementation of [CardholderInteraction] based on how the card information is obtained.<br>
 * <br>
 * For example:<br>
 *     - `ManualEntryInteraction`: Use when the card details are entered manually.<br>
 *     - `MagSwipeInteraction`: Use when the card is swiped using the magnetic stripe reader.<br>
 */
data class CheckBalanceParams(
    val forageVaultElement: ForageVaultElement<ElementState>,
    val paymentMethodRef: String,
    val interaction: CardholderInteraction
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
 * @property interaction Represents the method of interaction between the cardholder and the terminal.
 * This includes key card details such as PAN (Primary Account Number) and Track 2 data.
 * Use the appropriate implementation of [CardholderInteraction] based on how the card information is obtained.<br>
 * <br>
 * For example:<br>
 *     - `ManualEntryInteraction`: Use when the card details are entered manually.<br>
 *     - `MagSwipeInteraction`: Use when the card is swiped using the magnetic stripe reader.<br>
 */
data class CapturePaymentParams(
    val forageVaultElement: ForageVaultElement<ElementState>,
    val paymentRef: String,
    val interaction: CardholderInteraction
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
 * @property interaction Represents the method of interaction between the cardholder and the terminal.
 * This includes key card details such as PAN (Primary Account Number) and Track 2 data.
 * Use the appropriate implementation of [CardholderInteraction] based on how the card information is obtained.<br>
 * <br>
 * For example:<br>
 *     - `ManualEntryInteraction`: Use when the card details are entered manually.<br>
 *     - `MagSwipeInteraction`: Use when the card is swiped using the magnetic stripe reader.<br>
 */
data class DeferPaymentCaptureParams(
    val forageVaultElement: ForageVaultElement<ElementState>,
    val paymentRef: String,
    val interaction: CardholderInteraction
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
 * @property interaction Represents the method of interaction between the cardholder and the terminal.
 * This includes key card details such as PAN (Primary Account Number) and Track 2 data.
 * Use the appropriate implementation of [CardholderInteraction] based on how the card information is obtained.<br>
 * <br>
 * For example:<br>
 *     - `ManualEntryInteraction`: Use when the card details are entered manually.<br>
 *     - `MagSwipeInteraction`: Use when the card is swiped using the magnetic stripe reader.<br>
 */
data class RefundPaymentParams(
    val forageVaultElement: ForageVaultElement<ElementState>,
    val paymentRef: String,
    val amount: Float,
    val reason: String,
    val metadata: Map<String, String>? = null,
    val interaction: CardholderInteraction
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
 * @property interaction Represents the method of interaction between the cardholder and the terminal.
 * This includes key card details such as PAN (Primary Account Number) and Track 2 data.
 * Use the appropriate implementation of [CardholderInteraction] based on how the card information is obtained.<br>
 * <br>
 * For example:<br>
 *     - `ManualEntryInteraction`: Use when the card details are entered manually.<br>
 *     - `MagSwipeInteraction`: Use when the card is swiped using the magnetic stripe reader.<br>
 */
data class DeferPaymentRefundParams(
    val forageVaultElement: ForageVaultElement<ElementState>,
    val paymentRef: String,
    val interaction: CardholderInteraction
)

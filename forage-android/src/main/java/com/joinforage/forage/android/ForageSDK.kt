package com.joinforage.forage.android

import android.content.Context
import com.joinforage.forage.android.collect.VGSPinCollector
import com.joinforage.forage.android.core.Logger
import com.joinforage.forage.android.model.PanEntry
import com.joinforage.forage.android.model.getPanNumber
import com.joinforage.forage.android.network.CapturePaymentResponseService
import com.joinforage.forage.android.network.CheckBalanceResponseService
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.ForageConstants
import com.joinforage.forage.android.network.MessageStatusService
import com.joinforage.forage.android.network.OkHttpClientBuilder
import com.joinforage.forage.android.network.TokenizeCardService
import com.joinforage.forage.android.network.data.CapturePaymentRepository
import com.joinforage.forage.android.network.data.CheckBalanceRepository
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.ui.ForagePINEditText
import java.util.UUID

/**
 * Singleton responsible for implementing the SDK API
 */
object ForageSDK : ForageSDKApi {
    private var panEntry: PanEntry = PanEntry.Invalid("")
    private val logger = Logger.getInstance(BuildConfig.DEBUG)

    override suspend fun tokenizeEBTCard(
        merchantAccount: String,
        bearerToken: String,
        userId: String
    ): ForageApiResponse<String> {
        val currentEntry = panEntry
        logger.info("Tokenize $currentEntry")

        return when {
            shouldTokenize(currentEntry) -> TokenizeCardService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount,
                    idempotencyKey = UUID.randomUUID().toString()
                ),
                httpUrl = ForageConstants.provideHttpUrl()
            ).tokenizeCard(
                cardNumber = currentEntry.getPanNumber(),
                userId = userId
            )
            else -> ForageApiResponse.Failure(listOf(ForageError(400, "invalid_input_data", "Invalid PAN entry")))
        }
    }

    private fun shouldTokenize(panEntry: PanEntry): Boolean {
        return panEntry is PanEntry.Valid || BuildConfig.DEBUG
    }

    override suspend fun checkBalance(
        context: Context,
        pinForageEditText: ForagePINEditText,
        merchantAccount: String,
        bearerToken: String,
        paymentMethodRef: String,
        cardToken: String
    ): ForageApiResponse<String> {
        return CheckBalanceRepository(
            pinCollector = VGSPinCollector(
                context = context,
                pinForageEditText = pinForageEditText,
                merchantAccount = merchantAccount
            ),
            encryptionKeyService = EncryptionKeyService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(bearerToken, merchantAccount),
                httpUrl = ForageConstants.provideHttpUrl()
            ),
            messageStatusService = MessageStatusService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount
                ),
                httpUrl = ForageConstants.provideHttpUrl()
            ),
            checkBalanceResponseService = CheckBalanceResponseService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount
                ),
                httpUrl = ForageConstants.provideHttpUrl()
            ),
            logger = Logger.getInstance(BuildConfig.DEBUG)
        ).checkBalance(
            paymentMethodRef = paymentMethodRef,
            cardToken = cardToken
        )
    }

    override suspend fun capturePayment(
        context: Context,
        pinForageEditText: ForagePINEditText,
        merchantAccount: String,
        bearerToken: String,
        paymentRef: String,
        cardToken: String
    ): ForageApiResponse<String> {
        return CapturePaymentRepository(
            pinCollector = VGSPinCollector(
                context = context,
                pinForageEditText = pinForageEditText,
                merchantAccount = merchantAccount
            ),
            encryptionKeyService = EncryptionKeyService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(bearerToken, merchantAccount),
                httpUrl = ForageConstants.provideHttpUrl()
            ),
            messageStatusService = MessageStatusService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount
                ),
                httpUrl = ForageConstants.provideHttpUrl()
            ),
            capturePaymentResponseService = CapturePaymentResponseService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount
                ),
                httpUrl = ForageConstants.provideHttpUrl()
            ),
            logger = Logger.getInstance(BuildConfig.DEBUG)
        ).capturePayment(
            paymentRef = paymentRef,
            cardToken = cardToken
        )
    }

    internal fun storeEntry(entry: PanEntry) {
        panEntry = entry
    }
}

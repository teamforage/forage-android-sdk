package com.joinforage.forage.android

import android.content.Context
import com.joinforage.forage.android.core.Log
import com.joinforage.forage.android.model.PanEntry
import com.joinforage.forage.android.model.getPanNumber
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.ForageConstants
import com.joinforage.forage.android.network.MessageStatusService
import com.joinforage.forage.android.network.OkHttpClientBuilder
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.PaymentService
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
    private val logger = Log.getInstance(true)

    override suspend fun tokenizeEBTCard(
        merchantAccount: String,
        bearerToken: String,
        customerId: String
    ): ForageApiResponse<String> {
        val currentEntry = panEntry

        logger.i(
            "Tokenizing Payment Method",
            attributes = mapOf(
                "Merchant" to merchantAccount,
                "Customer ID" to customerId
            )
        )

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
                customerId = customerId
            )
            else -> {
                logger.e("PAN entry was invalid", attributes = mapOf("Merchant" to merchantAccount))
                ForageApiResponse.Failure(listOf(ForageError(400, "invalid_input_data", "Invalid PAN entry")))
            }
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
        paymentMethodRef: String
    ): ForageApiResponse<String> {
        logger.i(
            "Submitting balance check for Payment Method $paymentMethodRef",
            attributes = mapOf(
                "Merchant" to merchantAccount
            )
        )
        return CheckBalanceRepository(
            pinCollector = pinForageEditText.getCollector(
                merchantAccount
            ),
            encryptionKeyService = EncryptionKeyService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount
                ),
                httpUrl = ForageConstants.provideHttpUrl()
            ),
            paymentMethodService = PaymentMethodService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount
                ),
                httpUrl = ForageConstants.provideHttpUrl()
            ),
            messageStatusService = MessageStatusService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount
                ),
                httpUrl = ForageConstants.provideHttpUrl()
            ),
            logger = logger
        ).checkBalance(
            paymentMethodRef = paymentMethodRef
        )
    }

    override suspend fun capturePayment(
        context: Context,
        pinForageEditText: ForagePINEditText,
        merchantAccount: String,
        bearerToken: String,
        paymentRef: String
    ): ForageApiResponse<String> {
        logger.i(
            "Submitting capture request for Payment $paymentRef",
            attributes = mapOf(
                "Merchant" to merchantAccount
            )
        )
        return CapturePaymentRepository(
            pinCollector = pinForageEditText.getCollector(
                merchantAccount
            ),
            encryptionKeyService = EncryptionKeyService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount
                ),
                httpUrl = ForageConstants.provideHttpUrl()
            ),
            paymentService = PaymentService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount
                ),
                httpUrl = ForageConstants.provideHttpUrl()
            ),
            paymentMethodService = PaymentMethodService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount
                ),
                httpUrl = ForageConstants.provideHttpUrl()
            ),
            messageStatusService = MessageStatusService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount
                ),
                httpUrl = ForageConstants.provideHttpUrl()
            ),
            logger = logger
        ).capturePayment(
            paymentRef = paymentRef
        )
    }

    internal fun storeEntry(entry: PanEntry) {
        panEntry = entry
    }
}

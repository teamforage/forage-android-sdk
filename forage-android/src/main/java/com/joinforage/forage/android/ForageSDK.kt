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
import com.joinforage.forage.android.ui.ForagePANEditText
import com.joinforage.forage.android.ui.ForagePINEditText
import java.util.UUID

/**
 * Singleton responsible for implementing the SDK API
 */
object ForageSDK : ForageSDKApi {
    private val logger = Log.getInstance()

    // TODO: this should be a Config argument that uses the builder pattern
    override suspend fun tokenizeEBTCard(
        merchantAccount: String,
        panForageEditText: ForagePANEditText,
        bearerToken: String,
        customerId: String,
        reusable: Boolean
    ): ForageApiResponse<String> {
        logger.i(
            "Tokenizing Payment Method",
            attributes = mapOf(
                "merchant_ref" to merchantAccount,
                "customer_id" to customerId
            )
        )

        return when {
            panForageEditText.shouldTokenize() -> TokenizeCardService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount,
                    idempotencyKey = UUID.randomUUID().toString()
                ),
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ).tokenizeCard(
                cardNumber = panForageEditText.getPanNumber(),
                customerId = customerId,
                reusable = reusable
            )
            else -> {
                logger.e(
                    "PAN entry was invalid",
                    attributes = mapOf(
                        "merchant_ref" to merchantAccount,
                        "customer_id" to customerId
                    )
                )
                ForageApiResponse.Failure(listOf(ForageError(400, "invalid_input_data", "Invalid PAN entry")))
            }
        }
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
                "merchant_ref" to merchantAccount,
                "payment_method_ref" to paymentMethodRef
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
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            paymentMethodService = PaymentMethodService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount
                ),
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            messageStatusService = MessageStatusService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount
                ),
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
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
                "merchant_ref" to merchantAccount,
                "payment_ref" to paymentRef
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
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            paymentService = PaymentService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount
                ),
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            paymentMethodService = PaymentMethodService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount
                ),
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            messageStatusService = MessageStatusService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount
                ),
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            logger = logger
        ).capturePayment(
            paymentRef = paymentRef
        )
    }
}

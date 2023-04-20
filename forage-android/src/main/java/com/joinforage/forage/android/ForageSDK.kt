package com.joinforage.forage.android

import android.app.Application
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
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.PaymentService
import com.joinforage.forage.android.network.TokenizeCardService
import com.joinforage.forage.android.network.data.CapturePaymentRepository
import com.joinforage.forage.android.network.data.CheckBalanceRepository
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.ui.ForagePINEditText
import com.launchdarkly.sdk.LDContext
import com.launchdarkly.sdk.android.LDClient
import com.launchdarkly.sdk.android.LDConfig
import java.util.UUID

internal object VaultConstants {
    const val VGS_VAULT_TYPE = "vgs_vault_type"
    const val BT_VAULT_TYPE = "bt_vault_type"
}

internal object LDConstants {
    const val VAULT_TYPE_FLAG = "vault-primary-traffic-percentage"
    const val USER = "anonymous-user"
}

/**
 * Singleton responsible for implementing the SDK API
 */
object ForageSDK : ForageSDKApi {
    private var panEntry: PanEntry = PanEntry.Invalid("")
    private var vaultType: String? = null
    private const val LD_MOBILE_KEY = BuildConfig.LD_MOBILE_KEY

    // vaultType is instantiated lazily and is a singleton. Once we set the vault type once, we don't
    // want to overwrite it! We must take in the application as a parameter, which means that a
    // ForagePINEditText must be rendered before any of the ForageSDKApi functions are called.
    fun getVaultProvider(app: Application): String {
        if (vaultType != null) {
            return vaultType as String
        }
        val ldConfig: LDConfig = LDConfig.Builder()
            .mobileKey(LD_MOBILE_KEY)
            .build()
        val context = LDContext.create(LDConstants.USER)
        val client = LDClient.init(app, ldConfig, context, 0)
        val vaultPercent = client.doubleVariation(LDConstants.VAULT_TYPE_FLAG, 0.0)
        val randomNum = Math.random() * 100
        vaultType = VaultConstants.BT_VAULT_TYPE
        if (randomNum < vaultPercent) {
            vaultType = VaultConstants.VGS_VAULT_TYPE
        }
        return vaultType as String
    }

    override suspend fun tokenizeEBTCard(
        merchantAccount: String,
        bearerToken: String
    ): ForageApiResponse<String> {
        val currentEntry = panEntry

        return when {
            shouldTokenize(currentEntry) -> TokenizeCardService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount,
                    idempotencyKey = UUID.randomUUID().toString()
                ),
                httpUrl = ForageConstants.provideHttpUrl()
            ).tokenizeCard(
                cardNumber = currentEntry.getPanNumber()
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
        paymentMethodRef: String
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
            checkBalanceResponseService = CheckBalanceResponseService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount
                ),
                httpUrl = ForageConstants.provideHttpUrl()
            ),
            logger = Logger.getInstance(BuildConfig.DEBUG)
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
            capturePaymentResponseService = CapturePaymentResponseService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount
                ),
                httpUrl = ForageConstants.provideHttpUrl()
            ),
            logger = Logger.getInstance(BuildConfig.DEBUG)
        ).capturePayment(
            paymentRef = paymentRef
        )
    }

    internal fun storeEntry(entry: PanEntry) {
        panEntry = entry
    }
}

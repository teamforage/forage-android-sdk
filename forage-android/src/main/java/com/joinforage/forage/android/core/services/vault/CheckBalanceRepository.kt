package com.joinforage.forage.android.core.services.vault

import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeyService
import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeys
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.core.services.forageapi.polling.Message
import com.joinforage.forage.android.core.services.forageapi.polling.PollingService
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.telemetry.UserAction
import java.util.UUID

internal open class CheckBalanceRepository(
    private val vaultSubmitter: VaultSubmitter,
    private val encryptionKeyService: EncryptionKeyService,
    private val paymentMethodService: PaymentMethodService,
    private val pollingService: PollingService,
    protected val logger: Log
) {
    suspend fun checkBalance(
        merchantId: String,
        sessionToken: String,
        paymentMethodRef: String,
        getVaultRequestParams: ((EncryptionKeys, PaymentMethod) -> VaultSubmitterParams) = { encryptionKeys, paymentMethod ->
            buildVaultRequestParams(
                merchantId = merchantId,
                encryptionKeys = encryptionKeys,
                paymentMethod = paymentMethod,
                sessionToken = sessionToken
            )
        }
    ): ForageApiResponse<String> {
        val encryptionKeys = when (val response = encryptionKeyService.getEncryptionKey()) {
            is ForageApiResponse.Success -> EncryptionKeys.ModelMapper.from(response.data)
            else -> return response
        }
        val paymentMethod = when (val response = paymentMethodService.getPaymentMethod(paymentMethodRef)) {
            is ForageApiResponse.Success -> PaymentMethod(response.data)
            else -> return response
        }

        val vaultResponse = when (
            val response = vaultSubmitter.submit(
                getVaultRequestParams(encryptionKeys, paymentMethod)
            )
        ) {
            is ForageApiResponse.Success -> Message.ModelMapper.from(response.data)
            else -> return response
        }

        val pollingResponse = pollingService.execute(
            contentId = vaultResponse.contentId,
            operationDescription = "balance check of PaymentMethod $paymentMethodRef"
        )
        if (pollingResponse is ForageApiResponse.Failure) {
            return pollingResponse
        }

        return when (val paymentMethodResponse = paymentMethodService.getPaymentMethod(paymentMethodRef)) {
            is ForageApiResponse.Success -> {
                logger.i("[HTTP] Received updated balance information for Payment Method $paymentMethodRef")
                val paymentMethodWithBalance = PaymentMethod(paymentMethodResponse.data)
                return ForageApiResponse.Success(paymentMethodWithBalance.balance.toString())
            }
            else -> paymentMethodResponse
        }
    }

    protected fun buildVaultRequestParams(
        merchantId: String,
        encryptionKeys: EncryptionKeys,
        paymentMethod: PaymentMethod,
        sessionToken: String
    ): VaultSubmitterParams {
        return VaultSubmitterParams(
            encryptionKeys = encryptionKeys,
            idempotencyKey = UUID.randomUUID().toString(),
            merchantId = merchantId,
            path = AbstractVaultSubmitter.balancePath(paymentMethod.ref),
            paymentMethod = paymentMethod,
            sessionToken = sessionToken,
            userAction = UserAction.BALANCE
        )
    }
}

package com.joinforage.forage.android.core.services.vault

import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeyService
import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeys
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtBalance
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.core.services.forageapi.polling.PollingService
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.telemetry.UserAction
import java.util.UUID

internal class CheckBalanceRepository(
    private val vaultSubmitter: VaultSubmitter,
    private val encryptionKeyService: EncryptionKeyService,
    private val paymentMethodService: PaymentMethodService,
    private val pollingService: PollingService,
    private val logger: Log
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

        val balanceResponse = when (
            val response = vaultSubmitter.submit(
                getVaultRequestParams(encryptionKeys, paymentMethod)
            )
        ) {
            is ForageApiResponse.Success -> EbtBalance.fromVaultResponse(response)
            else -> return response
        }

        logger.i("[HTTP] Received updated balance information for Payment Method $paymentMethodRef")
        return balanceResponse.toForageApiResponse()
    }

    private fun buildVaultRequestParams(
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

package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.core.telemetry.UserAction
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.PollingService
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.Message
import com.joinforage.forage.android.pos.PosBalanceVaultSubmitterParams
import com.joinforage.forage.android.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.vault.VaultSubmitter
import com.joinforage.forage.android.vault.VaultSubmitterParams
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
            is ForageApiResponse.Success -> PaymentMethod.ModelMapper.from(response.data)
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
                val paymentMethodWithBalance = PaymentMethod.ModelMapper.from(paymentMethodResponse.data)
                return ForageApiResponse.Success(paymentMethodWithBalance.balance.toString())
            }
            else -> paymentMethodResponse
        }
    }

    suspend fun posCheckBalance(
        merchantId: String,
        paymentMethodRef: String,
        posTerminalId: String,
        sessionToken: String
    ): ForageApiResponse<String> {
        return checkBalance(
            merchantId = merchantId,
            paymentMethodRef = paymentMethodRef,
            sessionToken = sessionToken,
            getVaultRequestParams = { encryptionKeys, paymentMethod ->
                PosBalanceVaultSubmitterParams(
                    baseVaultSubmitterParams = buildVaultRequestParams(
                        merchantId = merchantId,
                        encryptionKeys = encryptionKeys,
                        paymentMethod = paymentMethod,
                        sessionToken = sessionToken
                    ),
                    posTerminalId = posTerminalId
                )
            }
        )
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

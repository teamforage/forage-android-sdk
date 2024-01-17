package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.collect.PinCollector
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.PollingService
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.Message
import com.joinforage.forage.android.pos.PosVaultRequestParams

internal class CheckBalanceRepository(
    private val pinCollector: PinCollector,
    private val encryptionKeyService: EncryptionKeyService,
    private val paymentMethodService: PaymentMethodService,
    private val pollingService: PollingService,
    private val logger: Log
) {
    suspend fun checkBalance(
        paymentMethodRef: String
    ): ForageApiResponse<String> {
        return when (val response = encryptionKeyService.getEncryptionKey()) {
            is ForageApiResponse.Success -> getTokenFromPaymentMethod(
                paymentMethodRef = paymentMethodRef,
                encryptionKey = pinCollector.parseEncryptionKey(
                    EncryptionKeys.ModelMapper.from(response.data)
                )
            )
            else -> response
        }
    }

    suspend fun posCheckBalance(paymentMethodRef: String, posTerminalId: String): ForageApiResponse<String> {
        return when (val response = encryptionKeyService.getEncryptionKey()) {
            is ForageApiResponse.Success -> posGetTokenFromPaymentMethod(
                paymentMethodRef = paymentMethodRef,
                encryptionKey = pinCollector.parseEncryptionKey(
                    EncryptionKeys.ModelMapper.from(response.data)
                ),
                posTerminalId = posTerminalId
            )
            else -> response
        }
    }

    private suspend fun getTokenFromPaymentMethod(
        paymentMethodRef: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        return when (val response = paymentMethodService.getPaymentMethod(paymentMethodRef)) {
            is ForageApiResponse.Success -> submitBalanceCheck(
                paymentMethodRef = paymentMethodRef,
                vaultRequestParams = BaseVaultRequestParams(
                    cardNumberToken = pinCollector.parseVaultToken(PaymentMethod.ModelMapper.from(response.data)),
                    encryptionKey = encryptionKey
                )
            )
            else -> response
        }
    }

    private suspend fun posGetTokenFromPaymentMethod(
        paymentMethodRef: String,
        encryptionKey: String,
        posTerminalId: String
    ): ForageApiResponse<String> {
        return when (val response = paymentMethodService.getPaymentMethod(paymentMethodRef)) {
            is ForageApiResponse.Success -> submitBalanceCheck(
                paymentMethodRef = paymentMethodRef,
                vaultRequestParams = PosVaultRequestParams(
                    cardNumberToken = pinCollector.parseVaultToken(
                        PaymentMethod.ModelMapper.from(
                            response.data
                        )
                    ),
                    encryptionKey = encryptionKey,
                    posTerminalId = posTerminalId
                )
            )
            else -> response
        }
    }

    private suspend fun submitBalanceCheck(
        paymentMethodRef: String,
        vaultRequestParams: BaseVaultRequestParams
    ): ForageApiResponse<String> {
        val response = pinCollector.submitBalanceCheck(
            paymentMethodRef = paymentMethodRef,
            vaultRequestParams = vaultRequestParams
        )

        return when (response) {
            is ForageApiResponse.Success -> pollingBalanceMessageStatus(
                contentId = Message.ModelMapper.from(response.data).contentId,
                paymentMethodRef = paymentMethodRef
            )
            else -> response
        }
    }

    private suspend fun pollingBalanceMessageStatus(
        contentId: String,
        paymentMethodRef: String
    ): ForageApiResponse<String> {
        logger.addAttribute("content_id", contentId)

        val pollingResponse = pollingService.execute(
            contentId = contentId,
            operationDescription = "balance check of Payment Method $paymentMethodRef"
        )
        if (pollingResponse is ForageApiResponse.Failure) {
            return pollingResponse
        }

        return when (val paymentMethodResponse = paymentMethodService.getPaymentMethod(paymentMethodRef)) {
            is ForageApiResponse.Success -> {
                logger.i(
                    "[HTTP] Received updated balance information for Payment Method $paymentMethodRef",
                    attributes = mapOf(
                        "payment_method_ref" to paymentMethodRef,
                        "content_id" to contentId
                    )
                )
                val paymentMethod = PaymentMethod.ModelMapper.from(paymentMethodResponse.data)
                return ForageApiResponse.Success(paymentMethod.balance.toString())
            }
            else -> paymentMethodResponse
        }
    }

    companion object {
        private fun ForageApiResponse<String>.getStringResponse() = when (this) {
            is ForageApiResponse.Failure -> this.errors[0].message
            is ForageApiResponse.Success -> this.data
        }
    }
}

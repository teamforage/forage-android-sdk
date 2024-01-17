package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.collect.AbstractVaultSubmitter
import com.joinforage.forage.android.collect.VaultSubmitter
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.core.telemetry.UserAction
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.PollingService
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.Message
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
        paymentMethodRef: String
    ): ForageApiResponse<String> {
        val encryptionKeys = when (val response = encryptionKeyService.getEncryptionKey()) {
            is ForageApiResponse.Success -> EncryptionKeys.ModelMapper.from(response.data)
            else -> return response
        }
        val paymentMethod = when (val response = paymentMethodService.getPaymentMethod(paymentMethodRef)) {
            is ForageApiResponse.Success -> PaymentMethod.ModelMapper.from(response.data)
            else -> return response
        }

       val vaultResponse = when (val response = vaultSubmitter.submit(
           encryptionKeys = encryptionKeys,
           idempotencyKey = UUID.randomUUID().toString(),
           merchantId = merchantId,
           path = AbstractVaultSubmitter.balancePath(paymentMethodRef),
           userAction = UserAction.BALANCE,
           paymentMethod = paymentMethod
       )) {
           is ForageApiResponse.Success -> response
           else -> return response
       }

        val pollingResponse = pollingService.execute(
            contentId = Message.ModelMapper.from(vaultResponse.data).contentId,
            operationDescription = "balance check of Payment Method $paymentMethodRef"
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

    suspend fun posCheckBalance(paymentMethodRef: String, posTerminalId: String): ForageApiResponse<String> {
        // TODO!
        return ForageApiResponse.Success("TODO")
    }
//    suspend fun posCheckBalance(paymentMethodRef: String, posTerminalId: String): ForageApiResponse<String> {
//        return when (val response = encryptionKeyService.getEncryptionKey()) {
//            is ForageApiResponse.Success -> posGetTokenFromPaymentMethod(
//                paymentMethodRef = paymentMethodRef,
//                encryptionKey = vaultSubmitter.parseEncryptionKey(
//                    EncryptionKeys.ModelMapper.from(response.data)
//                ),
//                posTerminalId = posTerminalId
//            )
//            else -> response
//        }
//    }
//
//    private suspend fun getTokenFromPaymentMethod(
//        paymentMethodRef: String,
//        encryptionKey: String
//    ): ForageApiResponse<String> {
//        return when (val response = paymentMethodService.getPaymentMethod(paymentMethodRef)) {
//            is ForageApiResponse.Success ->
//            else -> response
//        }
//    }
//
//    private suspend fun posGetTokenFromPaymentMethod(
//        paymentMethodRef: String,
//        encryptionKey: String,
//        posTerminalId: String
//    ): ForageApiResponse<String> {
//        return when (val response = paymentMethodService.getPaymentMethod(paymentMethodRef)) {
//            is ForageApiResponse.Success -> vaultSubmitter.submit()
//            else -> response
//        }
//    }
//
//    private suspend fun submitBalanceCheck(
//        paymentMethodRef: String,
//        vaultRequestParams: BaseVaultRequestParams
//    ): ForageApiResponse<String> {
//        val response = pinCollector.submitBalanceCheck(
//            paymentMethodRef = paymentMethodRef,
//            vaultRequestParams = vaultRequestParams
//        )
//
//        return when (response) {
//            is ForageApiResponse.Success -> pollingBalanceMessageStatus(
//                contentId = Message.ModelMapper.from(response.data).contentId,
//                paymentMethodRef = paymentMethodRef
//            )
//            else -> response
//        }
//    }
//
//    private suspend fun pollingBalanceMessageStatus(
//        contentId: String,
//        paymentMethodRef: String
//    ): ForageApiResponse<String> {
//        val pollingResponse = pollingService.execute(
//            contentId = contentId,
//            operationDescription = "balance check of Payment Method $paymentMethodRef"
//        )
//        if (pollingResponse is ForageApiResponse.Failure) {
//            return pollingResponse
//        }
//
//        return when (val paymentMethodResponse = paymentMethodService.getPaymentMethod(paymentMethodRef)) {
//            is ForageApiResponse.Success -> {
//                logger.i(
//                    "[HTTP] Received updated balance information for Payment Method $paymentMethodRef",
//                    attributes = mapOf(
//                        "payment_method_ref" to paymentMethodRef,
//                        "content_id" to contentId
//                    )
//                )
//                val paymentMethod = PaymentMethod.ModelMapper.from(paymentMethodResponse.data)
//                return ForageApiResponse.Success(paymentMethod.balance.toString())
//            }
//            else -> paymentMethodResponse
//        }
//    }
//
//    companion object {
//        private fun ForageApiResponse<String>.getStringResponse() = when (this) {
//            is ForageApiResponse.Failure -> this.errors[0].message
//            is ForageApiResponse.Success -> this.data
//        }
//    }
}

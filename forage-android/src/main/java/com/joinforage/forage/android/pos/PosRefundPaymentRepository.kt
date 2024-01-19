package com.joinforage.forage.android.pos

import com.joinforage.forage.android.collect.AbstractVaultSubmitter
import com.joinforage.forage.android.collect.VaultSubmitter
import com.joinforage.forage.android.collect.VaultSubmitterParams
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.core.telemetry.UserAction
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.Payment
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.PaymentService
import com.joinforage.forage.android.network.PollingService
import com.joinforage.forage.android.network.model.ForageApiResponse

internal class PosRefundPaymentRepository(
    private val vaultSubmitter: VaultSubmitter,
    private val encryptionKeyService: EncryptionKeyService,
    private val paymentMethodService: PaymentMethodService,
    private val paymentService: PaymentService,
    private val pollingService: PollingService,
    private val refundService: PosRefundService,
    private val logger: Log
) {
    /**
     * @return the ForageAPIResponse containing the Refund object on success or an error on failure.
     */
    suspend fun refundPayment(
        merchantId: String,
        posTerminalId: String,
        refundParams: RefundPaymentParams
    ): ForageApiResponse<String> {
        val paymentRef = refundParams.paymentRef

        val encryptionKeys = when (val response = encryptionKeyService.getEncryptionKey()) {
            is ForageApiResponse.Success -> EncryptionKeys.ModelMapper.from(response.data)
            else -> return response
        }
        val payment = when (val response = paymentService.getPayment(paymentRef)) {
            is ForageApiResponse.Success -> Payment.ModelMapper.from(response.data)
            else -> return response
        }
        val paymentMethod = when (val response = paymentMethodService.getPaymentMethod(payment.paymentMethod)) {
            is ForageApiResponse.Success -> PaymentMethod.ModelMapper.from(response.data)
            else -> return response
        }

        val vaultResponse = when (
            val response = vaultSubmitter.submit(
                params = PosRefundVaultSubmitterParams(
                    baseVaultSubmitterParams = VaultSubmitterParams(
                        encryptionKeys = encryptionKeys,
                        idempotencyKey = paymentRef,
                        merchantId = merchantId,
                        path = AbstractVaultSubmitter.refundPaymentPath(paymentRef),
                        paymentMethod = paymentMethod,
                        userAction = UserAction.REFUND
                    ),
                    posTerminalId = posTerminalId,
                    refundParams = refundParams
                )
            )
        ) {
            is ForageApiResponse.Success -> PosRefundVaultResponse.ModelMapper.from(response.data)
            else -> return response
        }

        val pollingResponse = pollingService.execute(
            contentId = vaultResponse.message.contentId,
            operationDescription = "refund of Payment $payment"
        )
        if (pollingResponse is ForageApiResponse.Failure) {
            return pollingResponse
        }

        val refundRef = vaultResponse.refundRef
        return when (val refundResponse = refundService.getRefund(paymentRef, refundRef)) {
            is ForageApiResponse.Success -> {
                logger.i("[HTTP] Received updated Refund $refundRef for Payment $paymentRef")
                return refundResponse
            }
            else -> refundResponse
        }
    }
}

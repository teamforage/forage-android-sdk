package com.joinforage.forage.android.pos.services.vault

import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeyService
import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeys
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.core.services.forageapi.payment.Payment
import com.joinforage.forage.android.core.services.forageapi.payment.PaymentService
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.core.services.vault.VaultSubmitter
import com.joinforage.forage.android.core.services.vault.VaultSubmitterParams
import com.joinforage.forage.android.pos.services.RefundPaymentParams
import com.joinforage.forage.android.pos.services.forageapi.refund.PosRefundService
import com.joinforage.forage.android.pos.services.forageapi.refund.Refund
import com.joinforage.forage.android.pos.services.vault.rosetta.PosRefundVaultSubmitterParams

internal class PosRefundPaymentRepository(
    private val vaultSubmitter: VaultSubmitter,
    private val encryptionKeyService: EncryptionKeyService,
    private val paymentMethodService: PaymentMethodService,
    private val paymentService: PaymentService,
    private val refundService: PosRefundService,
    private val logger: Log
) {
    /**
     * @return the ForageAPIResponse containing the Refund object on success or an error on failure.
     */
    suspend fun refundPayment(
        merchantId: String,
        posTerminalId: String,
        refundParams: RefundPaymentParams,
        sessionToken: String
    ): ForageApiResponse<String> {
        try {
            val paymentRef = refundParams.paymentRef

            val encryptionKeys = when (val response = encryptionKeyService.getEncryptionKey()) {
                is ForageApiResponse.Success -> EncryptionKeys.ModelMapper.from(response.data)
                else -> return response
            }
            val payment = when (val response = paymentService.getPayment(paymentRef)) {
                is ForageApiResponse.Success -> Payment(response.data)
                else -> return response
            }
            val paymentMethod = when (val response = paymentMethodService.getPaymentMethod(payment.paymentMethodRef)) {
                is ForageApiResponse.Success -> PaymentMethod(response.data)
                else -> return response
            }

            val response = vaultSubmitter.submit(
                params = PosRefundVaultSubmitterParams(
                    baseVaultSubmitterParams = VaultSubmitterParams(
                        encryptionKeys = encryptionKeys,
                        idempotencyKey = paymentRef,
                        merchantId = merchantId,
                        path = AbstractVaultSubmitter.refundPaymentPath(paymentRef),
                        paymentMethod = paymentMethod,
                        userAction = UserAction.REFUND,
                        sessionToken = sessionToken
                    ),
                    posTerminalId = posTerminalId,
                    refundParams = refundParams
                )
            )

            return when (response) {
                is ForageApiResponse.Success -> {
                    val refund = Refund(response.data)
                    logger.i("[HTTP] Received updated Refund ${refund.ref} for Payment $paymentRef")
                    return response
                }
                is ForageApiResponse.Failure -> {
                    logger.e(
                        "[POS] refundPayment failed for Payment $paymentRef on Terminal $posTerminalId: ${response.error}"
                    )
                    response
                }
            }
        } catch (err: Exception) {
            logger.e("Failed to refund Payment ${refundParams.paymentRef}", err)
            return UnknownErrorApiResponse
        }
    }
}

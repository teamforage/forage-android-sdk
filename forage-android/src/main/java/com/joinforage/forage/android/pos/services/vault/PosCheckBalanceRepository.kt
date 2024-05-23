package com.joinforage.forage.android.pos.services.vault

import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeyService
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.core.services.forageapi.polling.PollingService
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.CheckBalanceRepository
import com.joinforage.forage.android.core.services.vault.VaultSubmitter
import com.joinforage.forage.android.pos.services.vault.rosetta.PosBalanceVaultSubmitterParams

internal class PosCheckBalanceRepository(
    vaultSubmitter: VaultSubmitter,
    encryptionKeyService: EncryptionKeyService,
    paymentMethodService: PaymentMethodService,
    pollingService: PollingService,
    logger: Log
) : CheckBalanceRepository(
    vaultSubmitter,
encryptionKeyService,
paymentMethodService,
pollingService,
logger,
) {

    suspend fun posCheckBalance(
        merchantId: String,
        paymentMethodRef: String,
        posTerminalId: String,
        sessionToken: String
    ): ForageApiResponse<String> {
        val response = checkBalance(
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

        if (response is ForageApiResponse.Failure) {
            logger.e(
                "[POS] checkBalance failed for PaymentMethod $paymentMethodRef on Terminal $posTerminalId: ${response.errors[0]}",
                attributes =
                mapOf(
                    "payment_method_ref" to paymentMethodRef,
                    "pos_terminal_id" to posTerminalId
                )
            )
        }
        return response
    }

}

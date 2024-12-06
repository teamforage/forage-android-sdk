package com.joinforage.forage.android.pos.services.vault.submission

import com.joinforage.forage.android.core.services.forageapi.ClientApiRequest
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.vault.RosettaPinSubmitter
import com.joinforage.forage.android.core.services.vault.VaultPaymentMethod
import com.joinforage.forage.android.core.services.vault.requests.ISubmitRequestBuilder
import com.joinforage.forage.android.pos.services.CardholderInteraction
import com.joinforage.forage.android.pos.services.encryption.dukpt.DukptService
import com.joinforage.forage.android.pos.services.encryption.dukpt.SecureKeyStorageRegisters
import com.joinforage.forage.android.pos.services.encryption.iso4.PinBlockIso4
import com.joinforage.forage.android.pos.services.encryption.storage.KsnFileManager
import com.joinforage.forage.android.pos.services.vault.requests.IPosBuildRequestDelegate

internal class PosSubmitRequestBuilder(
    private val ksnFileManager: KsnFileManager,
    private val keystoreRegisters: SecureKeyStorageRegisters,
    private val interaction: CardholderInteraction,
    private val delegate: IPosBuildRequestDelegate
) : ISubmitRequestBuilder {
    override suspend fun buildRequest(
        paymentMethod: PaymentMethod,
        idempotencyKey: String,
        traceId: String,
        vaultSubmitter: RosettaPinSubmitter
    ): ClientApiRequest {
        val vaultPaymentMethod = VaultPaymentMethod(
            ref = paymentMethod.ref,
            token = vaultSubmitter.getVaultToken(paymentMethod)
        )

        val pinTranslationParams = buildPinTranslationParams(
            paymentMethod.fullPan,
            vaultSubmitter.plainTextPin
        )

        return delegate.buildRequest(
            vaultPaymentMethod,
            idempotencyKey,
            traceId,
            pinTranslationParams,
            interaction
        )
    }

    private fun buildPinTranslationParams(
        plainTextPan: String,
        plainTextPin: String
    ): PinTranslationParams {
        val ksn = ksnFileManager.readAll()
        val dukptService = DukptService(ksn, keystoreRegisters)
        val (workingKey, latestKsn) = dukptService.generateWorkingKey()
        ksnFileManager.updateKsn(latestKsn)
        return PinTranslationParams(
            encryptedPinBlock = PinBlockIso4(plainTextPan, plainTextPin, workingKey)
                .contents.toHexString().uppercase(),
            keySerialNumber = latestKsn.apcKsn,
            txnCounter = latestKsn.workingKeyTxCountAsBigEndian8CharHex
        )
    }
}

package com.joinforage.forage.android.pos

import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.pos.encryption.AesBlock
import com.joinforage.forage.android.pos.encryption.CertificateSigningRequest
import com.joinforage.forage.android.pos.encryption.CertificateSigningResponse
import com.joinforage.forage.android.pos.encryption.InitializePosRequest
import com.joinforage.forage.android.pos.encryption.InitializePosResponse
import com.joinforage.forage.android.pos.encryption.RosettaProxyApi
import com.joinforage.forage.android.pos.encryption.certificate.RsaKeyManager
import com.joinforage.forage.android.pos.encryption.dukpt.DukptService
import com.joinforage.forage.android.pos.encryption.storage.AndroidKeyStoreKeyRegisters
import com.joinforage.forage.android.pos.encryption.storage.KeySerialNumber
import com.joinforage.forage.android.pos.encryption.storage.KsnFileManager

internal class PosInitializationException(
    val reason: String,
    throwable: Throwable? = null
) : Exception("Failed to initialize POS terminal. $reason", throwable)

internal interface PosInitializer {
    @Throws(PosInitializationException::class)
    suspend fun execute(
        posTerminalId: String,
        merchantId: String,
        sessionToken: String
    )
}

/**
 * Handles the initialization steps for the [com.joinforage.forage.android.pos.ForageTerminalSDK.init] method.
 */
@RequiresApi(Build.VERSION_CODES.M)
internal class PosTerminalInitializer(
    private val ksnManager: KsnFileManager,
    private val logger: Log
) : PosInitializer {
    @Throws(PosInitializationException::class)
    override suspend fun execute(
        posTerminalId: String,
        merchantId: String,
        sessionToken: String
    ) {
        try {
            if (isKsnFileAvailable()) {
                logger.i("[POS] KSN file already exists on the device. Skipping initialization.")
                return
            }
            val rsaKeyManager = initRsaKeyManager()
            val base64encodedCsr = getBase64CSR(rsaKeyManager)

            val rosettaApi = RosettaProxyApi.from(
                posTerminalId = posTerminalId,
                PosForageConfig(
                    sessionToken = sessionToken,
                    merchantId = merchantId
                )
            )

            val response = signCertificate(rosettaApi, base64encodedCsr)
            val initializePosResponse = initializePos(
                rosettaApi,
                base64PublicKeyPEM = response.signedCertificate
            )
            val ksnStr = initializePosResponse.keySerialNumber
            ksnManager.init(ksnStr) // store the KSN in the KSN file

            val dukptService = DukptService(
                ksn = KeySerialNumber(ksnStr),
                keyRegisters = AndroidKeyStoreKeyRegisters()
            )

            val base64EncryptedIpek = Base64.decode(initializePosResponse.base64EncryptedIpek, Base64.DEFAULT)
            val decryptedInitialDerivationKey = rsaKeyManager.decrypt(base64EncryptedIpek)
            rsaKeyManager.deleteKeyPair()

            val ksn = dukptService.loadKey(AesBlock(decryptedInitialDerivationKey))
            ksnManager.updateKsn(ksn)
        } catch (e: Exception) {
            logger.e("Failed to initialize the ForageTerminalSDK", e)
            throw e
        }
    }

    companion object {
        private fun initRsaKeyManager(): RsaKeyManager {
            try {
                return RsaKeyManager.fromAndroidKeystore()
            } catch (e: Exception) {
                throw PosInitializationException("Failed to initialize RSA Key Manager", e)
            }
        }
    }

    /**
     *
     * @return true if the KSN file already exists, false otherwise.
     */
    @Throws(PosInitializationException::class)
    private fun isKsnFileAvailable(): Boolean {
        return try {
            ksnManager.isInitialized()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generates a Certificate Signing Request (CSR) using the RsaKeyManager
     * @return a base-64 encoded CSR
     */
    @Throws(PosInitializationException::class)
    private fun getBase64CSR(rsaKeyManager: RsaKeyManager): String {
        try {
            return rsaKeyManager.generateCSRBase64()
        } catch (e: Exception) {
            throw PosInitializationException("Failed to generate CSR", e)
        }
    }

    @Throws(PosInitializationException::class)
    private suspend fun signCertificate(
        rosettaApi: RosettaProxyApi,
        csr: String
    ): CertificateSigningResponse {
        try {
            logger.i("[Rosetta] Signing certificate with /api/terminal/certificate/")
            return rosettaApi.signCertificate(
                CertificateSigningRequest(csr)
            )
        } catch (e: Exception) {
            throw PosInitializationException("Failed to sign certificate", e)
        }
    }

    @Throws(PosInitializationException::class)
    private suspend fun initializePos(
        rosettaApi: RosettaProxyApi,
        base64PublicKeyPEM: String
    ): InitializePosResponse {
        try {
            logger.i("[Rosetta] Initializing POS terminal with /api/terminal/initialize")

            return rosettaApi.initializePos(
                InitializePosRequest(base64PublicKeyPEM = base64PublicKeyPEM)
            )
        } catch (e: Exception) {
            throw PosInitializationException("Failed to initialize POS terminal with Rosetta", e)
        }
    }
}

package com.joinforage.forage.android.pos.keys

import android.os.Build
import androidx.annotation.RequiresApi
import com.joinforage.forage.android.core.EnvConfig
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.pos.PosForageConfig

internal class PosInitializationException(
    val reason: String,
    throwable: Throwable? = null
) : Exception("Failed to initialize POS terminal. $reason", throwable)

internal interface PosInitializer {
    @Throws(PosInitializationException::class)
    suspend fun execute(
        merchantId: String,
        sessionToken: String
    )
}

/**
 * Handles the initialization steps for the [com.joinforage.forage.android.pos.ForageTerminalSDK.init] method.
 */
@RequiresApi(Build.VERSION_CODES.M)
internal class PosTerminalInitializer(
//    private val dukptService: DukptService,
    private val ksnManager: KsnManager,
    private val logger: Log
) : PosInitializer {
    @Throws(PosInitializationException::class)
    override suspend fun execute(
        merchantId: String,
        sessionToken: String
    ) {
        try {
            if (isKsnFileAvailable()) {
                return
            }
            val vaultUrl = EnvConfig.fromSessionToken(sessionToken).vaultBaseUrl
            val rsaKeyManager = initRsaKeyManager(vaultUrl)

            val base64encodedCsr = getBase64CSR(rsaKeyManager)

            val rosettaApi = RosettaProxyApi.from(
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

//            ksnManager.init(initializePosResponse)

            /**
             * Unpack:
             * encryptedIpek,
             * checkSum,
             * checksumAlgorithm,
             * keySerialNumber - KSN
             */
            val initialDerivationKey = rsaKeyManager.decrypt(initializePosResponse.encryptedIpek.toByteArray())

            // dukptService.loadKey(decryptedInitialDerivationKey)
        } catch (e: Exception) {
            logger.e("Failed to initialize the ForageTerminalSDK", e)
            throw e
        }
    }

    companion object {
        private fun initRsaKeyManager(vaultUrl: String): RsaKeyManager {
            try {
                return RsaKeyManager.fromAndroidKeystore(vaultUrl)
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
        try {
            val ksnManager = KsnManager.forJavaRuntime()
            if (ksnManager.isInitialized()) {
                logger.i("[POS] KSN Manager was already initialized. KSN file already exists on the device.")
                return true
            }
            return false
        } catch (e: Exception) {
            throw PosInitializationException("Failed to check for existing KSN file", e)
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
                InitializePosRequest(
                    base64PublicKeyPEM = base64PublicKeyPEM
                )
            )
        } catch (e: Exception) {
            throw PosInitializationException("Failed to initialize POS terminal with Rosetta", e)
        }
    }
}

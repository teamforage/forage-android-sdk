package com.joinforage.forage.android.pos.keys

import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.pos.PosForageConfig

internal class PosInitializationException(
    val reason: String,
    throwable: Throwable? = null
) : Exception("Failed to initialize POS terminal. $reason", throwable)

internal interface PosInitializer {
    /**
     * @throws PosInitializationException if any of the initialization steps fail.
     */
    suspend fun getPinTranslationParams(
        merchantId: String,
        sessionToken: String
    ): PinTranslationParams
}

internal data class PinTranslationParams(
    val ksn: String,
    val txnCounter: String
)

/**
 * Handles the initialization steps for the [com.joinforage.forage.android.pos.ForageTerminalSDK.init] method.
 */
internal class PosTerminalInitializer(
    private val ksnManager: KsnManager,
    private val logger: Log
) : PosInitializer {
    /**
     * @throws PosInitializationException if any of the initialization steps fail.
     */
    override suspend fun getPinTranslationParams(
        merchantId: String,
        sessionToken: String
    ): PinTranslationParams {
        try {
            if (isKsnFileAvailable()) {
                // TODO @devinmorgan: where do we increment the counter, after each /balance/, /capture/, /refund/, etc.?
                // TODO @devinmorgan: are the lines below accurate?

                val ksn = ksnManager.readInitialKeyId()
                val txnCounterHex = ksnManager.readTxCountStr()

                return PinTranslationParams(
                    ksn = ksn,
                    txnCounter = txnCounterHex
                )
            }

            // TODO: @devinMorgan to pass Context here if needed
//            val certificateAlias = "..."
//            val rsaKeyManager = RsaKeyManager.fromAndroidKeystore(..., certificateAlias)

            // TODO: @devinmorgan make sure this is the base64-encoded CSR.
            val base64encodedCsr = "...==" // call getBase64EncodedCsr()

            val rosettaApi = RosettaProxyApi.from(
                PosForageConfig(
                    sessionToken = sessionToken,
                    merchantId = merchantId
                )
            )

            signCertificate(rosettaApi, base64encodedCsr)

            // TODO: @devinmorgan replace with base64-encoded public key.
            val base64encodedPublicKey = "..."

            val initializePosResponse = initializePos(
                rosettaApi,
                base64encodedPublicKey = base64encodedPublicKey
            )

            ksnManager.init(initialKeyId = initializePosResponse.keySerialNumber)

            /**
             * Unpack:
             * encryptedIpek,
             * checkSum,
             * checksumAlgorithm,
             * keySerialNumber
             */

            // TODO: @devinmorgan 13. Decrypt key and generate first set of transaction keys
            // TODO: @devinmorgan 14. Delete DUKPT base key and public/private key pair.

            return PinTranslationParams(
                ksn = initializePosResponse.keySerialNumber,
                txnCounter = ksnManager.readTxCountStr()
            )
        } catch (e: PosInitializationException) {
            logger.e("Failed to initialize the ForageTerminalSDK", e)
            throw e
        } catch (e: Exception) {
            logger.e("Failed to initialize the ForageTerminalSDK", e)
            throw e
        }
    }

    /**
     * @throws PosInitializationException
     *
     * @return true if the KSN file already exists, false otherwise.
     */
    private fun isKsnFileAvailable(): Boolean {
        try {
            val ksnManager = KsnManager.forJavaRuntime()
            if (ksnManager.isInitialized()) {
                logger.i("[POS] KSN Manager was already initialized. KSN file already exists on the device.")
                return true
            }
            return false
        } catch (e: Exception) {
            throw PosInitializationException("Failed to check for existing KSN file")
        }
    }

    /**
     * @throws PosInitializationException
     */
    private suspend fun signCertificate(
        rosettaApi: RosettaProxyApi,
        csr: String
    ): CertificateSigningResponse {
        try {
            logger.i("[Rosetta] Signing certificate")
            return rosettaApi.signCertificate(
                CertificateSigningRequest(csr)
            )
        } catch (e: Exception) {
            throw PosInitializationException("Failed to sign certificate")
        }
    }

    /**
     * @throws PosInitializationException
     */
    private suspend fun initializePos(
        rosettaApi: RosettaProxyApi,
        base64encodedPublicKey: String
    ): InitializePosResponse {
        try {
            logger.i("[Rosetta] Calling /api/terminal/initialize")

            return rosettaApi.initializePos(
                InitializePosRequest(
                    base64EncodedPublicKey = base64encodedPublicKey
                )
            )
        } catch (e: Exception) {
            throw PosInitializationException("Failed to initialize POS terminal with Rosetta")
        }
    }
}

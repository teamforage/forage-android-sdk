package com.joinforage.forage.android.pos.services.init

import com.joinforage.forage.android.core.services.telemetry.LogLogger
import com.joinforage.forage.android.pos.services.encryption.AesBlock
import com.joinforage.forage.android.pos.services.encryption.certificate.IRsaKeyManager
import com.joinforage.forage.android.pos.services.encryption.dukpt.IDukptService
import com.joinforage.forage.android.pos.services.encryption.dukpt.SecureKeyStorageRegisters
import com.joinforage.forage.android.pos.services.encryption.storage.IKsnFileManager
import com.joinforage.forage.android.pos.services.encryption.storage.KeySerialNumber

/**
 * Handles the initialization steps for the [com.joinforage.forage.android.pos.ForageTerminalSDK.init] method.
 */
internal class PosTerminalInitializer(
    private val ksnManager: IKsnFileManager,
    logEngine: LogLogger,
    private val rosetta: IRosettaInitService,
    private val keyRegisters: SecureKeyStorageRegisters,
    private val base64: IBase64Util,
    private val rsaKeyManager: IRsaKeyManager,
    private val dukptServiceFactory: (KeySerialNumber) -> IDukptService
) {
    private val logger = Logger(logEngine)

    private suspend fun init(): Boolean {
        // TODO: add an initialization metric
        logger.beginInit()
        if (ksnManager.isInitialized()) {
            logger.alreadyInitialized()
            return false
        }
        val csr = rsaKeyManager.generateCSRBase64()
        val response = rosetta.signCertificate(csr)
        val posInitState = rosetta.initializePos(response.certificate)
        val ksnStr = posInitState.keySerialNumber
        ksnManager.init(ksnStr)
        val ipek = posInitState.base64EncryptedIpek
        val decodedIpek = base64.decode(ipek)
        val decryptedInitialDerivationKey = rsaKeyManager.decrypt(
            encryptedData = decodedIpek
        )
        rsaKeyManager.deleteKeyPair()

        val dukptService = dukptServiceFactory(KeySerialNumber(ksnStr))
        val ksn = dukptService.loadKey(AesBlock(decryptedInitialDerivationKey))
        ksnManager.updateKsn(ksn)

        logger.initComplete()
        return true
    }

    class ForageInitException : Exception("Failed to initialize the ForageTerminalSDK")

    class Logger(val logEngine: LogLogger) {
        fun beginInit() {
            logEngine.i("[START] POS Initialization")
        }
        fun initFailed(e: Exception) {
            logEngine.e("[END] Failed to initialize the ForageTerminalSDK", e)
        }
        fun alreadyInitialized() {
            logEngine.i("[END] Already initialized; skipping.")
        }
        fun initComplete() {
            logEngine.i("[END] ForageTerminalSDK is now initialized")
        }
    }

    suspend fun safeInit(): Boolean = try {
        // TODO: break these out into more expressive errors
        init()
    } catch (e: Exception) {
        // TODO: clear ksn file as it may be corrupted!
        logger.initFailed(e)
        throw ForageInitException()
    }
}

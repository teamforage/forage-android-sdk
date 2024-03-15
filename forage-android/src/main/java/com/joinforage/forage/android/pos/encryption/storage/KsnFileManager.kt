package com.joinforage.forage.android.pos.encryption.storage

import android.content.Context
import com.joinforage.forage.android.pos.encryption.ByteUtils
import com.joinforage.forage.android.pos.encryption.dukpt.KsnComponent
import java.io.File

private val KSN_FILE_NAME: String = "key_serial_number.txt"
private val INITIAL_TX_COUNT = 0u

internal data class KeySerialNumber(
        val baseDerivationKeyId: String, // 8 hex chars, which is 32 bits
        val deviceDerivationId: String, // 8 hex chars, which is 32 bits
        val txCount: UInt
) {

    init {
        require(baseDerivationKeyId.length == 8) {
            "The Base Derivation Key Id must be exactly 8 characters, which is 32 bits."
        }
        require(deviceDerivationId.length == 8) {
            "The Device Derivation Id must be exactly 8 characters, which is 32 bits."
        }
    }

    // this constructor is used for creating the initial KSN
    // because the DUKPT client never uses all 64 bits but
    // the Rosetta server returns all 64 bits. We make this
    // secondary constructor thus to play nice with Rosetta
    constructor(
            // 16 chars or 64 bits [baseDerivationKeyId|derivationDeviceId]
            initialKeyId: String
    ) : this(
            txCount = INITIAL_TX_COUNT,
            baseDerivationKeyId = initialKeyId.substring(0, 8),
            deviceDerivationId = initialKeyId.substring(8, 16)
    )

    constructor(
            baseDerivationKeyId: KsnComponent,
            deviceId: KsnComponent,
            txCount: KsnComponent
    ) : this(
            baseDerivationKeyId = baseDerivationKeyId.toHexString(),
            deviceDerivationId = deviceId.toHexString(),
            txCount = txCount.toUInt()
    )

    val fileContent = "$baseDerivationKeyId\n" + "$deviceDerivationId\n" + "$txCount\n"

    // this is the value that Amazon Payments Cryptography expects
    // as the `ksn` value sent up with each encrypted PIN block
    // we expose both for better Dev Exp
    val bdkIdAndDeviceId = "$baseDerivationKeyId$deviceDerivationId"
    val apcKsn = bdkIdAndDeviceId

    val txCountAsBigEndian8CharHex: String =
            ByteUtils.byteArray2Hex(ByteUtils.uintToByteArray(txCount))

    fun newKsnWithCount(txCount: KsnComponent): KeySerialNumber =
            KeySerialNumber(
                    baseDerivationKeyId = baseDerivationKeyId,
                    deviceDerivationId = deviceDerivationId,
                    txCount = txCount.toUInt()
            )
}

internal interface PersistentStorage {
    fun exists(): Boolean
    fun write(content: String)
    fun read(): List<String>
}

internal class PersistentFile(private val context: Context) : PersistentStorage {
    private fun _getFileSync() = File(context.filesDir, KSN_FILE_NAME)
    override fun write(content: String) {
        _getFileSync().outputStream().use { it.write(content.toByteArray()) }
    }
    override fun exists(): Boolean = _getFileSync().exists()

    override fun read(): List<String> = _getFileSync().readLines()
}

internal class PersistentString(private var content: String = "") : PersistentStorage {
    override fun write(content: String) {
        this.content = content
    }
    override fun exists(): Boolean = content.isNotEmpty()
    override fun read(): List<String> = content.lines()
}

internal class KsnFileManager(private val ksnFile: PersistentStorage) {

    fun init(initialKeyId: String): Boolean {
        require(initialKeyId.length == 16) {
            "The Initial Key Id must be exactly 16 characters, which is 64 bits."
        }
        val ksn = KeySerialNumber(initialKeyId)

        // if the a KSN file exists, there's a few other
        // checks we should run to make sure it's OK to
        // not run .init
        // NOTE: an error will be thrown if we attempt to
        // read the file (e.g. existing values) before
        // writing (creating) the file in the first place.
        // This is why we separate the subsequent checks
        // for the ksnFile.exists() check
        if (ksnFile.exists()) {
            // group the conditions into a single expression so
            // that if the file does not exist, it does not
            // attempt to read the Device Derivation Id, which would
            // throw an error
            val existingBdkId = readBaseDerivationKeyId()?.toHexString()
            val existingDeviceId = readDeviceDerivationId()?.toHexString()
            val runInit = existingBdkId.isNullOrBlank() // no bdk id? run init!
                    || existingDeviceId.isNullOrBlank() // no device id? run init!
                    || ksn.baseDerivationKeyId != existingBdkId  // mismatching bdk ids? run init!
                    || ksn.deviceDerivationId != existingDeviceId  // mismatching device ids? run init!
            if (!runInit) return false
        }

        // we're good to go? let's persist the ksn content
        ksnFile.write(ksn.fileContent)

        // double check that there were no issues when
        // trying to write to the file
        return ksnFile.exists()
    }

    // we care about the KSN file existing and not
    // being corrupted. Checking that the txCount
    // can be read and is an int seems like a
    // convenient way of killing two birds with
    // one stone
    fun isInitialized(): Boolean = readTxCount() != null

    // Base Derivation Key is line 0
    fun readBaseDerivationKeyId(): KsnComponent? {
        val deviceIdHexStr = ksnFile.read().getOrNull(0) ?: return null
        return if (deviceIdHexStr.isEmpty()) null else KsnComponent(deviceIdHexStr)
    }

    // Device Derivation Id is line 1
    fun readDeviceDerivationId(): KsnComponent? {
        val deviceIdHexStr = ksnFile.read().getOrNull(1) ?: return null
        return if (deviceIdHexStr.isEmpty()) null else KsnComponent(deviceIdHexStr)
    }

    // Tx Count is line 2
    fun readTxCount(): KsnComponent? {
        val txCount = ksnFile.read().getOrNull(2)?.toUIntOrNull() ?: return null
        return KsnComponent(txCount)
    }

    fun readAll(): KeySerialNumber? {
        val bdkId = readBaseDerivationKeyId() ?: return null
        val deviceId = readDeviceDerivationId() ?: return null
        val txCount = readTxCount() ?: return null
        return KeySerialNumber(baseDerivationKeyId = bdkId, deviceId = deviceId, txCount = txCount)
    }

    // the KSN File manager is not responsible for
    // incrementing the counter. It will blindly write
    // valid KSN content to the persistent file
    fun updateKsn(nextKsnState: KeySerialNumber) {
        val actualBdkId = nextKsnState.baseDerivationKeyId
        val expectedBdkId = readBaseDerivationKeyId()?.toHexString()
        require(actualBdkId == expectedBdkId) {
            "Key Serial Number state can only be updated with same Base Derivation Key Id"
        }
        val actualDeviceId = nextKsnState.deviceDerivationId
        val expectedDeviceId = readDeviceDerivationId()?.toHexString()
        require(actualDeviceId == expectedDeviceId) {
            "Key Serial Number state can only be updated with same Device Id"
        }
        ksnFile.write(nextKsnState.fileContent)
    }
    companion object {
        fun byFile(context: Context) = KsnFileManager(PersistentFile(context))
        fun byString() = KsnFileManager(PersistentString())
    }
}

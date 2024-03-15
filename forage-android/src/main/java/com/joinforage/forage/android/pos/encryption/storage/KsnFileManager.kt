package com.joinforage.forage.android.pos.encryption.storage

import android.content.Context
import com.joinforage.forage.android.pos.encryption.ByteUtils
import com.joinforage.forage.android.pos.encryption.dukpt.KsnComponent
import java.io.File


private val KSN_FILE_NAME: String = "key_serial_number.txt"
private val INITIAL_TX_COUNT = 0u

internal data class KeySerialNumber(
    val deviceDerivationId: String, // 8 chars or 32 bits
    val txCount: UInt,
) {
    val txCountAsBigEndian8CharHex: String

    init {
        require(deviceDerivationId.length == 8) {
            "The Device Derivation Id must be exactly 8 characters, which is 32 bits."
        }
        txCountAsBigEndian8CharHex = ByteUtils.byteArray2Hex(
            ByteUtils.uintToByteArray(txCount)
        )
    }


    // this constructor is used for creating the initial KSN
    // because the DUKPT client never uses all 64 bits but
    // the Rosetta server returns all 64 bits. We make this
    // secondary constructor thus to play nice with Rosetta
    constructor(
        // 16 chars or 64 bits [baseDerivationKeyId|derivationDeviceId]
        initialKeyId: String,
    ) : this(
        txCount = INITIAL_TX_COUNT,
        // only take the derivationDeviceId because the
        // DUKPT client never cares about the baseDerivationKeyId
        deviceDerivationId = initialKeyId.substring(8, 16),
    )

    constructor(
        deviceId: KsnComponent,
        txCount: KsnComponent,
    ) : this(deviceId.toHexString(), txCount.toUInt())

    // we expressly do not store the Base Derivation Key Id
    // first line is Device Derivation Id
    // second line is txCount
    val fileContent = "$deviceDerivationId\n$txCount"
}

internal interface PersistentStorage {
    fun exists() : Boolean
    fun write(content: String)
    fun read() : List<String>
}

internal class PersistentFile(private val context: Context) : PersistentStorage {
    private fun _getFileSync() = File(context.filesDir, KSN_FILE_NAME)
    override fun write(content: String) {
        _getFileSync().outputStream().use {
            it.write(content.toByteArray())
        }
    }
    override fun exists(): Boolean = _getFileSync().exists()

    override fun read(): List<String> = _getFileSync().readLines()
}
internal class PersistentString(
    private var content: String = ""
) : PersistentStorage {
    override fun write(content: String) { this.content = content }
    override fun exists(): Boolean = content.isNotEmpty()
    override fun read(): List<String> = content.lines()
}

internal class KsnFileManager(private val ksnFile: PersistentStorage) {

    fun init(initialKeyId: String) : Boolean {
        require(initialKeyId.length == 16) {
            "The Initial Key Id must be exactly 16 characters, which is 64 bits."
        }
        val ksn = KeySerialNumber(initialKeyId)

        // group the conditions into a single expression so
        // that if the file does not exist, it does not
        // attempt to read the Device Derivation Id, which would
        // throw an error
        val existingDeviceId = readDeviceDerivationId()?.toHexString()
        val runInit = !ksnFile.exists() // no ksn file? run init!
                || existingDeviceId == null // no device id? run init!
                || ksn.deviceDerivationId != existingDeviceId  // mismatching device ids? run init!
        if (!runInit) return false

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
    fun isInitialized() : Boolean = readTxCount() != null

    // Device Derivation Id is the first line
    fun readDeviceDerivationId() : KsnComponent? {
        val deviceIdHexStr = ksnFile.read().firstOrNull() ?: return null
        return KsnComponent(deviceIdHexStr)
    }

    // Tx Count is the second line
    fun readTxCount() : KsnComponent? {
        val txCount = ksnFile.read().getOrNull(1)?.toUIntOrNull() ?: return null
        return KsnComponent(txCount)
    }

    fun readAll() : KeySerialNumber? {
        val deviceId = readDeviceDerivationId() ?: return null
        val txCount = readTxCount() ?: return null
        return KeySerialNumber(deviceId, txCount)
    }

    // the KSN File manager is not responsible for
    // incrementing the counter. It will blindly write
    // valid KSN content to the persistent file
    fun updateKsnFile(nextKsnState: KeySerialNumber) {
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
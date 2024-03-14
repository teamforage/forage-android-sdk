package com.joinforage.forage.android.pos.keys

import android.content.Context
import java.io.File

private const val KSN_FILE_NAME: String = "key_serial_number.txt"
private const val UNINITIALIZED_TX_COUNT = -1

private data class KeySerialNumber(
    val initialKeyId: String,
    val txCount: Int = 0
) {
    val fileContent = "${initialKeyId}\n$txCount"
}

internal interface PersistentStorage {
    fun exists(): Boolean
    fun write(content: String)
    fun read(): List<String>
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

internal class KsnManager(private val ksnFile: PersistentStorage) {
    fun init(initialKeyId: String): Boolean {
        // group the conditions into a single expression so
        // that if the file does not exist, it does not
        // attempt to read the initialKeyId, which would
        // throw an error
        val runInit = !ksnFile.exists() || initialKeyId != readInitialKeyId()
        if (!runInit) return false

        val ksn = KeySerialNumber(initialKeyId)
        ksnFile.write(ksn.fileContent)

        // double check that there were no issues when
        // trying to write to the file
        return ksnFile.exists()
    }

    fun isInitialized(): Boolean {
        // we care about the KSN file existing and not
        // being corrupted. Checking that the txCount
        // can be read and is an int seems like a
        // convenient way of killing two birds with
        // one stone
        return readTxCountInt() >= 0
    }

    fun readTxCountStr(): String = readTxCountInt().toString(16).padStart(8, '0')
    private fun readTxCountInt(): Int = ksnFile.read().getOrNull(1)?.toIntOrNull() ?: UNINITIALIZED_TX_COUNT

    fun readInitialKeyId(): String = ksnFile.read().firstOrNull() ?: ""
    fun incTxCount(): Int {
        val newTxCount = readTxCountInt() + 1
        val newKsn = KeySerialNumber(readInitialKeyId(), newTxCount)
        ksnFile.write(newKsn.fileContent)
        return newTxCount
    }

    companion object {
        fun forAndroidRuntime(context: Context) = KsnManager(
            PersistentFile(context)
        )

        fun forJavaRuntime() = KsnManager(PersistentString())
    }
}

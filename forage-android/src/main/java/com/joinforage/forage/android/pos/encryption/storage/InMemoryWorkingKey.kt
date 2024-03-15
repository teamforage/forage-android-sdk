package com.joinforage.forage.android.pos.encryption.storage

import com.joinforage.forage.android.pos.encryption.AesBlock
import com.joinforage.forage.android.pos.encryption.dukpt.WorkingKey
import javax.crypto.Cipher

internal class InMemoryWorkingKey(
    private val keyRegisters: InMemoryKeyRegisters
) : WorkingKey {
    override fun aesEncryptEcb(aesBlock: AesBlock): AesBlock {
        val secretKey = keyRegisters.getKey(WORKING_KEY_ALIAS)
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
            .apply { init(Cipher.ENCRYPT_MODE, secretKey) }
        val result = cipher.doFinal(aesBlock.data)
        return AesBlock(result)
    }
    val keyMaterial: AesBlock
        get() = keyRegisters.getWorkingKeyMaterial()

    companion object {
        fun fromHex(
            heyStringKeyMaterial: String
        ): InMemoryWorkingKey {
            val keyRegisters = InMemoryKeyRegisters()
            keyRegisters.setWorkingKey(
                AesBlock.fromHexString(heyStringKeyMaterial)
            )
            return InMemoryWorkingKey(keyRegisters)
        }
    }
}

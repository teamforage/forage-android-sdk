package com.joinforage.forage.android.pos.services.encryption.certificate

import com.joinforage.forage.android.pos.encryption.certificate.generateRawCsr
import com.joinforage.forage.android.pos.services.init.IBase64Util
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Security
import java.security.spec.RSAKeyGenParameterSpec
import java.security.spec.RSAKeyGenParameterSpec.F4
import javax.crypto.Cipher

interface IRsaKeyManager {
    fun encrypt(str: String): ByteArray
    fun encrypt(data: ByteArray): ByteArray
    fun decrypt(encryptedData: ByteArray): ByteArray
    fun generateCSRBase64(): String
}

internal class RsaKeyManager(
    private val base64Encoder: IBase64Util
) : IRsaKeyManager {
    private val keyPair: KeyPair by lazy {
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())

        val keyPairGenerator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME)
        keyPairGenerator.initialize(RSAKeyGenParameterSpec(KEY_SIZE, F4))
        keyPairGenerator.generateKeyPair()
    }

    override fun encrypt(str: String): ByteArray {
        val asBytes = str.toByteArray(StandardCharsets.UTF_8)
        return encrypt(asBytes)
    }

    override fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(
            "RSA/NONE/OAEPWithSHA256AndMGF1Padding"
        ).apply {
            init(Cipher.ENCRYPT_MODE, keyPair.public)
        }
        return cipher.doFinal(data)
    }

    override fun decrypt(encryptedData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(
            "RSA/NONE/OAEPWithSHA256AndMGF1Padding"
        ).apply {
            init(Cipher.DECRYPT_MODE, keyPair.private)
        }
        return cipher.doFinal(encryptedData)
    }

    override fun generateCSRBase64(): String {
        val rawCsr = generateRawCsr(keyPair)
        return base64Encoder.encode(rawCsr)
    }

    companion object {
        private const val KEY_SIZE = 4096
        internal const val CERT_SUBJECT: String = "C = US, O = Forage Technology Corporation, OU = Engineering, ST = California, CN = Forage, L = San Francisco"
    }
}

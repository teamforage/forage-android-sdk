package com.joinforage.forage.android.pos.encryption.certificate

import android.os.Build
import androidx.annotation.RequiresApi
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Security
import java.security.spec.RSAKeyGenParameterSpec
import java.security.spec.RSAKeyGenParameterSpec.F4
import javax.crypto.Cipher
import android.util.Base64 as AndroidBase64

@RequiresApi(Build.VERSION_CODES.M)
class RsaKeyManager() {
    private var keyPair: KeyPair? = null

    init {
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())

        val keyPairGenerator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME)

        keyPairGenerator.initialize(RSAKeyGenParameterSpec(KEY_SIZE, F4))
        keyPair = keyPairGenerator.generateKeyPair()
    }

    fun encrypt(str: String): ByteArray {
        val asBytes = str.toByteArray(StandardCharsets.UTF_8)
        return encrypt(asBytes)
    }

    fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
            .apply { init(Cipher.ENCRYPT_MODE, keyPair!!.public) }
        return cipher.doFinal(data)
    }

    fun decrypt(encryptedData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding").apply {
            init(Cipher.DECRYPT_MODE, keyPair!!.private)
        }
        return cipher.doFinal(encryptedData)
    }

    fun deleteKeyPair() {
        keyPair = null
    }

    fun generateCSRBase64(): String {
        val rawCsr = generateRawCsr(keyPair!!)
        return AndroidBase64.encodeToString(rawCsr.toByteArray(), AndroidBase64.DEFAULT)
    }

    companion object {
        private const val KEY_SIZE = 4096
        internal const val CERT_SUBJECT: String = "C = US, O = Forage Technology Corporation, OU = Engineering, ST = California, CN = Forage, L = San Francisco"

        fun fromAndroidKeystore(): RsaKeyManager {
            Security.removeProvider("BC")
            Security.addProvider(BouncyCastleProvider())

            return RsaKeyManager()
        }
    }
}

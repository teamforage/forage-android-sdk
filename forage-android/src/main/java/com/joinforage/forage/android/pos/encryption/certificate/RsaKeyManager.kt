package com.joinforage.forage.android.pos.encryption.certificate

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.joinforage.forage.android.core.EnvConfig
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Security
import java.security.spec.RSAKeyGenParameterSpec
import java.security.spec.RSAKeyGenParameterSpec.F4
import java.util.Calendar
import javax.crypto.Cipher
import javax.security.auth.x500.X500Principal
import android.util.Base64 as AndroidBase64


internal class KeyExpirationTracker {
    val start: Calendar = Calendar.getInstance()
    val end: Calendar = Calendar.getInstance()
    val expired: Boolean
        get() = Calendar.getInstance().after(end)

    init {
        // thought process was that these key pairs only
        // need to survive long enough to hear obtain a
        // single response from the server, which should
        // never take more than a few seconds. So,
        // expiring after 1 minute seems like plenty of
        // time
        end.add(Calendar.MINUTE, 40) // TODO: revert back to 4 minutes.
    }
}

@RequiresApi(Build.VERSION_CODES.M)
class RsaKeyManager(
    _keyPairGenerator: KeyPairGenerator,
    private val spec: KeyGenParameterSpec,
    val fqdnAlias: String
) {
    private val keyPair: KeyPair

//    private val keyStoreEntry
//        get() = keyStore
//            .apply {
//                load(null)
//            }.getEntry(fqdnAlias, null) as KeyStore.PrivateKeyEntry
//    private val privateKey
//        get() = keyStoreEntry.privateKey
//    private val publicKey
//        get() = keyStoreEntry.certificate.publicKey

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
            .apply { init(Cipher.ENCRYPT_MODE, keyPair.public) }
        return cipher.doFinal(data)
    }

    fun decrypt(encryptedData: ByteArray): ByteArray {
//        val sp = OAEPParameterSpec(
//            "SHA-256",
//            "MGF1",
//            MGF1ParameterSpec("SHA-256"),
//            PSource.PSpecified.DEFAULT
//        )

        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding").apply {
            init(Cipher.DECRYPT_MODE, keyPair.private)
        }
        return cipher.doFinal(encryptedData)
    }

    fun generateCSRBase64(): String {
        val rawCsr = generateRawCsr(keyPair)



//        keyStore.setKeyEntry(fqdnAlias, privateKey, null, arrayOf(cert))

        return AndroidBase64.encodeToString(rawCsr.toByteArray(), AndroidBase64.DEFAULT)
    }

    companion object {
        private const val KEY_SIZE = 4096
        internal const val CERT_SUBJECT: String = "C = US, O = Forage Technology Corporation, OU = Engineering, ST = California, CN = Forage, L = San Francisco"

        /**
         * @return Fully Qualified Domain Name (FQDN) alias. ex: "mydomain.com"
         */
        private fun extractFqdn(url: String): String {
            if (url == EnvConfig.Local.vaultBaseUrl) {
                return "localhost"
            }
            return url
                .removePrefix("http://")
                .removePrefix("https://")
                .removeSuffix("/")
        }

        fun fromAndroidKeystore(vaultUrl: String): RsaKeyManager {
            Security.removeProvider("BC")
            Security.addProvider(BouncyCastleProvider())

            val fqdnAlias = extractFqdn(vaultUrl)

            val expirationTracker = KeyExpirationTracker()
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                fqdnAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setKeySize(KEY_SIZE)
                .setAlgorithmParameterSpec(RSAKeyGenParameterSpec(KEY_SIZE, F4))
                .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setCertificateSubject(X500Principal(CERT_SUBJECT))
                .setCertificateSerialNumber(BigInteger.ONE)
                .setCertificateNotBefore(expirationTracker.start.time)
                .setCertificateNotAfter(expirationTracker.end.time)
                .build()

            return RsaKeyManager(
                KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, BouncyCastleProvider.PROVIDER_NAME),
                keyGenParameterSpec,
                fqdnAlias
            )
        }
    }
}

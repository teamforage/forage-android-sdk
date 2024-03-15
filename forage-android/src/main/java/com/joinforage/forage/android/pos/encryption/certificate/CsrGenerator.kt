package com.joinforage.forage.android.pos.encryption.certificate

import android.os.Build
import androidx.annotation.RequiresApi
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder
import java.security.KeyStore.PrivateKeyEntry

@RequiresApi(Build.VERSION_CODES.M)
internal fun generateRawCsr(keyEntry: PrivateKeyEntry): String {
    val subject = X500Name(RsaKeyManager.CERT_SUBJECT)
    val csrBuilder = PKCS10CertificationRequestBuilder(subject, SubjectPublicKeyInfo.getInstance(keyEntry.certificate.publicKey.encoded))

    val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyEntry.privateKey)
    val csr = csrBuilder.build(signer)

    return csr.toPemString()
}

/**
 * Returns a PEM formatted string of the CSR.
 *
 * e.g. result starts with -----BEGIN CERTIFICATE REQUEST-----
 */
internal fun PKCS10CertificationRequest.toPemString(): String {
    val stringWriter = java.io.StringWriter()
    val pemWriter = JcaPEMWriter(stringWriter)
    pemWriter.writeObject(this)
    pemWriter.close()

    return stringWriter.toString()
}

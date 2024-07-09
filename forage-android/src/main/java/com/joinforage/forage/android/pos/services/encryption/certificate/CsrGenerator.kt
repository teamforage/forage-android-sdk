package com.joinforage.forage.android.pos.encryption.certificate

import android.os.Build
import androidx.annotation.RequiresApi
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder
import java.security.KeyPair

internal fun generateRawCsr(keyPair: KeyPair): String {
    val subject = X500Name(RsaKeyManager.CERT_SUBJECT)
    val csrBuilder = PKCS10CertificationRequestBuilder(subject, SubjectPublicKeyInfo.getInstance(keyPair.public.encoded))

    val signerBuilder = JcaContentSignerBuilder("SHA256withRSA")
    signerBuilder.setProvider(BouncyCastleProvider.PROVIDER_NAME)
    val signer = signerBuilder.build(keyPair.private)
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

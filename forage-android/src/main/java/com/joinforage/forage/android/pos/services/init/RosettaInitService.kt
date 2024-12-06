package com.joinforage.forage.android.pos.services.init

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.IHttpEngine
import org.json.JSONObject

internal data class SignedCertificate(val certificate: String) {
    internal constructor(jsonObject: JSONObject) : this(
        certificate = jsonObject.getString("certificate")
    )

    companion object {
        fun fromJsonString(jsonString: String) = SignedCertificate(JSONObject(jsonString))
    }
}

internal data class InitialPosState(
    val base64EncryptedIpek: String,
    val checksum: String,
    val checksumAlgorithm: String,
    val keySerialNumber: String
) {
    internal constructor(jsonString: String) : this(JSONObject(jsonString))
    internal constructor(jsonObject: JSONObject) : this(
        base64EncryptedIpek = jsonObject.getString("encrypted_ipek"),
        checksum = jsonObject.getString("cksum"),
        checksumAlgorithm = jsonObject.getString("cksum_algo"),
        keySerialNumber = jsonObject.getString("ksn")
    )
}
internal interface IRosettaInitService {
    suspend fun signCertificate(csr: String): SignedCertificate
    suspend fun initializePos(base64PublicKeyPEM: String): InitialPosState
}

internal class RosettaInitService(
    private val forageConfig: ForageConfig,
    private val traceId: String,
    private val posTerminalId: String,
    private val engine: IHttpEngine
) : IRosettaInitService {
    override suspend fun signCertificate(csr: String): SignedCertificate =
        engine.sendRequest(
            CertificateSigningRequest(
                forageConfig = forageConfig,
                traceId = traceId,
                posTerminalId = posTerminalId,
                csr = csr
            )
        ).let { SignedCertificate.fromJsonString(it) }

    override suspend fun initializePos(base64PublicKeyPEM: String): InitialPosState =
        engine.sendRequest(
            InitializePosRequest(
                forageConfig = forageConfig,
                traceId = traceId,
                posTerminalId = posTerminalId,
                base64PublicKeyPEM = base64PublicKeyPEM
            )
        ).let { InitialPosState(it) }
}

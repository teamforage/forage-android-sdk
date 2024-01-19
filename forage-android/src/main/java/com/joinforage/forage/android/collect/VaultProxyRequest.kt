package com.joinforage.forage.android.collect

internal open class VaultProxyRequest(
    open val headers: Map<String, String>,
    open val path: String,
    open val vaultToken: String
) {
    fun setPath(path: String) = VaultProxyRequest(headers, path, vaultToken)
    fun setHeader(header: String, value: String): VaultProxyRequest {
        val mutableCopy = headers.toMutableMap()
        mutableCopy[header] = value
        return VaultProxyRequest(
            headers = mutableCopy,
            path = path,
            vaultToken = vaultToken
        )
    }
    fun setToken(vaultToken: String) = VaultProxyRequest(headers, path, vaultToken)
    companion object {
        fun emptyRequest() = VaultProxyRequest(
            HashMap(),
            "",
            ""
        )
    }
}

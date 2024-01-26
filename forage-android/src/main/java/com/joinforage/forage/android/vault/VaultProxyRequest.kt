package com.joinforage.forage.android.vault

internal open class VaultProxyRequest(
    open val headers: Map<String, String>,
    open val path: String,
    open val vaultToken: String,
    val params: VaultSubmitterParams?
) {
    fun setPath(path: String) = VaultProxyRequest(headers, path, vaultToken, params)
    fun setParams(params: VaultSubmitterParams) = VaultProxyRequest(headers, path, vaultToken, params)
    fun setHeader(header: String, value: String): VaultProxyRequest {
        val mutableCopy = headers.toMutableMap()
        mutableCopy[header] = value
        return VaultProxyRequest(
            headers = mutableCopy,
            path = path,
            vaultToken = vaultToken,
            params = params
        )
    }
    fun setToken(vaultToken: String) = VaultProxyRequest(headers, path, vaultToken, params)
    companion object {
        fun emptyRequest() = VaultProxyRequest(
            HashMap(),
            "",
            "",
            null
        )
    }
}

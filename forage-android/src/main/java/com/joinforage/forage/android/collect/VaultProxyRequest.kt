package com.joinforage.forage.android.collect

internal data class VaultProxyRequest(
    val headers: Map<String, String>,
    val path: String,
    val ebtVaultToken: String
) {
    fun setPath(path: String) = VaultProxyRequest(headers, path, ebtVaultToken)
    fun setHeader(header: String, value: String) : VaultProxyRequest {
        val mutableCopy = headers.toMutableMap()
        mutableCopy[header] = value
        return VaultProxyRequest(mutableCopy, path, ebtVaultToken)
    }
    fun setToken(ebtVaultToken: String) = VaultProxyRequest(headers, path, ebtVaultToken)

    companion object {
        fun emptyRequest() = VaultProxyRequest(
            HashMap(), "", ""
        )
    }
}
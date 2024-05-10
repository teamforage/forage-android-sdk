package com.joinforage.forage.android.ecom.services.vault.bt

class BtResponseRegExp(res: BasisTheoryResponse) : BaseResponseRegExp(res) {
    // com.basistheory.ApiException isn't currently
    // publicly-exposed by the basis-theory-android package
    // so we parse the raw exception message to retrieve the body of the BasisTheory
    // errors
    val containsProxyError: Boolean = bodyText?.contains("proxy_error") ?: false
}

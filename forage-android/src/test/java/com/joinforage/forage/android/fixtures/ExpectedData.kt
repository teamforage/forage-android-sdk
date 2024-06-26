package com.joinforage.forage.android.fixtures

import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeys
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.Balance
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtBalance
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtCard
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.ui.element.state.pan.USState

internal object ExpectedData {
    const val sessionToken: String = "AbCaccesstokenXyz"
    const val merchantId: String = "1234567"

    // card tokenization
    const val cardNumber: String = "5076801234567845"
    const val customerId: String = "test-android-customer-id"
    val cardUsState: USState = USState.PENNSYLVANIA
    const val cardFingerprint: String = "470dda97b63f016a962de150cf53ad72a93aaea4c2a59de2541e0994f48e02ef"

    // PIN-related interactions
    const val paymentRef: String = "6ae6a45ff1"
    const val paymentMethodRef: String = "1f148fe399"
    const val contentId: String = "45639248-03f2-498d-8aa8-9ebd1c60ee65"
    val balance: Balance = EbtBalance(
        snap = "100.00",
        cash = "100.00"
    )

    val mockPaymentMethod = PaymentMethod(
        ref = "1f148fe399",
        type = "ebt",
        balance = null,
        card = EbtCard(
            last4 = "7845",
            token = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7,basis-theory-token",
            number = cardNumber,
            fingerprint = "fingerprint",
            usState = cardUsState
        ),
        customerId = "test-android-customer-id",
        reusable = true
    )
    val mockEncryptionKeys = EncryptionKeys("vgs-alias", "bt-alias")
}

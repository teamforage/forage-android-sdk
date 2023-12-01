package com.joinforage.forage.android.ui

import com.joinforage.forage.android.model.USState

/**
 * Represents card information derived from the user's current
 * Primary Account Number (PAN) input value.
 */
interface DerivedCardInfoInterface {
    /**
     * The US state that issued the EBT card, derived from the Issuer Identification Number (IIN),
     * also known as BIN (Bank Identification Number). The IIN is the first 6 digits of the PAN.
     */
    var usState: USState?
}

class DerivedCardInfo(usState: USState? = null) : DerivedCardInfoInterface {
    override var usState: USState? = usState
}


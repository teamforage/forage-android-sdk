package com.joinforage.forage.android.model

import android.os.Parcelable

/**
 * A model that represents a Forage API response object.
 */
interface ForageModel : Parcelable {
    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int
}

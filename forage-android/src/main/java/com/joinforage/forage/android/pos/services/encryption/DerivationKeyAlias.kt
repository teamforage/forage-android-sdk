package com.joinforage.forage.android.pos.encryption

internal val INTERMEDIATE_KEY_PREFIX = "dukpt_intermediate_derivation_key"
internal val INITIAL_DERIVATION_KEY_REGISTER_INDEX = 0u

internal data class DerivationKeyAlias(val targetKeyRegisterIndex: UInt) {
    override fun toString(): String =
        "${INTERMEDIATE_KEY_PREFIX}_$targetKeyRegisterIndex"

    companion object {
        fun forInitialDerivationKey(): DerivationKeyAlias =
            DerivationKeyAlias(INITIAL_DERIVATION_KEY_REGISTER_INDEX)
    }
}

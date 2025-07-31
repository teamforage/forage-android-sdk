package com.joinforage.forage.android.mock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import com.google.android.material.R
import com.joinforage.forage.android.ecom.ui.element.ForagePaymentSheet

class TestForagePaymentSheet {
    class TestForagePaymentSheetFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = ForagePaymentSheet(inflater.context)
    }

    fun create(): ForagePaymentSheet {
        val fragment = TestForagePaymentSheetFragment()
        launchFragmentInContainer(
            themeResId = R.style.Theme_AppCompat,
            instantiate = { fragment }
        )
        return fragment.view as ForagePaymentSheet
    }

    companion object {
        const val TEST_MERCHANT_ID = "1234567"
        const val MOCK_SESSION_TOKEN =
            "mock_eyJhIjogMjQ3NDQ0NCwgInNrIjogIk92Y25aNTI5QjJuZ0p2N0pZN2laeHc9PSIsICJ0IjogNDJ9.aHe7Tw.cx77tQZ6-c_9nGpvE3h4VgsUTTlP21Soa-ofDDnWF1o"
        const val TEST_HSA_FSA_CARD = "4000051230002839"
    }
}

package com.joinforage.forage.android.mock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import com.google.android.material.R
import com.joinforage.forage.android.ecom.ui.element.ForagePANEditText

class TestForagePANEditText {
    class TestForagePANEditTextFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = ForagePANEditText(inflater.context)
    }

    fun create(): ForagePANEditText {
        val fragment = TestForagePANEditTextFragment()
        launchFragmentInContainer(
            themeResId = R.style.Theme_AppCompat,
            instantiate = { fragment }
        )
        return fragment.view as ForagePANEditText
    }
}

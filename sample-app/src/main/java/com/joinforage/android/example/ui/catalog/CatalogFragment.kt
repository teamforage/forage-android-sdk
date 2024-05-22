package com.joinforage.android.example.ui.catalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.joinforage.android.example.R
import com.joinforage.android.example.databinding.FragmentCatalogBinding
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.pos.ui.element.ForagePINEditText

class CatalogFragment : Fragment() {

    private var _binding: FragmentCatalogBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val catalogViewModel = ViewModelProvider(this)[CatalogViewModel::class.java]

        _binding = FragmentCatalogBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val forageConfig = ForageConfig(
            sessionToken = "sandbox_420FoEvaMon",
            merchantId = "0123456"
        )

        // ForagePANEditText can be created via XML layouts or
        // dynamically. These different modes of creation are
        // a natural point of styling differences to emerge.
        // We add a ForagePANEditText to the catalog in the
        // hopes that we'll spot these discrepancies
        // NOTE: this view is currently unstyled compared to
        // the other ForagePINEditText in the catalog which was
        // created via XML and has some XML styles associated
        // with it.
        val dynamicPinEditText = ForagePINEditText(requireContext(), null, R.attr.catalogPinStyleRef)
        binding.root.addView(dynamicPinEditText)

        dynamicPinEditText.setForageConfig(forageConfig)
        binding.firstForageEditText.setForageConfig(forageConfig)

        // NOTE: we call setForageConfig a second time here so that
        //  the CI tests always confirm that running setForageConfig
        //  more than once is OK and does not cause a crash. So,
        //  these duplicate calls are intentional here
        binding.firstForageEditText.setForageConfig(forageConfig)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

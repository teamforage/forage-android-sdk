package com.joinforage.android.example.ui.catalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.joinforage.android.example.databinding.FragmentCatalogBinding
import com.joinforage.forage.android.ui.ForageConfig

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

        binding.firstForageEditText.setForageConfig(forageConfig)
        binding.secondEditText.setForageConfig(forageConfig)
        binding.thirdEditText.setForageConfig(forageConfig)
        binding.fourthEditText.setForageConfig(forageConfig)
        binding.foragePinEditText.setForageConfig(forageConfig)
        binding.secondForagePINEditText.setForageConfig(forageConfig)
        binding.thirdForagePINEditText.setForageConfig(forageConfig)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

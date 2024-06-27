package com.joinforage.android.example.ui.catalog

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.Fragment
import com.joinforage.android.example.databinding.FragmentCatalogBinding
import com.joinforage.forage.android.core.services.ForageConfig

class CatalogFragment : Fragment() {

    private var _binding: FragmentCatalogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCatalogBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // any string prefixed with "dev_" so that we can use
        // the dev feature flags to test Rosetta vs BT
        val forageConfig = ForageConfig(
            sessionToken = "dev_eyJhIjogNDI5ODYsICJzayI6ICJOd3haaHVmQnBybDlaU0VBb194SFNRPT0iLCAidCI6IDQ3fQ==.ZnyEXA.Msl6EOi1byNi27TfMB0fD3hT4tc",
            merchantId = "0123456"
        )

        binding.firstForageEditText.setForageConfig(forageConfig)
        binding.foragePinEditText.setForageConfig(forageConfig)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // anybody can get a binding to a ForagePINEditTExt
        val foragesSuperSafeEditTextElement = binding.foragePinEditText

        // anybody can repeatedly explore the view hierarchy by running
        // their app in debug mode and repeatedly doing `.getChildAt(...)`
        // to observe child views
        val isRosetta = (foragesSuperSafeEditTextElement.getChildAt(0) as ViewGroup).getChildAt(0) is EditText
        if (isRosetta) {
            println("Using Rosetta")
            // once somebody understands the view hierarchy of Rosetta...
            val editText = (foragesSuperSafeEditTextElement.getChildAt(0) as ViewGroup).getChildAt(0) as EditText

            // ...they will have free rein to observe whatever a user
            // in the PIN field
            // NOTE: .addTextChangedListener is just one way to observe
            // the text there other ways to do observe the PIN once you
            // have a reference to the view
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, co: Int, af: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) { println("Rosetta Pin Content: $s") }

            })
        } else {
            println("Using Basis Theory")

            // once somebody understands the view hierarchy of Basis Theory...
            val appCompatEditText = ((foragesSuperSafeEditTextElement.getChildAt(0) as ViewGroup).getChildAt(0) as ViewGroup).getChildAt(0) as AppCompatEditText

            // ...they will have free rein to observe whatever a user
            // in the PIN field
            // NOTE: .addTextChangedListener is just one way to observe
            // the text there other ways to do observe the PIN once you
            // have a reference to the view
            appCompatEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, co: Int, af: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) { println("BT Pin Content: $s") }

            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

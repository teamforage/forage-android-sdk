package com.joinforage.android.example.ui.catalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.joinforage.android.example.databinding.FragmentCatalogBinding
import com.joinforage.forage.android.ui.ForageConfig
import java.util.Timer
import kotlin.concurrent.timerTask

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

        foragesSuperSafeEditTextElement.viewTreeObserver.addOnWindowAttachListener (object :
            ViewTreeObserver.OnWindowAttachListener {
            override fun onWindowAttached() {
                // anybody can repeatedly explore the view hierarchy by running
                // their app in debug mode and repeatedly doing `.getChildAt(...)`
                // to observe child views
                val vgsEditText = ((foragesSuperSafeEditTextElement.getChildAt(0) as ViewGroup).getChildAt(0) as ViewGroup).getChildAt(0) as EditText

                // start observing a PIN's input
                val timer = Timer()
                timer.scheduleAtFixedRate(timerTask {
                    println("VGS Pin Content: ${vgsEditText.text}")
                }, 0, 1000) //
            }

            override fun onWindowDetached() {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.joinforage.android.example.ui.pos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.joinforage.android.example.databinding.FragmentPosBinding
import com.joinforage.android.example.pos.receipts.ReceiptLayout
import com.pos.sdk.DeviceManager
import com.pos.sdk.DevicesFactory
import com.pos.sdk.callback.ResultCallback

class POSFragment : Fragment() {
    private var _binding: FragmentPosBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPosBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val receiptView = ReceiptView(requireContext())
        receiptView.setReceiptLayout(ReceiptLayout.ExampleReceipt)
        binding.mainPosLayout.addView(receiptView)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // initialize the CPay SDK for later user
        DevicesFactory.create(
            requireContext(),
            object : ResultCallback<DeviceManager> {
                override fun onFinish(deviceManager: DeviceManager) {
                    println("onFinish success!")
                }

                override fun onError(i: Int, s: String) {
                    println("onError: $i,$s")
                }
            }
        )
    }
}

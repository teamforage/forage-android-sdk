package com.joinforage.android.example.ui.complete.flow.payment.capture

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.joinforage.android.example.R
import com.joinforage.android.example.databinding.FragmentFlowCapturePaymentBinding
import com.joinforage.android.example.ext.hideKeyboard
import com.joinforage.android.example.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FlowCapturePaymentFragment : BaseFragment<FragmentFlowCapturePaymentBinding>(
    R.layout.fragment_flow_capture_payment
) {

    override val viewModel: FlowCapturePaymentViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.captureSnapAmount.setOnClickListener {
            viewModel.captureSnapAmount(
                context = requireContext(),
                binding.snapPinEditText
            )
            it.context.hideKeyboard(it)
        }

        binding.captureNonSnapAmount.setOnClickListener {
            viewModel.captureCashAmount(
                context = requireContext(),
                binding.cashPinEditText
            )
            it.context.hideKeyboard(it)
        }
    }
}

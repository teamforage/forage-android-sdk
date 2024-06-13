package com.joinforage.android.example.ui.complete.flow.payment.capture

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.joinforage.android.example.R
import com.joinforage.android.example.databinding.FragmentFlowCapturePaymentBinding
import com.joinforage.android.example.ext.hideKeyboard
import com.joinforage.android.example.ui.base.BaseFragment
import com.joinforage.forage.android.core.services.ForageConfig
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FlowCapturePaymentFragment : BaseFragment<FragmentFlowCapturePaymentBinding>(
    R.layout.fragment_flow_capture_payment
) {

    override val viewModel: FlowCapturePaymentViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val snapEditText = binding.snapPinEditText
        val cashEditText = binding.cashPinEditText

        // as soon as possible set the forage context on
        // the ForageElement
        snapEditText.setForageConfig(
            ForageConfig(
                merchantId = viewModel.merchantAccount,
                sessionToken = viewModel.bearer
            )
        )
        cashEditText.setForageConfig(
            ForageConfig(
                merchantId = viewModel.merchantAccount,
                sessionToken = viewModel.bearer
            )
        )

        binding.captureSnapAmount.setOnClickListener {
            viewModel.captureSnapAmount(
                binding.snapPinEditText
            )
            it.context.hideKeyboard(it)
        }

        binding.deferCaptureSnap.setOnClickListener {
            viewModel.deferPaymentCaptureSnap(
                binding.snapPinEditText
            )
            it.context.hideKeyboard(it)
        }

        binding.captureCashAmount.setOnClickListener {
            viewModel.captureCashAmount(
                binding.cashPinEditText
            )
            it.context.hideKeyboard(it)
        }

        binding.deferCaptureCash.setOnClickListener {
            viewModel.deferPaymentCaptureCash(
                binding.cashPinEditText
            )
            it.context.hideKeyboard(it)
        }
    }
}

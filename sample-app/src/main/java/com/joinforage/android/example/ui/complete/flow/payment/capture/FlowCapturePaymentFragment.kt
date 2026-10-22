package com.joinforage.android.example.ui.complete.flow.payment.capture

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.joinforage.android.example.R
import com.joinforage.android.example.databinding.FragmentFlowCapturePaymentBinding
import com.joinforage.android.example.ext.hideKeyboard
import com.joinforage.android.example.ui.base.BaseFragment
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.ecom.services.CreateMomentParams
import com.joinforage.forage.android.ecom.services.ForageSDK
import com.joinforage.forage.android.ecom.services.moment.Moment
import com.joinforage.forage.android.ecom.services.moment.MomentDevSettings
import com.joinforage.forage.android.ecom.services.moment.MomentResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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

        binding.momentPaymentRef.setText(viewModel.uiState.value?.snapPaymentRef)
        binding.openMoment.setOnClickListener {
            it.context.hideKeyboard(it)
            openPaymentCollectPinMoment()
        }
    }

    // createMoment needs an activity to show the moment over, so it runs here rather
    // than in the view model
    private fun openPaymentCollectPinMoment() {
        // points the moment at a locally running custom-checkout. only dev and local tokens use it
        MomentDevSettings.baseUrlOverride = binding.momentBaseUrl.text.toString().ifBlank { null }
        val paymentRef = binding.momentPaymentRef.text.toString()

        binding.momentResult.text = "Opening..."
        lifecycleScope.launch {
            val result = ForageSDK().createMoment(
                requireActivity(),
                CreateMomentParams(
                    forageConfig = ForageConfig(
                        merchantId = viewModel.merchantAccount,
                        sessionToken = viewModel.bearer
                    ),
                    moment = Moment.PaymentCollectPin(paymentRef = paymentRef)
                )
            )
            binding.momentResult.text = when (result) {
                MomentResult.Completed -> "Completed"
                MomentResult.Dismissed -> "Dismissed"
                is MomentResult.Failed -> "Failed:\n${result.error}"
            }
        }
    }
}

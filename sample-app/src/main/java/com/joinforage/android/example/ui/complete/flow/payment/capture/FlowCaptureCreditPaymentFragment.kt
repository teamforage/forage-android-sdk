package com.joinforage.android.example.ui.complete.flow.payment.capture

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.joinforage.android.example.R
import com.joinforage.android.example.databinding.FragmentFlowCaptureCreditPaymentBinding
import com.joinforage.android.example.ext.hideKeyboard
import com.joinforage.android.example.network.model.PaymentResponse
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FlowCaptureCreditPaymentFragment : Fragment() {

    private val viewModel: FlowCaptureCreditPaymentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFlowCaptureCreditPaymentBinding.inflate(inflater, container, false)

        fun handleIsLoadingChange(isLoading: Boolean) {
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        fun handlePaymentResultChange(paymentResponse: PaymentResponse?) {
            binding.paymentMethodRef.text = getString(R.string.payment_method_ref, paymentResponse?.paymentMethod)
            binding.paymentRef.text = getString(R.string.payment_ref, paymentResponse?.ref.orEmpty())
            binding.paymentStatus.text = getString(R.string.payment_status, paymentResponse?.status.orEmpty())
            binding.paymentRequestedAmount.text = getString(R.string.payment_requested_amount, paymentResponse?.requestedAmount.orEmpty())
            when (paymentResponse?.status) {
                "authorized" -> Pair(paymentResponse.amount, null)
                "succeeded" -> Pair(paymentResponse.authorizationAmount, paymentResponse.amount)
                else -> Pair(null, null) // "Can't happen"
            }.let { (authed, captured) ->
                binding.paymentAuthorizedAmount.text = getString(R.string.payment_authorized_amount, authed.orEmpty())
                binding.paymentCapturedAmount.text = getString(R.string.payment_captured_amount, captured.orEmpty())
            }
            binding.paymentDetail.text = getString(R.string.payment_detail, paymentResponse?.toString().orEmpty())
        }

        fun handleErrorResultChange(message: String?) {
            binding.errorResponse.text = message.orEmpty()
        }

        binding.submitButton.setOnClickListener {
            val requestedCaptureAmount = binding.paymentAmountEditText.text.toString()
            viewModel.capturePayment(requestedCaptureAmount)
            it.context.hideKeyboard(it)
        }

        viewModel.isLoading.observe(viewLifecycleOwner, ::handleIsLoadingChange)
        viewModel.errorResult.observe(viewLifecycleOwner, ::handleErrorResultChange)
        viewModel.paymentResult.observe(viewLifecycleOwner, ::handlePaymentResultChange)

        //
        // Render initial state
        //
        handlePaymentResultChange(viewModel.args.payment)
        handleErrorResultChange(null)

        return binding.root
    }
}

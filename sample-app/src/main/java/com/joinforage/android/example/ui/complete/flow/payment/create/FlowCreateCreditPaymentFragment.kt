package com.joinforage.android.example.ui.complete.flow.payment.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.joinforage.android.example.R
import com.joinforage.android.example.databinding.FragmentFlowCreateCreditPaymentBinding
import com.joinforage.android.example.ext.hideKeyboard
import com.joinforage.android.example.network.model.PaymentResponse
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FlowCreateCreditPaymentFragment : Fragment() {

    private val viewModel: FlowCreateCreditPaymentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFlowCreateCreditPaymentBinding.inflate(inflater, container, false)
        var lastPaymentResponse: PaymentResponse? = null

        fun handleIsLoadingChange(isLoading: Boolean) {
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        fun handlePaymentResultChange(paymentResponse: PaymentResponse?) {
            lastPaymentResponse = paymentResponse
            binding.paymentRef.text = getString(R.string.payment_ref, paymentResponse?.ref.orEmpty())
            binding.paymentStatus.text = getString(R.string.payment_status, paymentResponse?.status.orEmpty())
            binding.paymentRequestedAmount.text = getString(R.string.payment_requested_amount, paymentResponse?.requestedAmount.orEmpty())
            binding.paymentAuthorizedAmount.text = getString(R.string.payment_authorized_amount, paymentResponse?.amount.orEmpty())
            binding.paymentDetail.text = getString(R.string.payment_detail, paymentResponse?.toString().orEmpty())
            binding.nextButton.isEnabled = (paymentResponse != null)
        }

        fun handleErrorResultChange(message: String?) {
            binding.errorResponse.text = message.orEmpty()
        }

        binding.submitButton.setOnClickListener {
            val requestedAuthorizationAmount = binding.requestedAuthorizationAmountEditText.text.toString()
            viewModel.createPayment(requestedAuthorizationAmount)
            it.context.hideKeyboard(it)
        }

        viewModel.isLoading.observe(viewLifecycleOwner, ::handleIsLoadingChange)
        viewModel.errorResult.observe(viewLifecycleOwner, ::handleErrorResultChange)
        viewModel.paymentResult.observe(viewLifecycleOwner, ::handlePaymentResultChange)

        binding.nextButton.setOnClickListener {
            findNavController().navigate(
                FlowCreateCreditPaymentFragmentDirections.actionFlowCreateCreditPaymentFragmentToFlowCaptureCreditPaymentFragment(
                    bearer = viewModel.args.bearer,
                    merchantAccount = viewModel.args.merchantAccount,
                    payment = lastPaymentResponse!!
                )
            )
        }

        //
        // Render initial state
        //
        binding.paymentMethodRef.text = getString(R.string.payment_method_ref, viewModel.args.paymentMethodRef)
        handlePaymentResultChange(null)
        handleErrorResultChange(null)

        return binding.root
    }
}

package com.joinforage.android.example.ui.complete.flow.tokenize

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.joinforage.android.example.R
import com.joinforage.android.example.databinding.FragmentFlowTokenizeCreditBinding
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.ecom.ui.element.ForagePaymentSheet.CombinedElementState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FlowTokenizePaymentSheetFragment : Fragment() {

    private val viewModel: FlowTokenizeViewPaymentSheetViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFlowTokenizeCreditBinding.inflate(inflater, container, false)

        // As soon as possible set the forage context on the ForageElement.
        binding.tokenizeForagePaymentSheet.setForageConfig(
            ForageConfig(
                viewModel.args.merchantAccount,
                viewModel.args.bearer
            )
        )

        fun handleIsLoadingChange(isLoading: Boolean) {
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        fun handlePaymentMethodChange(paymentMethod: PaymentMethod?) {
            val ref = paymentMethod?.ref.orEmpty()
            binding.paymentMethodRef.text = getString(R.string.payment_method_ref, ref)
            binding.nextButton.isEnabled = (paymentMethod != null)
        }

        fun handleErrorChange(message: String?) {
            binding.errorResponse.text = message.orEmpty()
        }

        fun handlePaymentSheetChange(elementState: CombinedElementState) {
            binding.submitButton.isEnabled = elementState.isComplete
            binding.cardholderNameState.text = getString(R.string.cardholder_name_state, elementState.cardholderNameState)
            binding.cardNumberState.text = getString(R.string.card_number_state, elementState.cardNumberState)
            binding.expirationState.text = getString(R.string.expiration_state, elementState.expirationState)
            binding.securityCodeState.text = getString(R.string.security_code_state, elementState.securityCodeState)
            binding.zipCodeState.text = getString(R.string.zip_code_state, elementState.zipCodeState)
            binding.combinedState.text = getString(R.string.combined_state, elementState)
        }

        viewModel.isLoading.observe(viewLifecycleOwner, ::handleIsLoadingChange)
        viewModel.paymentMethod.observe(viewLifecycleOwner, ::handlePaymentMethodChange)
        viewModel.error.observe(viewLifecycleOwner, ::handleErrorChange)

        binding.submitButton.setOnClickListener {
            viewModel.onSubmit(binding.tokenizeForagePaymentSheet)
        }

        binding.nextButton.setOnClickListener {
            findNavController().navigate(
                FlowTokenizePaymentSheetFragmentDirections.actionTokenizeCreditToCreateCreditPayment(
                    bearer = viewModel.args.bearer,
                    merchantAccount = viewModel.args.merchantAccount,
                    paymentMethodRef = viewModel.paymentMethod.value?.ref.orEmpty()
                )
            )
        }

        binding.tokenizeForagePaymentSheet.setOnChangeEventListener(::handlePaymentSheetChange)

        //
        // Render initial state
        //
        handlePaymentSheetChange(binding.tokenizeForagePaymentSheet.getElementState())
        handlePaymentMethodChange(null)
        handleErrorChange(null)

        return binding.root
    }
}

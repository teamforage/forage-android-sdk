package com.joinforage.android.example.ui.complete.flow.tokenize

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.joinforage.android.example.databinding.FragmentFlowTokenizeSheetBinding
import com.joinforage.forage.android.core.services.ForageConfig
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FlowTokenizeFragmentSheet : Fragment() {

    private val viewModel: FlowTokenizeViewModelSheet by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFlowTokenizeSheetBinding.inflate(inflater, container, false)

        // As soon as possible set the forage context on the ForageElement.
        binding.tokenizeForagePaymentSheet.setForageConfig(
            ForageConfig(
                viewModel.args.merchantAccount,
                viewModel.args.bearer
            )
        )

        viewModel.paymentMethod.observe(viewLifecycleOwner) { paymentMethod ->
            binding.paymentMethodRef.text =
                if (paymentMethod == null) "" else "ref: ${paymentMethod.ref}"
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            binding.errorResponse.text = error ?: ""
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        binding.submitButton.setOnClickListener {
            try {
                // Clear any previous error message.
                binding.errorResponse.text = ""

                viewModel.onSubmit(binding.tokenizeForagePaymentSheet)
            } catch (e: Exception) {
                binding.errorResponse.text = e.toString()
            }
        }

        return binding.root
    }
}

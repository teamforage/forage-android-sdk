package com.joinforage.android.example.ui.complete.flow.payment.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.joinforage.android.example.databinding.FragmentFlowCreatePaymentBinding
import com.joinforage.android.example.ext.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FlowCreatePaymentFragment : Fragment() {

    private val viewModel: FlowCreatePaymentViewModel by viewModels()

    private var _binding: FragmentFlowCreatePaymentBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFlowCreatePaymentBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val progressBar: ProgressBar = binding.progressBar
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            when (isLoading) {
                true -> progressBar.visibility = View.VISIBLE
                else -> progressBar.visibility = View.GONE
            }
        }

        binding.submitSnapAmount.setOnClickListener {
            viewModel.submitSnapAmount(
                getSnapAmount()
            )
            it.context.hideKeyboard(it)
        }

        binding.submitNonSnapAmount.setOnClickListener {
            viewModel.submitNonSnapAmount(
                getNonSnapAmount()
            )
            it.context.hideKeyboard(it)
        }

        viewModel.snapPaymentResult.observe(viewLifecycleOwner) {
            it?.let {
                binding.snapResponse.text = """
                    Amount: ${it.amount}
                    Funding Type: ${it.fundingType}
                    
                    $it
                """.trimIndent()

                binding.nextButton.visibility = View.VISIBLE
            }
        }

        viewModel.nonSnapPaymentResult.observe(viewLifecycleOwner) {
            it?.let {
                binding.nonSnapResponse.text = """
                    Amount: ${it.amount}
                    Funding Type: ${it.fundingType}
                    
                    $it
                """.trimIndent()

                binding.nextButton.visibility = View.VISIBLE
            }
        }

        binding.nextButton.setOnClickListener {
            findNavController().navigate(
                FlowCreatePaymentFragmentDirections.actionFlowCreatePaymentFragmentToFlowCapturePaymentFragment(
                    bearer = viewModel.bearer,
                    merchantAccount = viewModel.merchantAccount,
                    paymentMethodRef = viewModel.paymentMethodRef,
                    snapAmount = getSnapAmount(),
                    cashAmount = getNonSnapAmount(),
                    snapPaymentRef = viewModel.snapPaymentResult.value?.ref.orEmpty(),
                    cashPaymentRef = viewModel.nonSnapPaymentResult.value?.ref.orEmpty()
                )
            )
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getSnapAmount(): Long {
        return try {
            binding.snapAmountEditText.text.toString().toLong()
        } catch (e: NumberFormatException) {
            0
        }
    }

    private fun getNonSnapAmount(): Long {
        return try {
            binding.nonSnapAmountEditText.text.toString().toLong()
        } catch (e: NumberFormatException) {
            0
        }
    }
}

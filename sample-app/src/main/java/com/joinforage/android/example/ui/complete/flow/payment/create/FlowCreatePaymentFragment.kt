package com.joinforage.android.example.ui.complete.flow.payment.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.joinforage.android.example.databinding.FragmentFlowCreatePaymentBinding
import com.joinforage.android.example.ext.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FlowCreatePaymentFragment : Fragment() {

    private val viewModel: FlowCreatePaymentViewModel by viewModels()

    private var _binding: FragmentFlowCreatePaymentBinding? = null

    private val binding get() = _binding!!

    private var lastUsedSnapPaymentRef: String? = null
    private var lastUsedEbtCashPaymentRef: String? = null

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

        binding.setSnapRef.setOnClickListener {
            viewModel.setSnapRef(
                getPaymentRef(binding.snapAmountEditText)
            )
            it.context.hideKeyboard(it)
        }

        binding.submitEbtCashAmount.setOnClickListener {
            viewModel.submitEbtCashAmount(
                getEbtCashAmount()
            )
            it.context.hideKeyboard(it)
        }

        binding.setEbtCashRef.setOnClickListener {
            viewModel.setEbtCashRef(
                getPaymentRef(binding.ebtCashAmountEditText)
            )
            it.context.hideKeyboard(it)
        }

        viewModel.snapPaymentResult.observe(viewLifecycleOwner) {
            binding.snapPaymentRefResponse.text = ""
            it?.let {
                binding.snapPaymentResponse.text = """
                    Amount: ${it.amount}
                    Funding Type: ${it.fundingType}
                    
                    $it
                """.trimIndent()
                lastUsedSnapPaymentRef = it.ref
            }
        }

        viewModel.snapPaymentRefResult.observe(viewLifecycleOwner) {
            binding.snapPaymentResponse.text = ""
            it?.let {
                binding.snapPaymentRefResponse.text = "PaymentRef: $it"
                lastUsedSnapPaymentRef = it
            }
        }

        viewModel.ebtCashPaymentResult.observe(viewLifecycleOwner) {
            binding.ebtCashPaymentRefResponse.text = ""
            it?.let {
                binding.ebtCashResponse.text = """
                    Amount: ${it.amount}
                    Funding Type: ${it.fundingType}
                    
                    $it
                """.trimIndent()
                lastUsedEbtCashPaymentRef = it.ref
            }
        }

        viewModel.ebtCashPaymentRefResult.observe(viewLifecycleOwner) {
            binding.ebtCashResponse.text = ""
            it?.let {
                binding.ebtCashPaymentRefResponse.text = "PaymentRef: $it"
                lastUsedEbtCashPaymentRef = it
            }
        }

        binding.nextButton.setOnClickListener {
            findNavController().navigate(
                FlowCreatePaymentFragmentDirections.actionFlowCreatePaymentFragmentToFlowCapturePaymentFragment(
                    bearer = viewModel.bearer,
                    merchantAccount = viewModel.merchantAccount,
                    paymentMethodRef = viewModel.paymentMethodRef,
                    snapPaymentRef = lastUsedSnapPaymentRef.orEmpty(),
                    cashPaymentRef = lastUsedEbtCashPaymentRef.orEmpty()
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

    private fun getPaymentRef(textField: TextInputEditText): String {
        return try {
            textField.text.toString()
        } catch (e: NumberFormatException) {
            "Unknown value"
        }
    }

    private fun getEbtCashAmount(): Long {
        return try {
            binding.ebtCashAmountEditText.text.toString().toLong()
        } catch (e: NumberFormatException) {
            0
        }
    }
}

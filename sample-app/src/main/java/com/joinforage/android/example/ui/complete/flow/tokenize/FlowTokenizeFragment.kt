package com.joinforage.android.example.ui.complete.flow.tokenize

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.joinforage.android.example.databinding.FragmentFlowTokenizeBinding
import com.joinforage.android.example.ext.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FlowTokenizeFragment : Fragment() {

    private val viewModel: FlowTokenizeViewModel by viewModels()

    private var _binding: FragmentFlowTokenizeBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFlowTokenizeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.submitButton.apply {
            setOnClickListener {
                viewModel.onSubmit()
                it.context.hideKeyboard(it)
            }
        }

        val paymentRef: TextView = binding.paymentRef
        val cardLast4: TextView = binding.cardLast4
        val customerId: TextView = binding.customerId

        viewModel.paymentMethod.observe(viewLifecycleOwner) {
            when (it == null) {
                true -> {
                    paymentRef.text = "ref:"
                    cardLast4.text = "last_4:"
                    customerId.text = "customer_id:"
                    binding.nextButton.visibility = View.GONE
                }
                else -> {
                    paymentRef.text = "ref: ${it.ref}"
                    cardLast4.text = "last_4: ${it.card?.last4}"
                    customerId.text = "customer_id: ${it?.customerId}"
                    binding.nextButton.visibility = View.VISIBLE
                }
            }
        }

        viewModel.error.observe(viewLifecycleOwner) {
            it?.let {
                binding.errorResponse.text = it
            }
        }

        val progressBar: ProgressBar = binding.progressBar

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            when (isLoading) {
                true -> progressBar.visibility = View.VISIBLE
                else -> progressBar.visibility = View.GONE
            }
        }

        binding.nextButton.apply {
            setOnClickListener {
                findNavController().navigate(
                    FlowTokenizeFragmentDirections.actionNavigationFlowTokenizeToFlowBalanceFragment(
                        bearer = viewModel.bearer,
                        merchantAccount = viewModel.merchantAccount,
                        paymentMethodRef = viewModel.paymentMethod.value?.ref.orEmpty(),
                        customerId = viewModel.paymentMethod.value?.customerId.orEmpty()
                    )
                )
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

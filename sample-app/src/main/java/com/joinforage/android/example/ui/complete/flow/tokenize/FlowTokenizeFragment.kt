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
import com.joinforage.forage.android.core.ui.element.ForageConfig
import com.joinforage.forage.android.ecom.ui.ForagePANEditText
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FlowTokenizeFragment : Fragment() {

    private val viewModel: FlowTokenizeViewModel by viewModels()

    private var _binding: FragmentFlowTokenizeBinding? = null

    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tokenizeForagePanEditText.requestFocus()
        // our CI tests fail when we automatically show the keyboard
        // because it covers certain elements. So this code is
        // commented out by default. Uncomment it to make your
        // dev experience slightly better :)
//        foragePinEditText.showKeyboard()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFlowTokenizeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.submitButton.apply {
            setOnClickListener {
                viewModel.onSubmit(
                    foragePanEditText = binding.tokenizeForagePanEditText
                )
                it.context.hideKeyboard(it)
            }
        }

        // as soon as possible set the forage context on
        // the ForageElement
        val foragePanEditText: ForagePANEditText = binding.tokenizeForagePanEditText
        foragePanEditText.setForageConfig(
            ForageConfig(
                merchantId = viewModel.merchantAccount,
                sessionToken = viewModel.bearer
            )
        )

        foragePanEditText.requestFocus()

        val paymentMethodRef: TextView = binding.paymentMethodRef
        val cardLast4: TextView = binding.cardLast4
        val reusable: TextView = binding.reusable
        val customerId: TextView = binding.customerId
        val isFocused: TextView = binding.isFocused
        val isComplete: TextView = binding.isComplete
        val isEmpty: TextView = binding.isEmpty
        val isValid: TextView = binding.isValid
        val usState: TextView = binding.usState

        fun setState() {
            val state = foragePanEditText.getElementState()
            isFocused.text = "isFocused: ${state.isFocused}"
            isComplete.text = "isComplete: ${state.isComplete}"
            isEmpty.text = "isEmpty: ${state.isEmpty}"
            isValid.text = "isValid: ${state.isValid}"
            usState.text = "usState: ${state.derivedCardInfo.usState?.abbreviation}"
        }

        foragePanEditText.setOnFocusEventListener { setState() }
        foragePanEditText.setOnChangeEventListener { setState() }

        val state = foragePanEditText.getElementState()
        isFocused.text = "isFocused: ${state.isFocused}"
        isComplete.text = "isComplete: ${state.isComplete}"
        isEmpty.text = "isEmpty: ${state.isEmpty}"
        isValid.text = "isValid: ${state.isValid}"
        usState.text = "usState: ${state.derivedCardInfo.usState?.abbreviation}"

        viewModel.paymentMethod.observe(viewLifecycleOwner) {
            when (it == null) {
                true -> {
                    paymentMethodRef.text = "ref:"
                    cardLast4.text = "last_4:"
                    reusable.text = "reusable:"
                    customerId.text = "customer_id:"
                    binding.nextButton.visibility = View.GONE
                }
                else -> {
                    paymentMethodRef.text = "ref: ${it.ref}"
                    cardLast4.text = "last_4: ${it.card?.last4}"
                    reusable.text = "reusable: ${it.reusable}"
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

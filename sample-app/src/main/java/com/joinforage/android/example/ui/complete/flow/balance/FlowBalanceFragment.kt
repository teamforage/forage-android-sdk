package com.joinforage.android.example.ui.complete.flow.balance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.joinforage.android.example.databinding.FragmentFlowBalanceBinding
import com.joinforage.android.example.ext.hideKeyboard
import com.joinforage.forage.android.core.ui.element.ForageConfig
import com.joinforage.forage.android.ecom.ui.element.ForagePINEditText
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FlowBalanceFragment : Fragment() {

    private val viewModel: FlowBalanceViewModel by viewModels()

    private var _binding: FragmentFlowBalanceBinding? = null

    private val binding get() = _binding!!

    private lateinit var foragePinEditText: ForagePINEditText
    private lateinit var snap: TextView
    private lateinit var nonSnap: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        foragePinEditText.requestFocus()
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
        _binding = FragmentFlowBalanceBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val progressBar: ProgressBar = binding.progressBar
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            when (isLoading) {
                true -> progressBar.visibility = View.VISIBLE
                else -> progressBar.visibility = View.GONE
            }
        }

        snap = binding.snap
        nonSnap = binding.nonSnap

        // as soon as possible set the forage context on
        // the ForageElement
        foragePinEditText = binding.foragePinEditText
        foragePinEditText.setForageConfig(
            ForageConfig(
                merchantId = viewModel.merchantAccount,
                sessionToken = viewModel.bearer
            )
        )

        val isFocused: TextView = binding.isFocused
        val isComplete: TextView = binding.isComplete
        val isEmpty: TextView = binding.isEmpty
        val isValid: TextView = binding.isValid

        fun setState() {
            val state = foragePinEditText.getElementState()
            isFocused.text = "isFocused: ${state.isFocused}"
            isComplete.text = "isComplete: ${state.isComplete}"
            isEmpty.text = "isEmpty: ${state.isEmpty}"
            isValid.text = "isValid: ${state.isValid}"
        }

        foragePinEditText.setOnFocusEventListener { setState() }
        foragePinEditText.setOnChangeEventListener { setState() }

        val state = foragePinEditText.getElementState()
        isFocused.text = "isFocused: ${state.isFocused}"
        isComplete.text = "isComplete: ${state.isComplete}"
        isEmpty.text = "isEmpty: ${state.isEmpty}"
        isValid.text = "isValid: ${state.isValid}"

        binding.checkBalanceButton.setOnClickListener {
            requireContext().hideKeyboard(it)
            viewModel.checkBalance(
                requireContext(),
                foragePinEditText
            )
        }

        viewModel.nonSnap.observe(viewLifecycleOwner) {
            nonSnap.text = it
        }

        viewModel.snap.observe(viewLifecycleOwner) {
            snap.text = it
        }

        binding.nextButton.setOnClickListener {
            findNavController().navigate(
                FlowBalanceFragmentDirections.actionFlowBalanceFragmentToFlowCreatePaymentFragment(
                    viewModel.bearer,
                    viewModel.merchantAccount,
                    viewModel.paymentMethodRef
                )
            )
        }

        viewModel.error.observe(viewLifecycleOwner) {
            binding.error.text = it
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

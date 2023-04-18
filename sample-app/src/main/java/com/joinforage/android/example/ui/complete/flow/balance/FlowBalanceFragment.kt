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
import com.joinforage.forage.android.ui.ForagePINEditText
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FlowBalanceFragment : Fragment() {

    private val viewModel: FlowBalanceViewModel by viewModels()

    private var _binding: FragmentFlowBalanceBinding? = null

    private val binding get() = _binding!!

    private lateinit var foragePinEditText: ForagePINEditText
    private lateinit var snap: TextView
    private lateinit var nonSnap: TextView

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

        foragePinEditText = binding.foragePinEditText
        snap = binding.snap
        nonSnap = binding.nonSnap

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

        viewModel.isNextVisible.observe(viewLifecycleOwner) {
            if (it == null || !it) {
                binding.nextButton.visibility = View.GONE
            } else {
                binding.nextButton.visibility = View.VISIBLE
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

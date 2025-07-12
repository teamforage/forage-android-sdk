package com.joinforage.android.example.ui.complete.flow.tokens

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.joinforage.android.example.R
import com.joinforage.android.example.databinding.FragmentFlowTokensBinding
import com.joinforage.android.example.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FlowTokensFragment : BaseFragment<FragmentFlowTokensBinding>(R.layout.fragment_flow_tokens) {

    override val viewModel: TokensViewModel by viewModels()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.enterEbtButton.apply {
            setOnClickListener {
                findNavController().navigate(
                    R.id.action_navigation_complete_flow_to_FlowTokenizeFragment,
                    viewModel.getBundle()
                )
            }
        }

        binding.enterCreditButton.apply {
            setOnClickListener {
                findNavController().navigate(
                    R.id.action_navigation_complete_flow_to_FlowTokenizeFragment_sheet,
                    viewModel.getBundle()
                )
            }
        }
    }
}

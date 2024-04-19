package com.joinforage.android.example.ui.complete.flow.tokens

import androidx.fragment.app.viewModels
import com.joinforage.android.example.R
import com.joinforage.android.example.databinding.FragmentFlowTokensBinding
import com.joinforage.android.example.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FlowTokensFragment : BaseFragment<FragmentFlowTokensBinding>(R.layout.fragment_flow_tokens) {
    override val viewModel: TokensViewModel by viewModels()
}

package com.joinforage.android.example.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.joinforage.android.example.BR

abstract class BaseFragment<DB : ViewDataBinding>(
    @LayoutRes private val layoutRes: Int
) : Fragment() {

    private var _binding: DB? = null

    protected val binding get() = _binding!!

    abstract val viewModel: BaseViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = setUpDataBinding(inflater, container)

        return binding.root
    }

    private fun setUpDataBinding(inflater: LayoutInflater, container: ViewGroup?) =
        DataBindingUtil.inflate<DB>(
            inflater,
            layoutRes,
            container,
            false
        ).apply {
            setVariable(BR.viewModel, viewModel)
            lifecycleOwner = viewLifecycleOwner
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

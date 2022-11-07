package com.joinforage.android.example.ui.complete.flow.payment.capture

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.joinforage.android.example.databinding.FragmentFlowCapturePaymentBinding
import com.joinforage.android.example.ext.hideKeyboard
import com.joinforage.forage.android.ForageSDK
import com.joinforage.forage.android.network.model.Response
import com.joinforage.forage.android.network.model.ResponseListener
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject

@AndroidEntryPoint
class FlowCapturePaymentFragment : Fragment() {

    private val viewModel: FlowCapturePaymentViewModel by viewModels()

    private var _binding: FragmentFlowCapturePaymentBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFlowCapturePaymentBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val progressBar: ProgressBar = binding.progressBar
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            when (isLoading) {
                true -> progressBar.visibility = View.VISIBLE
                else -> progressBar.visibility = View.GONE
            }
        }

        binding.captureSnapAmount.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE

            ForageSDK.capturePayment(
                context = it.context,
                pinForageEditText = binding.snapPinEditText,
                merchantAccount = viewModel.merchantAccount,
                bearerToken = viewModel.bearer,
                paymentRef = viewModel.snapPaymentRef,
                cardToken = viewModel.cardToken,
                onResponseListener = object : ResponseListener {
                    override fun onResponse(response: Response?) {
                        binding.progressBar.visibility = View.GONE

                        when (response) {
                            is Response.SuccessResponse -> {
                                println("Capture Payment Response Code: \n ${response.successCode}")
                                println("Capture Payment Response Code: \n $response")

                                val resp = response.body?.let { JSONObject(it) }

                                activity?.runOnUiThread {
                                    binding.snapResponse.text = resp.toString()
                                }
                            }
                            is Response.ErrorResponse -> {
                                println("Capture Payment Response Code: \n $response")
                                activity?.runOnUiThread {
                                    binding.snapResponse.text = response.body.toString()
                                }
                            }
                            else -> {
                            }
                        }
                    }
                }
            )
            it.context.hideKeyboard(it)
        }

        binding.captureNonSnapAmount.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE

            ForageSDK.capturePayment(
                context = it.context,
                pinForageEditText = binding.cashPinEditText,
                merchantAccount = viewModel.merchantAccount,
                bearerToken = viewModel.bearer,
                paymentRef = viewModel.cashPaymentRef,
                cardToken = viewModel.cardToken,
                onResponseListener = object : ResponseListener {
                    override fun onResponse(response: Response?) {
                        binding.progressBar.visibility = View.GONE
                        when (response) {
                            is Response.SuccessResponse -> {
                                println("Capture Payment Response Code: \n ${response.successCode}")
                                println("Capture Payment Response Code: \n $response")

                                val resp = response.body?.let { JSONObject(it) }
                                binding.cashResponse.text = resp.toString()
                            }
                            is Response.ErrorResponse -> {
                                println("Capture Payment Response Code: \n $response")
                                binding.cashResponse.text = response.body.toString()
                            }
                            else -> {
                            }
                        }
                    }
                }
            )
            it.context.hideKeyboard(it)
        }

        viewModel.uiState.observe(viewLifecycleOwner) {
            if (it.isCaptureSnapVisible) {
                binding.captureSnapAmount.visibility = View.VISIBLE
                binding.snapAmountEditText.visibility = View.VISIBLE
                binding.snapAmountEditText.setText(it.snapAmount.toString())
                binding.snapPinEditText.visibility = View.VISIBLE
            } else {
                binding.captureSnapAmount.visibility = View.GONE
                binding.snapAmountEditText.visibility = View.GONE
                binding.snapPinEditText.visibility = View.GONE
            }

            if (it.isCaptureCashVisible) {
                binding.captureNonSnapAmount.visibility = View.VISIBLE
                binding.nonSnapAmountEditText.visibility = View.VISIBLE
                binding.nonSnapAmountEditText.setText(it.cashAmount.toString())
            } else {
                binding.captureNonSnapAmount.visibility = View.GONE
                binding.nonSnapAmountEditText.visibility = View.GONE
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

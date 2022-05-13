package com.kovalak.bakalarka

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.kovalak.bakalarka.databinding.ActivityMainBinding
import com.kovalak.bakalarka.entities.Result
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        Timber.plant(Timber.DebugTree())

        binding.connectButton.setOnClickListener {
            binding.ip.clearFocus()
            hideKeyboard()
            viewModel.onConnect(binding.ip.text.toString())
        }
        binding.cancelButton.setOnClickListener { viewModel.onCancel() }

        viewModel.result.observe(this, ::onResult)
    }

    private fun onResult(result: Result) {
        when (result) {
            Result.Processing -> {
                binding.resultText.text = ""
                binding.connectButton.visibility = View.INVISIBLE
                binding.progressIndicator.visibility = View.VISIBLE
                binding.cancelButton.visibility = View.VISIBLE
            }
            is Result.Failure -> {
                binding.connectButton.visibility = View.VISIBLE
                binding.progressIndicator.visibility = View.INVISIBLE
                binding.cancelButton.visibility = View.INVISIBLE

                binding.resultText.text = getString(R.string.failure, result.message)
            }
            is Result.Success -> {
                binding.connectButton.visibility = View.VISIBLE
                binding.progressIndicator.visibility = View.INVISIBLE
                binding.cancelButton.visibility = View.INVISIBLE

                binding.resultText.text =
                    getString(R.string.success, result.key.toHexString())

                viewModel.save3DESKey(result.key)
            }
        }
    }

    private fun hideKeyboard() {
        val imm: InputMethodManager =
            this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }
}
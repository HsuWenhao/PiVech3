package com.example.pivech3.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.pivech3.R
import com.example.pivech3.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // Slider and label logic
        val slider = binding.slider
        val sliderValueLabel = binding.sliderValueLabel
        // 初始化时设置为0
        slider.progress = 0
        sliderValueLabel.text = getString(R.string.slider_value, 0)
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                sliderValueLabel.text = getString(R.string.slider_value, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val value = seekBar?.progress ?: 0
                Toast.makeText(
                    requireContext(),
                    getString(R.string.slider_value_alert, value),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        // Joystick + live value label
        val joystickValueLabel = binding.joystickValueLabel
        joystickValueLabel.text = getString(R.string.joystick_value, 0f, 0f)
        binding.joystickView.setOnMoveListener { x, y ->
            joystickValueLabel.text = getString(R.string.joystick_value, x, y)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
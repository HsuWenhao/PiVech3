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
import kotlin.math.hypot

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
        val joystickVectorValue = binding.joystickVectorValue
        val joystickXValue = binding.joystickXValue
        val joystickYValue = binding.joystickYValue

        fun renderJoystick(x: Float, y: Float) {
            joystickXValue.text = getString(R.string.joystick_x_value, x)
            joystickYValue.text = getString(R.string.joystick_y_value, y)
            val mag = hypot(x.toDouble(), y.toDouble()).toFloat()
            joystickVectorValue.text = getString(R.string.joystick_vector_value, mag)
        }

        renderJoystick(0f, 0f)
        binding.joystickView.setOnMoveListener { x, y ->
            renderJoystick(x, y)
        }

        // Joystick2 + live value label (independent)
        val joystickVectorValue2 = binding.joystickVectorValue2
        val joystickXValue2 = binding.joystickXValue2
        val joystickYValue2 = binding.joystickYValue2

        fun renderJoystick2(x: Float, y: Float) {
            joystickXValue2.text = getString(R.string.joystick_x_value, x)
            joystickYValue2.text = getString(R.string.joystick_y_value, y)
            val mag = hypot(x.toDouble(), y.toDouble()).toFloat()
            joystickVectorValue2.text = getString(R.string.joystick_vector_value, mag)
        }

        renderJoystick2(0f, 0f)
        binding.joystickView2.setOnMoveListener { x, y ->
            renderJoystick2(x, y)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
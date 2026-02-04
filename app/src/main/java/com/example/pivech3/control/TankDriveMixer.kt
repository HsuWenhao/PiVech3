package com.example.pivech3.control

import kotlin.math.roundToInt

/**
 * Pure mixer: maps joystick (x,y) in [-1,1] to left/right motor speed in [-100,100].
 * Convention: +Y is downward (from JoystickView), so forward is -y.
 */
object TankDriveMixer {

    data class MotorSpeeds(val left: Int, val right: Int)

    fun mix(x: Float, y: Float): MotorSpeeds {
        val fx = x.coerceIn(-1f, 1f)
        val fy = y.coerceIn(-1f, 1f)

        val forward = -fy
        val left = ((forward + fx) * 100f).roundToInt().coerceIn(-100, 100)
        val right = ((forward - fx) * 100f).roundToInt().coerceIn(-100, 100)
        return MotorSpeeds(left = left, right = right)
    }
}

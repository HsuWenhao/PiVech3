package com.example.pivech3.control

import org.junit.Assert.assertEquals
import org.junit.Test

class TankDriveMixerTest {

    @Test
    fun forward_is_both_positive() {
        val s = TankDriveMixer.mix(0f, -1f)
        assertEquals(100, s.left)
        assertEquals(100, s.right)
    }

    @Test
    fun backward_is_both_negative() {
        val s = TankDriveMixer.mix(0f, 1f)
        assertEquals(-100, s.left)
        assertEquals(-100, s.right)
    }

    @Test
    fun turn_right_in_place() {
        val s = TankDriveMixer.mix(1f, 0f)
        assertEquals(100, s.left)
        assertEquals(-100, s.right)
    }
}

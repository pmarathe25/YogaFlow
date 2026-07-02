package com.example.game.battle

import kotlin.random.Random

interface RandomProvider {
    fun nextFloat(): Float
    fun nextInt(until: Int): Int
}

object DefaultRandomProvider : RandomProvider {
    override fun nextFloat(): Float = Random.nextFloat()
    override fun nextInt(until: Int): Int = Random.nextInt(until)
}

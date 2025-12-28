package com.marrineer.elyEco.models

import com.marrineer.elyEco.models.data.EconomyStats
import com.marrineer.elyEco.models.data.PlayerStats

data class PlayerProfile(
    val player: PlayerStats,
    val economy: EconomyStats
)

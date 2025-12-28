package com.marrineer.elyEco.models

import com.marrineer.elyEco.models.stats.EconomyStats
import com.marrineer.elyEco.models.stats.PlayerStats

data class PlayerProfile(
    val player: PlayerStats,
    val economy: EconomyStats
)

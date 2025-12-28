package com.marrineer.elyEco.models

import com.marrineer.elyEco.models.stats.EconomyStats
import com.marrineer.elyEco.models.stats.PlayerStats

/**
 * A container for all data related to a specific player.
 */
data class PlayerProfile(
    val playerStats: PlayerStats,
    val economyStats: EconomyStats
) {
    val uuid get() = playerStats.uuid
    val username get() = playerStats.username
    val balance get() = playerStats.balance
}
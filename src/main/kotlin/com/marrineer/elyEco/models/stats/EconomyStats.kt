package com.marrineer.elyEco.models.stats

import java.util.*

data class EconomyStats(
    val uuid: UUID,
    var totalEarned: Double,
    var totalSpent: Double
)
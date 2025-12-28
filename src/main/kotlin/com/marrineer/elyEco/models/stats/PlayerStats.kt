package com.marrineer.elyEco.models.stats

import java.util.*

data class PlayerStats(
    val uuid: UUID,
    var balance: Double,
    val createdAt: Long,
    var updatedAt: Long
)

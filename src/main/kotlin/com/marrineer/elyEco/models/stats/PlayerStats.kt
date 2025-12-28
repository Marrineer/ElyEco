package com.marrineer.elyEco.models.stats

import java.util.*

data class PlayerStats(
    val uuid: UUID,
    var username: String,
    var balance: Double,
    val firstSeen: Long,
    var lastSeen: Long
)
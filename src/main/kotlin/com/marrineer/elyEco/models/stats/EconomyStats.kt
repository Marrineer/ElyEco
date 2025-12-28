package com.marrineer.elyEco.models.stats

import java.util.*

data class EconomyStats(
    val uuid: UUID,
    var totalReceived: Double = 0.0,
    var totalSent: Double = 0.0,
    var transferCount: Int = 0,
    var lastTransferAt: Long? = null
)

package com.marrineer.elyEco.models.data

import java.util.UUID

data class EconomyStats(
    val uuid: UUID,
    var totalReceived: Double = 0.0,
    var totalSent: Double = 0.0,
    var transferCount: Int = 0,
    var lastTransferAt: Long? = null
)

package com.marrineer.elyEco.models.data

import java.util.UUID

data class PlayerStats(
    val uuid: UUID,
    var balance: Double,
    val createdAt: Long,
    var updatedAt: Long
)

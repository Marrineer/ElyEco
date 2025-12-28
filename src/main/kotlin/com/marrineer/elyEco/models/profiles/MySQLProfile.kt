package com.marrineer.elyEco.models.profiles

data class MySQLProfile(
    val host: String,
    val port: Int,
    val dbname: String,
    val username: String,
    val password: String,
    val usessl: Boolean
)
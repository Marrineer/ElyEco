package com.marrineer.elyEco.models.profiles

import com.marrineer.elyEco.data.DatabaseManager
import com.marrineer.elyEco.models.PlayerProfile

class SQLiteProfile(dbManager: DatabaseManager) : AbstractProfileDao(dbManager) {

    override suspend fun saveBatch(profiles: Collection<PlayerProfile>) {
        val playerSql = """
            INSERT OR REPLACE INTO elyeco_players (uuid, username, balance, first_seen, last_seen) 
            VALUES (?, ?, ?, ?, ?);
        """.trimIndent()
        val statsSql = """
            INSERT OR REPLACE INTO elyeco_stats (uuid, total_earned, total_spent) 
            VALUES (?, ?, ?);
        """.trimIndent()

        dbManager.query { conn ->
            conn.autoCommit = false
            try {
                conn.prepareStatement(playerSql).use { psPlayer ->
                    conn.prepareStatement(statsSql).use { psStats ->
                        for (profile in profiles) {
                            psPlayer.setString(1, profile.uuid.toString())
                            psPlayer.setString(2, profile.username)
                            psPlayer.setDouble(3, profile.balance)
                            psPlayer.setLong(4, profile.playerStats.firstSeen)
                            psPlayer.setLong(5, profile.playerStats.lastSeen)
                            psPlayer.addBatch()

                            psStats.setString(1, profile.uuid.toString())
                            psStats.setDouble(2, profile.economyStats.totalEarned)
                            psStats.setDouble(3, profile.economyStats.totalSpent)
                            psStats.addBatch()
                        }
                        psPlayer.executeBatch()
                        psStats.executeBatch()
                    }
                }
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            } finally {
                conn.autoCommit = true
            }
        }
    }

    override suspend fun create(profile: PlayerProfile) {
        // In SQLite, "INSERT OR REPLACE" handles this, so we can just call update.
        update(profile)
    }

    override suspend fun update(profile: PlayerProfile) {
        // Using saveBatch with a single item is efficient enough and avoids code duplication.
        saveBatch(listOf(profile))
    }
}

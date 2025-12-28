package com.marrineer.elyEco.models.profiles

import com.marrineer.elyEco.data.DatabaseManager
import com.marrineer.elyEco.models.PlayerProfile
import com.marrineer.elyEco.models.stats.EconomyStats
import com.marrineer.elyEco.models.stats.PlayerStats
import java.sql.ResultSet
import java.util.*

abstract class AbstractProfileDao(protected val dbManager: DatabaseManager) : ProfileDao {

    protected fun fromResultSet(rs: ResultSet): PlayerProfile {
        val uuid = UUID.fromString(rs.getString("uuid"))
        val playerStats = PlayerStats(
            uuid = uuid,
            username = rs.getString("username"),
            balance = rs.getDouble("balance"),
            firstSeen = rs.getLong("first_seen"),
            lastSeen = rs.getLong("last_seen")
        )
        // Handle cases where a player might not have stats yet.
        val economyStats = EconomyStats(
            uuid = uuid,
            totalEarned = rs.getDouble("total_earned"),
            totalSpent = rs.getDouble("total_spent")
        )
        return PlayerProfile(playerStats, economyStats)
    }

    override suspend fun get(uuid: UUID): PlayerProfile? {
        val sql = """
            SELECT p.*, s.total_earned, s.total_spent
            FROM elyeco_players p
            LEFT JOIN elyeco_stats s ON p.uuid = s.uuid
            WHERE p.uuid = ?
        """.trimIndent()
        return dbManager.query { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, uuid.toString())
                ps.executeQuery().use { rs ->
                    if (rs.next()) fromResultSet(rs) else null
                }
            }
        }
    }

    override suspend fun getAll(): Map<UUID, PlayerProfile> {
        val sql = """
            SELECT p.*, s.total_earned, s.total_spent
            FROM elyeco_players p
            LEFT JOIN elyeco_stats s ON p.uuid = s.uuid
        """.trimIndent()
        return dbManager.query { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.executeQuery().use { rs ->
                    val profiles = mutableMapOf<UUID, PlayerProfile>()
                    while (rs.next()) {
                        val profile = fromResultSet(rs)
                        profiles[profile.uuid] = profile
                    }
                    profiles
                }
            }
        } ?: emptyMap()
    }

    override suspend fun getTop(limit: Int): List<PlayerProfile> {
        val sql = """
            SELECT p.*, s.total_earned, s.total_spent
            FROM elyeco_players p
            LEFT JOIN elyeco_stats s ON p.uuid = s.uuid
            ORDER BY p.balance DESC
            LIMIT ?
        """.trimIndent()
        return dbManager.query { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setInt(1, limit)
                ps.executeQuery().use { rs ->
                    val profiles = mutableListOf<PlayerProfile>()
                    while (rs.next()) {
                        profiles.add(fromResultSet(rs))
                    }
                    profiles
                }
            }
        } ?: emptyList()
    }
}

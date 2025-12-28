package com.marrineer.elyEco.managers

import com.marrineer.elyEco.ElyEco
import com.marrineer.elyEco.data.ConfigManager
import com.marrineer.elyEco.data.DatabaseManager
import com.marrineer.elyEco.models.PlayerProfile
import com.marrineer.elyEco.models.profiles.MySQLProfile
import com.marrineer.elyEco.models.profiles.ProfileDao
import com.marrineer.elyEco.models.profiles.SQLiteProfile
import com.marrineer.elyEco.models.stats.EconomyStats
import com.marrineer.elyEco.models.stats.PlayerStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ProfileManager(private val plugin: ElyEco, private val dbManager: DatabaseManager) {

    private val profileDao: ProfileDao
    private val profileCache = ConcurrentHashMap<UUID, PlayerProfile>()
    private val dirtyProfiles = Collections.newSetFromMap(ConcurrentHashMap<UUID, Boolean>())

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        profileDao = if (dbManager.databaseType == DatabaseManager.DatabaseType.MYSQL) {
            MySQLProfile(dbManager)
        } else {
            SQLiteProfile(dbManager)
        }
        plugin.logger.info("Using ${dbManager.databaseType.displayName} profile DAO.")
    }

    fun handlePlayerJoin(player: Player) {
        scope.launch {
            val profile = profileDao.get(player.uniqueId)
            if (profile != null) {
                // Profile exists, update username and cache it
                profile.playerStats.username = player.name
                profile.playerStats.lastSeen = System.currentTimeMillis()
                profileCache[player.uniqueId] = profile
                dirtyProfiles.add(player.uniqueId)
                plugin.logger.info("Loaded profile for ${player.name}.")
            } else {
                // New player, create a default profile
                val now = System.currentTimeMillis()
                val newProfile = PlayerProfile(
                    playerStats = PlayerStats(
                        uuid = player.uniqueId,
                        username = player.name,
                        balance = ConfigManager.startingBalance,
                        firstSeen = now,
                        lastSeen = now
                    ),
                    economyStats = EconomyStats(
                        uuid = player.uniqueId,
                        totalEarned = 0.0,
                        totalSpent = 0.0
                    )
                )
                profileCache[player.uniqueId] = newProfile
                dirtyProfiles.add(player.uniqueId) // Mark as dirty to be saved by the next batch save
                plugin.logger.info("Created new profile for ${player.name}.")
            }
        }
    }

    fun handlePlayerQuit(player: Player) {
        val uuid = player.uniqueId
        val profile = profileCache[uuid]
        if (profile != null) {
            scope.launch {
                profileDao.update(profile)
                profileCache.remove(uuid)
                dirtyProfiles.remove(uuid)
                plugin.logger.info("Saved and unloaded profile for ${player.name}.")
            }
        }
    }

    fun getProfile(uuid: UUID): PlayerProfile? {
        return profileCache[uuid]
    }

    fun getProfile(player: Player): PlayerProfile? {
        return getProfile(player.uniqueId)
    }

    fun markDirty(uuid: UUID) {
        dirtyProfiles.add(uuid)
    }

    fun startSaveTask() {
        val interval = ConfigManager.cacheFlushInterval * 20L // Convert seconds to ticks
        plugin.server.scheduler.runTaskTimerAsynchronously(plugin, Runnable {
            // Launch in the manager's IO scope
            scope.launch {
                saveAllDirtyProfiles()
            }
        }, interval, interval)
    }

    suspend fun saveAllDirtyProfiles() {
        if (dirtyProfiles.isEmpty()) return

        val profilesToSave = dirtyProfiles.mapNotNull { profileCache[it] }
        if (profilesToSave.isEmpty()) {
            dirtyProfiles.clear()
            return
        }

        plugin.logger.info("Saving ${profilesToSave.size} dirty profiles...")
        profileDao.saveBatch(profilesToSave)
        dirtyProfiles.clear()
        plugin.logger.info("Save task complete.")
    }

    fun loadAllOnlinePlayers() {
        plugin.server.onlinePlayers.forEach { handlePlayerJoin(it) }
    }

    suspend fun getTopProfiles(limit: Int): List<PlayerProfile> {
        return profileDao.getTop(limit)
    }
}

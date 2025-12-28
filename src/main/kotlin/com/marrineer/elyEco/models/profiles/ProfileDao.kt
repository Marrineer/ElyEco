package com.marrineer.elyEco.models.profiles

import com.marrineer.elyEco.models.PlayerProfile
import java.util.*

/**
 * Data Access Object (DAO) interface for player profiles.
 * Defines the contract for all database operations related to player data.
 */
interface ProfileDao {
    /**
     * Retrieves a player profile from the database.
     * @param uuid The UUID of the player.
     * @return The PlayerProfile if found, otherwise null.
     */
    suspend fun get(uuid: UUID): PlayerProfile?

    /**
     * Retrieves all player profiles from the database.
     * @return A map of UUIDs to PlayerProfiles.
     */
    suspend fun getAll(): Map<UUID, PlayerProfile>

    /**
     * Saves a collection of player profiles to the database using an efficient batch operation.
     * This should perform an "upsert" (insert or update).
     * @param profiles The collection of profiles to save.
     */
    suspend fun saveBatch(profiles: Collection<PlayerProfile>)

    /**
     * Creates a new player profile in the database.
     * @param profile The PlayerProfile to create.
     */
    suspend fun create(profile: PlayerProfile)

    /**
     * Updates an existing player profile in the database.
     * @param profile The PlayerProfile to update.
     */
    suspend fun update(profile: PlayerProfile)

    /**
     * Retrieves a list of player profiles ordered by balance, descending.
     * @param limit The maximum number of profiles to return.
     * @return A list of PlayerProfiles.
     */
    suspend fun getTop(limit: Int): List<PlayerProfile>
}

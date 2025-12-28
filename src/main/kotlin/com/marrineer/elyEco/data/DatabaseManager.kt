package com.marrineer.elyEco.data

import com.marrineer.elyEco.ElyEco
import com.marrineer.elyEco.models.stats.EconomyStats
import com.marrineer.elyEco.models.PlayerProfile
import com.marrineer.elyEco.models.stats.PlayerStats
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.Bukkit
import java.io.File
import java.sql.Connection
import java.sql.Types
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class DatabaseManager(private val plugin: ElyEco, private val configManager: ConfigManager) {

    private var dataSource: HikariDataSource? = null
    private var databaseType: DatabaseType? = null

    private val dbPool = Executors.newFixedThreadPool(
        (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(2)
    ) { r ->
        Thread(r, "ElyEco-Database").apply { isDaemon = true }
    }

    private val playerProfiles = ConcurrentHashMap<UUID, PlayerProfile>()
    @Volatile private var isCacheInitialized = false

    init {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable {
            saveAll()
        }, 12000L, 12000L)
    }

    fun initialize() {
        try {
            val typeName = configManager.databaseType
            this.databaseType = DatabaseType.fromString(typeName)

            val config = HikariConfig().apply {
                poolName = "ElyEco-Pool"
                connectionTimeout = 30_000
                idleTimeout = 600_000
                maxLifetime = 1_800_000
                leakDetectionThreshold = 60_000
            }

            if (databaseType == DatabaseType.SQLITE) {
                setupSQLite(config)
            } else {
                setupMySQL(config)
            }

            this.dataSource = HikariDataSource(config)
            initializeDatabase()

            loadCache(null)

        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Could not initialize database pool", e)

            if (databaseType == DatabaseType.MYSQL) {
                plugin.logger.warning("Falling back to SQLite database...")
                setupFallbackSQLite()
            }
        }
    }

    private fun initializeDatabase() {
        dataSource?.connection?.use { conn ->
            conn.createStatement().use { stmt ->
                val playerTable = """
                    CREATE TABLE IF NOT EXISTS elyeco_players (
                        uuid VARCHAR(36) PRIMARY KEY,
                        balance DOUBLE NOT NULL DEFAULT 0.0,
                        created_at BIGINT NOT NULL,
                        updated_at BIGINT NOT NULL
                    );
                """.trimIndent()

                val ecoTable = """
                    CREATE TABLE IF NOT EXISTS elyeco_stats (
                        uuid VARCHAR(36) PRIMARY KEY,
                        total_received DOUBLE DEFAULT 0.0,
                        total_sent DOUBLE DEFAULT 0.0,
                        transfer_count INT DEFAULT 0,
                        last_transfer_at BIGINT,
                        FOREIGN KEY (uuid) REFERENCES elyeco_players(uuid) ON DELETE CASCADE
                    );
                """.trimIndent()

                stmt.execute(playerTable)
                stmt.execute(ecoTable)
            }
        }
    }

    // =========================================================================
    // CONFIGURATION SETUP
    // =========================================================================

    private fun setupSQLite(config: HikariConfig) {
        val dbFile = File(configManager.sqLiteProfile.databasefile, "elyeco.db")
        config.apply {
            maximumPoolSize = 5
            minimumIdle = 1
            jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
            driverClassName = "org.sqlite.JDBC"
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }
    }

    private fun setupMySQL(config: HikariConfig) {
        val profile = configManager.mySQLProfile
        val url = "jdbc:mysql://${profile.host}:${profile.port}/${profile.dbname}?useSSL=${profile.usessl}&useUnicode=true&characterEncoding=UTF-8"

        config.apply {
            jdbcUrl = url
            driverClassName = "com.mysql.cj.jdbc.Driver"
            username = profile.username
            password = profile.password
            maximumPoolSize = 10
            minimumIdle = 2

            // Performance optimizations
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("useServerPrepStmts", "true")
            addDataSourceProperty("useLocalSessionState", "true")
            addDataSourceProperty("rewriteBatchedStatements", "true")
            addDataSourceProperty("cacheResultSetMetadata", "true")
            addDataSourceProperty("cacheServerConfiguration", "true")
            addDataSourceProperty("elideSetAutoCommits", "true")
            addDataSourceProperty("maintainTimeStats", "false")
        }
    }

    private fun setupFallbackSQLite() {
        try {
            this.databaseType = DatabaseType.SQLITE
            val config = HikariConfig().apply {
                val dbFile = File(configManager.sqLiteProfile.databasefile, "elyeco_fallback.db")
                jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
                driverClassName = "org.sqlite.JDBC"
                poolName = "ElyEco-SQLite-Fallback"
                maximumPoolSize = 5
                minimumIdle = 1
            }
            this.dataSource = HikariDataSource(config)
            initializeDatabase()
            loadCache(null)
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "SQLite fallback failed", e)
        }
    }

    // =========================================================================
    // PUBLIC API (ABSTRACTION LAYER)
    // =========================================================================

    /**
     * Lấy profile an toàn. Nếu chưa có -> Tạo mới + Lưu DB -> Trả về.
     */
    fun getProfile(uuid: UUID): PlayerProfile {
        return playerProfiles.computeIfAbsent(uuid) { k ->
            val now = System.currentTimeMillis()
            // Tạo default data
            val pStats = PlayerStats(k, configManager.startingBalance, now, now)
            val eStats = EconomyStats(k, 0.0, 0.0, 0, null)
            val newProfile = PlayerProfile(pStats, eStats)

            saveProfile(newProfile)
            newProfile
        }
    }

    fun hasProfile(uuid: UUID): Boolean = playerProfiles.containsKey(uuid)

    /**
     * Lưu thủ công 1 user (khi out game / giao dịch lớn)
     */
    fun saveUser(uuid: UUID) {
        playerProfiles[uuid]?.let { saveProfile(it) }
    }

    // =========================================================================
    // DATABASE LOGIC (ASYNC & BATCH)
    // =========================================================================

    fun loadCache(targetUuid: UUID?): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            val query = if (targetUuid == null) {
                "SELECT p.uuid, p.balance, p.created_at, p.updated_at, e.total_received, e.total_sent, e.transfer_count, e.last_transfer_at FROM elyeco_players p LEFT JOIN elyeco_stats e ON p.uuid = e.uuid"
            } else {
                "SELECT p.uuid, p.balance, p.created_at, p.updated_at, e.total_received, e.total_sent, e.transfer_count, e.last_transfer_at FROM elyeco_players p LEFT JOIN elyeco_stats e ON p.uuid = e.uuid WHERE p.uuid = ?"
            }

            try {
                dataSource?.connection?.use { conn ->
                    conn.prepareStatement(query).use { ps ->
                        if (targetUuid != null) {
                            ps.setString(1, targetUuid.toString())
                        }

                        ps.executeQuery().use { rs ->
                            while (rs.next()) {
                                val uuid = UUID.fromString(rs.getString("uuid"))

                                val pStats = PlayerStats(
                                    uuid,
                                    rs.getDouble("balance"),
                                    rs.getLong("created_at"),
                                    rs.getLong("updated_at")
                                )

                                // Xử lý null cho last_transfer_at
                                val lastTransferVal = rs.getLong("last_transfer_at")
                                val lastTransfer = if (rs.wasNull()) null else lastTransferVal

                                val eStats = EconomyStats(
                                    uuid,
                                    rs.getDouble("total_received"),
                                    rs.getDouble("total_sent"),
                                    rs.getInt("transfer_count"),
                                    lastTransfer
                                )

                                playerProfiles[uuid] = PlayerProfile(pStats, eStats)
                            }
                        }
                    }
                }

                if (targetUuid == null) {
                    isCacheInitialized = true
                    plugin.logger.info("Database loaded. Cached ${playerProfiles.size} profiles.")
                }

            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, "Failed to load player data", e)
            }
        }, dbPool)
    }

    private fun saveProfile(profile: PlayerProfile) {
        profile.player.updatedAt = System.currentTimeMillis()

        dbPool.submit {
            try {
                dataSource?.connection?.use { conn ->
                    conn.autoCommit = false
                    try {
                        savePlayerStats(conn, profile.player)
                        saveEconomyStats(conn, profile.economy)
                        conn.commit()
                    } catch (e: Exception) {
                        conn.rollback()
                        plugin.logger.log(Level.SEVERE, "Save failed for ${profile.player.uuid}", e)
                    } finally {
                        conn.autoCommit = true
                    }
                }
            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, "Database connection error", e)
            }
        }
    }

    @Synchronized
    fun saveAll() {
        if (playerProfiles.isEmpty()) return

        val start = System.currentTimeMillis()
        plugin.logger.info("Starting auto-save for ${playerProfiles.size} profiles...")

        try {
            dataSource?.connection?.use { conn ->
                conn.autoCommit = false

                val playerSql = if (databaseType == DatabaseType.MYSQL)
                    "INSERT INTO elyeco_players (uuid, balance, created_at, updated_at) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE balance = VALUES(balance), updated_at = VALUES(updated_at)"
                else
                    "INSERT OR REPLACE INTO elyeco_players (uuid, balance, created_at, updated_at) VALUES (?, ?, ?, ?)"

                val statsSql = if (databaseType == DatabaseType.MYSQL)
                    "INSERT INTO elyeco_stats (uuid, total_received, total_sent, transfer_count, last_transfer_at) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE total_received = VALUES(total_received), total_sent = VALUES(total_sent), transfer_count = VALUES(transfer_count), last_transfer_at = VALUES(last_transfer_at)"
                else
                    "INSERT OR REPLACE INTO elyeco_stats (uuid, total_received, total_sent, transfer_count, last_transfer_at) VALUES (?, ?, ?, ?, ?)"

                conn.prepareStatement(playerSql).use { psPlayer ->
                    conn.prepareStatement(statsSql).use { psStats ->

                        for (profile in playerProfiles.values) {
                            profile.player.updatedAt = System.currentTimeMillis()

                            // Batch Player
                            psPlayer.setString(1, profile.player.uuid.toString())
                            psPlayer.setDouble(2, profile.player.balance)
                            psPlayer.setLong(3, profile.player.createdAt)
                            psPlayer.setLong(4, profile.player.updatedAt)
                            psPlayer.addBatch()

                            // Batch Stats
                            psStats.setString(1, profile.economy.uuid.toString())
                            psStats.setDouble(2, profile.economy.totalReceived)
                            psStats.setDouble(3, profile.economy.totalSent)
                            psStats.setInt(4, profile.economy.transferCount)

                            if (profile.economy.lastTransferAt != null) {
                                psStats.setLong(5, profile.economy.lastTransferAt!!)
                            } else {
                                psStats.setNull(5, Types.BIGINT)
                            }
                            psStats.addBatch()
                        }

                        psPlayer.executeBatch()
                        psStats.executeBatch()
                        conn.commit()
                    }
                }
                // Auto-commit restore by 'use' block or manually if needed, but 'use' closes connection anyway
                conn.autoCommit = true
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Bulk save failed!", e)
        }

        val time = System.currentTimeMillis() - start
        plugin.logger.info("Auto-saved complete in ${time}ms.")
    }

    private fun savePlayerStats(conn: Connection, stats: PlayerStats) {
        val query = if (databaseType == DatabaseType.MYSQL) {
            "INSERT INTO elyeco_players (uuid, balance, created_at, updated_at) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE balance = VALUES(balance), updated_at = VALUES(updated_at)"
        } else {
            "INSERT OR REPLACE INTO elyeco_players (uuid, balance, created_at, updated_at) VALUES (?, ?, ?, ?)"
        }

        conn.prepareStatement(query).use { ps ->
            ps.setString(1, stats.uuid.toString())
            ps.setDouble(2, stats.balance)
            ps.setLong(3, stats.createdAt)
            ps.setLong(4, stats.updatedAt)
            ps.executeUpdate()
        }
    }

    private fun saveEconomyStats(conn: Connection, stats: EconomyStats) {
        val query = if (databaseType == DatabaseType.MYSQL) {
            "INSERT INTO elyeco_stats (uuid, total_received, total_sent, transfer_count, last_transfer_at) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE total_received = VALUES(total_received), total_sent = VALUES(total_sent), transfer_count = VALUES(transfer_count), last_transfer_at = VALUES(last_transfer_at)"
        } else {
            "INSERT OR REPLACE INTO elyeco_stats (uuid, total_received, total_sent, transfer_count, last_transfer_at) VALUES (?, ?, ?, ?, ?)"
        }

        conn.prepareStatement(query).use { ps ->
            ps.setString(1, stats.uuid.toString())
            ps.setDouble(2, stats.totalReceived)
            ps.setDouble(3, stats.totalSent)
            ps.setInt(4, stats.transferCount)

            if (stats.lastTransferAt != null) {
                ps.setLong(5, stats.lastTransferAt!!)
            } else {
                ps.setNull(5, Types.BIGINT)
            }
            ps.executeUpdate()
        }
    }

    fun shutdown() {
        if (dataSource != null && !dataSource!!.isClosed) {
            plugin.logger.info("Database shutting down. Saving all data...")

            // Blocking save on main thread
            saveAll()

            // Shutdown pool
            dbPool.shutdown()
            try {
                if (!dbPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    dbPool.shutdownNow()
                }
            } catch (e: InterruptedException) {
                dbPool.shutdownNow()
            }

            dataSource?.close()
            plugin.logger.info("Database closed successfully.")
        }
    }

    enum class DatabaseType(val displayName: String) {
        SQLITE("SQLite"),
        MYSQL("MySQL");

        companion object {
            fun fromString(name: String?): DatabaseType {
                return try {
                    valueOf(name?.uppercase(Locale.ROOT) ?: "SQLITE")
                } catch (e: IllegalArgumentException) {
                    SQLITE
                }
            }
        }
    }
}
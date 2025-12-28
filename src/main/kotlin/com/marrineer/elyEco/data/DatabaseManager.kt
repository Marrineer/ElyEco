package com.marrineer.elyEco.data

import com.marrineer.elyEco.ElyEco
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.sql.Connection
import java.util.logging.Level
import com.marrineer.elyEco.data.ConfigManager as CM

class DatabaseManager(private val plugin: ElyEco) {

    private var dataSource: HikariDataSource? = null
    var databaseType: DatabaseType = DatabaseType.SQLITE
        private set

    private val scope = CoroutineScope(Dispatchers.IO)

    fun initialize() {
        // Determine database type from config
        val configuredType = DatabaseType.fromString(CM.dbType)

        // Attempt to connect to the primary choice (MySQL) if configured
        if (configuredType == DatabaseType.MYSQL) {
            try {
                plugin.logger.info("Connecting to MySQL database...")
                val mysqlConfig = createHikariConfig(DatabaseType.MYSQL)
                dataSource = HikariDataSource(mysqlConfig)
                databaseType = DatabaseType.MYSQL
                plugin.logger.info("Successfully connected to MySQL.")
            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, "Failed to connect to MySQL database.", e)
                if (!CM.dbFallback) {
                    plugin.logger.severe("Database connection failed and fallback is disabled. Disabling plugin.")
                    plugin.server.pluginManager.disablePlugin(plugin)
                    return
                }
                plugin.logger.warning("Falling back to SQLite database...")
                setupFallbackSQLite()
            }
        } else {
            // Default to SQLite
            plugin.logger.info("Using SQLite database.")
            setupFallbackSQLite()
        }

        // Create tables if connection was successful
        scope.launch {
            createTables()
        }
    }

    private fun setupFallbackSQLite() {
        try {
            val sqliteConfig = createHikariConfig(DatabaseType.SQLITE)
            dataSource = HikariDataSource(sqliteConfig)
            databaseType = DatabaseType.SQLITE
            plugin.logger.info("Successfully connected to SQLite.")
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize SQLite as a fallback.", e)
            plugin.logger.severe("Both database connections failed. Disabling plugin.")
            plugin.server.pluginManager.disablePlugin(plugin)
        }
    }

    private fun createHikariConfig(type: DatabaseType): HikariConfig {
        return HikariConfig().apply {
            poolName = "ElyEco-Pool-${type.displayName}"
            maximumPoolSize = CM.dbMaxPoolSize
            minimumIdle = CM.dbMinIdle
            idleTimeout = CM.dbIdleTimeout
            connectionTimeout = CM.dbConnectionTimeout
            maxLifetime = CM.dbMaxLifetime

            when (type) {
                DatabaseType.MYSQL -> {
                    jdbcUrl = "jdbc:mysql://${CM.dbHost}:${CM.dbPort}/${CM.dbName}?useSSL=${CM.dbUseSSL}"
                    driverClassName = "com.mysql.cj.jdbc.Driver"
                    username = CM.dbUser
                    password = CM.dbPass
                    addDataSourceProperty("cachePrepStmts", "true")
                    addDataSourceProperty("prepStmtCacheSize", "250")
                    addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                }

                DatabaseType.SQLITE -> {
                    val dbFile = File(plugin.dataFolder, "elyeco.db")
                    if (!dbFile.exists()) {
                        dbFile.createNewFile()
                    }
                    jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
                    driverClassName = "org.sqlite.JDBC"
                }
            }
        }
    }

    private suspend fun createTables() = withContext(Dispatchers.IO) {
        val playerTableSql = """
            CREATE TABLE IF NOT EXISTS elyeco_players (
                uuid VARCHAR(36) PRIMARY KEY,
                username VARCHAR(16) NOT NULL,
                balance DECIMAL(19, 4) NOT NULL DEFAULT 0.0,
                first_seen BIGINT NOT NULL,
                last_seen BIGINT NOT NULL
            );
        """.trimIndent()

        val statsTableSql = """
            CREATE TABLE IF NOT EXISTS elyeco_stats (
                uuid VARCHAR(36) PRIMARY KEY,
                total_earned DECIMAL(19, 4) NOT NULL DEFAULT 0.0,
                total_spent DECIMAL(19, 4) NOT NULL DEFAULT 0.0,
                FOREIGN KEY (uuid) REFERENCES elyeco_players(uuid) ON DELETE CASCADE
            );
        """.trimIndent()

        try {
            dataSource?.connection?.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.execute(playerTableSql)
                    stmt.execute(statsTableSql)
                    plugin.logger.info("Database tables verified/created successfully.")
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Could not create database tables.", e)
        }
    }

    suspend fun <T> query(block: suspend (Connection) -> T): T? {
        return try {
            withContext(Dispatchers.IO) {
                dataSource?.connection?.use { connection ->
                    block(connection)
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "An error occurred in a database query.", e)
            null
        }
    }


    fun shutdown() {
        if (dataSource?.isClosed == false) {
            plugin.logger.info("Closing database connection pool...")
            dataSource?.close()
            plugin.logger.info("Database connection pool closed.")
        }
    }

    enum class DatabaseType(val displayName: String) {
        SQLITE("SQLite"),
        MYSQL("MySQL");

        companion object {
            fun fromString(name: String?): DatabaseType {
                return try {
                    valueOf(name?.uppercase() ?: "SQLITE")
                } catch (e: IllegalArgumentException) {
                    SQLITE
                }
            }
        }
    }
}

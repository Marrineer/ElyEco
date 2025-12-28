package com.marrineer.elyEco.data

import com.marrineer.elyEco.ElyEco
import org.bukkit.configuration.file.FileConfiguration

object ConfigManager {

    private lateinit var config: FileConfiguration

    // Database
    var dbType: String = "SQLite"
    var dbFallback: Boolean = true
    var dbHost: String = "localhost"
    var dbPort: Int = 3306
    var dbName: String = "elyeco"
    var dbUser: String = "user"
    var dbPass: String = "password"
    var dbUseSSL: Boolean = false
    var dbMaxPoolSize: Int = 10
    var dbMinIdle: Int = 5
    var dbIdleTimeout: Long = 30000
    var dbConnectionTimeout: Long = 30000
    var dbMaxLifetime: Long = 1800000

    // Economy
    var startingBalance: Double = 100.0
    var currencySymbol: String = "$"
    var currencyFormat: String = "{symbol}{amount}"

    // Settings
    var cacheFlushInterval: Long = 300
    var debugLogging: Boolean = false

    // Messages
    var prefix: String = ""
    private val messages = mutableMapOf<String, String>()

    fun load(plugin: ElyEco) {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        config = plugin.config

        // Load settings
        prefix = config.getString("prefix", "<gray>[<gradient:#5e4fa2:#f79459>ElyEco</gradient>]</gray> ") ?: ""
        debugLogging = config.getBoolean("settings.debug_logging", false)
        cacheFlushInterval = config.getLong("settings.cache_flush_interval", 300)

        // Load database settings
        dbType = config.getString("database.type", "SQLite") ?: "SQLite"
        dbFallback = config.getBoolean("database.fallback", true)
        config.getConfigurationSection("database.mysql")?.let {
            dbHost = it.getString("host", "localhost") ?: "localhost"
            dbPort = it.getInt("port", 3306)
            dbName = it.getString("database", "elyeco") ?: "elyeco"
            dbUser = it.getString("username", "user") ?: "user"
            dbPass = it.getString("password", "password") ?: "password"
            dbUseSSL = it.getBoolean("use_ssl", false)
        }
        config.getConfigurationSection("database.pool_settings")?.let {
            dbMaxPoolSize = it.getInt("maximum_pool_size", 10)
            dbMinIdle = it.getInt("minimum_idle", 5)
            dbIdleTimeout = it.getLong("idle_timeout", 30000)
            dbConnectionTimeout = it.getLong("connection_timeout", 30000)
            dbMaxLifetime = it.getLong("max_lifetime", 1800000)
        }

        // Load economy settings
        config.getConfigurationSection("economy")?.let {
            startingBalance = it.getDouble("starting_balance", 100.0)
            currencySymbol = it.getString("currency_symbol", "$") ?: "$"
            currencyFormat = it.getString("format", "{symbol}{amount}") ?: "{symbol}{amount}"
        }

        // Load messages
        messages.clear()
        config.getConfigurationSection("messages")?.let { section ->
            for (key in section.getKeys(true)) {
                if (section.isString(key)) {
                    messages[key.replace('.', '_')] = section.getString(key) ?: ""
                }
            }
        }
    }

    fun getMessage(key: String, default: String = ""): String {
        return messages[key] ?: default
    }
}

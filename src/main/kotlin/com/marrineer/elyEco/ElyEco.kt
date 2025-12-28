package com.marrineer.elyEco

import com.marrineer.elyEco.data.ConfigManager
import com.marrineer.elyEco.data.DatabaseManager
import com.marrineer.elyEco.economy.Economy
import com.marrineer.elyEco.managers.MessageManager
import com.marrineer.elyEco.managers.TextManager
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

class ElyEco : JavaPlugin() {
    lateinit var adventure: BukkitAudiences
    lateinit var configManager: ConfigManager
    lateinit var textManager: TextManager
    lateinit var messageManager: MessageManager
    lateinit var databaseManager: DatabaseManager

    override fun onEnable() {
        adventure = BukkitAudiences.create(this)
        configManager = ConfigManager(this)
        textManager = TextManager(this, configManager.prefix)
        MessageManager.init(this)
        databaseManager = DatabaseManager(this, configManager)

        try {
            databaseManager.initialize()
        } catch (ignore: Exception) {
            logger.log(Level.SEVERE, "Failed to connect to database! Disabling plugin...", ignore)
            server.pluginManager.disablePlugin(this)
            return
        }
        if (server.pluginManager.getPlugin("Vault") != null) {
            setupEconomy()
        } else {
            logger.warning("Vault not found! Economy features will be disabled.")
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {
        adventure.close()
    }

    fun audience(player: Player): Audience {
        return adventure.player(player)
    }

    fun audience(sender: CommandSender): Audience {
        return adventure.sender(sender)
    }

    fun getConfigManager(): FileConfiguration {
        return configManager.config
    }
    private fun setupEconomy() {
        val implementer = Economy(this)
        server.servicesManager.register(
            net.milkbowl.vault.economy.Economy::class.java,
            implementer,
            this,
            ServicePriority.Highest
        )
        logger.info("Hooked into Vault Economy successfully.")
    }
}

package com.marrineer.elyEco

import com.marrineer.elyEco.commands.BalanceCommand
import com.marrineer.elyEco.commands.BalanceTopCommand
import com.marrineer.elyEco.commands.ElyEcoCommand
import com.marrineer.elyEco.commands.PayCommand
import com.marrineer.elyEco.data.ConfigManager
import com.marrineer.elyEco.data.DatabaseManager
import com.marrineer.elyEco.economy.ElyEcoEconomy
import com.marrineer.elyEco.listeners.PlayerConnectionListener
import com.marrineer.elyEco.managers.MessageManager
import com.marrineer.elyEco.managers.ProfileManager
import com.marrineer.elyEco.managers.TextManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.milkbowl.vault.economy.Economy
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin

class ElyEco : JavaPlugin() {

    lateinit var audiences: BukkitAudiences
        private set
    lateinit var databaseManager: DatabaseManager
        private set
    lateinit var profileManager: ProfileManager
        private set
    lateinit var textManager: TextManager
        private set
    lateinit var messageManager: MessageManager
        private set
    lateinit var economy: Economy
        private set

    override fun onEnable() {
        val startTime = System.currentTimeMillis()

        // Initialize Adventure
        audiences = BukkitAudiences.create(this)

        // Load configurations
        logger.info("Loading configuration...")
        ConfigManager.load(this)

        // Initialize managers and services
        logger.info("Initializing services...")
        if (!setupServices()) {
            server.pluginManager.disablePlugin(this)
            return
        }

        // Register listeners and commands
        registerListeners()
        registerCommands()

        // Start background tasks
        profileManager.startSaveTask()

        // Load data for any players who were already online (e.g., during a /reload)
        profileManager.loadAllOnlinePlayers()

        val endTime = System.currentTimeMillis()
        logger.info("ElyEco has been enabled successfully in ${endTime - startTime}ms.")
    }

    override fun onDisable() {
        logger.info("Disabling ElyEco...")

        // Save all pending data synchronously before shutting down
        logger.info("Saving all player data...")
        runBlocking(Dispatchers.IO) {
            profileManager.saveAllDirtyProfiles()
        }

        // Shutdown database connections
        databaseManager.shutdown()

        // Close Adventure audiences
        audiences.close()

        logger.info("ElyEco has been disabled.")
    }

    private fun setupServices(): Boolean {
        databaseManager = DatabaseManager(this)
        databaseManager.initialize()

        profileManager = ProfileManager(this, databaseManager)
        textManager = TextManager(this)
        messageManager = MessageManager(this, textManager)

        // Check for Vault and register Economy
        if (server.pluginManager.getPlugin("Vault") == null) {
            logger.severe("Vault not found! ElyEco requires Vault to function.")
            return false
        }
        economy = ElyEcoEconomy(profileManager)
        server.servicesManager.register(
            Economy::class.java,
            economy, this,
            ServicePriority.Normal
        )
        logger.info("Vault hook established.")
        return true
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(PlayerConnectionListener(this), this)
        logger.info("Registered event listeners.")
    }

    private fun registerCommands() {
        getCommand("elyeco")?.setExecutor(ElyEcoCommand(this))
        getCommand("balance")?.setExecutor(BalanceCommand(this))
        getCommand("pay")?.setExecutor(PayCommand(this))
        getCommand("balancetop")?.setExecutor(BalanceTopCommand(this))
        logger.info("Registered commands.")
    }
}
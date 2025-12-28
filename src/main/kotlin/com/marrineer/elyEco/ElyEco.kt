package com.marrineer.elyEco

import com.marrineer.elyEco.data.ConfigManager
import com.marrineer.elyEco.managers.MessageManager
import com.marrineer.elyEco.utils.TextManager
import net.kyori.adventure.audience.Audience
import org.bukkit.plugin.java.JavaPlugin
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player

class ElyEco : JavaPlugin() {
    lateinit var adventure: BukkitAudiences
    lateinit var configManager: ConfigManager
    lateinit var textManager: TextManager
    lateinit var messageManager: MessageManager

    override fun onEnable() {
        adventure = BukkitAudiences.create(this)
        configManager = ConfigManager(this)
        textManager = TextManager(this, configManager.prefix)
        MessageManager.init(this)
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
}

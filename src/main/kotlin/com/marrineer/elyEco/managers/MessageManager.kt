package com.marrineer.elyEco.managers

import com.marrineer.elyEco.ElyEco
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object MessageManager {
    lateinit var message: FileConfiguration
        private set
    lateinit var file: File
    lateinit var plugin: ElyEco
    lateinit var defaultMessage: FileConfiguration
    fun init(plugin: ElyEco) {
        this.plugin = plugin
        file = File(plugin.dataFolder, "messages.yml")
        if(!file.exists()) {
            plugin.saveResource("messages.yml", false)
        }
        reloadMessage()
    }
    public fun reloadMessage() {
        message = YamlConfiguration.loadConfiguration(file)
         defaultMessage = YamlConfiguration.loadConfiguration(
            InputStreamReader(
                plugin.getResource("messages.yml")!!,
                StandardCharsets.UTF_8
            )
        )
        message.setDefaults(defaultMessage)
        message.options().copyDefaults(true)
    }

    public fun get(key: String, fallback: String = "Missing message: $key"): String {
        return message.getValidString(key)
            ?: defaultMessage.getValidString(key)
            ?: fallback
    }

    private fun FileConfiguration.getValidString(path: String): String? {
        val value = getString(path)
        return if (value.isNullOrBlank()) null else value
    }
}
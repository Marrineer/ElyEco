package com.marrineer.elyEco.commands

import com.marrineer.elyEco.ElyEco
import com.marrineer.elyEco.data.ConfigManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import java.util.logging.Level

class ElyEcoCommand(private val plugin: ElyEco) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("elyeco.admin")) {
            plugin.messageManager.send(sender, "error.no_permission")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("/eco <reload>")
            return false
        }

        when (args[0].lowercase()) {
            "reload" -> {
                try {
                    ConfigManager.load(plugin)
                    plugin.messageManager.send(sender, "admin.reload_success")
                    plugin.logger.info("Configuration reloaded by ${sender.name}.")
                } catch (e: Exception) {
                    plugin.logger.log(Level.SEVERE, "Failed to reload configuration.", e)
                    plugin.messageManager.send(sender, "admin.reload_fail")
                }
            }

            else -> {
                sender.sendMessage("/eco <reload>")
                return false
            }
        }
        return true
    }
}

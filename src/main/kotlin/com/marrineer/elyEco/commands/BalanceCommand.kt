package com.marrineer.elyEco.commands

import com.marrineer.elyEco.ElyEco
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class BalanceCommand(private val plugin: ElyEco) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            // Check self balance
            if (sender !is Player) {
                plugin.messageManager.send(sender, "error.console_not_supported")
                return true
            }
            // Permission elyeco.balance is granted by default in plugin.yml
            plugin.messageManager.send(sender, "balance.self")
            return true
        }

        // Check other's balance
        if (!sender.hasPermission("elyeco.balance.others")) {
            plugin.messageManager.send(sender, "error.no_permission")
            return true
        }

        val targetName = args[0]
        val targetPlayer = Bukkit.getPlayer(targetName)
        if (targetPlayer == null) {
            plugin.messageManager.send(sender, "error.player_not_found", mapOf("player" to targetName))
            return true
        }

        // Use a different set of placeholders for the target
        val replacements = mapOf(
            "player" to targetPlayer.name,
            "balance" to plugin.economy.format(plugin.economy.getBalance(targetPlayer))
        )

        plugin.messageManager.send(sender, "balance.other", replacements)
        return true
    }
}

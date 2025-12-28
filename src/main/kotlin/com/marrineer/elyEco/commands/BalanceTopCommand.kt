package com.marrineer.elyEco.commands

import com.marrineer.elyEco.ElyEco
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class BalanceTopCommand(private val plugin: ElyEco) : CommandExecutor {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("elyeco.balancetop")) {
            plugin.messageManager.send(sender, "error.no_permission")
            return true
        }

        // Let the user know we are fetching the data
        sender.sendMessage("Fetching leaderboard...")

        scope.launch {
            val topProfiles = plugin.profileManager.getTopProfiles(10)

            withContext(Dispatchers.Main) {
                plugin.messageManager.sendBare(sender, "balancetop.header")

                if (topProfiles.isEmpty()) {
                    sender.sendMessage("No player data found.") // Or a configurable message
                } else {
                    topProfiles.forEachIndexed { index, profile ->
                        plugin.messageManager.sendBare(
                            sender,
                            "balancetop.format",
                            mapOf(
                                "rank" to index + 1,
                                "player" to profile.username,
                                "balance" to plugin.economy.format(profile.balance)
                            )
                        )
                    }
                }

                plugin.messageManager.sendBare(sender, "balancetop.footer")
            }
        }

        return true
    }
}

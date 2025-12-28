package com.marrineer.elyEco.commands

import com.marrineer.elyEco.ElyEco
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class PayCommand(private val plugin: ElyEco) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            plugin.messageManager.send(sender, "error.console_not_supported")
            return true
        }

        if (args.size != 2) {
            sender.sendMessage("/pay <player> <amount>")
            return false
        }

        val recipientName = args[0]
        val recipient = Bukkit.getPlayer(recipientName)
        if (recipient == null) {
            plugin.messageManager.send(sender, "error.player_not_found", mapOf("player" to recipientName))
            return true
        }

        if (recipient == sender) {
            plugin.messageManager.send(sender, "pay.cannot_pay_self")
            return true
        }

        val amount = args[1].toDoubleOrNull()
        if (amount == null || amount <= 0) {
            plugin.messageManager.send(sender, "error.invalid_amount")
            return true
        }

        val economy = plugin.economy
        val withdrawalResponse = economy.withdrawPlayer(sender, amount)

        if (withdrawalResponse.type == EconomyResponse.ResponseType.SUCCESS) {
            val depositResponse = economy.depositPlayer(recipient, amount)
            if (depositResponse.type != EconomyResponse.ResponseType.SUCCESS) {
                // Refund the sender if the deposit fails
                economy.depositPlayer(sender, amount)
                plugin.messageManager.send(sender, "error.generic", mapOf("error" to depositResponse.errorMessage))
                return true
            }

            val replacements = mapOf(
                "amount" to economy.format(amount),
                "recipient" to recipient.name,
                "sender" to sender.name
            )
            plugin.messageManager.send(sender, "pay.success_sender", replacements)
            plugin.messageManager.send(recipient, "pay.success_recipient", replacements)

        } else {
            plugin.messageManager.send(sender, "pay.not_enough_money")
        }

        return true
    }
}

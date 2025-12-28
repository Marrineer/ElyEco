package com.marrineer.elyEco.managers

import com.marrineer.elyEco.ElyEco
import com.marrineer.elyEco.data.ConfigManager
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@SuppressWarnings("unused")
class TextManager(
    private val plugin: ElyEco,
    private val prefix: String
) {
    private val miniMessage: MiniMessage = MiniMessage.miniMessage()

    //===================== GET FROM FILE =====================//
    public fun get(type: ConfigManager.FileType, placeholder: String) {
        when (type) {
            ConfigManager.FileType.CONFIG -> {
                plugin.configManager.get(placeholder)
            }

            ConfigManager.FileType.MESSAGE -> {
                plugin.messageManager.get(placeholder)
            }
        }
    }

    //===================== SEND WITH PREFIX =====================//
    public fun sendWithPrefixToSender(sender: CommandSender, text: String) {
        sendToSender(
            sender,
            String.format("%s %s", prefix, text)
        )
    }

    public fun sendWithPrefixToPlayer(player: Player, text: String) {
        sendToPlayer(
            player,
            String.format("%s %s", prefix, text)
        )
    }

    //===================== SEND ONLY =====================//
    public fun sendToSender(sender: CommandSender, text: String) {
        plugin.audience(sender).sendMessage {
            miniMessage.deserialize(text)
        }
    }

    public fun sendToPlayer(player: Player, text: String) {
        plugin.audience(player).sendMessage {
            miniMessage.deserialize(text)
        }
    }
}
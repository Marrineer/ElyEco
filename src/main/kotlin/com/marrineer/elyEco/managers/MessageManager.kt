package com.marrineer.elyEco.managers

import com.marrineer.elyEco.ElyEco
import com.marrineer.elyEco.data.ConfigManager
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class MessageManager(private val plugin: ElyEco, private val textManager: TextManager) {

    private val miniMessage: MiniMessage = MiniMessage.miniMessage()

    /**
     * Sends a formatted message to a specific CommandSender (Player or Console).
     *
     * @param target The recipient of the message.
     * @param messageKey The key of the message in config.yml.
     * @param replacements A map of one-off placeholders to replace (e.g., "{amount}").
     */
    fun send(target: CommandSender, messageKey: String, replacements: Map<String, Any> = emptyMap()) {
        val rawMessage = ConfigManager.getMessage(messageKey.replace('.', '_'))
        if (rawMessage.isBlank()) {
            if (ConfigManager.debugLogging) {
                plugin.logger.warning("Missing message for key: $messageKey")
            }
            return
        }

        // Combine prefix with the message
        val fullMessage = ConfigManager.prefix + rawMessage

        // Player context for player-specific placeholders
        val playerContext = if (target is Player) target else null

        // First pass: replace contextual placeholders like {player_name}, {balance}
        var message = textManager.parse(fullMessage, playerContext)

        // Second pass: replace one-off placeholders like {amount}, {recipient}
        val tagResolvers = replacements.map { (key, value) ->
            Placeholder.unparsed(key, value.toString())
        }.toTypedArray()

        val component = miniMessage.deserialize(message, *tagResolvers)

        plugin.audiences.sender(target).sendMessage(component)
    }

    /**
     * Sends a message without the global prefix.
     */
    fun sendBare(target: CommandSender, messageKey: String, replacements: Map<String, Any> = emptyMap()) {
        val rawMessage = ConfigManager.getMessage(messageKey.replace('.', '_'))
        if (rawMessage.isBlank()) {
            if (ConfigManager.debugLogging) {
                plugin.logger.warning("Missing message for key: $messageKey")
            }
            return
        }

        val playerContext = if (target is Player) target else null
        var message = textManager.parse(rawMessage, playerContext)

        val tagResolvers = replacements.map { (key, value) ->
            Placeholder.unparsed(key, value.toString())
        }.toTypedArray()

        val component = miniMessage.deserialize(message, *tagResolvers)

        plugin.audiences.sender(target).sendMessage(component)
    }

    /**
     * Broadcasts a message to the entire server.
     */
    fun broadcast(messageKey: String, replacements: Map<String, Any> = emptyMap()) {
        val rawMessage = ConfigManager.getMessage(messageKey.replace('.', '_'))
        if (rawMessage.isBlank()) {
            if (ConfigManager.debugLogging) {
                plugin.logger.warning("Missing message for key: $messageKey")
            }
            return
        }

        // Placeholders in broadcasts are tricky as there's no single player context.
        // We only parse generic placeholders and one-off replacements.
        var message = textManager.parse(ConfigManager.prefix + rawMessage)

        val tagResolvers = replacements.map { (key, value) ->
            Placeholder.unparsed(key, value.toString())
        }.toTypedArray()

        val component = miniMessage.deserialize(message, *tagResolvers)

        plugin.audiences.all().sendMessage(component)
    }
}

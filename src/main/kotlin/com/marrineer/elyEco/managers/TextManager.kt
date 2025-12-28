package com.marrineer.elyEco.managers

import com.marrineer.elyEco.ElyEco
import com.marrineer.elyEco.data.ConfigManager
import org.bukkit.entity.Player
import java.text.DecimalFormat

class TextManager(private val plugin: ElyEco) {

    private val playerPlaceholders = mutableMapOf<String, (Player) -> Any>()
    private val genericPlaceholders = mutableMapOf<String, () -> Any>()
    private val numberFormat = DecimalFormat("#,##0.00")

    init {
        registerDefaultPlaceholders()
    }

    private fun registerDefaultPlaceholders() {
        // Player-specific placeholders
        registerPlayerPlaceholder("player_name") { it.name }
        registerPlayerPlaceholder("balance") {
            plugin.profileManager.getProfile(it)?.balance?.let { bal -> numberFormat.format(bal) } ?: "0.00"
        }
        registerPlayerPlaceholder("balance_raw") {
            plugin.profileManager.getProfile(it)?.balance ?: 0.0
        }

        // Generic placeholders
        registerGenericPlaceholder("currency_symbol") { ConfigManager.currencySymbol }
    }

    fun registerPlayerPlaceholder(key: String, resolver: (Player) -> Any) {
        playerPlaceholders[key.lowercase()] = resolver
    }

    fun registerGenericPlaceholder(key: String, resolver: () -> Any) {
        genericPlaceholders[key.lowercase()] = resolver
    }

    fun parse(text: String, player: Player? = null): String {
        var result = text

        // Process generic placeholders first
        for ((key, resolver) in genericPlaceholders) {
            if (result.contains("{$key}", ignoreCase = true)) {
                result = result.replace("{$key}", resolver().toString(), ignoreCase = true)
            }
        }

        // Process player placeholders if a player context is available
        if (player != null) {
            for ((key, resolver) in playerPlaceholders) {
                if (result.contains("{$key}", ignoreCase = true)) {
                    result = result.replace("{$key}", resolver(player).toString(), ignoreCase = true)
                }
            }
        }

        return result
    }
}
package com.marrineer.elyEco.economy

import com.marrineer.elyEco.data.ConfigManager
import com.marrineer.elyEco.managers.ProfileManager
import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

class ElyEcoEconomy(private val profileManager: ProfileManager) : Economy {

    override fun isEnabled(): Boolean = true

    override fun getName(): String = "ElyEco"

    override fun hasBankSupport(): Boolean = false

    override fun fractionalDigits(): Int = 2

    override fun format(amount: Double): String {
        return ConfigManager.currencyFormat
            .replace("{symbol}", ConfigManager.currencySymbol, ignoreCase = true)
            .replace("{amount}", "%.2f".format(amount), ignoreCase = true)
    }

    override fun currencyNamePlural(): String = ConfigManager.currencySymbol
    override fun currencyNameSingular(): String = ConfigManager.currencySymbol

    // Account Checks
    @Deprecated("Deprecated in Java", ReplaceWith("hasAccount(Bukkit.getOfflinePlayer(playerName))"))
    override fun hasAccount(playerName: String): Boolean = hasAccount(Bukkit.getOfflinePlayer(playerName))

    override fun hasAccount(player: OfflinePlayer): Boolean {
        // In our system, an account is implicitly created on first join/data load.
        // We can check if a profile exists, but it's often more robust to just try to get/create it.
        // For Vault's purposes, we can say true if they have a profile in cache or DB.
        // The ProfileManager will lazy-load, so just getting the profile is enough.
        return profileManager.getProfile(player.uniqueId) != null
    }

    @Deprecated("Deprecated in Java", ReplaceWith("hasAccount(player)"))
    override fun hasAccount(playerName: String, worldName: String): Boolean = hasAccount(playerName)
    override fun hasAccount(player: OfflinePlayer, worldName: String): Boolean = hasAccount(player)

    // Get Balance
    @Deprecated("Deprecated in Java", ReplaceWith("getBalance(Bukkit.getOfflinePlayer(playerName))"))
    override fun getBalance(playerName: String): Double = getBalance(Bukkit.getOfflinePlayer(playerName))

    override fun getBalance(player: OfflinePlayer): Double {
        return profileManager.getProfile(player.uniqueId)?.balance ?: 0.0
    }

    @Deprecated("Deprecated in Java", ReplaceWith("getBalance(player)"))
    override fun getBalance(playerName: String, world: String): Double = getBalance(playerName)
    override fun getBalance(player: OfflinePlayer, world: String): Double = getBalance(player)

    // Has Amount
    @Deprecated("Deprecated in Java", ReplaceWith("has(Bukkit.getOfflinePlayer(playerName), amount)"))
    override fun has(playerName: String, amount: Double): Boolean = has(Bukkit.getOfflinePlayer(playerName), amount)

    override fun has(player: OfflinePlayer, amount: Double): Boolean {
        return getBalance(player) >= amount
    }

    @Deprecated("Deprecated in Java", ReplaceWith("has(player, amount)"))
    override fun has(playerName: String, worldName: String, amount: Double): Boolean = has(playerName, amount)
    override fun has(player: OfflinePlayer, worldName: String, amount: Double): Boolean = has(player, amount)


    // Withdraw
    @Deprecated("Deprecated in Java", ReplaceWith("withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount)"))
    override fun withdrawPlayer(playerName: String, amount: Double): EconomyResponse =
        withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount)

    override fun withdrawPlayer(player: OfflinePlayer, amount: Double): EconomyResponse {
        if (amount < 0) return EconomyResponse(
            0.0,
            0.0,
            EconomyResponse.ResponseType.FAILURE,
            "Cannot withdraw negative amount."
        )

        val profile = profileManager.getProfile(player.uniqueId)
            ?: return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player account not found.")

        if (profile.balance < amount) {
            return EconomyResponse(0.0, profile.balance, EconomyResponse.ResponseType.FAILURE, "Insufficient funds.")
        }

        profile.playerStats.balance -= amount
        profile.economyStats.totalSpent += amount
        profileManager.markDirty(player.uniqueId)

        return EconomyResponse(amount, profile.balance, EconomyResponse.ResponseType.SUCCESS, null)
    }

    @Deprecated("Deprecated in Java", ReplaceWith("withdrawPlayer(player, amount)"))
    override fun withdrawPlayer(playerName: String, worldName: String, amount: Double): EconomyResponse =
        withdrawPlayer(playerName, amount)

    override fun withdrawPlayer(player: OfflinePlayer, worldName: String, amount: Double): EconomyResponse =
        withdrawPlayer(player, amount)

    // Deposit
    @Deprecated("Deprecated in Java", ReplaceWith("depositPlayer(Bukkit.getOfflinePlayer(playerName), amount)"))
    override fun depositPlayer(playerName: String, amount: Double): EconomyResponse =
        depositPlayer(Bukkit.getOfflinePlayer(playerName), amount)

    override fun depositPlayer(player: OfflinePlayer, amount: Double): EconomyResponse {
        if (amount < 0) return EconomyResponse(
            0.0,
            0.0,
            EconomyResponse.ResponseType.FAILURE,
            "Cannot deposit negative amount."
        )

        val profile = profileManager.getProfile(player.uniqueId)
            ?: return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player account not found.")

        profile.playerStats.balance += amount
        profile.economyStats.totalEarned += amount
        profileManager.markDirty(player.uniqueId)

        return EconomyResponse(amount, profile.balance, EconomyResponse.ResponseType.SUCCESS, null)
    }

    @Deprecated("Deprecated in Java", ReplaceWith("depositPlayer(player, amount)"))
    override fun depositPlayer(playerName: String, worldName: String, amount: Double): EconomyResponse =
        depositPlayer(playerName, amount)

    override fun depositPlayer(player: OfflinePlayer, worldName: String, amount: Double): EconomyResponse =
        depositPlayer(player, amount)


    // Create Account
    @Deprecated("Deprecated in Java", ReplaceWith("createPlayerAccount(Bukkit.getOfflinePlayer(playerName))"))
    override fun createPlayerAccount(playerName: String): Boolean =
        createPlayerAccount(Bukkit.getOfflinePlayer(playerName))

    override fun createPlayerAccount(player: OfflinePlayer): Boolean {
        // Our ProfileManager handles account creation on join, so we just confirm success.
        // If they are not in the cache, it implies they haven't been loaded, but Vault expects this to work.
        // We will return true, assuming the profile will be created on next join if not present.
        if (hasAccount(player)) {
            return false // Account already exists
        }
        // It's difficult to create an account for an offline player without their name if they never joined.
        // But the join listener handles creation, so for Vault's API, we can be optimistic.
        return true
    }

    @Deprecated("Deprecated in Java", ReplaceWith("createPlayerAccount(player)"))
    override fun createPlayerAccount(playerName: String, worldName: String): Boolean = createPlayerAccount(playerName)
    override fun createPlayerAccount(player: OfflinePlayer, worldName: String): Boolean = createPlayerAccount(player)


    // Bank Methods (Not Implemented)
    private fun bankError(): EconomyResponse =
        EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "ElyEco does not support banks.")

    override fun createBank(name: String, player: String): EconomyResponse = bankError()
    override fun createBank(name: String, player: OfflinePlayer): EconomyResponse = bankError()
    override fun deleteBank(name: String): EconomyResponse = bankError()
    override fun bankBalance(name: String): EconomyResponse = bankError()
    override fun bankHas(name: String, amount: Double): EconomyResponse = bankError()
    override fun bankWithdraw(name: String, amount: Double): EconomyResponse = bankError()
    override fun bankDeposit(name: String, amount: Double): EconomyResponse = bankError()
    override fun isBankOwner(name: String, playerName: String): EconomyResponse = bankError()
    override fun isBankOwner(name: String, player: OfflinePlayer): EconomyResponse = bankError()
    override fun isBankMember(name: String, playerName: String): EconomyResponse = bankError()
    override fun isBankMember(name: String, player: OfflinePlayer): EconomyResponse = bankError()
    override fun getBanks(): List<String> = emptyList()
}

package com.marrineer.elyEco.economy

import com.marrineer.elyEco.ElyEco
import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.Locale

class Economy(private val plugin: ElyEco) : Economy {
    private val db = plugin.databaseManager

    // Basic Info

    override fun isEnabled(): Boolean = plugin.isEnabled

    override fun getName(): String = "ElyEco"

    override fun hasBankSupport(): Boolean = false

    override fun fractionalDigits(): Int = 2

    override fun format(amount: Double): String {
        return String.format(Locale.ROOT, "%.2f %s", amount, currencyNamePlural())
    }

    override fun currencyNamePlural(): String = "$"
    override fun currencyNameSingular(): String = "$"

    // Account Checks

    @Deprecated("Deprecated in Java")
    override fun hasAccount(playerName: String): Boolean {
        return hasAccount(Bukkit.getOfflinePlayer(playerName))
    }

    override fun hasAccount(player: OfflinePlayer): Boolean {
        return db.hasProfile(player.uniqueId)
    }

    @Deprecated("Deprecated in Java")
    override fun hasAccount(playerName: String, worldName: String): Boolean = hasAccount(playerName)
    override fun hasAccount(player: OfflinePlayer, worldName: String): Boolean = hasAccount(player)

    // Get Balance

    @Deprecated("Deprecated in Java")
    override fun getBalance(playerName: String): Double {
        return getBalance(Bukkit.getOfflinePlayer(playerName))
    }

    override fun getBalance(player: OfflinePlayer): Double {
        return db.getProfile(player.uniqueId).player.balance
    }

    @Deprecated("Deprecated in Java")
    override fun getBalance(playerName: String, world: String): Double = getBalance(playerName)
    override fun getBalance(player: OfflinePlayer, world: String): Double = getBalance(player)

    // Has Amount

    @Deprecated("Deprecated in Java")
    override fun has(playerName: String, amount: Double): Boolean {
        return getBalance(playerName) >= amount
    }

    override fun has(player: OfflinePlayer, amount: Double): Boolean {
        return getBalance(player) >= amount
    }

    @Deprecated("Deprecated in Java")
    override fun has(playerName: String, worldName: String, amount: Double): Boolean = has(playerName, amount)
    override fun has(player: OfflinePlayer, worldName: String, amount: Double): Boolean = has(player, amount)

    // Withdraw

    @Deprecated("Deprecated in Java")
    override fun withdrawPlayer(playerName: String, amount: Double): EconomyResponse {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount)
    }

    override fun withdrawPlayer(player: OfflinePlayer, amount: Double): EconomyResponse {
        if (amount < 0) return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Cannot withdraw negative amount")

        val profile = db.getProfile(player.uniqueId)
        val balance = profile.player.balance

        if (balance < amount) {
            return EconomyResponse(0.0, balance, EconomyResponse.ResponseType.FAILURE, "Insufficient funds")
        }

        profile.player.balance -= amount
        profile.economy.totalSent += amount

        db.saveUser(player.uniqueId)

        return EconomyResponse(amount, profile.player.balance, EconomyResponse.ResponseType.SUCCESS, null)
    }

    @Deprecated("Deprecated in Java")
    override fun withdrawPlayer(playerName: String, worldName: String, amount: Double): EconomyResponse = withdrawPlayer(playerName, amount)
    override fun withdrawPlayer(player: OfflinePlayer, worldName: String, amount: Double): EconomyResponse = withdrawPlayer(player, amount)

    // Deposit

    @Deprecated("Deprecated in Java")
    override fun depositPlayer(playerName: String, amount: Double): EconomyResponse {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount)
    }

    override fun depositPlayer(player: OfflinePlayer, amount: Double): EconomyResponse {
        if (amount < 0) return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Cannot deposit negative amount")

        val profile = db.getProfile(player.uniqueId)

        profile.player.balance += amount
        profile.economy.totalReceived += amount

        db.saveUser(player.uniqueId)

        return EconomyResponse(amount, profile.player.balance, EconomyResponse.ResponseType.SUCCESS, null)
    }

    @Deprecated("Deprecated in Java")
    override fun depositPlayer(playerName: String, worldName: String, amount: Double): EconomyResponse = depositPlayer(playerName, amount)
    override fun depositPlayer(player: OfflinePlayer, worldName: String, amount: Double): EconomyResponse = depositPlayer(player, amount)

    // Create Account

    @Deprecated("Deprecated in Java")
    override fun createPlayerAccount(playerName: String): Boolean {
        return createPlayerAccount(Bukkit.getOfflinePlayer(playerName))
    }

    override fun createPlayerAccount(player: OfflinePlayer): Boolean {
        if (db.hasProfile(player.uniqueId)) return false
        db.getProfile(player.uniqueId)
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun createPlayerAccount(playerName: String, worldName: String): Boolean = createPlayerAccount(playerName)
    override fun createPlayerAccount(player: OfflinePlayer, worldName: String): Boolean = createPlayerAccount(player)

    // BANK SECTION

    @Deprecated("Deprecated in Java")
    override fun createBank(name: String, player: String): EconomyResponse = bankError()
    override fun createBank(name: String, player: OfflinePlayer): EconomyResponse = bankError()
    override fun deleteBank(name: String): EconomyResponse = bankError()
    override fun bankBalance(name: String): EconomyResponse = bankError()
    override fun bankHas(name: String, amount: Double): EconomyResponse = bankError()
    override fun bankWithdraw(name: String, amount: Double): EconomyResponse = bankError()
    override fun bankDeposit(name: String, amount: Double): EconomyResponse = bankError()
    @Deprecated("Deprecated in Java")
    override fun isBankOwner(name: String, playerName: String): EconomyResponse = bankError()
    override fun isBankOwner(name: String, player: OfflinePlayer): EconomyResponse = bankError()
    @Deprecated("Deprecated in Java")
    override fun isBankMember(name: String, playerName: String): EconomyResponse = bankError()
    override fun isBankMember(name: String, player: OfflinePlayer): EconomyResponse = bankError()

    override fun getBanks(): List<String> = emptyList()

    private fun bankError(): EconomyResponse {
        return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "ElyEco does not support banks")
    }
}
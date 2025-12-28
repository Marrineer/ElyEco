package com.marrineer.elyEco.data;

import com.marrineer.elyEco.ElyEco;
import com.marrineer.elyEco.models.profiles.MySQLProfile;
import com.marrineer.elyEco.models.profiles.SQLiteProfile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

@SuppressWarnings("unused")
public class ConfigManager {
    private final ElyEco plugin;
    private final FileConfiguration config;

    public ConfigManager(ElyEco plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
    }

    public String getPrefix() {
        return config.getString("global-prefix", "");
    }

    public String getDatabaseType() {
        return config.getString("database.type", "SQLITE");
    }

    public double getStartingBalance() {
        return config.getDouble("starting-balance", 100.0);
    }

    public MySQLProfile getMySQLProfile() {
        ConfigurationSection section = Objects.requireNonNull(config.getConfigurationSection("database.MYSQL"));
        return new MySQLProfile(
                section.getString("host", "localhost"),
                section.getInt("port", 3306),
                section.getString("dbname", "elyeco_db"),
                section.getString("username", "elyeco"),
                section.getString("password", "password"),
                section.getBoolean("use-ssl", true)
        );
    }

    public SQLiteProfile getSQLiteProfile() {
        ConfigurationSection section = Objects.requireNonNull(config.getConfigurationSection("database.SQLITE"));
        return new SQLiteProfile(
                section.getString("database-file", "elyeco.db")
        );
    }

    public Object get(String placeholder) {
        return config.get(placeholder, "Missing config: " + placeholder);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public enum FileType {
        CONFIG, MESSAGE
    }
}

# ElyEco

A flexible and modern economy plugin for Paper servers, built to be production-ready.

## Features

- **Hybrid Database Support:** Works with both **MySQL** and **SQLite**, with automatic fallback to SQLite if MySQL fails.
- **High Performance:** Uses the **HikariCP** connection pool for fast and reliable database access. All database operations are **asynchronous** to prevent server lag.
- **Robust Data Caching:** Implements a write-behind cache to minimize database load. Player data is loaded on join, cached in memory, and saved periodically and on quit.
- **Modern Messaging:** All messages are powered by the **Adventure** library with **MiniMessage** support for advanced formatting.
- **Fully Configurable:** Almost every aspect, from database credentials to message formats, can be configured in `config.yml`. Includes a safe reload feature.
- **Vault Integration:** Provides a standard economy implementation for any plugin that hooks into the Vault API.
- **Custom Placeholder Engine:** A simple, built-in placeholder engine (`{player_name}`, `{balance}`, etc.) that can be expanded.

## Commands

- `/balance [player]` - Checks your or another player's balance.
- `/pay <player> <amount>` - Pays another player.
- `/balancetop` - Shows the server's richest players.
- `/elyeco reload` - Reloads the plugin's configuration.

## Permissions

- `elyeco.balance`: Allows checking your own balance. (Default: true)
- `elyeco.balance.others`: Allows checking others' balance. (Default: op)
- `elyeco.pay`: Allows paying other players. (Default: true)
- `elyeco.balancetop`: Allows viewing the balance leaderboard. (Default: true)
- `elyeco.admin`: Allows access to the `/elyeco` command. (Default: op)

## Build Instructions

This project is built using **Apache Maven**.

1.  **Prerequisites:**
    *   Java Development Kit (JDK) 21 or newer.
    *   Apache Maven.

2.  **Build Command:**
    Navigate to the project's root directory and run the following command:
    ```shell
    mvn clean package
    ```

3.  **Output:**
    The compiled plugin JAR file will be located in the `target/` directory, named `ElyEco-0.1.0.jar`.

## Testing

This plugin is designed with a self-test architecture in mind. While full mock/embedded tests are not yet implemented, the core components can be tested as follows:

1.  **Build the plugin** using the instructions above.
2.  **Place the JAR file** in the `plugins/` directory of a Paper server (1.21 or newer) that also has the [Vault](https://www.spigotmc.org/resources/vault.34315/) plugin installed.
3.  **Start the server.**
4.  **Database:**
    *   Check the console logs to ensure the database connected successfully (either to your configured MySQL or the fallback SQLite file).
    *   Verify that `plugins/ElyEco/elyeco.db` is created if using SQLite.
5.  **Functionality:**
    *   Join the server. Your account should be created automatically.
    *   Use the `/balance` command to check your starting balance.
    *   Use `/pay` to test transactions with another player.
    *   Use `/elyeco reload` and check the console for confirmation.
    *   Use `/balancetop` to verify the leaderboard query.
6.  **Data Persistence:**
    *   Stop the server. Check the logs for messages about saving data and shutting down the database connection pool.
    *   Restart the server and check your balance again to ensure it persisted.
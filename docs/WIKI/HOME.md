# Welcome!

PunisherX is a comprehensive punishment system for Paper/Folia-based Minecraft servers. It covers commands such as `kick`, `warn`, `mute`, `jail`, `ban`, and `ban-ip` (temporary or permanent), plus utilities like `/check`, `/history`, and `/banlist` for quick monitoring. Customizable punishment templates ensure consistent enforcement, and a fully translatable message file supports MiniMessage, Legacy, and plain Minecraft formatting. The plugin includes a rich configuration file and supports MySQL/MariaDB or PostgreSQL for multi-server synchronization, with SQLite or H2 available for lightweight standalone use.

## Technical information
- Designed for compatibility with Minecraft 1.20.6 - 1.21+, optimized for Paper, Pufferfish, Purple, and Leaves forks (while maintaining full compatibility with Folia and its forks!)
- Written in modern Kotlin requires Java 21 or newer
- Language file with full MiniMessage and Legacy formatting support
- Supports MySQL/MariaDB, MSSQL, PostgreSQL, SQLite, and H2 databases
- Extensive configuration file [see the default settings here](https://github.com/SyntaxDevTeam/PunisherX/blob/main/src/main/resources/config.yml)
- **Automatic migrations** in case of changes in the config file - you always have the latest version without losing your current settings
- Optional update checker with automatic plugin updates
- IP geolocation lookup (requires a free MaxMind license key)
- **Discord webhook** integration and **PlaceholderAPI** support with a list of helpful placeholders available
- Simple database export/import/migrate commands, fallback ban system, and one-command penalty clearance
- Full jail system via command or configuration

If you have questions, join our [Discord](https://discord.gg/Zk6mxv7eMh).

## Get started

### Download
* Always up-to-date stable version available for download on:
    * Modrinth ![Modrinth Downloads](https://img.shields.io/modrinth/dt/VCNRcwC2)
    * Hangar   [![Available on Hangar](https://img.shields.io/hangar/dt/PunisherX)](https://hangar.papermc.io/SyntaxDevTeam/PunisherX)
* SNAPSHOT and DEV versions available immediately on Github [![GitHub Release Date](https://img.shields.io/github/release-date/SyntaxDevTeam/PunisherX)](https://github.com/SyntaxDevTeam/PunisherX/releases)
* You can also build the development version yourself [![Available on GitHub](https://img.shields.io/badge/GitHub.com-PunisherX-green)](https://github.com/SyntaxDevTeam/PunisherX) or download the last valid build [![Build Plugin](https://github.com/SyntaxDevTeam/PunisherX/actions/workflows/gradle.yml/badge.svg?branch=main)](https://github.com/SyntaxDevTeam/PunisherX/actions/workflows/gradle.yml)

### Installation
1. Download the latest PunisherX release
2. Place the JAR file in your server’s `plugins` folder
3. Start your Minecraft server
4. Configure the plugin through `config.yml`
5. Restart your Minecraft server
6. Enjoy.

## Contact
Need help or have questions? Reach out on our [Discord](https://discord.gg/Zk6mxv7eMh).

## License
This plugin is licensed under the MIT License. See the [LICENSE](https://github.com/SyntaxDevTeam/PunisherX/blob/main/LICENSE) file for details.

**Thank you for using PunisherX! We hope it meets your expectations. 😊**
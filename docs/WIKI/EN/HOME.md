# PunisherX Wiki

This Wiki describes the current `main` branch. Check the [changelog](../../../CHANGELOG.md) for features in a particular release. PunisherX provides punishments, player investigations, a moderation panel and player reports on Paper/Folia and Spigot, with optional Velocity and BungeeCord bridges.

## Start here

| Topic | Guide |
| --- | --- |
| Installation and daily operations | [Administrator guide](ADMIN-GUIDE.md) |
| Reporting a player and handling reports | [Player and staff report guide](REPORTS.md) |
| Command syntax and flags | [Commands](COMMANDS.md) |
| Player and staff access | [Permissions](PERMISSIONS.md) |
| Settings and safe updates | [Configuration](CONFIGS.md) |
| PlaceholderAPI and LuckPerms names | [Placeholders](PLACEHOLDERS.md) |
| Integrating your own plugin | [Developer API](API.md) |

## Anti-cheat integration

**Mark automatic penalties by check and reverse false positives.** `--mark=grim.speed` links a penalty to a check, `/unmark` reverses it, and `/unmark pause grim.speed` also suppresses further marked penalties until the check is fixed. [Full setup, examples and limitations](ANTICHEAT-MARKS.md).

## Platforms and installation

Choose `PunisherX-Paper-<version>.jar` for Paper/Folia or `PunisherX-Spigot-<version>.jar` for Spigot. Use Java 21+ and meet your server platform's requirements. The Velocity bridge requires Java 25+, and BungeeCord Java 21+; install the bridge on the proxy in addition to backend server plugins.

1. Download from [Hangar](https://hangar.papermc.io/SyntaxDevTeam/PunisherX), [Modrinth](https://modrinth.com/plugin/punisherx) or [GitHub Releases](https://github.com/SyntaxDevTeam/PunisherX/releases).
2. Place the matching JAR in `plugins/` and start the server to generate files.
3. Set the language, database and permissions using the administrator guide.
4. Run `/prx validate`, then `/prx reload`. Restart the server when replacing the JAR.

Native dialogs require Paper 1.21.7+; older Paper and Spigot use inventory/chat interfaces. Report books are available on both editions. Databases: SQLite, MySQL, MariaDB, PostgreSQL and SQL Server. **H2 is unsupported** — export or convert an older H2 database before upgrading.

[Polski](../PL/HOME.md) · [Discord support](https://discord.gg/Zk6mxv7eMh) · [MIT License](../../../LICENSE)

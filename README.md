# Welcome to PunisherX
This is a comprehensive punishment system that offers warnings, mutes, bans, IP bans (including temporary ones), and kicks. With the Check function, you can quickly check the current penalties imposed on a player. The plugin includes customizable templates for punishment reasons, making it easier to issue them quickly. Our PunisherX plugin offers a complete message file that allows modification and translation of all messages with full support for Minimessage. Additionally, the plugin has a detailed configuration file with many useful settings. The plugin is optimized for Paper servers and their forks, such as Pufferfish and Purpur. It supports both MySQL/MariaDB for synchronization between multiple servers and a local SQLite database for high performance.

## Information
* [x] PunisherX is designed specifically for the latest version of Minecraft from 1.20.6 to 1.21+
* [x] Written and optimized for the Paper engine, and thus also for its modifications such as Pufferfish, Purpur and Leaves
* [x] Requires Java 21 or newer to work properly
* [x] Written in the modern programming language Kotlin, which is more expressive and safer than Java.
* [x] Language file with full support for Minimessage and Legacy (Minecraft) formatting
* [x] Extensive configuration file for flexible settings. Check the default settings here
* [x] Update notification system with the option to set automatic plugin updates.
* [x] Geolocation for administration insight. However, you need to set the appropriate license key in the config.

If you have any questions, you might find the solution on our  [Discord](https://discord.gg/Zk6mxv7eMh)

## Download
* Always up-to-date stable version available for download on Hangar [![Available on Hangar](https://img.shields.io/hangar/dt/PunisherX)](https://hangar.papermc.io/SyntaxDevTeam/PunisherX)
* You can also build the development version yourself from [![Available on GitHub](https://img.shields.io/badge/GitHub.com-PunisherX-green)](https://github.com/SyntaxDevTeam/PunisherX)

## Installation
* Download the latest version of the plugin from the releases section.
* Place the JAR file in the plugins folder on your server.
* Start the Minecraft server.
* Configuration: In the config.yml file, you will find a rich set of configuration options.

## Commands

* `/ban <player> (time) <reason> [--force]` - Command to ban a player. Immediately bans and kicks the player if they are online. The command is additionally secured against database connection loss by using the built-in Paper engine - BanList.
* `/banip <ip|player> (time) <reason> [--force]` - Command to ban a player’s IP. Immediately bans and kicks the player if they are online, based on their IP or nickname. The command is additionally secured against database connection loss by using the built-in Paper engine command “/ban-ip”.
* `/unban <ip|player>` - Cancels the ban for a given player depending on the usage by their nickname or IP.
* (**NEW**) `/jail <player> (time) <reason> [--force]` - Allows administrators to jail a player in a specified location in the config.yml for a set duration.
* (**NEW**) `/unjail <player>` - Counterpart to the jail command. Instantly releases a player from jail, teleporting them to "World Spawn".
* (**NEW**) `/setjail radius <radius>` - Sets the jail area with a specified radius. Simply stand at the desired location and use the command to designate the jail area. This simplifies the process, avoiding tedious config settings.
* `/mute <player> (time) <reason> [--force]` - Command to mute a player. The player cannot write anything publicly in the chat. In the config file, you can also set whether attempts to communicate in private messages should be blocked and the set of blocked commands for those messages.
* `/unmute <player>` - Cancels the mute for a player.
* `/warn <player> (time) <reason>` - Command to give a warning to a player. You can warn a player by giving them a warning for minor offenses. In the config file, you can set the reaction that should occur after a certain number of warnings, e.g., whether to kick or ban for a certain time. You can set any command available on your server, omitting the “/” sign.
```YAML
WarnActions:
3: "kick {player} You have received your 3rd warning!"
4: "ban {player} 30m You have received your 4th warning!"
5: "ban {player} 5h You have received your 5th warning!"
6: "ban {player} 7d You have received your 6th warning!"
7: "ban {player} 30d You have received your {warn_no}th warning!"
10: "ban {player} You have received your 10th warning!"
```
* `/unwarn <player>` - Cancels the last warning each time, which is in the database
* (NEW)`/clearall <player>` - Clears all active penalties for a given player
* `/kick <player> <reason>` - Command that immediately kicks the player from the server with a specified reason.
* `/change-reason <penalty_id> <new_reason>` - Changes the reason for the penalty with the given ID
* `/check <player> <warn|mute|ban|all>` - Checks the active penalties of a given player. Available options to check only for warn|mute|ban or all types through “all”
* `/history <player> (X)` - Checks the entire penalty history of a given player from the beginning sorted from newest to oldest with page division. You can navigate in the game chat window by clicking the appropriate field [NEXT]|[Previous] or adding the page number as an argument to the command.
* `/banlist [--h]` - Displays a list of all banned players, paginated 10 per page. The --h argument allows you to display the full history of banned players (including those with inactive punishments).
* `/punisherx|prx <help|version|reload|export|import>` - Default command displaying depending on the argument shows the help screen with available commands, the current version of the plugin with information about it, and reloading the config file. Additionally, the export|import arguments allow you to make a database backup or import it in the plugin directory.
> Arguments specified in <> are required, while those in parentheses () are optional for standard command usage. Arguments in square brackets [] are additional and are only used in extraordinary situations. When using time (punishment for a specified duration), you must specify it in the available formats, i.e., Xs|Xm|Xh|Xd, where X represents the time in seconds|minutes|hours|days. Leaving the time field empty will automatically assign an indefinite punishment that will never expire unless a command to cancel the punishment is used.

## Permissions
### Commands:
* `punisherx.warn`
* `punisherx.unwarn`
* `punisherx.mute`
* `punisherx.unmute`
* `punisherx.ban`
* `punisherx.banip`
* `punisherx.unban`
* `punisherx.clearall`
* `punisherx.jail` (NEW)
* `punisherx.unjail` (NEW)
* `punisherx.setjail` (NEW)
* `punisherx.kick`
* `punisherx.check` - Not required if the player checks themselves.
  * `punisherx.view_ip` - Additional permission allowing to view the player’s IP in the /check command
* `punisherx.history` - Not required if the player checks themselves.
* `punisherx.banlist` (**NEW**)
* `punisherx.help`
* `punisherx.version`
* `punisherx.reload`
* `punisherx.export`
* `punisherx.import`
### Visibility of chat messages:
* `punisherx.see.ban`
* `punisherx.see.banip`
* `punisherx.see.unban`
* `punisherx.see.jail` (NEW)
* `punisherx.see.unjail` (NEW)
* `punisherx.see.mute`
* `punisherx.see.warns`
* `punisherx.see.kick`
* `punisherx.update.notify`
### Bypass permissions (do not allow the command to be executed on authorized users):
* `punisherx.bypass.warn`
* `punisherx.bypass.mute`
* `punisherx.bypass.jail` (NEW)
* `punisherx.bypass.ban`
* `punisherx.bypass.banip` - does not work when using the IP number directly

## Contact
If you have any questions or need help, feel free to contact us on our [Discord](https://discord.gg/Zk6mxv7eMh) or write directly on PM

## License
This plugin is available under the MIT license. Details can be found in the LICENSE file.


**Thank you for using PunisherX! I hope it meets your expectations. 😊**


![syntaxdevteam_logo.png](assets/syntaxdevteam_logo.png)
---
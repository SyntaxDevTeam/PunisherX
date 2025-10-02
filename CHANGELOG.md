# Changelog

## [1.5.0] - 2025-10-04

### Functional changes:
- [x] **Added support for the latest version 1.21.9**
    * Added version compatibility management
    * Added appropriate checks and methods to maintain compatibility between Minecraft versions 1.20.6 and 1.21.9
- [x] Updated placeholder formatting. Changed from `{}` to `<>`
    - Twice the messaging performance by using the built-in TagResolver for the MiniMessage format
    - **Attention!** Due to the use of the MiniMessage format for Placeholders, it is necessary to work with the previous version of the language files.

      After starting the server, the following commands are required:
        * `/langfix` - for a quick update to the new version of the messages_xx.yml file
        * `/prx reload` or restart the server to reload the correct placeholders
        * You can also skip the previous steps and simply delete the `lang` folder from the PunisherX directory. The plugin will generate a default file with the latest version.
      > Using `/prx reload` does not guarantee reloading messages downloaded "permanently" at plugin startup

      > Never use `/reload` commands to attempt to reload a plugin. This can cause serious performance issues for the plugin and the entire server.
- [x] Added "all" argument for use instead of player name in `/kick` command to kick all players at once.
- [x] The config.yml file has been rebuilt to be more intuitive and adapted to the latest improvements.
- [x] Added the ability to set an alias for the setspawn command
- [x] Full integration with the getSmartMessage method providing greater flexibility in notification settings for kick, mute, ban, jail, etc. commands.

  Usage example in messages_en.yml:
    ```YAML
   broadcast: "<dark_gray>Player <gray><player></gray> was jailed for <gray><reason></gray> for <gray><time></gray></dark_gray>"
    ```
  lub
    ```YAML
       broadcast:
          - "<dark_gray>*************** Your Server Name *************** </dark_gray>"
          - ""
          - "<red>   Player <white><player></white> was jailed</red>"
          - "   Reason: <white><reason></white>"
          - "   Duration: <white><time></white>"
          - ""
          - "<dark_gray>*************************************************** </dark_gray>"
    ```
- [x] Restored functionality for "auto-download"
    * Added search for the latest version in several locations.
    * Added the ability to set where to search for and download the latest plugin version.

  New settings structure in config.yml:
  ```YAML
  update:
    check-for-updates: true
    auto-download: false
    hangar: true
    github: true
    modrinth: true 
  ```
- [x] Player cache data can now be stored in the database.
    - Settings have been added to config.
  ```YAML
  playerCache:
    # Choose where player IP information is stored: "file" or "database"
    storage: "file"
  ```
- [x] Improved handling of teleporting to and from prison.
    - Added a check to ensure the destination is safe, and the entire process is fully asynchronous.
- [x] Added support for UK, IT and PT languages

### Technical changes:
- [x] Redesigned `/banip` command handling logic (improved performance and efficiency)
- [x] Added plugin initialization in a separate class to improve code clarity and performance
- [x] Improved plugin reload logic for the `/prx reload` command
- [x] Refactored PlayerJoinEvent handling and minor corrections
- [x] Optimized login checks with asynchronous UUID cache
- [x] Improved handling of teleporting to and from prison.
    - Added a check to ensure the destination is safe, and the entire process is fully asynchronous.
- [x] Refactored UUIDManager usage to Paper/Bukkit
- [x] HOTFIX: "Failed to decrypt data" error
- [x] HOTFIX: `/prx checkupdates` command not working

### In testing phase (experimental – full functionality in a future version):
- [x] Added GUI for managing players and punishments (kick, ban, mute, etc.
    - Asynchronous data downloads minimize latency.
    - Ability to search for players by nickname.
    - Ability to sort players (online, offline, banned, muted, etc.).
- [x] The DatabaseHandler class has been adapted to use the latest version of the SyntaxCore 1.2.5-SNAPSHOT authoring library.
    - Added support for asynchronous database operations to minimize server lag.
    - Improved error handling and logging for better debugging and maintenance.
    - Optimized database queries for better performance and efficiency.
    - Added support MSSQL database type
- [x] Added migrateDatabase method
    * **(Highly experimental)** Added the ability to migrate from one database type to another `prx migrateDatabase <from> <to>`. Supported types: `sqlite`, `mysql`, `mariadb`, `postgresql`, `mssql`

      Example:
      ```LOG
      /prx migrateDatabase sqlite mysql
      ```

## [1.4.3] - 2025-07-19
## Technical changes:
* Added support for version 1.21.8
* Added support for Folia to JAIL related methods
* Improved the permissions system and eliminated minor bugs
* Updated dependencies and libraries

## [1.4.2-HOTFIX] - 2025-07-16
**HOTFIX**: Typo in permission logic for banip command

## [1.4.2-SNAPSHOT] - 2025-07-10
## Functional changes
* Added support for version 1.21.7
* Added Russian translation (thanks to [marat2509](https://github.com/marat2509))
* Changed message for muted players (remaining penalty time)
```YAML
mute:
  mute_info_message: "<red>You're trying to speak but you're still muted for {time}</red>"
```

## Technical changes:
* **HOTFIX**: Tightening the permissions system
    * Fixed a logical error with permission checking
* Improved logic when failing to ban a player
* Added support for version 1.21.7
    * Listener registration based on the appropriate server version for backward compatibility
    * Added methods to check the minimum server version
    * Dropped PlayerLoginEvent usage for servers from 1.21.7
* Added information about support or lack thereof for a given server version

## [1.4.1] - 2025-06-24
* Added support for version 1.21.6
* Fixed a logical error in permissions checking.
* Updated libraries and dependencies

## [1.4.0] - 2025-06-06
### Functional Changes:
* Added support for version 1.21.5
* Updated information about available commands (`/prx help`)
* Added NL language. Thanks to _**LegacyHeaven**_
* Added ability to set up a Webhook for Discord

Config e.g.:
```YAML
webhook:
  discord:
    enabled: false
    url: "YOUR_WEBHOOK_URL_HERE"
```
* Added PunisherXAPI version 1.0-Beta - more info in [Wiki](https://github.com/SyntaxDevTeam/PunisherX/wiki/API)
* Added placeholders for mute, warn and jail penalty times
  * From now on, the following placeholders are available:
    * `%prx_mute_remaining_time%`
    * `%prx_warn_remaining_time%`
    * `%prx_jail_remaining_time%`
    * `%prx_total_active_punishments%`
    * `%prx_total_punishments%`

  * Additionally, in the language file in the placeholders category, you can set any format in which they will be displayed.

For example:
```YAML
placeholders:
  mute_remaining_time: "<gray>Muted until: <gold>"
  warn_remaining_time: "<gray>Warning valid until: <gold>"
  jail_remaining_time: "<gray>Jail time remaining: <gold>"
  total_punishments: "<gray>Total punishments: <gold>"
  total_active_punishments: "<gray>Total active punishments: <gold>"
```
* Corrected the syntax of the /setjail command which now looks like this: `/setjail <radius>`

Include this in your language file so that it looks correct, e.g.:
```YAML
setjail:
  usage: "Correct usage is: <gold>/setjail <radius></gold>"
```
* Teleporting the player on login when offline "jail" has expired
  * Teleportation location to be set in config file
* Added `/setspawn` command for more convenient respawn location setting after serving a prison sentence
* Added information about the new version in the chat
* All libraries, dependencies, and Kotlin and Gradle themselves have been updated to the latest versions

### Fixed:
* Fixed the problem with the error Integer value is too big for SQLite
* Hotfix for removing active warnings
* Hotfix: [Issue Closes #34](https://github.com/SyntaxDevTeam/PunisherX/issues/34)

### Technical changes:
* Changed maximumPoolSize settings for MySQL connections
* Added helper methods for setting spawn location after prison sentence
* Replacing ConcurrentHashMap with the efficient and optimal Caffeine cache library
* Added centralized class to manage all permissions in the plugin - change of permission system
  * From now on, the plugin switches to permissions according to the `plugin.type.type.parameter` standard, e.g. `punisherx.cmd.ban`
  * Backward compatibility has been ensured with simultaneous information about the need to update permissions on your server
* Added getSmartMessage method to handle multi-line messages for Broadcast

For example::
```YAML
jail:
  broadcast: "<dark_gray>Player <gray>{player}</gray> has been jailed for <gray>{reason}</gray> for <gray>{time}</gray></dark_gray>"
  # Alternatively, you can use the message format below if you wish to further emphasize this message
  # broadcast:
    #- "<dark_gray>*************** Your Server Name *************** </dark_gray>"
    #- ""
    #- "<red>   Player <white>{player}</white> has been locked up</red>"
    #- "   Reason: <white>{reason}</white>"
    #- "   Duration: <white>{time}</white>"
    #- ""
    #- "<dark_gray>*************************************************** </dark_gray>"
```

## [1.3.3-HOTFIX] - 2025-03-12
### Functional Changes:
HOTFIX Missing entries entries in language files.

## [1.3.3] - 2025-03-11
### Functional Changes:
* **Hotfix incorrectly generated UUID**
* Added new command `/banlist` (perm: `punisherx.banlist`)
* Rebuilding the unban command logic and improving specific messages
* Update all necessary libraries
* Added CommandLogger class
* Updated kotlin and gradle versions
* Updated all dependencies and classes to their latest versions for greater stability and performance.


## [1.3.2] - 2025-02-02
### Functional Changes:

* Significantly improved error handling for GeoIP checking
* Geolocation display logic has been improved
* Added several helper methods to handle different types of formatting
* Added full conversion of text formatting to MiniMessage
* Added missing entries in lang file
* Improved internal messaging
* Fixed issue with displaying results in console in check and history command
* **Moved the responsibility of handling Minimessage formatting to a plugin**
* Minor fixes + added more debug logs


## [1.3.1-SNAPSHOT] - 2025-02-02
### Functional Changes:
* Hotfix for all errors related to missing entries in the language file for commands.
* Significantly improved permission handling for commands and their auto-completion.
* **Downgrade config file synchronization method while critical bug is resolved**
* Numerous performance improvements.
* Added several missing minor entries in the language file.
* Various bug fixes, both minor and major

### Technical Changes:
Update all dependency


## [1.3.0] - 2024-12-23
### Functional Changes:
* New command: `/jail <player> (time) <reason>` - Allows administrators to jail a player in a specified location in the config.yml for a set duration.
  * Utilized cache system for player checks (performance improvement)
  * Minimized database connections (performance improvement)
* New command: `/unjail <player>` - Counterpart to the `jail` command. Instantly releases a player from jail, teleporting them to "World Spawn".
* New command: `/setjail radius <radius>` - Sets the jail area with a specified radius. Simply stand at the desired location and use the command to designate the jail area. This simplifies the process, avoiding tedious config settings.
* Added `--force` argument for `ban`, `jail`, `mute` commands to provide additional security. Just add it at the end of the command, e.g., `/ban player reason --force`, to execute the command even on `OP` or with `bypass` permission in case of server abuse.

### Folia server support
* Chunk loading before teleportation as per Folia documentation
* Asynchronous teleportation as per Folia documentation for all cases related to `jail` and `unjail` commands.

### Purpur server support
* Resolved occasional class conflicts between the plugin and Purpur engine in the plugin update system.

### Fixed
* Resolved minor issues related to the punishment system to enhance stability.
* Simulated and fixed all potential issues where players were not correctly teleported to the jail location under various circumstances.
* Simulated and resolved issues with detecting if a jailed player left the designated area to ensure the system's functionality.

### Technical Changes:
* Significantly reduced cache memory usage required for the plugin while improving performance.
  * Improved code structure for better performance and maintenance.
  * Implemented DRY principle for all classes.
  * Reduced the number of single-use variables to almost zero.
* Updated dependency gradle to v8.12.


## [1.2.2] - 2024-12-15
### Added
- Ability to publish on Hangar using Gradle for faster plugin update releases.
- Changelog support.

### Fixed
- Minor bug fixes in the punishment system.

### In Progress
- Work is underway on new command `/jail`. Stay tuned for updates in future releases!

## [1.2.1] - 2024-12-14
### Functional Changes:
- (**New!**) Added automatic completion of missing entries in the language file. From now on, any new entries in the messages.yml file will be added automatically.
- (New!) Added automatic completion of missing entries in the config.yml file. From now on, any new entries in the configuration file will be added automatically.
- Added compatibility with Minecraft version **1.21.4**.
- Various bug fixes, both minor and major.

### Fixes
- Fixed a major issue with SQLite handling. The database file locking error [SQLITE_BUSY] has been resolved.
- Fixed minor initialization bugs.

### Technical Changes:
- Improved event handling logic to avoid potential conflicts and streamline logging.
- Improved the structure and clarity of the main plugin class.
- Changed to dependency gradleup to 9.0.0-beta4
- Updated all librarieses version

## [1.2.0] - 2024-12-04
### Functional Changes:
- Database support has been rewritten. Significant performance improvement!
  - The following database types are now supported: MariaDB/MySQL, SQLite, H2, PostgreSQL 
- Added new command `/clearall <player>`
- Added suggestions to command arguments
- Updated comments in config file
- Change the display style for some commands to be more user-friendly
- Improved UUID reading class
  - Added alternative API
- Improved the logs to make them more understandable

### Fixes:
- HOTFIX: Resolve conflicts in AsyncChatEvent handling between plugins
- Various bug fixes, both minor and major

### Additions:
- Added logging of blocked player messages with Mute
- Added appropriate messages in the language file. 
  - **Very important! Remove your language file from the plugin folder in the lang directory or add the missing entry. You can find it here**

### Technical Changes:
- Dependency management has been reorganized
- Changed to dependency gradleup to 9.0.0-beta2
- Update plugin org.jetbrains.kotlin.jvmto v2.1.0
- Updated all libraries to the latest versions
- Directory reorganization adapted to SyntaxDevTeam standards
- Move part of the logic inside the classes
- Separate classes into individual files

## [1.1.1] - 2024-10-26
### Fix
- Hotfix classpath to loader!

### Update
- Update dependency org.mariadb.jdbc:mariadb-java-client to v3.5.0
- Update dependency org.yaml:snakeyaml to v2.3

## [1.1.0] - 2024-10-25
### Changes and improvements:
- Very likely compatibility with versions 1.21.2/3
- Significant weight reduction
- Loader usage (Maven Library Resolver)
- Minor corrections to the version display command
- Reorganization of packages
- Added compatibility with Leaves (fork of Paper)
- Hotfix "java.lang.NoSuchMethodError: 'java.nio.file.attribute.FileTime org.apache.commons.io.file.attribute.FileTimes.fromUnixTime(long)'" error
- Replaced dependencies org.apache.commons to org.apache.ant
- Change obsolete json-simple to Gson
- Updated all dependencies
- Updated dependency gradle to v8.10.2
- Latest Kotlin 2.1.0-Beta2 used

## [1.0.0] - 2024-09-15
### Release
- Full release 1.0

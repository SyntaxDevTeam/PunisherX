# Changelog

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

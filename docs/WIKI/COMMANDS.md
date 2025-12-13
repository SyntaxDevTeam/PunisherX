## Basic commands
* `/ban <player> (time) <reason> [--force]` — bans and immediately kicks the player if online, falling back to the Paper `PROFILE` ban list when the database is unavailable.
* `/banip <ip|player|uuid> (time) <reason> [--force]` — bans every IP associated with the target, dispatching Paper's `ban-ip` command as a fallback on database errors and kicking online targets.
* `/unban <ip|player>` — lifts bans by player name, IP, or UUID, including linked IPs in the database.
* `/jail <player> (time) <reason> [--force]` — teleports the player to the configured jail location (respecting bypass unless `--force`) and caches their previous position for release.
* `/unjail <player>` — removes the cached jail punishment and teleports the player to the configured unjail location when available.
* `/setjail <radius>` — saves the sender's location and radius as the jail region in `config.yml`.
* `/setunjail` — saves the sender's location as the post-jail respawn point used by `/unjail`.
* `/mute <player> (time) <reason> [--force]` / `/unmute <player>` — toggles chat mutes, with optional force to ignore bypass for online targets.
* `/warn <player> (time) <reason>` / `/unwarn <player>` — adds or removes warnings, supporting timed warnings that trigger configured actions.
* `/kick <player> <reason>` — immediately removes a player from the server with the provided reason.
* `/clearall <player>` — deletes active bans, mutes, and warns for the player and notifies them if they are online.
* `/change-reason <penalty_id> <new_reason>` — updates the stored reason for an existing punishment entry.
* `/check <player> <all|warn|mute|ban|jail>` — lists active punishments (or player-only access to their own data) filtered by type.
* `/history <player> (page)` — paginated history of all punishments for a player.
* `/banlist (page) [--h]` — paginated list of currently banned players, with `--h` to show historical bans.

## Administrative utilities

* `/punisherx|/prx help|version|reload|export|import|migrate <from> <to> [--force]` — main admin command for help, config reloads, and database export/import/migration (requires confirmation unless `--force`).
* `/panel` — opens the PunisherX GUI dashboard when enabled
* `/langfix` — converts legacy `{}` placeholders in language files to MiniMessage `<>` format.
* `/cache` — debugging command that prints cached punishment entries to the sender.
* Configurable aliases from `config.yml` can mirror the core commands above (e.g., alternate names for `/ban`, `/history`, `/kick`, etc.).

> Arguments specified in <> are required, while those in parentheses () are optional for standard command usage. Arguments in square brackets [] are additional and are only used in extraordinary situations. When using time (punishment for a specified duration), you must specify it in the available formats, i.e., Xs|Xm|Xh|Xd, where X represents the time in seconds|minutes|hours|days. Leaving the time field empty will automatically assign an indefinite punishment that will never expire unless a command to cancel the punishment is used.

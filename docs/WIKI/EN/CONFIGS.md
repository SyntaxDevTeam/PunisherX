# Configuration (config.yml)

This page explains the main configurable options in `config.yml`, how to tune it, and why it matters for your server setup.

## Automatic config migrations (never lose your tweaks)
- PunisherX tracks a `config-version` in your file. On startup and `/punisherx reload`, the plugin compares it to the built-in template and automatically merges new keys while keeping your values intact.
- When migrating from older versions, PunisherX also creates a backup named `config.yml.<oldVersion>.bak` before touching your file, so you can always roll back.
- No manual resets are needed—drop in a new JAR and your configuration is upgraded to the latest schema with your custom settings preserved.

## Global scope (experimental phase)
- `server` — logical server name used to scope punishments across a network; keep `"network"` to apply everywhere, or set per-server names (e.g., `"survival"`, `"skyblock"`) to isolate punishments.

## Database and player cache
- `database.type` — choose `mariadb`, `mysql`, `mssql`,  `postgresql`, `sqlite`.
- `database.sql.*` — host/port/dbname/username/password for SQL engines (ignored for SQLite).
- `playerCache.storage` — store encrypted player IP data in `file` (per-server) or `database` (shared across networked servers).

## Punishment logging and follow-up actions
- `kick.history` — if `true`, kicks are written into the player’s history.
- `actions.kicked` / `actions.mute` — commands run when the player is kicked or muted (e.g., economy deductions).
- `actions.warn.count` — map warning counts to commands; `{player}` and `{warn_no}` placeholders are available in the examples.

## Mute handling
- `mute.pm` — block private messages while muted.
- `mute.cmd` — command whitelist/blacklist for muted players when `mute.pm` is enabled.

## Notifications
- `notifications.punishment_bar.enabled` — show an action bar countdown for active `jail`/`mute`.
- `notifications.punishment_bar.period_ticks` — refresh rate (20 ticks = 1 second).

## Placeholders formatting
- `placeholders.punishment_list_limit` — max rows returned by list placeholders.
- `placeholders.message_format` — format for placeholder output: `MINI_MESSAGE`, `LEGACY_AMPERSAND`, `LEGACY_SECTION`, or `PLAIN_TEXT`.

## Jail and release flow
- `jail.location` / `jail.radius` — jail center and allowed radius.
- `jail.allowed_commands` — commands permitted while jailed (`*` allows everything).
- `unjail.unjail_location` — default spawn after release (used when `spawn_type_select.set` is `unjail`).
- `unjail.spawn_type_select.set` — choose respawn logic: `unjail`, `last_location`, `bed`, `essx` (EssentialsX/FoliEssentials), or `world`.

## GUI presets (experimental phase)
- `gui.punish.times` — time buttons shown in the GUI.
- `gui.punish.reasons` — default reasons shown in the GUI.
- `gui.punish.use_force` — append `--force` to GUI-issued commands to bypass standard bypass permissions.

## Language and aliases
- `language` — built-in options: EN, PL, NL, ES, FR, DE, RU, UK, IT, PT, AR, KO; `auto` selects language automatically.
- `aliases.*` — custom command aliases for every main command (ban, history, jail, etc.).

## Integrations and updates
- `geoDatabase.licenseKey` — MaxMind key for GeoIP lookups.
- `webhook.discord.enabled` / `url` — toggle Discord webhook notifications.
- `webhook.discord.username` / `avatar-url` — override the webhook username and avatar.
- `webhook.discord.colors.*` — per-punishment embed colors with a `default` fallback; accepts Discord decimal values, hex (with `#` or `0x`), or basic names like `red`, `orange`, `blue`, `white`, `black`.
- `webhook.discord.embed.thumbnail-url` / `image-url` / `author.*` / `footer.icon-url` — control embed visuals.
- `webhook.discord.embed.fields.*` — toggle which fields (player, operator, type, reason, time) appear in the embed.
- `update.check-for-updates` / `auto-download` — control update checking and auto-downloads; `hangar`, `github`, `modrinth` flags choose sources.

## Debugging and telemetry
- `debug` — enable verbose logging.
- `stats.enabled` — toggle anonymous stats collection.
> Warning! Features and settings marked as experimental may function incorrectly or not at all, and the author advises against using them on production servers.

## Reports and interfaces

- `reports.player-interface` / `reports.admin-interface` — independently select `AUTO` (default), `GUI`, `DIALOG` or `BOOK`.
- `reports.use-dialogs` / `reports.admin-use-dialogs` — default `true`; control the respective interface in AUTO mode. Dialogs require Paper 1.21.7+; older Paper and Spigot use inventory/chat.
- `reports.max-open-per-reporter` — default `3`, minimum `1`; limits open reports per author. Regardless of the limit, each author can have only one open report per target.
- `reports.page-size` — default `10`, clamped to 1–50 for text lists.
- `reports.reasons` — reason categories in the player menu.
- `dialogs.use-change-reason` / `dialogs.use-list-views` — reason editing form and `/check`, `/history`, `/banlist` dialogs on supported Paper; default `true`.
- `dialogs.list-page-size` — default `10`, clamped to 1–20 for punishment list dialogs.

[Player and staff report instructions](REPORTS.md). Anti-cheat marks and pauses are managed by commands, not this configuration section: [Anti-cheat guide](ANTICHEAT-MARKS.md).

## Safe YAML and database upgrades

`/prx validate` (or `/punisherx validate`) checks files without changing them and reports invalid files. `/prx reload` and `/punisherx reload` perform the same preflight before dismantling the working instance. They check:

- `config.yml`,
- `punish-templates.yml`,
- `DBAPI_config.yml`,
- the selected language file, or all available `messages_*.yml` files when `language: auto` is enabled.

Invalid YAML aborts reload before tasks, listeners, database services or message handling are dismantled. The existing working instance continues running.

Custom `lang/messages_*.yml` files use a read-only overlay in this order: valid user value, bundled value from the JAR, final missing-message fallback. Missing keys do not cause the file to be rewritten. If a language file contains invalid YAML at startup, the plugin can use the bundled translation in memory. The original is preserved byte-for-byte and a timestamped safety copy is created under `lang/backups/`.

H2 is unsupported. Back up the database and plugin folder before upgrading; export or convert an older H2 database before replacing the JAR. Restart the server when updating the plugin.

## LuckPerms names

`name-formatting.operator-display` and `name-formatting.player-display` set name layouts (default `$prefix$name$suffix`). `name-formatting.name-color-meta` selects the nickname colour metadata key (default `name-color`). [Placeholders and example](PLACEHOLDERS.md).

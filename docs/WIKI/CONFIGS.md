# Configuration (config.yml)

This page explains every configurable option in `config.yml`, how to tune it, and why it matters for your server setup.

## Automatic config migrations (never lose your tweaks)
- PunisherX tracks a `config-version` in your file. On startup and `/punisherx reload`, the plugin compares it to the built-in template and automatically merges new keys while keeping your values intact.
- When migrating from older versions, PunisherX also creates a backup named `config.yml.<oldVersion>.bak` before touching your file, so you can always roll back.
- No manual resets are needed—drop in a new JAR and your configuration is upgraded to the latest schema with your custom settings preserved.

## Global scope (experimental phase)
- `server` — logical server name used to scope punishments across a network; keep `"network"` to apply everywhere, or set per-server names (e.g., `"survival"`, `"skyblock"`) to isolate punishments.

## Database and player cache
- `database.type` — choose `mariadb`, `mysql`, `mssql`,  `postgresql`, `sqlite`, or `h2`.
- `database.sql.*` — host/port/dbname/username/password for SQL engines (ignored for SQLite/H2).
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
- `language` — built-in options: EN, PL, NL, ES, FR, DE, RU, UK, IT, PT, AR.
- `aliases.*` — custom command aliases for every main command (ban, history, jail, etc.).

## Integrations and updates
- `geoDatabase.licenseKey` — MaxMind key for GeoIP lookups.
- `webhook.discord.enabled` / `url` — toggle Discord webhook notifications.
- `update.check-for-updates` / `auto-download` — control update checking and auto-downloads; `hangar`, `github`, `modrinth` flags choose sources.

## Debugging and telemetry
- `debug` — enable verbose logging.
- `stats.enabled` — toggle anonymous stats collection.

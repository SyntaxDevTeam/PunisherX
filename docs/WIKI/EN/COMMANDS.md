## Basic commands

* `/ban <player> (time) <reason> [--force] [--silent]` — bans and immediately kicks the player if online, falling back to the Paper `PROFILE` ban list when the database is unavailable.
* `/banip <ip|player|uuid> (time) <reason> [--force] [--silent]` — bans IPs associated with the target and kicks online targets. A failed database write does not create a fallback platform IP ban.
* `/unban <ip|player|uuid> [--silent]` — lifts bans by player name, IP, or UUID, including linked IPs in the database.
* `/jail <player> (time) <reason> [--force] [--silent]` — teleports the player to the configured jail location (respecting bypass unless `--force`) and caches their previous position for release.
* `/unjail <player> [--silent]` — removes the cached jail punishment and teleports the player to the configured unjail location when available.
* `/setjail <radius>` — saves the sender's location and radius as the jail region in `config.yml`.
* `/setunjail` — saves the sender's location as the post-jail respawn point used by `/unjail`.
* `/mute <player> (time) <reason> [--force] [--silent]` / `/unmute <player> [--silent]` — toggles chat mutes, with optional force to ignore bypass for online targets.
* `/warn <player> (time) <reason> [--force] [--silent]` / `/unwarn <player> [--silent]` — adds or removes warnings, supporting timed warnings that trigger configured actions.
* `/kick <player> <reason> [--force] [--silent]` — immediately removes a player from the server with the provided reason.
* `/clearall <player> [--silent]` — removes active punishments for the player and notifies them if they are online.
* `/change-reason <penalty_id> <new_reason>` — updates the stored reason for an existing punishment entry.
* `/check <player> <all|warn|mute|ban|jail>` — lists active punishments (or player-only access to their own data) filtered by type.
* `/history <player> (page)` — paginated history of all punishments for a player.
* `/banlist (page) [--h]` — paginated list of currently banned players, with `--h` to show historical bans.

## Punishment templates and panel

* `/punish <player> <template> (level) [--force] [--silent]` — applies a template from `punish-templates.yml`. Without a level, the plugin selects the next escalation level from punishment history. Requires `punisherx.cmd.punish`.
* `/unpunish <player> <template> [--silent]` — reverses the latest remaining punishment in that template ladder and removes its contribution to escalation. Requires `punisherx.cmd.unpunish`. Works after expiry or a normal `/unmute`/`/unban`. Each call reverses one event; a multi-IP ban is one event. Original history is retained and marked as reversed. Names with spaces can be quoted, for example `/unpunish Alex "Chat spam"`.
* New template history is linked to the template name, so templates sharing a reason remain separate and editing a reason does not break undo. Older history uses the original reason-based matching, including direct punishments with that reason. Renaming or deleting a template breaks the link to its new history. Undo cannot reverse a kick that already happened or external commands/actions triggered by a punishment.
* `/panel` — opens the moderation dashboard; requires `punisherx.cmd.panel` or management access. Individual actions still check their permissions.
* The player panel offers search, punishment details and quick template punishments. Quick punishments require `punisherx.cmd.punish`; editing duration/reason presets requires `punisherx.manage` or `punisherx.manage.*`. Presets are saved to `gui.punish.times` and `gui.punish.reasons` in `config.yml`.

## Player reports

| Command | Action | Permission |
| --- | --- | --- |
| `/report` | Select a player and reason in a form, book or inventory GUI. | Available to players; no dedicated permission. |
| `/report <player>` | Select a reason for the target. | Available to players. |
| `/report <player> <reason>` | Submit a report with a custom reason. | Available to players. |
| `/reports` | Open the staff inbox; show a list in the console. | `punisherx.see.reports` or report management access. |
| `/reports gui (page)` | Open the staff report browser. | Same read access; players only. |
| `/reports book (page)` | Open the book inbox regardless of configuration. | Report read access; players only. |
| `/reports list (page)` | Browse open reports. | Same read access. |
| `/reports view <id>` | Inspect a report and its decision, if closed. | Same read access. |
| `/reports history (page)` | Browse resolved and rejected reports. | Same read access. |
| `/reports resolve <id> <explanation>` | Close a report as resolved. | `punisherx.manage.reports` or management access. |
| `/reports reject <id> <explanation>` | Close a report as rejected. | Same management access. |

Reasons and decision explanations must contain 3–255 characters. Players cannot report themselves or submit another open report against the same target. `reports.max-open-per-reporter` limits open submissions (default: `3`). Closing a report frees a slot; it does not automatically punish the target. Page numbers start at `1`. All staff operations except `gui` and `book` also work in the console.

## Administrative utilities

`/prx` is an alias of `/punisherx`. The administrative operations below require `punisherx.cmd.prx`; `/prx help` is handled separately on Paper.

| Command | Action |
| --- | --- |
| `/prx help (page)` | Show command help. |
| `/prx version` | Show plugin information and version. |
| `/prx validate` | Check `config.yml`, `punish-templates.yml`, `DBAPI_config.yml` and selected language files without changing them. |
| `/prx reload` | Validate YAML, then reload the plugin. Invalid YAML aborts reload before the working instance is dismantled. |
| `/prx diag` or `/prx diagnostics` | Show runtime diagnostics; Paper also checks runtime libraries. |
| `/prx export` | Export the database. |
| `/prx import` | Import the database. |
| `/prx migrate <from> <to> [--force]` | Migrate between database types. Configure the target connection first; repeat the command to confirm, or use `--force`. |
| `/langfix` | Convert legacy language placeholders. Requires `punisherx.cmd.prx`. |
| `/cache` | Print the latest player IP cache records per UUID. Console only. |

Database migration is separate from the Paper UUID migration service used by AuthGatewayX; the latter is an integration API, not a `/prx migrate` subcommand.

## Platform and syntax notes

Paper 1.21.7+ can use native dialogs for reports, `/banlist`, `/history`, `/check` and `/change-reason`. On supported Paper versions, `/change-reason` or `/change-reason <id>` can open an editing form; `/change-reason <id> <new_reason>` remains available. Dialogs are controlled by `reports.use-dialogs`, `reports.admin-use-dialogs`, `dialogs.use-change-reason` and `dialogs.use-list-views`. Older Paper and Spigot use inventory/chat interfaces. `/reports list` may also open a native dialog when enabled.

Aliases in `config.yml` provide alternate names for supported core commands. On Spigot, command dispatch additionally checks the permissions declared in `plugin.yml`; see [Permissions](PERMISSIONS.md) for legacy node differences.

`<argument>` is required; `(argument)` is optional; `[--force]` is an optional flag. Duration examples: `30s`, `10m`, `2h`, `7d`. Omitting the duration gives an indefinite punishment. For supported punishment commands, `--force` bypasses target protection; for database migration it skips confirmation.

`--silent` suppresses target chat notifications and punishment broadcasts. The operator
receives a private confirmation marked as silent. Customize it with `silent_success`
in each command’s language section. It also works with `kick all`, punishment removal and all `/punish` types.
Errors remain visible to the operator. Disconnect screens, enforcement, history and
logs remain active. Configured follow-up PunisherX punishment commands inherit
`--silent`; messages from other plugins and custom actions follow their own settings.

Both `/punish` flags can be combined before or after the optional level:
`/punish Alex spam --force --silent` or `/punish Alex spam 2 --silent --force`.
`--force` bypasses target immunity; `--silent` alone does not change permission checks.

## Anti-cheat marks

Add `--mark` or `--mark=grim.speed` to `/ban`, `/banip`, `/mute`, `/warn`, `/jail`, `/kick`, or `/punish`. Bare `--mark` uses `default`; named checks are case insensitive.

- `/unmark <player|all> [check] [--silent]` reverses existing marked penalties.
- `/unmark pause <check>` reverses the check's marked penalties and persistently suppresses future ones.
- `/unmark resume <check>` allows new penalties after fixing the anti-cheat.

Requires `punisherx.cmd.unmark`. Manual penalties remain active. Reversals retain audit history and remove template escalation contributions. Automatic PunisherX penalties inherit the check mark. Kicks cannot reconnect players; reversal audits their history. `--force` cannot bypass a paused check.

[Examples, server scopes and limitations](ANTICHEAT-MARKS.md).

For the full workflow, books and interface settings, see [Player reports](REPORTS.md).

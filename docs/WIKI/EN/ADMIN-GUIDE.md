# Administrator guide

[Wiki home](HOME.md) · [Commands](COMMANDS.md) · [Permissions](PERMISSIONS.md) · [Polski](../PL/ADMIN-GUIDE.md)

## Installation and upgrades

1. Choose the Paper/Folia or Spigot edition as described on the [home page](HOME.md). Install the JAR in `plugins/` and start the server.
2. Set `language: EN`, the database connection, templates in `punish-templates.yml` and staff permissions.
3. Run `/prx validate`, then `/prx reload`. Check `/prx version` and `/prx diag`.
4. Always restart the server after replacing a JAR.

Before upgrading, back up `plugins/PunisherX` and the database. The first startup needs access to library repositories; no separate SyntaxDB plugin is required. SQLite suits a standalone server. Networks can use MySQL/MariaDB, PostgreSQL or SQL Server. H2 is unsupported: export or convert an older H2 database before upgrading.

## Networks and backups

Configure backends to use the same database and matching connection settings in proxy bridges. `server: network` selects the shared punishment scope; local names such as `survival` separate scopes. Install the bridge on the proxy as well. Reports are shared across the database. [Velocity bridge instructions](../../../punisherx-velocity-bridge/README.md).

`/prx export` and `/prx import` run in the background and report their result. Only one such operation runs at a time in an instance. Import appends records rather than replacing the database; a conflict rolls back the transaction. Keep an independent database backup before importing. `/prx migrate <from> <to>` changes database engines: configure the target connection first, then repeat the command to confirm, or use `--force`. These operations require `punisherx.cmd.prx`.

## Staff permissions

Grant specific nodes from the [permission reference](PERMISSIONS.md) rather than global `*`. `punisherx.cmd.panel` or management access opens `/panel`, but each action checks its own permission. Quick template punishments require `punisherx.cmd.punish`; editing time/reason presets requires `punisherx.manage` or `punisherx.manage.*`.

Grant `punisherx.see.reports` for report reading and notifications, plus `punisherx.manage.reports` for decisions. Reserve `/unmark` access (`punisherx.cmd.unmark`) for staff responsible for the anti-cheat integration. On Spigot also check the command dispatch permissions in the reference.

## Daily moderation and the panel

Open `/panel`, search for a player and inspect active punishments and history. Alternatively use `/check Steve all`, `/history Steve` and `/banlist`. The panel is available now and supports online and offline players.

Configure escalation ladders in `punish-templates.yml`. `/punish Steve spam` selects the next level from history; `/punish Steve spam 2` specifies a level. `/unpunish Steve spam` reverses the latest unreversed template event and its escalation contribution while retaining history. Each call reverses one event; delivered kicks and external plugin effects cannot be undone. Renaming or deleting a template breaks its link to new history.

`--silent` suppresses chat notifications and broadcasts for supported commands while retaining private operator confirmation. Disconnect screens, enforcement and history remain active. `--force` bypasses target protection; the flags can be combined. See [Commands](COMMANDS.md) for full syntax and behaviour.

## Reports: from submission to decision

Players use `/report`, `/report Steve` or `/report Steve Reason for reporting`. Staff open `/reports`, investigate, apply any justified punishment separately and close the report using `resolve` or `reject` with an explanation. `/reports history` retains decisions.

The [complete report guide](REPORTS.md) covers limits, console commands, permissions, books, GUIs and dialogs. Choose player and staff presentation independently through `reports.player-interface` and `reports.admin-interface`.

## Anti-cheat integration

**Mark automatically issued penalties**, for example `ban Steve 7d Cheating --mark=grim.speed`. For false positives, `/unmark pause grim.speed` reverses marked penalties and suppresses future ones from that check. After fixing the check, use `/unmark resume grim.speed`. Ordinary unmarked penalties remain active.

The [dedicated anti-cheat guide](ANTICHEAT-MARKS.md) explains server scopes, IP ban reversal, inherited marks and limitations. Pauses survive restarts and cannot be bypassed with `--force`.

## Safe configuration and troubleshooting

- Run `/prx validate` before reloading. Invalid YAML aborts `/prx reload` before the working instance is dismantled.
- Missing language keys use bundled defaults. An invalid language file at startup is preserved with a safety copy in `lang/backups/`; the plugin can use the bundled translation.
- If a command fails, check permissions, aliases and `/prx version`.
- For synchronization issues, check the database connection, `server` and bridge logs. For MySQL/MariaDB character write errors, check column and connection encoding.
- If book buttons stop working, reopen the menu. Dialogs require Paper 1.21.7+; use GUI or BOOK on Spigot.

See [Configuration](CONFIGS.md) for settings. When requesting help on [Discord](https://discord.gg/Zk6mxv7eMh) or GitHub Issues, include the plugin version, server platform/version, Java version, relevant logs and reproduction steps.

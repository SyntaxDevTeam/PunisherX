# Anti-cheat integration and marked penalties

[Wiki home](HOME.md) · [Commands](COMMANDS.md) · [Polski](../PL/ANTICHEAT-MARKS.md)

PunisherX on Paper/Folia and Spigot can associate penalties with an anti-cheat check. Configure the anti-cheat to run ordinary PunisherX commands with `--mark` or `--mark=<check>`. A marked command enforces the same penalty as its unmarked equivalent unless that check is paused.

```text
ban Steve 7d Cheating --mark=grim.speed --silent
warn Steve Suspicious movement --mark=grim.speed
punish Steve "Movement cheats" --mark=grim.speed
```

Anti-cheat command lists usually omit the leading `/`; follow that plugin's syntax. The example template `Movement cheats` must exist in `punish-templates.yml`. Choose your own check labels; no dedicated adapter is required. Issuing a marked penalty requires the usual permission for that punishment command.

Supported commands: `/ban`, `/banip`, `/mute`, `/warn`, `/jail`, `/kick` (including `all`), and `/punish`. Marks are independent of the displayed reason. `--mark` uses the name `default`. Named marks are case insensitive, contain 1–64 ASCII letters, digits, `_`, `.`, `:`, or `-`, and start with a letter or digit. Supply exactly one mark option; an invalid or repeated mark aborts the command. `--force` bypasses player exemptions but does not bypass a paused check.

## Reverse penalties

Requires `punisherx.cmd.unmark` (operator by default; the usual owner/legacy permission rules also apply).

| Command | Result |
| --- | --- |
| `/unmark Steve` | Reverse all marked history entries belonging to Steve. |
| `/unmark all` | Reverse all marked history entries in the accessible server scopes. |
| `/unmark Steve grim.speed` | Reverse only Steve's penalties marked `grim.speed`. |
| `/unmark all grim.speed` | Reverse only penalties marked `grim.speed`, for all players. |
| `/unmark pause grim.speed` | Persistently pause the check and reverse its existing marked penalties in one transaction. |
| `/unmark resume grim.speed` | Lift this server scope's pause, allowing new marked penalties. Previous reversals remain in effect. |

The first four commands only reverse existing penalties. Use `pause` when future commands from a faulty check must also be suppressed. Add `--silent` for a private silent reversal confirmation; `/unmark` does not broadcast or run configured punishment actions.

Pauses survive restarts and plugin reloads. Updating the anti-cheat does not automatically resume a check. To handle false positives:

1. Run `/unmark pause grim.speed`.
2. Correct or update the anti-cheat check.
3. Run `/unmark resume grim.speed`.

A local server scope can reverse its own and `network` history, following the existing punishment scope rules. The `network` scope can access all history. Pauses follow the same visibility rules: a network pause applies to every server, while a local pause applies to that server and commands issued in the network scope. Resume removes only the pause owned by the current configured scope; a pause created on another scope must be resumed there. Use distinct check labels when different anti-cheat installations need independent controls.

## What reversal does

- Removes the exact marked active punishment; manual penalties and other marks remain active.
- Preserves the original history and records who reversed it and when. Template entries stop contributing to `/punish` escalation.
- Reverses every IP row of a marked player IP ban using the stored player UUID. Renaming a player does not change ownership.
- Clears local jail cache and releases the player when no other active jail remains. Shared-database jail caches on other server instances follow their existing lifecycle; run the reversal on the server holding the jailed player for immediate local release.
- Removes pending proxy ban events once no other corresponding ban remains. Already delivered disconnects and kick screens cannot be undone; players can reconnect if no other ban remains.

Marked kicks are recorded even if ordinary kick history is disabled. The result reports history entries, so a multi-IP ban may contribute several entries. Expired, previously removed, or already delivered penalties can still be audited as reversed. Repeating a reversal does not reverse the same history twice.

Automatic PunisherX punishment commands configured under `actions` or legacy warning thresholds inherit the originating mark, including aliases and the `punisherx:` namespace. The originating mark replaces any mark configured on that follow-up. Commands belonging to other plugins and removal commands are dispatched as configured; external effects such as economy deductions are outside `/unmark`.

## Backups and reliability

Exports, imports and database-engine migrations preserve marks and pauses. A failed marked database write does not create an untracked platform ban. Keep a database backup before maintenance.

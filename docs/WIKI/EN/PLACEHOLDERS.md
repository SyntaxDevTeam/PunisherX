This document describes all placeholders supported by `PlaceholderHandler`, along with their behavior, requirements, and usage examples. The expansion identifier is `prx`, so each placeholder should be written in the `%prx_<name>%` format.

## General rules
- Placeholders use the `placeholders.message_format` setting to choose the message format applied by `MessageHandler` (config.yml).
- Most placeholders require a player context. If `Player` is `null`, an empty string is returned and no player-dependent placeholders are resolved (the punishment lists are the exception, because you can pass a target player parameter).
- All placeholders live in the `prx` namespace (expansion identifier).

## Punishment lists
Placeholders in this group can be called for the current player **or** by specifying another player in the placeholder name.

### Active punishments
- **Placeholders:**
    - `%prx_active_punishments_list%` – list of active punishments for the current player.
    - `%prx_active_punishments_list_<player>%` – list of active punishments for the specified player (e.g., `%prx_active_punishments_list_Notch%`).
- **Scope:** Retrieves up to `placeholders.punishment_list_limit` entries (defaults to 5; if the config is ≤ 0, the value from config.yml is used).
- **Format (language file):**
    - An empty list returns the `punishment_list_empty` message from `messages_xx.yml`.
    - Each entry uses the `punishment_list_entry` template, where `<type>`, `<reason>`, `<operator>`, `<start>`, and `<end>` are substituted. Dates are formatted as `yyyy-MM-dd HH:mm:ss`; permanent punishments replace `<end>` with the `punishment_list_permanent` message.
    - The entire list is injected into the `active_punishments_list` message, where `<limit>` is replaced with the limit and `<list>` with the formatted list of rows.

### Punishment history
- **Placeholders:**
    - `%prx_punishment_history_list%` – punishment history of the current player.
    - `%prx_punishment_history_list_<player>%` – punishment history of the specified player.
- **Scope and format:** Follows the same rules as active punishments, except the data comes from history and the list is wrapped in the `punishment_history_list` message (`messages_xx.yml`).

## Other player-only placeholders
These placeholders work only for the current player (you cannot provide another player in the name).

### Remaining time for timed punishments
- **Placeholders:** `%prx_mute_remaining_time%`, `%prx_warn_remaining_time%`, `%prx_jail_remaining_time%`.
- **Behavior:**
    - Look for an active punishment of the given type (`MUTE`, `WARN`, `JAIL`) with an end time in the future.
    - Return the message for the corresponding key (`mute_remaining_time`, `warn_remaining_time`, `jail_remaining_time`) concatenated with the result of `timeHandler.formatTime` for the number of seconds left until the punishment ends.
    - If the punishment does not exist, has expired, or the remaining time is ≤ 0, the placeholder returns `null`, which results in an empty string in PlaceholderAPI.

### Global punishment counters
- **Placeholders:** `%prx_total_active_punishments%`, `%prx_total_punishments%`.
- **Behavior:**
    - The first returns the number of all active punishments (`countAllPunishments`) prefixed with the `total_active_punishments` message from `messages_xx.yml`.
    - The second returns the number of all punishments in history (`countAllPunishmentHistory`) prefixed with the `total_punishments` message.
    - If a count is 0, the placeholder returns `null` (which produces an empty result).

## Usage tips
- Punishment list placeholders let you build custom menus/boards directly from language file configuration via the keys: `active_punishments_list`, `punishment_history_list`, `punishment_list_entry`, `punishment_list_permanent`, `punishment_list_empty`.
- To avoid empty results, ensure message keys are filled and set a sensible `placeholders.punishment_list_limit`.
- Timed placeholders return a value only when the punishment is active; combine them with PlaceholderAPI conditions or formatting to hide inactive labels.

## Summary:
### Full set of available placeholders:
* `%prx_warn_remaining_time%` - remaining time of the player's latest `warn`.
* `%prx_mute_remaining_time%` - remaining time of the player's latest `mute`.
* `%prx_jail_remaining_time%` - remaining time of the player's latest `jail`.
* `%prx_total_active_punishments%` - current count of all still active punishments on the server.
* `%prx_total_punishments%` - total count of all punishments ever issued on the server.
* `%prx_active_punishments_list%` – list of active punishments for the current player.
* `%prx_active_punishments_list_<player>%` – list of active punishments for the specified player (e.g., `%prx_active_punishments_list_Notch%`).
* `%prx_punishment_history_list%` – punishment history of the current player.
* `%prx_punishment_history_list_<player>%` – punishment history of the specified player.

### Configuration
`config.yml`:
```YAML
placeholders:
  # Maximum number of punishments displayed by PlaceholderAPI list placeholders.
  punishment_list_limit: 5
  # Force MessageHandler to format placeholder results using the selected format.
  # Available options include: MINI_MESSAGE, LEGACY_AMPERSAND (recommended for most plugins), LEGACY_SECTION, PLAIN_TEXT.
  message_format: "LEGACY_AMPERSAND"
```
### Formatting:
`messages_en.yml`:
```YAML
placeholders:
  mute_remaining_time: "<gray>Muted until: <gold>"
  warn_remaining_time: "<gray>Warning valid until: <gold>"
  jail_remaining_time: "<gray>In jail until: <gold>"
  total_punishments: "<gray>Total punishments: <gold>"
  total_active_punishments: "<gray>Total active punishments: <gold>"
  active_punishments_list: "<gray>Active punishments (last <gold><limit></gold>):</gray><newline><list>"
  punishment_history_list: "<gray>Punishment history (last <gold><limit></gold>):</gray><newline><list>"
  punishment_list_entry: "<gray>- <gold><type></gold> <dark_gray>|</dark_gray> <gray><reason></gray> <dark_gray>|</dark_gray> <yellow><start></yellow> → <yellow><end></yellow> <dark_gray>|</dark_gray> <gray><operator></gray></gray>"
  punishment_list_empty: "<gray>No punishments found.</gray>"
  punishment_list_permanent: "undefined"
```

## LuckPerms names in messages

Paper and Spigot provide these placeholders in PunisherX messages with a player or operator context:

| Placeholder | Value |
| --- | --- |
| `<operator_display>` | Staff member's prefix, coloured name and suffix |
| `<player_display>` | Player's prefix, coloured name and suffix |
| `<operator_prefix>`, `<player_prefix>` | Prefix only |
| `<operator_suffix>`, `<player_suffix>` | Suffix only |

`<operator>` and `<player>` keep their plain names. Values come directly from the LuckPerms API; PlaceholderAPI is not required. They belong to the person named in the message, regardless of the recipient. Prefix changes affect subsequent messages.

Example in `messages_en.yml`:

```yaml
mute:
  broadcast: "<player_display> <gray>was muted by</gray> <operator_display><gray>. Reason: <reason></gray>"
```

Prefixes and suffixes accept MiniMessage and legacy formatting (`&`, `§`, `&#RRGGBB`, `§x…`). Include any required space after the prefix in its LuckPerms value. Without a separate name colour, the name inherits the final prefix colour.

Configure the two name layouts independently in `config.yml`:

```yaml
name-formatting:
  operator-display: "$prefix $name $suffix"
  player-display: "$name <gray>($prefix)</gray>"
  name-color-meta: name-color
```

Tokens are `$prefix`, `$name` and `$suffix`. `$player` and `$operator` are aliases for `$name`: both refer to the person whose name the current layout formats. Reorder or omit tokens and add text, MiniMessage or legacy formatting as needed. Metadata colour applies at the name position. Tokens inside inserted prefixes, suffixes or names are not expanded again. `$$` produces a literal dollar sign.

Spaces are preserved exactly. The default `$prefix$name$suffix` preserves spacing from LuckPerms. If you add layout spaces, remove redundant spaces from prefix/suffix values. Without LuckPerms data, prefixes and suffixes are empty but the layout still applies. Configuration migration to version 168 adds the default layouts.

The optional `name-formatting.name-color-meta` setting selects the LuckPerms metadata key used for name colour; its default is `name-color`. For example:

```text
/lp group admin meta set name-color "#ff5555"
```

Values can also be `red`, `&c` or `<red>`. LuckPerms has no universal name-colour metadata key, so configure the key to match your setup.

Without LuckPerms or available user data, the placeholders use the plain name with empty prefixes and suffixes. PunisherX uses already loaded LuckPerms data and does not wait for offline user data while rendering messages. History messages use the currently available prefix, rather than the prefix at the time of punishment. The console normally has no LuckPerms data and displays its ordinary name.

After replacing the JAR, restart the server. Check messages from a prefixed staff account and the console, legacy and hex colours, operation without LuckPerms, and prefix changes without restarting.

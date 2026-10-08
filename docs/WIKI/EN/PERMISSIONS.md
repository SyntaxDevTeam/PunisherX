The plugin offers a comprehensive set of permissions to manage access to its various features. Below is a categorized list of available permissions along with their descriptions.
Recommended permissions plugin: [LuckPerms](https://luckperms.net/)

### Command permissions

| Permission | Description |
| --- | --- |
| `punisherx.cmd.ban` | Allows banning a player, preventing them from joining the server. |
| `punisherx.cmd.banip` | Enables banning a player's IP address, blocking access from that address. |
| `punisherx.cmd.unban` | Allows unbanning a player or IP address. |
| `punisherx.cmd.jail` | Allows jailing a player in a specified location for a set duration. |
| `punisherx.cmd.unjail` | Allows releasing a player from jail. |
| `punisherx.cmd.mute` | Allows muting a player, preventing them from sending messages. |
| `punisherx.cmd.unmute` | Allows unmuting a player, restoring their ability to send messages. |
| `punisherx.cmd.warn` | Allows warning a player with a specified reason. |
| `punisherx.cmd.unwarn` | Allows removing a warning from a player. |
| `punisherx.cmd.kick` | Enables kicking a player from the server with a specified reason. |
| `punisherx.cmd.punish` | Uses `/punish` templates and quick template punishments in the player GUI. |
| `punisherx.cmd.change_reason` | Allows changing the reason for a punishment. |
| `punisherx.cmd.banlist` | Displays a list of all banned players. |
| `punisherx.cmd.check` | Checks the punishments of a player. Not required for players checking themselves. |
| `punisherx.cmd.history` | Enables checking the entire penalty history of a given player. Not required if the player checks themselves. |
| `punisherx.view_ip` | Shows player IP information in investigation commands and player GUIs. |
| `punisherx.cmd.clear_all` | Enables clearing all active penalties for a given player. |
| `punisherx.cmd.prx` | Access to `/prx` / `/punisherx` administration, including validate, reload, diagnostics and database operations; also `/langfix`. |
| `punisherx.cmd.panel` | Opens `/panel`. Management access can also allow entry; panel actions check their own permissions. |
### Management permissions

| Permission | Description |
| --- | --- |
| `punisherx.manage` | Management access, including editing GUI duration/reason presets and managing reports. |
| `punisherx.manage.set_jail` | Allows setting the jail location. |
| `punisherx.manage.set_spawn` | Saves the release location with `/setunjail` (the permission keeps its historical name). |

### Report access

| Permission | Description |
| --- | --- |
| `punisherx.see.reports` | Reads reports and receives new-report notifications. Does not allow resolving or rejecting them. |
| `punisherx.manage.reports` | Reads, resolves and rejects reports, including through `/reports` and the staff GUI/dialog. |

`/report` is available to players without a dedicated permission node. Report managers can also read reports. Parent permissions `punisherx.see` / `punisherx.see.*` grant report reading; `punisherx.manage` / `punisherx.manage.*` grant report management. Notifications use the visibility permission checks.

### Wildcard

| Permission | Description |
| --- | --- |
| `punisherx.owner` | Allows using all PunisherX commands. |
| `punisherx.cmd.*` | Grants access to all PunisherX commands. |
| `punisherx.manage.*` | Grants access to all management commands. |
| `punisherx.see.*` | Grants visibility of punishment notifications and report read access. |
| `punisherx.bypass.*` | Prevents punishments from being applied to the user. |


### Bypass permissions

| Permission | Description |
| --- | --- |
| `punisherx.bypass` | Allows bypassing all punishments. |
| `punisherx.bypass.warn` | Allows bypassing warnings. |
| `punisherx.bypass.mute` | Allows bypassing mutes. |
| `punisherx.bypass.ban` | Allows bypassing bans. |
| `punisherx.bypass.banip` | Allows bypassing IP bans. |
| `punisherx.bypass.jail` | Allows bypassing jail sentences. |
| `punisherx.bypass.kick` | Allows bypassing kicks. |

### Permissions to view messages

| Permission | Description |
| --- | --- |
| `punisherx.see` | Allows viewing all punishments. |
| `punisherx.see.ban` | Allows viewing ban punishments. |
| `punisherx.see.banip` | Allows viewing IP ban punishments. |
| `punisherx.see.unban` | Allows viewing unban punishments. |
| `punisherx.see.jail` | Allows viewing jail punishments. |
| `punisherx.see.unjail` | Allows viewing unjail punishments. |
| `punisherx.see.mute` | Allows viewing mute punishments. |
| `punisherx.see.unmute` | Allows viewing unmute punishments. |
| `punisherx.see.warn` | Allows viewing warn punishments. |
| `punisherx.see.unwarn` | Allows viewing unwarn punishments. |
| `punisherx.see.kick` | Allows viewing kick punishments. |
| `punisherx.see.update` | Allows viewing update notifications. |

### Compatibility and platform notes

Console and OP senders, `punisherx.owner`, `punisherx.*` and global `*` are accepted by the permission checker. Legacy nodes remain supported where command handlers use the compatibility checker; prefer the current nodes listed above for new setups.

Spigot also checks command permissions declared in `plugin.yml` before running handlers. The current manifest uses `punisherx.cmd.setjail` for `/setjail`, `punisherx.cmd.clearall` for `/clearall`, and `punisherx.check` for `/check`. When granting these commands individually on Spigot, also grant those dispatch nodes alongside `punisherx.manage.set_jail`, `punisherx.cmd.clear_all`, and `punisherx.cmd.check`, respectively. `/check` and `/history` handlers allow self-lookups, but Spigot's command permission gate can still require access to the command.

GUI preset editing uses `punisherx.manage` / `punisherx.manage.*`; there is no separate preset-edit permission. Quick template actions use `punisherx.cmd.punish`. `/cache` is console-only and cannot be enabled for players through a permission.

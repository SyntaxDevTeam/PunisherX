The plugin offers a comprehensive set of permissions to manage access to its various features. Below is a categorized list of available permissions along with their descriptions.


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
| `punisherx.cmd.change_reason` | Allows changing the reason for a punishment. |
| `punisherx.cmd.banlist` | Displays a list of all banned players. |
| `punisherx.cmd.check` | Checks the punishments of a player. Not required for players checking themselves. |
| `punisherx.cmd.history` | Enables checking the entire penalty history of a given player. Not required if the player checks themselves. |
| `punisherx.view_ip` | Allows viewing the player's IP in the `/check` and `/history` command. |
| `punisherx.cmd.clear_all` | Enables clearing all active penalties for a given player. |
| `punisherx.cmd.prx` | Allows using `/punisherx` (alias `/prx`) and utilities such as `/langfix`, `/panel`, reload, export/import, and migrate. |
| `punisherx.cmd.panel` | Reserved for the `/panel` GUI command (currently governed by `punisherx.cmd.prx`). |
| --- | --- |
| `punisherx.manage` | Allows managing the plugin (configuration-level actions). |
| `punisherx.manage.set_jail` | Allows setting the jail location. |
| `punisherx.manage.set_spawn` | Allows setting the respawn location after jail. |

### Wildcard
| Permission | Description |
| --- | --- |
| `punisherx.owner` | Allows using all PunisherX commands. |
| `punisherx.cmd.*` | Grants access to all PunisherX commands. |
| `punisherx.manage.*` | Grants access to all management commands. |
| `punisherx.see.*` | Allows visibility of all punishment chat notifications. |
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

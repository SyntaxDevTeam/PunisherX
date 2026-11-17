# IP privacy and configurable jail

These options apply to both Paper and Spigot. Existing configurations are updated to version 169.

## IP display

`punisherx.view_ip` controls IP display in `/check`, `/history` and online, offline and administrator lists.

- `privacy.ip.enabled: false` disables IP display for everyone, including console and operators.
- `privacy.ip.restrict_operators: true` requires an explicit `punisherx.view_ip` assignment for operators. Owner/manage shortcuts do not bypass this restriction. The permission defaults to false; ordinary OP access is preserved by the display policy when this option is false. Permission-provider grants (including groups) remain supported.
- `privacy.ip.hidden_players` lists protected UUIDs (recommended) or names. No viewer can bypass this protection.
- Explicit `punisherx.privacy.hide_ip` protects the permission holder. This preference is saved on join/logout in `ip-privacy.yml` for offline display. Use the UUID list when changing protection for an offline player.

BANIP addresses stored as punishment identifiers are also redacted in ban lists and punishment details when the viewer is not allowed to see them. Protection of any known player sharing that address also hides the address.

This policy governs structured IP display by PunisherX. Free-form reasons and administrator-authored messages are not filtered. IP storage, IP bans and other plugins remain independent.

## Jail

JAIL confines the player to the configured jail radius and sets Adventure mode. Release uses the existing configured release-location rules and restores Survival mode.

- `jail.restrictions.mute: true` blocks public chat while JAIL is active. Set false to allow it. No separate MUTE record is created; independently issued MUTE penalties still apply.
- `jail.restrictions.commands: true` blocks commands except `jail.allowed_commands` (the existing jail bypass still applies). Set false to disable this restriction.
- `jail.online_time_only: false` keeps the existing wall-clock countdown, including offline time.
- `jail.online_time_only: true` pauses timed JAIL on logout and resumes on join. Permanent sentences remain permanent. Pauses persist in the punishment database and survive a graceful server shutdown. Unexpected process termination does not generate a logout/shutdown checkpoint.

Paused database deadlines use negative values below -1 to encode remaining milliseconds; -1 retains its permanent meaning. Back up the database before downgrading to a plugin version that does not understand paused deadlines. Display queries project paused durations relative to the current time. Existing history entries retain their original deadlines.

For shared databases, use consistent jail settings and server scope on all servers. Switching countdown mode should be done after releasing existing paused sentences; paused sentences resume on their next login.

## Configuration GUI

The configuration panel exposes IP visibility, operator restrictions, the protected-player list, jail mute, jail command blocking and online-only countdown. Switches show the current value and save immediately. The protected-player editor accepts names or canonical UUIDs separated by commas or spaces; `-` clears the list and `cancel` cancels input.

Privacy changes require `punisherx.manage`; jail settings require `punisherx.manage.set_jail` (existing management/owner shortcuts still apply). Permissions are checked again when chat input is submitted. Saving closes existing plugin inventory panels so previously visible IP lore does not remain open. Change the countdown mode only after releasing active jail sentences; the panel enforces this rule. Setting a new jail location through the panel preserves the configured radius.

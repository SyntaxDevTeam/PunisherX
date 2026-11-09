package pl.syntaxdevteam.punisher.gui.admin

import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

/** Mutation boundary shared by inventory clicks and delayed chat input. */
internal class ConfigGuiSettings(private val plugin: PunisherX) {
    enum class Toggle(val path: String, val default: Boolean, val label: String, val privacy: Boolean = false) {
        IP("privacy.ip.enabled", true, "ipEnabled", true),
        OP("privacy.ip.restrict_operators", false, "restrictOperators", true),
        MUTE("jail.restrictions.mute", true, "jailMute"),
        COMMANDS("jail.restrictions.commands", true, "jailCommands"),
        ONLINE("jail.online_time_only", false, "onlineTimeOnly")
    }
    enum class Result { SAVED, DENIED, ACTIVE_JAIL, INVALID }

    fun allowed(player: Player, privacy: Boolean): Boolean = if (privacy)
        PermissionChecker.hasWithLegacy(player, PermissionChecker.PermissionKey.MANAGE)
    else PermissionChecker.hasWithManage(player, PermissionChecker.PermissionKey.MANAGE_SET_JAIL)

    fun toggle(player: Player, option: Toggle): Result {
        if (!allowed(player, option.privacy)) return Result.DENIED
        if (option == Toggle.ONLINE && plugin.databaseHandler.getJailedPlayers(Int.MAX_VALUE, 0).any { it.end == -1L || it.end > System.currentTimeMillis() }) return Result.ACTIVE_JAIL
        plugin.config.set(option.path, !plugin.config.getBoolean(option.path, option.default))
        save()
        return Result.SAVED
    }

    fun hiddenPlayers(player: Player, input: String): Result {
        if (!allowed(player, true)) return Result.DENIED
        val values = if (input.trim() == "-") emptyList() else input.trim().split(Regex("[,\\s]+"))
        if (values.any { value -> !value.matches(Regex("[A-Za-z0-9_]{1,16}")) &&
                runCatching { java.util.UUID.fromString(value).toString().equals(value, true) }.getOrDefault(false).not() }) return Result.INVALID
        plugin.config.set("privacy.ip.hidden_players", values.distinctBy { it.lowercase(java.util.Locale.ROOT) })
        save()
        return Result.SAVED
    }

    private fun save() {
        plugin.saveConfig()
        // Existing item lore may contain IPs allowed by the previous policy.
        plugin.guiRuntime.reset()
    }
}

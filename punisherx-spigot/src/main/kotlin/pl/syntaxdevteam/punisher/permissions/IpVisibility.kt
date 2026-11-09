package pl.syntaxdevteam.punisher.permissions

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import java.util.UUID

/** Presentation policy only: IP ban enforcement and storage remain independent. */
object IpVisibility {
    @Synchronized
    fun rememberPreference(plugin: PunisherX, player: Player) {
        val file = java.io.File(plugin.dataFolder, "ip-privacy.yml")
        val preferences = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file)
        preferences.set(player.uniqueId.toString(), player.isPermissionSet("punisherx.privacy.hide_ip") && player.hasPermission("punisherx.privacy.hide_ip"))
        preferences.save(file)
    }

    @Synchronized
    private fun offlinePreference(plugin: PunisherX, uuid: UUID): Boolean =
        org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(java.io.File(plugin.dataFolder, "ip-privacy.yml")).getBoolean(uuid.toString(), false)

    /** BANIP stores the address in the UUID column; never expose it as an identifier. */
    fun displayField(plugin: PunisherX, viewer: CommandSender,
                     data: pl.syntaxdevteam.punisher.databases.PunishmentData, value: String): String {
        if (data.type != "BANIP" || value != data.uuid) return value
        if (!canView(plugin, viewer, UUID.nameUUIDFromBytes(data.uuid.toByteArray()), data.name)) return "[hidden]"
        return displayAddress(plugin, viewer, data.uuid)
    }

    fun displayAddress(plugin: PunisherX, viewer: CommandSender, value: String): String {
        // Minecraft names cannot contain dots or colons. Do not parse hostnames or perform DNS.
        if (!value.contains(':') && !value.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+"))) return value
        if (!canView(plugin, viewer, UUID.nameUUIDFromBytes(value.toByteArray()), value)) return "[hidden]"
        val records = plugin.playerIPManager.getAllDecryptedRecords().filter { it.playerIP == value }
        val visible = records.all { record ->
            runCatching { canView(plugin, viewer, UUID.fromString(record.playerUUID), record.playerName) }.getOrDefault(false)
        }
        return if (visible) value else "[hidden]"
    }

    fun canView(plugin: PunisherX, viewer: CommandSender, uuid: UUID, name: String): Boolean {
        if (!plugin.config.getBoolean("privacy.ip.enabled", true)) return false
        val protected = plugin.config.getStringList("privacy.ip.hidden_players").any {
            it.equals(uuid.toString(), true) || it.equals(name, true)
        } || (plugin.server.getPlayer(uuid)?.let { target ->
            // Bukkit's implicit OP defaults must not make every operator protected.
            target.isPermissionSet("punisherx.privacy.hide_ip") && target.hasPermission("punisherx.privacy.hide_ip")
        } ?: offlinePreference(plugin, uuid))
        if (protected) return false
        if (viewer is Player && viewer.isOp && plugin.config.getBoolean("privacy.ip.restrict_operators", false)) {
            return viewer.isPermissionSet("punisherx.view_ip") && viewer.hasPermission("punisherx.view_ip")
        }
        return PermissionChecker.hasWithLegacy(viewer, PermissionChecker.PermissionKey.VIEW_IP)
    }
}

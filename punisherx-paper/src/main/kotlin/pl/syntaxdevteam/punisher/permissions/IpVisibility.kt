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

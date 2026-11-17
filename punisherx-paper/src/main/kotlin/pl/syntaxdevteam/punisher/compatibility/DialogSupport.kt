package pl.syntaxdevteam.punisher.compatibility

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX

/** Checks dialog availability without loading classes that reference the newer Paper Dialog API. */
object DialogSupport {
    fun canUseListDialogs(plugin: PunisherX, sender: CommandSender): Boolean =
        sender is Player &&
            plugin.config.getBoolean("dialogs.use-list-views", true) &&
            plugin.versionCompatibility.supports(VersionCompatibility.CompatibilityFlag.DIALOGS)
}

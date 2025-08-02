@file:Suppress("UnstableApiUsage")

package pl.syntaxdevteam.punisher.listeners

import com.destroystokyo.paper.profile.PlayerProfile
import io.papermc.paper.connection.PlayerLoginConnection
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import pl.syntaxdevteam.punisher.PunisherX

class ModernLoginListener(private val plugin: PunisherX) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onValidateLogin(event: PlayerConnectionValidateLoginEvent) {

        val loginConn = event.connection as? PlayerLoginConnection
            ?: return event.allow()

        val profile: PlayerProfile? = loginConn.unsafeProfile ?: loginConn.authenticatedProfile
        if (profile?.name.isNullOrBlank()) {
            plugin.logger.debug("ModernLogin: brak profilu → allow()")
            return event.allow()
        }

        val playerName = profile.name
        val uuid = plugin.uuidManager.getUUID(playerName!!)
        plugin.logger.debug("Checking punishment for player: $playerName")

        val ip = loginConn.clientAddress.address.hostAddress

        try {

            val punishments = plugin.databaseHandler.getPunishments(uuid.toString()) +
                    plugin.databaseHandler.getPunishmentsByIP(ip)

            if (punishments.isEmpty()) {
                plugin.logger.debug("No punishments found for player: $playerName")
                return event.allow()
            }

            for (punishment in punishments) {
                if (plugin.punishmentManager.isPunishmentActive(punishment)
                    && (punishment.type == "BAN" || punishment.type == "BANIP")) {

                    val endTime = punishment.end
                    val remainingSecs = if (endTime == -1L) -1L
                    else (endTime - System.currentTimeMillis()) / 1000
                    val duration = if (endTime == -1L) "permanent"
                    else plugin.timeHandler.formatTime(remainingSecs.toString())
                    val reason = punishment.reason
                    val kickLines = when (punishment.type) {
                        "BAN"   -> plugin.messageHandler.getComplexMessage(
                            "ban", "kick_message", mapOf("reason" to reason, "time" to duration)
                        )
                        "BANIP" -> plugin.messageHandler.getComplexMessage(
                            "banip", "kick_message", mapOf("reason" to reason, "time" to duration)
                        )
                        else    -> emptyList()
                    }
                    val kickMessage = Component.text().also { b ->
                        kickLines.forEach { line ->
                            b.append(line).append(Component.newline())
                        }
                    }.build()

                    event.kickMessage(kickMessage)
                    event.connection.disconnect(kickMessage)
                    plugin.logger.debug("Player $playerName was kicked for: $reason")
                    return
                } else if (!plugin.punishmentManager.isPunishmentActive(punishment)) {
                    plugin.databaseHandler.removePunishment(uuid.toString(), punishment.type, true)
                    plugin.logger.debug("Punishment for UUID: $uuid has expired and has been removed")
                }
            }
            event.allow()
        } catch (ex: Exception) {
            plugin.logger.severe("Error in onValidateLogin for $playerName: ${ex.message}")
            ex.printStackTrace()
            event.allow()
        }
    }
}

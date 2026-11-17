package pl.syntaxdevteam.punisher.listeners
import pl.syntaxdevteam.punisher.compatibility.*

import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import pl.syntaxdevteam.punisher.PunisherX
import java.util.UUID

class ModernLoginListener(private val plugin: PunisherX) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
        if (event.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            plugin.logger.debug(
                "ModernLogin(pre): existing login result ${event.loginResult} for ${event.name} → keep as is"
            )
            return
        }

        val playerName = event.name

        val ip = event.address.hostAddress
        if (ip.isNullOrBlank()) {
            plugin.logger.debug("ModernLogin(pre): brak adresu IP dla $playerName → leaving result ${event.loginResult}")
            return
        }

        try {
            val uuid = event.uniqueId
            when (val action = evaluatePunishments(uuid, playerName, ip)) {
                is LoginAction.Allow -> Unit
                is LoginAction.Deny -> {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, action.message)
                }
            }
        } catch (ex: Exception) {
            plugin.logger.severe("Error during async login check for $playerName: ${ex.message}")
            plugin.reportError(ex)
            ex.printStackTrace()
            plugin.logger.debug("ModernLogin(pre): exception encountered → leaving result ${event.loginResult}")
        }
    }

    private fun evaluatePunishments(uuid: UUID, playerName: String, ip: String): LoginAction {
        if (!plugin.databaseHandler.isReady()) {
            plugin.logger.debug("ModernLogin: database not ready yet, denying $playerName to avoid bypass")
            val waitMessage = plugin.messageHandler.stringMessageToComponent("error", "db_not_ready")
            return LoginAction.Deny(waitMessage)
        }

        plugin.logger.debug("Checking punishment for player: $playerName")
        val punishments = plugin.databaseHandler.getPunishments(uuid.toString()) +
                plugin.databaseHandler.getPunishmentsByIP(ip)

        if (punishments.isEmpty()) {
            plugin.logger.debug("No punishments found for player: $playerName")
            return LoginAction.Allow
        }
        punishments.forEach { punishment ->
            if (plugin.punishmentManager.isPunishmentActive(punishment)) {
                if (punishment.type == "BAN" || punishment.type == "BANIP") {

                    val endTime = punishment.end
                    val remainingSecs = if (endTime == -1L) -1L
                    else (endTime - System.currentTimeMillis()) / 1000
                    val duration = if (endTime == -1L) "permanent"
                    else plugin.timeHandler.formatTime(remainingSecs.toString())
                    val reason = punishment.reason
                    val kickLines = when (punishment.type) {
                        "BAN" -> plugin.messageHandler.getSmartMessage(
                            "ban",
                            "kick_message",
                            mapOf("reason" to reason, "time" to duration)
                        )
                        "BANIP" -> plugin.messageHandler.getSmartMessage(
                            "banip",
                            "kick_message",
                            mapOf("reason" to reason, "time" to duration)
                        )
                        else -> emptyList()
                    }
                    val kickMessage = Component.text().also { builder ->
                        kickLines.forEach { line ->
                            builder.append(line).append(Component.newline())
                        }
                    }.build()

                    plugin.logger.debug("Player $playerName was kicked for: $reason")

                    return LoginAction.Deny(kickMessage)
                }
            } else {
                plugin.databaseHandler.removePunishment(uuid.toString(), punishment.type, true)
                plugin.logger.debug("Punishment for UUID: $uuid has expired and has been removed")
            }
        }

        return LoginAction.Allow
    }

    private sealed interface LoginAction {
        data object Allow : LoginAction
        data class Deny(val message: Component) : LoginAction
    }
}

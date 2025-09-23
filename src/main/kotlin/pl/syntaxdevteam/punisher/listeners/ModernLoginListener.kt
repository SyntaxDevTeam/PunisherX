@file:Suppress("UnstableApiUsage")

package pl.syntaxdevteam.punisher.listeners

import com.destroystokyo.paper.profile.PlayerProfile
import io.papermc.paper.connection.PlayerLoginConnection
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import pl.syntaxdevteam.punisher.PunisherX
import java.time.Duration
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ModernLoginListener(private val plugin: PunisherX) : Listener {

    private data class PendingDecision(val message: Component, val expiresAt: Long) {
        fun isExpired(now: Long): Boolean = now > expiresAt
    }

    private val decisionTtl = Duration.ofSeconds(30).toMillis()
    private val pendingDecisions: MutableMap<String, PendingDecision> = ConcurrentHashMap()

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
        cleanupDecisions()

        val playerName = event.name

        val ip = event.address.hostAddress
        if (ip.isNullOrBlank()) {
            plugin.logger.debug("ModernLogin(pre): brak adresu IP dla $playerName → allow()")
            event.allow()
            return
        }

        try {
            val uuid = plugin.resolvePlayerUuid(playerName)
            when (val action = evaluatePunishments(uuid, playerName, ip)) {
                is LoginAction.Allow -> {
                    pendingDecisions.remove(playerName.lowercase(Locale.ROOT))
                    event.allow()
                }

                is LoginAction.Deny -> {
                    val cacheKey = playerName.lowercase(Locale.ROOT)
                    pendingDecisions[cacheKey] = PendingDecision(action.message, System.currentTimeMillis() + decisionTtl)
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, action.message)
                }
            }
        } catch (ex: Exception) {
            plugin.logger.severe("Error during async login check for $playerName: ${ex.message}")
            ex.printStackTrace()
            event.allow()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onValidateLogin(event: PlayerConnectionValidateLoginEvent) {
        cleanupDecisions()
        val loginConn = event.connection as? PlayerLoginConnection ?: run {
            plugin.logger.debug("ModernLogin: brak PlayerLoginConnection → allow()")
            event.allow()
            return
        }


        val profile: PlayerProfile? = loginConn.unsafeProfile ?: loginConn.authenticatedProfile
        if (profile?.name.isNullOrBlank()) {
            plugin.logger.debug("ModernLogin: brak nazwy profilu → allow()")
            event.allow()
            return
        }

        val playerName = profile.name!!
        val cacheKey = playerName.lowercase(Locale.ROOT)
        val decision = pendingDecisions.remove(cacheKey)?.takeUnless { it.isExpired(System.currentTimeMillis()) }
        if (decision != null) {
            event.kickMessage(decision.message)
            event.connection.disconnect(decision.message)
            return
        }

        event.allow()
    }

    private fun cleanupDecisions() {
        val now = System.currentTimeMillis()
        pendingDecisions.entries.removeIf { it.value.isExpired(now) }
    }

    private fun evaluatePunishments(uuid: UUID, playerName: String, ip: String): LoginAction {
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

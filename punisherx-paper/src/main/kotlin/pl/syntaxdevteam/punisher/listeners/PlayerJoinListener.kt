package pl.syntaxdevteam.punisher.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import pl.syntaxdevteam.punisher.basic.PunishmentChecker
import pl.syntaxdevteam.punisher.players.PlayerIPManager

class PlayerJoinListener(
    private val playerIPManager: PlayerIPManager,
    private val punishmentChecker: PunishmentChecker
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        playerIPManager.handlePlayerJoin(event)
        punishmentChecker.handlePlayerJoin(event)
    }
}
package pl.syntaxdevteam.punisher.listeners
import pl.syntaxdevteam.punisher.compatibility.*

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import pl.syntaxdevteam.punisher.basic.PunishmentChecker
import pl.syntaxdevteam.punisher.players.PlayerIPManager

class PlayerJoinListener(
    private val playerIPManager: PlayerIPManager,
    private val punishmentChecker: PunishmentChecker,
    private val plugin: pl.syntaxdevteam.punisher.PunisherX
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: org.bukkit.event.player.PlayerQuitEvent) {
        val player = event.player
        val save = pl.syntaxdevteam.punisher.gui.stats.PlayerStatsService.captureLogoutLocation(player.uniqueId, player.location)
        plugin.schedulerAdapter.runAsync(save)
    }


    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        playerIPManager.handlePlayerJoin(event)
        punishmentChecker.handlePlayerJoin(event)
    }
}
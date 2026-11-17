package pl.syntaxdevteam.punisher.listeners

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
        pl.syntaxdevteam.punisher.permissions.IpVisibility.rememberPreference(plugin, player)
        plugin.databaseHandler.updateJailClock(player.uniqueId.toString(), pause = true)
        plugin.localPlayerSkins.capture(player)
        val save = pl.syntaxdevteam.punisher.gui.stats.PlayerStatsService.captureLogoutLocation(player.uniqueId, player.location)
        plugin.schedulerAdapter.runAsync(save)
    }


    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        pl.syntaxdevteam.punisher.permissions.IpVisibility.rememberPreference(plugin, event.player)
        plugin.localPlayerSkins.capture(event.player)
        plugin.databaseHandler.updateJailClock(event.player.uniqueId.toString(), pause = false)
        plugin.databaseHandler.getPunishments(event.player.uniqueId.toString()).firstOrNull { it.type == "JAIL" }?.let {
            plugin.cache.addOrUpdatePunishment(event.player.uniqueId, it.end,
                if (plugin.cache.isPlayerInCache(event.player.uniqueId)) null else event.player.location.clone())
        }
        playerIPManager.handlePlayerJoin(event)
        punishmentChecker.handlePlayerJoin(event)
    }
}
package pl.syntaxdevteam.punisher.events

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import pl.syntaxdevteam.punisher.core.punishment.PunishmentCacheRefresher

class PunishmentCacheSyncListener(
    private val cacheRefresher: PunishmentCacheRefresher,
) : Listener {

    @EventHandler
    fun onPunishmentApplied(event: PunishmentAppliedEvent) {
        cacheRefresher.refreshAsync(event.target)
    }

    @EventHandler
    fun onPunishmentRevoked(event: PunishmentRevokedEvent) {
        cacheRefresher.evictAsync(event.target)
    }
}

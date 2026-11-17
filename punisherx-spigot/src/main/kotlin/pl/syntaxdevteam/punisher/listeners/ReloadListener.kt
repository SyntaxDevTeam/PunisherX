package pl.syntaxdevteam.punisher.listeners
import pl.syntaxdevteam.punisher.compatibility.*

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.ServerLoadEvent
import pl.syntaxdevteam.punisher.PunisherX

class ReloadListener(private val plugin: PunisherX) : Listener {

    @EventHandler
    fun onReload(event: ServerLoadEvent) {
        if (event.type != ServerLoadEvent.LoadType.RELOAD) return
        plugin.logger.debug("[Reload] Triggered by server reload command. Reloading PunisherX.")

        runCatching { plugin.onReload() }
            .onSuccess {
                plugin.logger.success("[Reload] PunisherX reloaded successfully via server reload command.")
            }
            .onFailure { throwable ->
                plugin.logger.err("[Reload] Failed to reload PunisherX: ${throwable.message ?: throwable.javaClass}")
                plugin.reportError(throwable)
            }
    }
}

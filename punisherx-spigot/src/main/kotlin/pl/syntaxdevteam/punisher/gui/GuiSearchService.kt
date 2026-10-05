package pl.syntaxdevteam.punisher.gui

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerQuitEvent
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.compatibility.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GuiSearchService(private val plugin: PunisherX) : Listener {
    private val pending = ConcurrentHashMap<UUID, (Player, String) -> Unit>()

    fun request(player: Player, prompt: String, callback: (Player, String) -> Unit) {
        pending[player.uniqueId] = callback
        player.closeInventory()
        player.sendMessage(plugin.messageHandler.miniMessageFormat(prompt))
    }

    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        val callback = pending.remove(event.player.uniqueId) ?: return
        event.isCancelled = true
        val query = event.message.trim()
        plugin.schedulerAdapter.runForPlayer(event.player, Runnable {
            if (!query.equals("cancel", true) && !query.equals("anuluj", true)) callback(event.player, query)
        })
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        pending.remove(event.player.uniqueId)
    }
}

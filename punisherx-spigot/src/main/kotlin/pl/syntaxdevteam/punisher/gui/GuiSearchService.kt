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
    private data class PendingInput(
        val callback: (Player, String) -> Unit,
        val onCancel: ((Player) -> Unit)?
    )

    private val pending = ConcurrentHashMap<UUID, PendingInput>()

    fun request(player: Player, prompt: String, callback: (Player, String) -> Unit) {
        request(player, prompt, null, callback)
    }

    fun request(
        player: Player,
        prompt: String,
        onCancel: ((Player) -> Unit)?,
        callback: (Player, String) -> Unit
    ) {
        pending[player.uniqueId] = PendingInput(callback, onCancel)
        player.closeInventory()
        player.sendMessage(plugin.messageHandler.miniMessageFormat(prompt))
    }

    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        val request = pending.remove(event.player.uniqueId) ?: return
        event.isCancelled = true
        val query = event.message.trim()
        plugin.schedulerAdapter.runForPlayer(event.player, Runnable {
            if (query.equals("cancel", true) || query.equals("anuluj", true)) {
                request.onCancel?.invoke(event.player)
            } else {
                request.callback(event.player, query)
            }
        })
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        pending.remove(event.player.uniqueId)
    }
}

package pl.syntaxdevteam.punisher.gui

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import pl.syntaxdevteam.punisher.PunisherX
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
    fun onChat(event: AsyncChatEvent) {
        val callback = pending.remove(event.player.uniqueId) ?: return
        event.isCancelled = true
        val query = PlainTextComponentSerializer.plainText().serialize(event.originalMessage()).trim()
        plugin.schedulerAdapter.runRegionally(event.player.location, Runnable {
            if (query.equals("cancel", true) || query.equals("anuluj", true)) return@Runnable
            callback(event.player, query)
        })
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        pending.remove(event.player.uniqueId)
    }
}

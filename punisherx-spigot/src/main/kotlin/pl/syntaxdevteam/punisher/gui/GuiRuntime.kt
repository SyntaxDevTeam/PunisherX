package pl.syntaxdevteam.punisher.gui

import org.bukkit.inventory.Inventory
import pl.syntaxdevteam.punisher.PunisherX
import java.util.WeakHashMap

class GuiRuntime(private val plugin: PunisherX) {
    @Volatile var generation: Long = 0
        private set
    private val inventories = WeakHashMap<Inventory, Unit>()
    @Synchronized fun track(inventory: Inventory) { inventories[inventory] = Unit }

    fun reset() {
        val previous = synchronized(this) {
            generation++
            inventories.keys.toSet().also { inventories.clear() }
        }
        if (previous.isEmpty()) return
        plugin.server.onlinePlayers.forEach { player ->
            plugin.schedulerAdapter.runForPlayer(player, Runnable {
                if (player.openInventory.topInventory in previous) player.closeInventory()
            })
        }
    }
}

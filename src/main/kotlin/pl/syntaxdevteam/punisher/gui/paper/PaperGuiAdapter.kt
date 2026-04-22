package pl.syntaxdevteam.punisher.gui.paper

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

/**
 * Adapter responsible for opening, redrawing and routing clicks for Paper inventory screens.
 */
class PaperGuiAdapter(private val plugin: JavaPlugin) : Listener {

    private val screens = mutableMapOf<String, PaperScreen>()

    fun register(screen: PaperScreen): PaperGuiAdapter {
        screens[screen.id] = screen
        return this
    }

    fun registerListener() {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    fun open(player: Player, screenId: String) {
        val screen = screens[screenId] ?: return
        val inventory = createInventory(player, screen)
        screen.render(player, inventory)
        player.openInventory(inventory)
    }

    fun redraw(player: Player) {
        val holder = player.openInventory.topInventory.holder as? ScreenHolder ?: return
        val screen = screens[holder.screenId] ?: return
        val inventory = player.openInventory.topInventory
        inventory.clear()
        screen.render(player, inventory)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.view.topInventory.holder as? ScreenHolder ?: return
        if (holder.viewerId != event.whoClicked.uniqueId) return
        val screen = screens[holder.screenId] ?: return
        screen.handleClick(event, this)
    }

    private fun createInventory(player: Player, screen: PaperScreen): Inventory {
        val holder = ScreenHolder(player.uniqueId, screen.id)
        val inventory = Bukkit.createInventory(holder, screen.size(player), screen.title(player))
        holder.bind(inventory)
        return inventory
    }

    data class ScreenHolder(val viewerId: UUID, val screenId: String) : InventoryHolder {
        private lateinit var backingInventory: Inventory

        fun bind(inventory: Inventory) {
            this.backingInventory = inventory
        }

        override fun getInventory(): Inventory = backingInventory
    }
}

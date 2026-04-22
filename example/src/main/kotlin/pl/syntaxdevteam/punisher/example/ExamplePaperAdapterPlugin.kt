package pl.syntaxdevteam.punisher.example

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.Inventory
import org.bukkit.plugin.java.JavaPlugin
import pl.syntaxdevteam.punisher.gui.paper.NavigationComponents
import pl.syntaxdevteam.punisher.gui.paper.PaperGuiAdapter
import pl.syntaxdevteam.punisher.gui.paper.PaperScreen

/**
 * Example module showing how to connect listener + renderer via [PaperGuiAdapter].
 */
class ExamplePaperAdapterPlugin : JavaPlugin(), Listener {

    private lateinit var adapter: PaperGuiAdapter

    override fun onEnable() {
        adapter = PaperGuiAdapter(this)
            .register(ExampleScreen())
        adapter.registerListener()
        server.pluginManager.registerEvents(this, this)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        adapter.open(event.player, ExampleScreen.ID)
    }

    private class ExampleScreen : PaperScreen {
        companion object {
            const val ID = "example-main"
        }

        override val id: String = ID

        override fun title(viewer: Player): Component =
            Component.text("PunisherX Example", NamedTextColor.GOLD)

        override fun render(viewer: Player, inventory: Inventory) {
            val center = org.bukkit.inventory.ItemStack(Material.EMERALD)
            val meta = center.itemMeta
            meta.displayName(Component.text("Przerysuj", NamedTextColor.GREEN))
            center.itemMeta = meta
            inventory.setItem(22, center)

            NavigationComponents.place(
                inventory = inventory,
                back = NavigationComponents.back(Component.text("Zamknij", NamedTextColor.RED)),
                redraw = NavigationComponents.redraw(Component.text("Odśwież", NamedTextColor.YELLOW))
            )
        }

        override fun handleClick(event: org.bukkit.event.inventory.InventoryClickEvent, adapter: PaperGuiAdapter) {
            event.isCancelled = true
            val player = event.whoClicked as? Player ?: return
            when (event.rawSlot) {
                NavigationComponents.BACK_SLOT -> player.closeInventory()
                NavigationComponents.REDRAW_SLOT,
                22 -> adapter.redraw(player)
            }
        }
    }
}

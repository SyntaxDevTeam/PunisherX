package pl.syntaxdevteam.punisher.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import pl.syntaxdevteam.punisher.PunisherX

/**
 * Main menu of PunisherX.
 */
class PunisherMain(private val plugin: PunisherX) : GUI {

    /**
     * Representation of a clickable menu entry.
     */
    private data class MenuEntry(
        val title: String,
        val material: Material,
        val slot: Int,
        val onClick: (Player) -> Unit
    )

    /**
     * Buttons displayed in the main menu.
     */
    private val menuEntries = listOf(
        MenuEntry("Informacje o serwerze", Material.PAPER, 13) { _ -> },
        MenuEntry("Gracze online", Material.PLAYER_HEAD, 22) { player ->
            PlayerListGUI(plugin).open(player)
        },
    )

    override fun open(player: Player) {
        val inventory = Bukkit.createInventory(null, 27, getTitle())

        menuEntries.forEach { entry ->
            val lore = when (entry.slot) {
                13 -> listOf(
                    "<gray>Nazwa serwera: <green>${plugin.getServerName()}</green>",
                    "<gray>Ilość graczy online: <yellow>${plugin.server.onlinePlayers.size}</yellow>",
                    "<gray>Ilość wykonanych kar dzisiaj: <yellow>0</yellow>",
                    "<gray>Czas działania serwera: <green>1h</green> (<green>20.0</green>)"
                )
                else -> emptyList()
            }
            inventory.setItem(entry.slot, createItem(entry.material, entry.title, lore))
        }

        player.openInventory(inventory)
    }

    override fun handleClick(event: InventoryClickEvent) {
        val view = event.view
        val topSize = view.topInventory.size
        val slot = event.rawSlot

        if (slot !in 0 until topSize) return

        val entry = menuEntries.firstOrNull { it.slot == slot } ?: return

        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        entry.onClick(player)
    }

    override fun getTitle(): Component {
        return plugin.messageHandler.getLogMessage("GUI", "PunisherMain")
    }

    private fun createItem(material: Material, name: String, loreLines: List<String> = emptyList()): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(plugin.messageHandler.formatMixedTextToMiniMessage(name, TagResolver.empty()))
        if (loreLines.isNotEmpty()) {
            meta.lore(loreLines.map { plugin.messageHandler.formatMixedTextToMiniMessage(it, TagResolver.empty()) })
        }
        item.itemMeta = meta
        return item
    }
}
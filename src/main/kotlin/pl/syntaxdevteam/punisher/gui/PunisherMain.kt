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
 * Main menu of PunisherX. The GUI is composed of a list of [MenuEntry] items
 * that are loaded from the configuration. This allows easy expansion without
 * keeping separate variables for every button.
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
     * Helper used to load a [MenuEntry] from the configuration.
     */
    private fun loadEntry(
        path: String,
        defaultTitle: String,
        defaultMaterial: Material,
        defaultIndex: Int,
        onClick: (Player) -> Unit
    ): MenuEntry {
        val title = plugin.config.getString("$path.title", defaultTitle)!!
        val materialName = plugin.config.getString("$path.material", defaultMaterial.name)!!
        val material = Material.matchMaterial(materialName) ?: defaultMaterial
        val index = plugin.config.getInt("$path.index", defaultIndex)
        return MenuEntry(title, material, index, onClick)
    }

    /**
     * Definitions for all buttons displayed in the main menu. New entries can
     * be appended here without touching the rest of the logic.
     */
    private val menuEntries = listOf(
        loadEntry("GUI.punishments", "Kary graczy", Material.PAPER, 11) { player ->
            // Future: open GUI with player punishments and sorting options
            player.sendMessage(plugin.messageHandler.getMessage("GUI", "opening_punishments"))
        },
        loadEntry("GUI.serverinfo", "Informacje o serwerze", Material.COMPASS, 13) { player ->
            // Example information about the server – will be expanded later
            val serverName = plugin.getServerName()
            player.sendMessage(Component.text("Serwer: $serverName"))
        },
        loadEntry("GUI.online", "Gracze online", Material.PLAYER_HEAD, 15) { player ->
            // Simple listing of players currently online
            val online = plugin.server.onlinePlayers.joinToString(", ") { it.name }
            player.sendMessage(Component.text("Online: $online"))
        },
        loadEntry("GUI.stats", "Statystyki kar", Material.BOOK, 22) { player ->
            // Placeholder stats – proper counting will be implemented later
            player.sendMessage(Component.text("Łącznie: 0"))
            player.sendMessage(Component.text("Ten miesiąc: 0"))
            player.sendMessage(Component.text("Ten tydzień: 0"))
            player.sendMessage(Component.text("Dzisiaj: 0"))
        },
        loadEntry("GUI.settings", "Ustawienia", Material.REDSTONE_TORCH, 26) { player ->

        }
    )

    override fun open(player: Player) {
        val inventory = Bukkit.createInventory(null, 27, getTitle())

        menuEntries.forEach { entry ->
            inventory.setItem(entry.slot, createItem(entry.material, entry.title))
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

    private fun createItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(plugin.messageHandler.formatMixedTextToMiniMessage(name, TagResolver.empty()))
        item.itemMeta = meta
        return item
    }
}
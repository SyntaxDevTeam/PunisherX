package pl.syntaxdevteam.punisher.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.stats.PlayerStatsService

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
        MenuEntry(plugin.messageHandler.getCleanMessage("GUI", "PunisherMain.serwerInfo.title"), Material.PAPER, 4) { _ -> },
        MenuEntry(plugin.messageHandler.getCleanMessage("GUI", "PunisherMain.playerOnline.title"), Material.PLAYER_HEAD, 19) { player ->
            PlayerListGUI(plugin).open(player)
        },
        MenuEntry(plugin.messageHandler.getCleanMessage("GUI", "PunisherMain.adminOnline.title"), Material.COMMAND_BLOCK, 22) { _ ->
        },
        MenuEntry(plugin.messageHandler.getCleanMessage("GUI", "PunisherMain.compare.title"), Material.GOLDEN_CARROT, 25) { _ ->
        },
    )

    override fun open(player: Player) {
        val inventory = Bukkit.createInventory(null, 36, getTitle())
        val serverName = plugin.getServerName()
        val onlinePlayers = Bukkit.getOnlinePlayers().size.toString()
        val daily = "0"
        val time = "1h"
        val tps = "20.0"

        menuEntries.forEach { entry ->
            val lore = when (entry.slot) {
                4 -> listOf(
                    plugin.messageHandler.getCleanMessage("GUI", "PunisherMain.serwerInfo.serverName", mapOf("servername" to serverName)),
                    plugin.messageHandler.getCleanMessage("GUI", "PunisherMain.serwerInfo.daily", mapOf("daily" to daily)),
                    plugin.messageHandler.getCleanMessage("GUI", "PunisherMain.serwerInfo.tps", mapOf("time" to time, "tps" to tps)),
                )
                19 -> listOf(
                    plugin.messageHandler.getCleanMessage("GUI", "PunisherMain.playerOnline.online", mapOf("onlineplayers" to onlinePlayers)),
                    plugin.messageHandler.getCleanMessage("GUI", "PunisherMain.playerOnline.clickToView")
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
        return plugin.messageHandler.getLogMessage("GUI", "PunisherMain.playerOnline.title")
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
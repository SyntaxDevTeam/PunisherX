package pl.syntaxdevteam.punisher.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import pl.syntaxdevteam.punisher.PunisherX

/**
 * GUI displaying currently online players.
 */
class PlayerListGUI(private val plugin: PunisherX) : GUI {

    /**
     * Inventory holder used to store the current page of the GUI.
     */
    private class Holder(var page: Int) : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    override fun open(player: Player) {
        open(player, 0)
    }

    /**
     * Opens the player list GUI for the given page.
     */
    private fun open(player: Player, page: Int) {
        val online = plugin.server.onlinePlayers.toList()
        val playersPerPage = 45 // 5 rows of heads
        val totalPages = if (online.isEmpty()) 1 else (online.size - 1) / playersPerPage + 1
        val currentPage = page.coerceIn(0, totalPages - 1)

        val startIndex = currentPage * playersPerPage
        val playersPage = online.drop(startIndex).take(playersPerPage)

        val rows = ((playersPage.size - 1) / 9 + 2).coerceAtLeast(2)
        val size = rows * 9
        val holder = Holder(currentPage)
        val inventory = Bukkit.createInventory(holder, size, getTitle())
        holder.inv = inventory

        playersPage.forEachIndexed { index, target ->
            val head = ItemStack(Material.PLAYER_HEAD)
            val meta = head.itemMeta as SkullMeta
            meta.owningPlayer = target
            meta.displayName(
                plugin.messageHandler.formatMixedTextToMiniMessage("<yellow>${target.name}</yellow>", TagResolver.empty())
            )
            meta.lore(
                listOf(
                    plugin.messageHandler.formatMixedTextToMiniMessage("<gray>Czas online: <green>5m</green>", TagResolver.empty()),
                    plugin.messageHandler.formatMixedTextToMiniMessage("<gray>Czas łączny: <green>1h</green>", TagResolver.empty()),
                    plugin.messageHandler.formatMixedTextToMiniMessage("<gray>Kara: <red>Brak</red>", TagResolver.empty()),
                    plugin.messageHandler.formatMixedTextToMiniMessage("<gray>Łącznie kar: <yellow>0</yellow>", TagResolver.empty())
                )
            )
            head.itemMeta = meta
            inventory.setItem(index, head)
        }

        if (totalPages > 1) {
            if (currentPage > 0) {
                inventory.setItem(size - 9, createNavItem(Material.PAPER, "<yellow>Poprzednia strona</yellow>"))
            }
            if (currentPage < totalPages - 1) {
                inventory.setItem(size - 1, createNavItem(Material.BOOK, "<yellow>Następna strona</yellow>"))
            }
        }

        player.openInventory(inventory)
    }

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val holder = event.view.topInventory.holder as? Holder ?: return
        val player = event.whoClicked as? Player ?: return

        val online = plugin.server.onlinePlayers.toList()
        val playersPerPage = 45
        val totalPages = if (online.isEmpty()) 1 else (online.size - 1) / playersPerPage + 1
        val inventorySize = event.view.topInventory.size
        val slot = event.rawSlot

        when (slot) {
            inventorySize - 9 -> if (holder.page > 0) open(player, holder.page - 1)
            inventorySize - 1 -> if (holder.page < totalPages - 1) open(player, holder.page + 1)
        }
    }

    override fun getTitle(): Component {
        return plugin.messageHandler.formatMixedTextToMiniMessage("<gray>Gracze online</gray>", TagResolver.empty())
    }
    private fun createNavItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(plugin.messageHandler.formatMixedTextToMiniMessage(name, TagResolver.empty()))
        item.itemMeta = meta
        return item
    }
}
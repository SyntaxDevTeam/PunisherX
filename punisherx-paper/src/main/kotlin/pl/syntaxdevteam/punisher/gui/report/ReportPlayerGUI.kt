package pl.syntaxdevteam.punisher.gui.report

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

/**
 * GUI that lists online players to report.
 */
class ReportPlayerGUI(plugin: PunisherX) : BaseGUI(plugin) {

    private val centerSlots = intArrayOf(
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        28,29,30,31,32,33,34
    )

    private class Holder(val players: List<Player>, var page: Int) : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    override fun open(player: Player) {
        val online = ArrayList(plugin.server.onlinePlayers)
        // Do not include the player themselves in the list
        online.removeIf { it.uniqueId == player.uniqueId }
        online.sortBy { it.name.lowercase() }
        open(player, 0, online)
    }

    private fun open(player: Player, page: Int, list: List<Player>) {
        val playersPerPage = 27
        val totalPages = if (list.isEmpty()) 1 else (list.size - 1) / playersPerPage + 1
        val currentPage = page.coerceIn(0, totalPages - 1)

        val startIndex = currentPage * playersPerPage
        val playersPage = list.drop(startIndex).take(playersPerPage)

        val holder = Holder(list, currentPage)
        val inv = Bukkit.createInventory(holder, 45, getTitle())
        holder.inv = inv

        inv.fillWithFiller()

        playersPage.forEachIndexed { index, target ->
            val slot = if (index < centerSlots.size) centerSlots[index] else index
            val head = ItemStack(Material.PLAYER_HEAD)
            val meta = head.itemMeta as SkullMeta
            meta.owningPlayer = target
            meta.displayName(mH.formatMixedTextToMiniMessage("<gold><b>${target.name}</b></gold>", TagResolver.empty()))
            val loreText = mH.stringMessageToStringNoPrefix("GUI", "Report.lore.clickToReport")
            meta.lore(listOf(mH.formatMixedTextToMiniMessage(loreText, TagResolver.empty())))
            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES)
            head.itemMeta = meta
            inv.setItem(slot, head)
        }

        if (currentPage > 0)
            inv.setItem(36, createNavItem(Material.PAPER, mH.stringMessageToStringNoPrefix("GUI", "Nav.previous")))

        inv.setItem(40, createNavItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")))

        if (currentPage < totalPages - 1)
            inv.setItem(44, createNavItem(Material.BOOK, mH.stringMessageToStringNoPrefix("GUI", "Nav.next")))

        player.openInventory(inv)
    }

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val holder = event.view.topInventory.holder as? Holder ?: return
        val clicker = event.whoClicked as? Player ?: return

        val slot = event.rawSlot
        val playersPerPage = 27

        // Map centered slots back to index
        val localIndex = centerSlots.indexOf(slot)
        if (localIndex >= 0) {
            val index = holder.page * playersPerPage + localIndex
            if (index < holder.players.size) {
                val target = holder.players[index]
                if (target.uniqueId == clicker.uniqueId) {
                    clicker.sendMessage(mH.stringMessageToComponent("error", "cannot-report-self"))
                    return
                }
                ReportReasonGUI(plugin).open(clicker, target)
            }
            return
        }

        when (slot) {
            36 -> if (holder.page > 0) open(clicker, holder.page - 1, holder.players)
            40 -> ReportSelectorGUI(plugin).open(clicker)
            44 -> {
                val totalPages = if (holder.players.isEmpty()) 1 else (holder.players.size - 1) / playersPerPage + 1
                if (holder.page < totalPages - 1) open(clicker, holder.page + 1, holder.players)
            }
        }
    }

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "Report.player.title")
    }
}

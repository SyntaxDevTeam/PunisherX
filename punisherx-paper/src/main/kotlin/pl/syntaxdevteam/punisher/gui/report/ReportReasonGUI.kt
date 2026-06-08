package pl.syntaxdevteam.punisher.gui.report

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.util.UUID

/**
 * GUI to pick a reason for a report.
 */
class ReportReasonGUI(plugin: PunisherX) : BaseGUI(plugin) {

    private val centerSlots = intArrayOf(
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        28,29,30,31,32,33,34
    )

    private class Holder(val target: UUID, var page: Int, val reasons: List<String>) : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    fun open(player: Player, target: OfflinePlayer) {
        val reasons = plugin.config.getStringList("gui.punish.reasons").ifEmpty { listOf("Cheating", "Griefing", "Spamming") }
        openPaged(player, target, 0, reasons)
    }

    private fun openPaged(player: Player, target: OfflinePlayer, page: Int, reasons: List<String>) {
        val perPage = 27
        val totalPages = if (reasons.isEmpty()) 1 else (reasons.size - 1) / perPage + 1
        val currentPage = page.coerceIn(0, totalPages - 1)
        val startIndex = currentPage * perPage
        val pageReasons = reasons.drop(startIndex).take(perPage)

        val holder = Holder(target.uniqueId, currentPage, reasons)
        val inventory = Bukkit.createInventory(holder, 45, getTitle())
        holder.inv = inventory
        inventory.fillWithFiller()

        pageReasons.forEachIndexed { idx, reason ->
            val slot = if (idx < centerSlots.size) centerSlots[idx] else idx
            val material = when (reason.trim().lowercase()) {
                "cheating" -> Material.FISHING_ROD
                "griefing", "greefing" -> Material.NETHERITE_AXE
                "spamming" -> Material.BOOK
                "offensive language", "offensive_language" -> Material.PAPER
                else -> Material.BOOK
            }
            val item = ItemStack(material)
            val meta: ItemMeta = item.itemMeta
            meta.displayName(mH.formatMixedTextToMiniMessage("<gold><b>$reason</b></gold>", TagResolver.empty()))
            val loreText = mH.stringMessageToStringNoPrefix("GUI", "Report.lore.clickToChoose")
            meta.lore(listOf(mH.formatMixedTextToMiniMessage(loreText, TagResolver.empty())))
            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES)
            item.itemMeta = meta
            inventory.setItem(slot, item)
        }

        if (currentPage > 0)
            inventory.setItem(36, createNavItem(Material.PAPER, mH.stringMessageToStringNoPrefix("GUI", "Nav.previous")))

        inventory.setItem(40, createNavItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")))

        if (currentPage < totalPages - 1)
            inventory.setItem(44, createNavItem(Material.BOOK, mH.stringMessageToStringNoPrefix("GUI", "Nav.next")))

        player.openInventory(inventory)
    }

    override fun open(player: Player) { /* not used */ }

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val holder = event.view.topInventory.holder as? Holder ?: return
        val reporter = event.whoClicked as? Player ?: return
        val target = Bukkit.getOfflinePlayer(holder.target)
        val reasons = holder.reasons

        val slot = event.rawSlot

        // Map centered slots to local index
        val localIndex = centerSlots.indexOf(slot)
        if (localIndex >= 0) {
            val index = holder.page * 27 + localIndex
            if (index < reasons.size) {
                val reason = reasons[index]
                reporter.closeInventory()

                val success = plugin.databaseHandler.addReport(reporter.uniqueId, target.uniqueId, reason)
                if (success) {
                    reporter.sendMessage(
                        mH.stringMessageToComponent(
                            "reports",
                            "report-sent",
                            mapOf("target" to (target.name ?: target.uniqueId.toString()), "reason" to reason)
                        )
                    )

                    plugin.server.onlinePlayers
                        .filter { PermissionChecker.hasWithSee(it, PermissionChecker.PermissionKey.SEE_REPORTS) }
                        .forEach { staff ->
                            staff.sendMessage(
                                mH. stringMessageToComponentNoPrefix("reports", "admin-notify", mapOf(
                                        "reporter" to reporter.name,
                                        "target" to (target.name ?: target.uniqueId.toString()),
                                        "reason" to reason
                                    )
                                )
                            )
                        }
                } else {
                    reporter.sendMessage(
                        mH.stringMessageToComponentNoPrefix("error", "db_error")
                    )
                }
                return
            }
        }

        when (slot) {
            36 -> if (holder.page > 0) openPaged(reporter, target, holder.page - 1, reasons)
            40 -> ReportSelectorGUI(plugin).open(reporter)
            44 -> {
                val totalPages = if (reasons.isEmpty()) 1 else (reasons.size - 1) / 27 + 1
                if (holder.page < totalPages - 1) openPaged(reporter, target, holder.page + 1, reasons)
            }
        }
    }

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "Report.reason.title")
    }
}

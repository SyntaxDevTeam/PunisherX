package pl.syntaxdevteam.punisher.gui.report
import pl.syntaxdevteam.punisher.compatibility.*

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

/**
 * GUI to pick a reason for a report.
 */
class ReportReasonGUI(plugin: PunisherX) : BaseGUI(plugin) {

    private val centerSlots = intArrayOf(
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        28,29,30,31,32,33,34
    )

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

        val gui = createGui(5)

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
            val meta: ItemMeta = item.itemMeta ?: return@forEachIndexed
            meta.displayName(mH.formatMixedTextToMiniMessage("<gold><b>$reason</b></gold>", TagResolver.empty()))
            val loreText = mH.stringMessageToStringNoPrefix("GUI", "Report.lore.clickToChoose")
            meta.lore(listOf(mH.formatMixedTextToMiniMessage(loreText, TagResolver.empty())))
            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES)
            item.itemMeta = meta
            gui.setItem(slot, createGuiItem(item) { reporter ->
                submitReport(reporter, target, reason)
            })
        }

        if (currentPage > 0)
            gui.setItem(36, createNavGuiItem(Material.PAPER, mH.stringMessageToStringNoPrefix("GUI", "Nav.previous")) { clicker ->
                openPaged(clicker, target, currentPage - 1, reasons)
            })

        gui.setItem(40, createNavGuiItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")) { clicker ->
            ReportSelectorGUI(plugin).open(clicker)
        })

        if (currentPage < totalPages - 1)
            gui.setItem(44, createNavGuiItem(Material.BOOK, mH.stringMessageToStringNoPrefix("GUI", "Nav.next")) { clicker ->
                openPaged(clicker, target, currentPage + 1, reasons)
            })

        gui.open(player)
    }

    override fun open(player: Player) { /* not used */ }

    private fun submitReport(reporter: Player, target: OfflinePlayer, reason: String) {
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
                        mH.stringMessageToComponentNoPrefix(
                            "reports",
                            "admin-notify",
                            mapOf(
                                "reporter" to reporter.name,
                                "target" to (target.name ?: target.uniqueId.toString()),
                                "reason" to reason
                            )
                        )
                    )
                }
        } else {
            reporter.sendMessage(mH.stringMessageToComponentNoPrefix("error", "db_error"))
        }
    }

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "Report.reason.title")
    }
}

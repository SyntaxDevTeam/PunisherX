package pl.syntaxdevteam.punisher.gui.report
import pl.syntaxdevteam.punisher.compatibility.*

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

/**
 * GUI that lists recently offline players (last hour) to report.
 */
class ReportOfflineGUI(plugin: PunisherX) : BaseGUI(plugin) {

    private val centerSlots = intArrayOf(
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        28,29,30,31,32,33,34
    )

    override fun open(player: Player) {
        val now = System.currentTimeMillis()
        val oneHourMs = 60 * 60 * 1000L

        val recent = Bukkit.getOfflinePlayers()
            .asSequence()
            .filter { it.name != null && it.name!!.isNotBlank() }
            .filter { !it.isOnline }
            .map { player -> player to player.lastSeen }
            .filter { (_, lastSeen) -> lastSeen > 0L && (now - lastSeen) <= oneHourMs }
            .map { (player, _) -> player }
            .sortedBy { it.name!!.lowercase() }
            .toList()

        open(player, 0, recent)
    }

    private fun open(player: Player, page: Int, list: List<OfflinePlayer>) {
        val playersPerPage = 27
        val totalPages = if (list.isEmpty()) 1 else (list.size - 1) / playersPerPage + 1
        val currentPage = page.coerceIn(0, totalPages - 1)

        val startIndex = currentPage * playersPerPage
        val pageItems = list.drop(startIndex).take(playersPerPage)

        val gui = createGui(5)

        if (pageItems.isEmpty()) {
            gui.setItem(22, createGuiItem(
                Material.GRAY_DYE,
                "<gray>No recent offline players</gray>",
                listOf("<gray>Only players who left within the last hour are shown.</gray>")
            ))
        } else {
            pageItems.forEachIndexed { index, target ->
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
                gui.setItem(slot, createGuiItem(head) { clicker ->
                    if (target.uniqueId == clicker.uniqueId) {
                        clicker.sendMessage(mH.stringMessageToComponent("reports", "cannot-report-self"))
                        return@createGuiItem
                    }
                    ReportReasonGUI(plugin).open(clicker, target)
                })
            }
        }

        if (currentPage > 0)
            gui.setItem(36, createNavGuiItem(Material.PAPER, mH.stringMessageToStringNoPrefix("GUI", "Nav.previous")) { clicker ->
                open(clicker, currentPage - 1, list)
            })

        gui.setItem(40, createNavGuiItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")) { clicker ->
            ReportSelectorGUI(plugin).open(clicker)
        })

        if (currentPage < totalPages - 1)
            gui.setItem(44, createNavGuiItem(Material.BOOK, mH.stringMessageToStringNoPrefix("GUI", "Nav.next")) { clicker ->
                open(clicker, currentPage + 1, list)
            })

        gui.open(player)
    }

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "Report.offline.title")
    }
}

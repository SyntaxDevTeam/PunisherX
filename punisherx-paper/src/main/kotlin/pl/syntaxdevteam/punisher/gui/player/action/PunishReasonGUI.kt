package pl.syntaxdevteam.punisher.gui.player.action

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.OfflinePlayer
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

class PunishReasonGUI(plugin: PunisherX) : BaseGUI(plugin) {

    fun open(player: Player, target: OfflinePlayer, type: String, time: String) {
        val reasons = plugin.config.getStringList("gui.punish.reasons")
        openPage(player, target, type, time, reasons, 0)
    }

    private fun openPage(player: Player, target: OfflinePlayer, type: String, time: String, reasons: List<String>, page: Int) {
        val gui = createGui(5)
        val slots = (10..16) + (19..25) + (28..34)
        val perPage = slots.size
        val totalPages = maxOf(1, (reasons.size + perPage - 1) / perPage)
        val currentPage = page.coerceIn(0, totalPages - 1)
        reasons.drop(currentPage * perPage).take(perPage).forEachIndexed { index, reason ->
            gui.setItem(slots[index], createGuiItem(Material.PAPER, "<yellow>$reason</yellow>") { clicker ->
                clicker.closeInventory()
                plugin.guiPunishmentService.apply(clicker, target, type, time, reason)
            })
        }
        if (currentPage > 0) gui.setItem(36, createNavGuiItem(Material.ARROW, mH.stringMessageToStringNoPrefix("GUI", "Nav.previous")) { clicker ->
            openPage(clicker, target, type, time, reasons, currentPage - 1)
        })
        gui.setItem(40, createNavGuiItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")) { clicker ->
            PunishTimeGUI(plugin).open(clicker, target, type)
        })
        if (currentPage < totalPages - 1) gui.setItem(44, createNavGuiItem(Material.ARROW, mH.stringMessageToStringNoPrefix("GUI", "Nav.next")) { clicker ->
            openPage(clicker, target, type, time, reasons, currentPage + 1)
        })
        gui.open(player)
    }

    override fun open(player: Player) {}

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "PunishReason.title")
    }
}

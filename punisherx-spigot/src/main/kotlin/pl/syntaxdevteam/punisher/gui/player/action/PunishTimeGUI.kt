package pl.syntaxdevteam.punisher.gui.player.action

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.OfflinePlayer
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

class PunishTimeGUI(plugin: PunisherX) : BaseGUI(plugin) {

    fun open(player: Player, target: OfflinePlayer, type: String) {
        val times = plugin.config.getStringList("gui.punish.times")
        openPage(player, target, type, times, 0)
    }

    private fun openPage(player: Player, target: OfflinePlayer, type: String, times: List<String>, page: Int) {
        val gui = createGui(5)
        val slots = (10..16) + (19..25) + (28..34)
        val totalPages = maxOf(1, (times.size + slots.size - 1) / slots.size)
        val currentPage = page.coerceIn(0, totalPages - 1)
        times.drop(currentPage * slots.size).take(slots.size).forEachIndexed { index, time ->
            gui.setItem(slots[index], createGuiItem(Material.PAPER, "<yellow>$time</yellow>") { clicker ->
                PunishReasonGUI(plugin).open(clicker, target, type, time)
            })
        }
        if (currentPage > 0) gui.setItem(36, createNavGuiItem(Material.ARROW, mH.stringMessageToStringNoPrefix("GUI", "Nav.previous")) { openPage(it, target, type, times, currentPage - 1) })
        gui.setItem(40, createNavGuiItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")) { PunishTypeGUI(plugin).open(it, target) })
        if (currentPage < totalPages - 1) gui.setItem(44, createNavGuiItem(Material.ARROW, mH.stringMessageToStringNoPrefix("GUI", "Nav.next")) { openPage(it, target, type, times, currentPage + 1) })
        gui.open(player)
    }

    override fun open(player: Player) {}

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "PunishTime.title")
    }
}

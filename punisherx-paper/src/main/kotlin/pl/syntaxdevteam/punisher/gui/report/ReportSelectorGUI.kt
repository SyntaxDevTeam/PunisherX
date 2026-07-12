package pl.syntaxdevteam.punisher.gui.report

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

/**
 * First-step selector for /report: choose between Online and Offline (last hour) players.
 */
class ReportSelectorGUI(plugin: PunisherX) : BaseGUI(plugin) {

    override fun open(player: Player) {
        val gui = createGui(5)

        gui.setItem(20, createGuiItem(
            Material.GREEN_DYE,
            mH.stringMessageToStringNoPrefix("GUI", "Report.menu.online")
        ) { clicker -> ReportPlayerGUI(plugin).open(clicker) })

        gui.setItem(24, createGuiItem(
            Material.RED_DYE,
            mH.stringMessageToStringNoPrefix("GUI", "Report.menu.offline")
        ) { clicker -> ReportOfflineGUI(plugin).open(clicker) })

        gui.setItem(40, createNavGuiItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")) { clicker ->
            clicker.closeInventory()
        })

        gui.open(player)
    }

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "Report.menu.title")
    }
}

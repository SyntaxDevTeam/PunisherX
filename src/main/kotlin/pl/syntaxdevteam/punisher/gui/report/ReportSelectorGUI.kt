package pl.syntaxdevteam.punisher.gui.report

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

class ReportSelectorGUI(plugin: PunisherX) : BaseGUI(plugin) {
    override fun open(player: Player) {
        plugin.punisherMainGuiController.openReportSelector(player)
    }

    override fun handleClick(event: InventoryClickEvent) = Unit

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "Report.menu.title")
    }
}

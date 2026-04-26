package pl.syntaxdevteam.punisher.gui.interfaces

import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.admin.ConfigGUI
import pl.syntaxdevteam.punisher.gui.punishments.BanListGUI
import pl.syntaxdevteam.punisher.gui.punishments.JailListGUI
import pl.syntaxdevteam.punisher.gui.report.ReportOfflineGUI
import pl.syntaxdevteam.punisher.gui.report.ReportPlayerGUI
import pl.syntaxdevteam.punisher.gui.report.ReportReasonGUI
import pl.syntaxdevteam.punisher.gui.report.ReportSelectorGUI

class GUIHandler(private val plugin: PunisherX) : Listener {

    private val guiHandlers: List<Pair<Component, (InventoryClickEvent) -> Unit>> = listOf(
        guiHandler("BanList.title") { BanListGUI(plugin).handleClick(it) },
        guiHandler("JailList.title") { JailListGUI(plugin).handleClick(it) },
        guiHandler("Config.title") { ConfigGUI(plugin).handleClick(it) },
        guiHandler("Report.menu.title") { ReportSelectorGUI(plugin).handleClick(it) },
        guiHandler("Report.player.title") { ReportPlayerGUI(plugin).handleClick(it) },
        guiHandler("Report.offline.title") { ReportOfflineGUI(plugin).handleClick(it) },
        guiHandler("Report.reason.title") { ReportReasonGUI(plugin).handleClick(it) }
    )

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val title = event.view.title()
        guiHandlers.firstOrNull { (guiTitle, _) -> guiTitle == title }?.second?.invoke(event)
    }

    private fun guiHandler(messageKey: String, clickHandler: (InventoryClickEvent) -> Unit): Pair<Component, (InventoryClickEvent) -> Unit> {
        val title = plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", messageKey)
        return title to clickHandler
    }
}

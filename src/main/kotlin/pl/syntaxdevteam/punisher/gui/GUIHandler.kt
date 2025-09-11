package pl.syntaxdevteam.punisher.gui

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import pl.syntaxdevteam.punisher.PunisherX

class GUIHandler(private val plugin: PunisherX) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val title = event.view.title()

        when (title) {
            plugin.messageHandler.getLogMessage("GUI", "PunisherMain.title") -> PunisherMain(plugin).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "PunisherMain.playerOnline.title") -> PlayerListGUI(plugin).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "PunisherMain.adminOnline.title") -> AdminListGUI(plugin).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "PunisherMain.playerOffline.title") -> OfflinePlayerListGUI(plugin).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "PlayerAction.title") -> PlayerActionGUI(plugin).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "PlayerAction.confirmDelete.title") -> ConfirmDeleteGUI(plugin).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "PunishType.title") -> PunishTypeGUI(plugin).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "PunishTime.title") -> PunishTimeGUI(plugin).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "PunishReason.title") -> PunishReasonGUI(plugin).handleClick(event)
        }
    }
}
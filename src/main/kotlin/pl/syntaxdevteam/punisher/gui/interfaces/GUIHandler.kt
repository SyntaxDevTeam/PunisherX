package pl.syntaxdevteam.punisher.gui.interfaces

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.admin.AdminListGUI
import pl.syntaxdevteam.punisher.gui.punishments.BanListGUI
import pl.syntaxdevteam.punisher.gui.admin.ConfigGUI
import pl.syntaxdevteam.punisher.gui.player.action.ConfirmDeleteGUI
import pl.syntaxdevteam.punisher.gui.punishments.JailListGUI
import pl.syntaxdevteam.punisher.gui.player.OfflinePlayerListGUI
import pl.syntaxdevteam.punisher.gui.player.action.PlayerActionGUI
import pl.syntaxdevteam.punisher.gui.player.PlayerListGUI
import pl.syntaxdevteam.punisher.gui.player.action.PunishReasonGUI
import pl.syntaxdevteam.punisher.gui.player.action.PunishTimeGUI
import pl.syntaxdevteam.punisher.gui.player.action.PunishTypeGUI
import pl.syntaxdevteam.punisher.gui.punishments.PunishedListGUI
import pl.syntaxdevteam.punisher.gui.PunisherMain

class GUIHandler(private val plugin: PunisherX) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val title = event.view.title()

        when (title) {
            plugin.messageHandler.getLogMessage("GUI", "PunisherMain.title") -> PunisherMain(plugin).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "PunisherMain.playerOnline.title") -> PlayerListGUI(plugin).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "PunisherMain.adminOnline.title") -> AdminListGUI(plugin).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "PunisherMain.playerOffline.title") -> OfflinePlayerListGUI(
                plugin
            ).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "PlayerAction.title") -> PlayerActionGUI(plugin).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "PlayerAction.confirmDelete.title") -> ConfirmDeleteGUI(plugin).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "PunishType.title") -> PunishTypeGUI(plugin).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "PunishTime.title") -> PunishTimeGUI(plugin).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "PunishReason.title") -> PunishReasonGUI(plugin).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "PunishedList.title") -> PunishedListGUI(plugin).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "BanList.title") -> BanListGUI(plugin).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "JailList.title") -> JailListGUI(plugin).handleClick(event)
            plugin.messageHandler.getLogMessage("GUI", "Config.title") -> ConfigGUI(plugin).handleClick(event)
        }
    }
}
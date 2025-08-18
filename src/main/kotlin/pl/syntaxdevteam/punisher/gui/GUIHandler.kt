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
            plugin.messageHandler.getLogMessage("GUI", "PlayerList.title") -> PlayerListGUI(plugin).handleClick(event)
        }
    }
}
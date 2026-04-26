package pl.syntaxdevteam.punisher.gui.interfaces

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import pl.syntaxdevteam.punisher.PunisherX

/**
 * Legacy title-based dispatcher retained only for backward compatibility.
 * All current menus are handled by syntaxgui-api listeners.
 */
class GUIHandler(private val plugin: PunisherX) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        // no-op: syntaxgui-api handles migrated GUIs
    }
}

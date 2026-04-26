package pl.syntaxdevteam.punisher.gui.admin

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

class ConfigGUI(plugin: PunisherX) : BaseGUI(plugin) {

    override fun open(player: Player) {
        plugin.punisherMainGuiController.openConfig(player)
    }

    override fun handleClick(event: InventoryClickEvent) = Unit

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "Config.title")
    }
}

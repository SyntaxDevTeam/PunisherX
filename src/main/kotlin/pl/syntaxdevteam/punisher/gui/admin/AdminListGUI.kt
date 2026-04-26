package pl.syntaxdevteam.punisher.gui.admin

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

class AdminListGUI(plugin: PunisherX) : BaseGUI(plugin) {
    override fun open(player: Player) {
        plugin.punisherMainGuiController.openAdminList(player)
    }

    override fun handleClick(event: InventoryClickEvent) = Unit

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "PunisherMain.adminOnline.title")
    }
}

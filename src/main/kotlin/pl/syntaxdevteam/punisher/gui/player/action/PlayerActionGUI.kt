package pl.syntaxdevteam.punisher.gui.player.action

import net.kyori.adventure.text.Component
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

class PlayerActionGUI(plugin: PunisherX) : BaseGUI(plugin) {
    fun open(player: Player, target: OfflinePlayer) {
        plugin.punisherMainGuiController.openPlayerAction(player, target)
    }

    override fun open(player: Player) = Unit
    override fun handleClick(event: InventoryClickEvent) = Unit

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "PlayerAction.title")
    }
}

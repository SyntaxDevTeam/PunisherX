package pl.syntaxdevteam.punisher.gui.player.action
import pl.syntaxdevteam.punisher.compatibility.*

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.OfflinePlayer
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

class ConfirmDeleteGUI(plugin: PunisherX) : BaseGUI(plugin) {

    fun open(player: Player, target: OfflinePlayer) {
        val gui = createGui(3)

        gui.setItem(11, createGuiItem(Material.GREEN_WOOL, mH.stringMessageToStringNoPrefix("GUI", "PlayerAction.confirmDelete.confirm")) { clicker ->
            clicker.closeInventory()
            target.player?.kick(mH.stringMessageToComponentNoPrefix("GUI", "PlayerAction.deleteMessage"))
            plugin.databaseHandler.deletePlayerData(target.uniqueId.toString())
            plugin.playerIPManager.deletePlayerInfo(target.uniqueId)
        })
        gui.setItem(15, createGuiItem(Material.RED_WOOL, mH.stringMessageToStringNoPrefix("GUI", "PlayerAction.confirmDelete.cancel")) { clicker ->
            clicker.closeInventory()
            PlayerActionGUI(plugin).open(clicker, target)
        })

        gui.open(player)
    }

    override fun open(player: Player) {}

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "PlayerAction.confirmDelete.title")
    }
}

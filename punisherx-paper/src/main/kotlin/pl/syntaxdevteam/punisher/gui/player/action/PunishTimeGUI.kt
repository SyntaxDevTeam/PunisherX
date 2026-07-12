package pl.syntaxdevteam.punisher.gui.player.action

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.OfflinePlayer
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

class PunishTimeGUI(plugin: PunisherX) : BaseGUI(plugin) {

    fun open(player: Player, target: OfflinePlayer, type: String) {
        val gui = createGui(3)
        val times = plugin.config.getStringList("gui.punish.times")
        times.forEachIndexed { index, time ->
            gui.setItem(10 + index, createGuiItem(Material.PAPER, "<yellow>$time</yellow>") { clicker ->
                PunishReasonGUI(plugin).open(clicker, target, type, time)
            })
        }
        gui.open(player)
    }

    override fun open(player: Player) {}

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "PunishTime.title")
    }
}

package pl.syntaxdevteam.punisher.gui.player.action

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.OfflinePlayer
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

class PunishReasonGUI(plugin: PunisherX) : BaseGUI(plugin) {

    fun open(player: Player, target: OfflinePlayer, type: String, time: String) {
        val reasons = plugin.config.getStringList("gui.punish.reasons")
        val size = ((reasons.size / 9) + 1) * 9
        val gui = createGui((if (size < 27) 27 else size) / 9)
        reasons.forEachIndexed { index, reason ->
            gui.setItem(index, createGuiItem(Material.PAPER, "<yellow>$reason</yellow>") { clicker ->
                clicker.closeInventory()
                val force = plugin.config.getBoolean("gui.punish.use_force", false)
                val command = if (time.equals("perm", true)) {
                    "$type ${target.name} $reason" + if (force) " --force" else ""
                } else {
                    "$type ${target.name} $time $reason" + if (force) " --force" else ""
                }
                clicker.performCommand(command)
            })
        }
        gui.open(player)
    }

    override fun open(player: Player) {}

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "PunishReason.title")
    }
}

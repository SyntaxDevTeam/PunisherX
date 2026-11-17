package pl.syntaxdevteam.punisher.gui.punishments

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

class JailListGUI(plugin: PunisherX) : BaseGUI(plugin) {
    override fun open(player: Player) = PunishmentBrowserGUI(plugin).open(player, "JAIL")
    override fun getTitle(): Component = mH.stringMessageToComponentNoPrefix("GUI", "JailList.title")
}

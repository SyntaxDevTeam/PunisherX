package pl.syntaxdevteam.punisher.gui.punishments

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.PunisherMain
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

class PunishedListGUI(plugin: PunisherX) : BaseGUI(plugin) {

    override fun open(player: Player) {
        val gui = createGui(5)
        gui.setItem(20, createGuiItem(Material.IRON_SWORD, mH.stringMessageToStringNoPrefix("GUI", "PunishedList.banned")) { clicker ->
            BanListGUI(plugin).open(clicker)
        })
        gui.setItem(
            24,
            createGuiItem(
                plugin.guiMaterialResolver.resolveMaterial("IRON_CHAIN", "IRON_BARS", "CHAIN"),
                mH.stringMessageToStringNoPrefix("GUI", "PunishedList.jailed")
            ) { clicker ->
                JailListGUI(plugin).open(clicker)
            }
        )
        gui.setItem(40, createNavGuiItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")) { clicker ->
            PunisherMain(plugin).open(clicker)
        })
        gui.open(player)
    }

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "PunishedList.title")
    }
}

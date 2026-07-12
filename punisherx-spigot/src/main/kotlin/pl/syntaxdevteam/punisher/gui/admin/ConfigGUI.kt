package pl.syntaxdevteam.punisher.gui.admin

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.PunisherMain
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

class ConfigGUI(plugin: PunisherX) : BaseGUI(plugin) {

    override fun open(player: Player) {
        val gui = createGui(5)

        gui.setItem(20, createGuiItem(Material.COMPASS, mH.stringMessageToStringNoPrefix("GUI", "Config.setunjail")) { clicker ->
            clicker.closeInventory()
            clicker.performCommand("setunjail")
        })
        gui.setItem(
            24,
            createGuiItem(
                plugin.guiMaterialResolver.resolveMaterial("IRON_CHAIN", "CHAIN", "IRON_BARS"),
                mH.stringMessageToStringNoPrefix("GUI", "Config.setjail")
            ) { clicker ->
                clicker.closeInventory()
                clicker.performCommand("setjail 5")
            }
        )
        gui.setItem(40, createNavGuiItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")) { clicker ->
            PunisherMain(plugin).open(clicker)
        })
        gui.open(player)
    }

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "Config.title")
    }
}

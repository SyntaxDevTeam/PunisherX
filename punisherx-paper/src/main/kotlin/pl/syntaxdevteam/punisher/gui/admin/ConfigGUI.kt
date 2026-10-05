package pl.syntaxdevteam.punisher.gui.admin

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.PunisherMain
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import pl.syntaxdevteam.punisher.basic.JailUtils
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class ConfigGUI(plugin: PunisherX) : BaseGUI(plugin) {

    override fun open(player: Player) {
        val gui = createGui(5)

        gui.setItem(20, createGuiItem(Material.COMPASS, mH.stringMessageToStringNoPrefix("GUI", "Config.setunjail")) { clicker ->
            clicker.closeInventory()
            if (!PermissionChecker.hasWithLegacy(clicker, PermissionChecker.PermissionKey.MANAGE_SET_SPAWN)) {
                clicker.sendMessage(mH.stringMessageToComponent("error", "no_permission")); return@createGuiItem
            }
            val location = clicker.location
            if (JailUtils.setUnjailLocation(plugin.config, location)) {
                plugin.saveConfig()
                clicker.sendMessage(mH.stringMessageToComponent("setunjail", "set", mapOf("world" to (location.world?.name ?: "world"), "locationx" to location.blockX.toString(), "locationy" to location.blockY.toString(), "locationz" to location.blockZ.toString())))
            } else clicker.sendMessage(mH.stringMessageToComponent("setunjail", "set_error"))
        })
        gui.setItem(
            24,
            createGuiItem(
                plugin.guiMaterialResolver.resolveMaterial("IRON_CHAIN", "CHAIN", "IRON_BARS"),
                mH.stringMessageToStringNoPrefix("GUI", "Config.setjail")
            ) { clicker ->
                clicker.closeInventory()
                if (!PermissionChecker.hasWithManage(clicker, PermissionChecker.PermissionKey.MANAGE_SET_JAIL)) {
                    clicker.sendMessage(mH.stringMessageToComponent("error", "no_permission")); return@createGuiItem
                }
                val location = clicker.location
                if (JailUtils.setJailLocation(plugin.config, location, 5.0)) {
                    plugin.saveConfig()
                    clicker.sendMessage(mH.stringMessageToComponent("setjail", "set", mapOf("world" to (location.world?.name ?: "world"), "locationx" to location.blockX.toString(), "locationy" to location.blockY.toString(), "locationz" to location.blockZ.toString(), "radius" to "5.0")))
                } else clicker.sendMessage(mH.stringMessageToComponent("setjail", "set_error"))
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

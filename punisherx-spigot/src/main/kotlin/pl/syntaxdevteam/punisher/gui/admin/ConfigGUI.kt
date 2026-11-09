package pl.syntaxdevteam.punisher.gui.admin
import pl.syntaxdevteam.punisher.compatibility.*

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
                val radius = plugin.config.getDouble("jail.radius", 5.0)
                if (JailUtils.setJailLocation(plugin.config, location, radius)) {
                    plugin.saveConfig()
                    clicker.sendMessage(mH.stringMessageToComponent("setjail", "set", mapOf("world" to (location.world?.name ?: "world"), "locationx" to location.blockX.toString(), "locationy" to location.blockY.toString(), "locationz" to location.blockZ.toString(), "radius" to radius.toString())))
                } else clicker.sendMessage(mH.stringMessageToComponent("setjail", "set_error"))
            }
        )
        val settings = ConfigGuiSettings(plugin)
        val materials = listOf(Material.ENDER_EYE, Material.COMMAND_BLOCK, Material.PAPER, Material.BARRIER, Material.CLOCK)
        ConfigGuiSettings.Toggle.entries.forEachIndexed { index, option ->
            val enabled = plugin.config.getBoolean(option.path, option.default)
            val status = mH.stringMessageToStringNoPrefix("GUI", "Config." + if (enabled) "enabled" else "disabled")
            gui.setItem(10 + index, createGuiItem(materials[index],
                mH.stringMessageToStringNoPrefix("GUI", "Config.${option.label}"), listOf(status)) { clicker ->
                showResult(clicker, settings.toggle(clicker, option))
            })
        }
        gui.setItem(16, createGuiItem(Material.NAME_TAG,
            mH.stringMessageToStringNoPrefix("GUI", "Config.hiddenPlayers"),
            listOf(mH.stringMessageToStringNoPrefix("GUI", "Config.hiddenCount",
                mapOf("count" to plugin.config.getStringList("privacy.ip.hidden_players").size.toString())))) { clicker ->
            if (!settings.allowed(clicker, true)) {
                showResult(clicker, ConfigGuiSettings.Result.DENIED)
                return@createGuiItem
            }
            plugin.guiSearchService.request(clicker,
                mH.stringMessageToStringNoPrefix("GUI", "Config.hiddenPrompt"),
                { cancelled -> ConfigGUI(plugin).open(cancelled) }) { editor, input ->
                showResult(editor, settings.hiddenPlayers(editor, input))
            }
        })
        gui.setItem(40, createNavGuiItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")) { clicker ->
            PunisherMain(plugin).open(clicker)
        })
        openGui(gui, player)
    }

    private fun showResult(player: Player, result: ConfigGuiSettings.Result) {
        val key = when (result) {
            ConfigGuiSettings.Result.DENIED -> { player.sendMessage(mH.stringMessageToComponent("error", "no_permission")); return }
            ConfigGuiSettings.Result.ACTIVE_JAIL -> "activeJail"
            ConfigGuiSettings.Result.INVALID -> "invalidPlayers"
            ConfigGuiSettings.Result.SAVED -> "saved"
        }
        player.sendMessage(mH.stringMessageToComponent("GUI", "Config.$key"))
        ConfigGUI(plugin).open(player)
    }

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "Config.title")
    }
}

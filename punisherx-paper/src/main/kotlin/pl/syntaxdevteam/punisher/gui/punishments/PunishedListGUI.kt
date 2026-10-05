package pl.syntaxdevteam.punisher.gui.punishments

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.PunisherMain
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class PunishedListGUI(plugin: PunisherX) : BaseGUI(plugin) {

    override fun open(player: Player) {
        if (!PermissionChecker.hasWithSee(player, PermissionChecker.PermissionKey.SEE) &&
            !PermissionChecker.hasWithLegacy(player, PermissionChecker.PermissionKey.BAN_LIST)) {
            player.sendMessage(mH.stringMessageToComponent("error", "no_permission"))
            return
        }
        val gui = createGui(5)
        gui.setItem(13, createGuiItem(Material.BOOK, "<yellow>All active punishments</yellow>") { clicker ->
            PunishmentBrowserGUI(plugin).open(clicker)
        })
        gui.setItem(20, createGuiItem(Material.IRON_SWORD, mH.stringMessageToStringNoPrefix("GUI", "PunishedList.banned")) { clicker ->
            PunishmentBrowserGUI(plugin).open(clicker, "BANS")
        })
        gui.setItem(
            24,
            createGuiItem(
                plugin.guiMaterialResolver.resolveMaterial("IRON_CHAIN", "IRON_BARS", "CHAIN"),
                mH.stringMessageToStringNoPrefix("GUI", "PunishedList.jailed")
            ) { clicker ->
                PunishmentBrowserGUI(plugin).open(clicker, "JAIL")
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

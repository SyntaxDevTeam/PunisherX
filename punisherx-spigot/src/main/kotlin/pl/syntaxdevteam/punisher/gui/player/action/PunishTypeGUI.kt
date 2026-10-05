package pl.syntaxdevteam.punisher.gui.player.action

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.OfflinePlayer
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import pl.syntaxdevteam.punisher.compatibility.*

class PunishTypeGUI(plugin: PunisherX) : BaseGUI(plugin) {

    fun open(player: Player, target: OfflinePlayer) {
        val gui = createGui(3)

        gui.setItem(10, createGuiItem(Material.IRON_SWORD, mH.stringMessageToStringNoPrefix("GUI", "PunishType.ban")) { clicker ->
            PunishTimeGUI(plugin).open(clicker, target, "ban")
        })
        gui.setItem(11, createGuiItem(Material.REDSTONE_BLOCK, mH.stringMessageToStringNoPrefix("GUI", "PunishType.banip")) { clicker ->
            clicker.closeInventory()
            val reasonBan = mH.stringMessageToString("banip", "no_reasons")
            plugin.guiPunishmentService.banIp(clicker, target, reasonBan)
        })
        gui.setItem(12, createGuiItem(Material.BLAZE_ROD, mH.stringMessageToStringNoPrefix("GUI", "PunishType.kick")) { clicker ->
            val online = target.player ?: return@createGuiItem
            clicker.closeInventory()
            if (!PermissionChecker.hasWithLegacy(clicker, PermissionChecker.PermissionKey.KICK)) {
                clicker.sendMessage(mH.stringMessageToComponent("error", "no_permission")); return@createGuiItem
            }
            if (!plugin.config.getBoolean("gui.punish.use_force", false) && PermissionChecker.hasWithLegacy(online, PermissionChecker.PermissionKey.BYPASS_KICK)) {
                clicker.sendMessage(mH.stringMessageToComponent("error", "bypass", mapOf("player" to online.name))); return@createGuiItem
            }
            val reasonKick = mH.stringMessageToString("kick", "no_reasons")
            online.kick(mH.stringMessageToComponent("kick", "kick_message", mapOf("player" to online.name, "operator" to clicker.name, "reason" to reasonKick)))
            clicker.sendMessage(mH.stringMessageToComponent("kick", "kick", mapOf("player" to online.name, "reason" to reasonKick)))
        })
        gui.setItem(
            14,
            createGuiItem(
                plugin.guiMaterialResolver.resolveMaterial("IRON_CHAIN", "IRON_BARS", "CHAIN"),
                mH.stringMessageToStringNoPrefix("GUI", "PunishType.jail")
            ) { clicker -> PunishTimeGUI(plugin).open(clicker, target, "jail") }
        )
        gui.setItem(15, createGuiItem(Material.BOOK, mH.stringMessageToStringNoPrefix("GUI", "PunishType.mute")) { clicker ->
            PunishTimeGUI(plugin).open(clicker, target, "mute")
        })
        gui.setItem(16, createGuiItem(Material.PAPER, mH.stringMessageToStringNoPrefix("GUI", "PunishType.warn")) { clicker ->
            PunishTimeGUI(plugin).open(clicker, target, "warn")
        })

        gui.open(player)
    }

    override fun open(player: Player) {}

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "PunishType.title")
    }
}

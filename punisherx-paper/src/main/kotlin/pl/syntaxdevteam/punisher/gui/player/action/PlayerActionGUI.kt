package pl.syntaxdevteam.punisher.gui.player.action

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import pl.syntaxdevteam.punisher.gui.player.OfflinePlayerListGUI
import pl.syntaxdevteam.punisher.gui.player.PlayerListGUI
import pl.syntaxdevteam.punisher.gui.stats.PlayerStatsService

class PlayerActionGUI(plugin: PunisherX) : BaseGUI(plugin) {

    fun open(player: Player, target: OfflinePlayer) {
        val gui = createGui(5)

        gui.setItem(11, createGuiItem(Material.MACE, mH.stringMessageToStringNoPrefix("GUI", "PlayerAction.punish")) { clicker ->
            PunishTypeGUI(plugin).open(clicker, target)
        })

        val targetName = target.name ?: return

        gui.setItem(13, createGuiItem(Material.TOTEM_OF_UNDYING, mH.stringMessageToStringNoPrefix("GUI", "PlayerAction.undo")) { clicker ->
            clicker.closeInventory()
            val punishments = plugin.databaseHandler.getPunishments(target.uniqueId.toString())
            if (punishments.isEmpty()) {
                clicker.sendMessage(mH.stringMessageToComponent("error", "no_data"))
            } else {
                punishments.forEach { punishment ->
                    val command = when (punishment.type) {
                        "BAN", "BANIP" -> "unban $targetName"
                        "MUTE" -> "unmute $targetName"
                        "WARN" -> "unwarn $targetName"
                        "JAIL" -> "unjail $targetName"
                        else -> null
                    }
                    if (command != null) {
                        clicker.performCommand(command)
                    }
                }
            }
        })
        gui.setItem(15, createGuiItem(Material.BOOK, mH.stringMessageToStringNoPrefix("GUI", "PlayerAction.history")) { clicker ->
            clicker.closeInventory()
            clicker.performCommand("history $targetName")
        })
        gui.setItem(29, createGuiItem(Material.PAPER, mH.stringMessageToStringNoPrefix("GUI", "PlayerAction.active")) { clicker ->
            clicker.closeInventory()
            clicker.performCommand("check $targetName all")
        })

        gui.setItem(31, createGuiItem(Material.ENDER_PEARL, mH.stringMessageToStringNoPrefix("GUI", "PlayerAction.teleport")) { clicker ->
            clicker.closeInventory()
            val online = target.player
            if (online != null) {
                plugin.safeTeleportService.teleportSafely(clicker, online.location)
            } else {
                val loc = PlayerStatsService.getLastLocation(target.uniqueId)
                if (loc != null) {
                    plugin.safeTeleportService.teleportSafely(clicker, loc)
                } else {
                    clicker.sendMessage(mH.stringMessageToComponent("error", "no_data"))
                }
            }
        })
        gui.setItem(33, createGuiItem(Material.TNT, mH.stringMessageToStringNoPrefix("GUI", "PlayerAction.delete")) { clicker ->
            ConfirmDeleteGUI(plugin).open(clicker, target)
        })
        gui.setItem(40, createNavGuiItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")) { clicker ->
            if (target.isOnline) PlayerListGUI(plugin).open(clicker) else OfflinePlayerListGUI(plugin).open(clicker)
        })

        gui.open(player)
    }

    override fun open(player: Player) {}

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "PlayerAction.title")
    }
}

package pl.syntaxdevteam.punisher.gui.player.action

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.common.TeleportUtils
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import pl.syntaxdevteam.punisher.gui.player.OfflinePlayerListGUI
import pl.syntaxdevteam.punisher.gui.player.PlayerListGUI
import pl.syntaxdevteam.punisher.gui.stats.PlayerStatsService
import java.util.UUID

class PlayerActionGUI(plugin: PunisherX) : BaseGUI(plugin) {

    private class Holder(val target: UUID) : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    fun open(player: Player, target: OfflinePlayer) {
        val holder = Holder(target.uniqueId)
        val inventory = Bukkit.createInventory(holder, 45, getTitle())
        holder.inv = inventory

        inventory.fillWithFiller()

        inventory.setItem(11, createItem(Material.MACE, mH.getCleanMessage("GUI", "PlayerAction.punish")))

        inventory.setItem(13, createItem(Material.TOTEM_OF_UNDYING, mH.getCleanMessage("GUI", "PlayerAction.undo")))
        inventory.setItem(15, createItem(Material.BOOK, mH.getCleanMessage("GUI", "PlayerAction.history")))
        inventory.setItem(29, createItem(Material.PAPER, mH.getCleanMessage("GUI", "PlayerAction.active")))
        inventory.setItem(31, createItem(Material.ENDER_PEARL, mH.getCleanMessage("GUI", "PlayerAction.teleport")))
        inventory.setItem(33, createItem(Material.TNT, mH.getCleanMessage("GUI", "PlayerAction.delete")))
        inventory.setItem(40, createNavItem(Material.BARRIER, mH.getCleanMessage("GUI", "Nav.back")))

        player.openInventory(inventory)
    }

    override fun open(player: Player) {}

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val holder = event.view.topInventory.holder as? Holder ?: return
        val player = event.whoClicked as? Player ?: return
        val target = Bukkit.getOfflinePlayer(holder.target)
        val targetName = target.name ?: return


        when (event.rawSlot) {
            11 -> PunishTypeGUI(plugin).open(player, target)
            13 -> {
                player.closeInventory()
                val punishments = plugin.databaseHandler.getPunishments(target.uniqueId.toString())
                if (punishments.isEmpty()) {
                    player.sendMessage(mH.getMessage("error", "no_data"))
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
                            player.performCommand(command)
                        }
                    }
                }
            }
            15 -> {
                player.closeInventory()
                player.performCommand("history $targetName")
            }
            29 -> {
                player.closeInventory()
                player.performCommand("check $targetName all")
            }

            31 -> {
                player.closeInventory()
                val online = target.player
                if (online != null) {
                    TeleportUtils.teleportSafely(plugin, player, online.location)
                } else {
                    val loc = PlayerStatsService.getLastLocation(target.uniqueId)
                    if (loc != null) {
                        TeleportUtils.teleportSafely(plugin, player, loc)
                    } else {
                        player.sendMessage(mH.getMessage("error", "no_data"))
                    }
                }
            }
            33 -> ConfirmDeleteGUI(plugin).open(player, target)
            40 -> if (target.isOnline) PlayerListGUI(plugin).open(player) else OfflinePlayerListGUI(plugin).open(player)
        }
    }

    override fun getTitle(): Component {
        return mH.getLogMessage("GUI", "PlayerAction.title")
    }
}
package pl.syntaxdevteam.punisher.gui.player.action

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.OfflinePlayer
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import java.util.UUID

class PunishTypeGUI(plugin: PunisherX) : BaseGUI(plugin) {

    private class Holder(val target: UUID) : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    fun open(player: Player, target: OfflinePlayer) {
        val holder = Holder(target.uniqueId)
        val inventory = Bukkit.createInventory(holder, 27, getTitle())
        holder.inv = inventory

        inventory.fillWithFiller()

        inventory.setItem(10, createItem(Material.IRON_SWORD, mH.stringMessageToStringNoPrefix("GUI", "PunishType.ban")))
        inventory.setItem(11, createItem(Material.REDSTONE_BLOCK, mH.stringMessageToStringNoPrefix("GUI", "PunishType.banip")))
        inventory.setItem(12, createItem(Material.BLAZE_ROD, mH.stringMessageToStringNoPrefix("GUI", "PunishType.kick")))
        inventory.setItem(
            14,
            createItem(
                plugin.versionCompatibility.resolveMaterial("IRON_CHAIN", "IRON_BARS", "CHAIN"),
                mH.stringMessageToStringNoPrefix("GUI", "PunishType.jail")
            )
        )
        inventory.setItem(15, createItem(Material.BOOK, mH.stringMessageToStringNoPrefix("GUI", "PunishType.mute")))
        inventory.setItem(16, createItem(Material.PAPER, mH.stringMessageToStringNoPrefix("GUI", "PunishType.warn")))

        player.openInventory(inventory)
    }

    override fun open(player: Player) {}

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val holder = event.view.topInventory.holder as? Holder ?: return
        val player = event.whoClicked as? Player ?: return
        val target = Bukkit.getOfflinePlayer(holder.target)
        val reasonKick = mH.stringMessageToString("kick", "no_reasons")
        val reasonBan = mH.stringMessageToString("banip", "no_reasons")
        val force = plugin.config.getBoolean("gui.punish.use_force", false)
        when (event.rawSlot) {
            10 -> PunishTimeGUI(plugin).open(player, target, "ban")
            11 -> {
                player.closeInventory()
                val command = buildString {
                    append("banip ")
                    append(target.name)
                    append(' ')
                    append(reasonBan)
                    if (force) append(" --force")
                }
                player.performCommand(command)
            }
            12 -> {
                val online = target.player ?: return
                player.closeInventory()
                val command = buildString {
                    append("kick ")
                    append(online.name)
                    append(' ')
                    append(reasonKick)
                    if (force) append(" --force")
                }
                player.performCommand(command)
            }
            14 -> PunishTimeGUI(plugin).open(player, target, "jail")
            15 -> PunishTimeGUI(plugin).open(player, target, "mute")
            16 -> PunishTimeGUI(plugin).open(player, target, "warn")
        }
    }

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "PunishType.title")
    }
}
package pl.syntaxdevteam.punisher.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.OfflinePlayer
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.stats.PlayerStatsService
import java.util.UUID

class PlayerActionGUI(private val plugin: PunisherX) : GUI {

    private val mH = plugin.messageHandler

    private class Holder(val target: UUID) : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    fun open(player: Player, target: OfflinePlayer) {
        val holder = Holder(target.uniqueId)
        val inventory = Bukkit.createInventory(holder, 45, getTitle())
        holder.inv = inventory

        for (slot in 0 until 45) {
            inventory.setItem(slot, createFillerItem())
        }

        inventory.setItem(11, createItem(Material.MACE, mH.getCleanMessage("GUI", "PlayerAction.punish")))
        if (target.isOnline) {
            inventory.setItem(13, createItem(Material.BLAZE_ROD, mH.getCleanMessage("GUI", "PlayerAction.kick")))
        }
        inventory.setItem(15, createItem(Material.REDSTONE_BLOCK, mH.getCleanMessage("GUI", "PlayerAction.banip")))
        inventory.setItem(29, createItem(Material.ENDER_PEARL, mH.getCleanMessage("GUI", "PlayerAction.teleport")))
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
        val reasonKick = mH.getSimpleMessage("kick", "no_reasons")
        val reasonBan = mH.getSimpleMessage("banip", "no_reasons")
        val force = plugin.config.getBoolean("gui.punish.use_force", false)

        when (event.rawSlot) {
            11 -> PunishTypeGUI(plugin).open(player, target)
            13 -> {
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
            15 -> {
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
            29 -> {
                player.closeInventory()
                val online = target.player
                if (online != null) {
                    player.teleport(online)
                } else {
                    val loc = PlayerStatsService.getLastLocation(target.uniqueId)
                    if (loc != null) {
                        player.teleport(loc)
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

    private fun createItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(mH.formatMixedTextToMiniMessage(name, TagResolver.empty()))
        item.itemMeta = meta
        return item
    }

    private fun createNavItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(mH.formatMixedTextToMiniMessage(name, TagResolver.empty()))
        item.itemMeta = meta
        return item
    }

    private fun createFillerItem(): ItemStack {
        val item = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val meta = item.itemMeta
        meta.displayName(Component.text(" "))
        item.itemMeta = meta
        return item
    }
}

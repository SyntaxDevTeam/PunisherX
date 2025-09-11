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
import java.util.UUID

class PlayerActionGUI(private val plugin: PunisherX) : GUI {

    private val mH = plugin.messageHandler

    private class Holder(val target: UUID) : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    fun open(player: Player, target: OfflinePlayer) {
        val holder = Holder(target.uniqueId)
        val inventory = Bukkit.createInventory(holder, 27, getTitle())
        holder.inv = inventory

        inventory.setItem(10, createItem(Material.MACE, mH.getCleanMessage("GUI", "PlayerAction.punish")))
        if (target.isOnline) {
            inventory.setItem(13, createItem(Material.BLAZE_ROD, mH.getCleanMessage("GUI", "PlayerAction.kick")))
        }
        inventory.setItem(16, createItem(Material.TNT, mH.getCleanMessage("GUI", "PlayerAction.delete")))

        player.openInventory(inventory)
    }

    override fun open(player: Player) {}

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val holder = event.view.topInventory.holder as? Holder ?: return
        val player = event.whoClicked as? Player ?: return
        val target = Bukkit.getOfflinePlayer(holder.target)
        val reason = mH.getSimpleMessage("kick", "no_reasons")
        val force = plugin.config.getBoolean("gui.punish.use_force", false)

        when (event.rawSlot) {
            10 -> PunishTypeGUI(plugin).open(player, target)
            13 -> {
                val online = target.player ?: return
                player.closeInventory()

                val command = buildString {
                    append("kick ")
                    append(online.name)
                    append(' ')
                    append(reason)
                    if (force) append(" --force")
                }

                player.performCommand(command)
            }
            16 -> ConfirmDeleteGUI(plugin).open(player, target)
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
}

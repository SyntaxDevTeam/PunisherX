package pl.syntaxdevteam.punisher.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
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

    fun open(player: Player, target: Player) {
        val holder = Holder(target.uniqueId)
        val inventory = Bukkit.createInventory(holder, 27, getTitle())
        holder.inv = inventory

        inventory.setItem(10, createItem(Material.ANVIL, mH.getCleanMessage("GUI", "PlayerAction.punish")))
        inventory.setItem(12, createItem(Material.BLAZE_ROD, mH.getCleanMessage("GUI", "PlayerAction.kick")))
        inventory.setItem(14, createItem(Material.TNT, mH.getCleanMessage("GUI", "PlayerAction.delete")))

        player.openInventory(inventory)
    }

    override fun open(player: Player) {}

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val holder = event.view.topInventory.holder as? Holder ?: return
        val player = event.whoClicked as? Player ?: return
        val target = Bukkit.getPlayer(holder.target) ?: return
        when (event.rawSlot) {
            10 -> PunishTypeGUI(plugin).open(player, target)
            12 -> {
                player.closeInventory()
                player.performCommand("kick ${target.name} --force")
            }
            14 -> {
                player.closeInventory()
                target.kick(mH.getLogMessage("GUI", "PlayerAction.deleteMessage"))
                plugin.databaseHandler.deletePlayerData(target.uniqueId.toString())
                plugin.playerIPManager.deletePlayerInfo(target.uniqueId)
            }
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

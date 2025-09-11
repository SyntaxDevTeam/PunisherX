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

class PunishTypeGUI(private val plugin: PunisherX) : GUI {

    private val mH = plugin.messageHandler

    private class Holder(val target: UUID) : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    fun open(player: Player, target: OfflinePlayer) {
        val holder = Holder(target.uniqueId)
        val inventory = Bukkit.createInventory(holder, 27, getTitle())
        holder.inv = inventory

        inventory.setItem(10, createItem(Material.IRON_SWORD, mH.getCleanMessage("GUI", "PunishType.ban")))
        inventory.setItem(12, createItem(Material.IRON_BARS, mH.getCleanMessage("GUI", "PunishType.jail")))
        inventory.setItem(14, createItem(Material.BOOK, mH.getCleanMessage("GUI", "PunishType.mute")))
        inventory.setItem(16, createItem(Material.PAPER, mH.getCleanMessage("GUI", "PunishType.warn")))

        player.openInventory(inventory)
    }

    override fun open(player: Player) {}

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val holder = event.view.topInventory.holder as? Holder ?: return
        val player = event.whoClicked as? Player ?: return
        val target = Bukkit.getOfflinePlayer(holder.target)
        when (event.rawSlot) {
            10 -> PunishTimeGUI(plugin).open(player, target, "ban")
            12 -> PunishTimeGUI(plugin).open(player, target, "jail")
            14 -> PunishTimeGUI(plugin).open(player, target, "mute")
            16 -> PunishTimeGUI(plugin).open(player, target, "warn")
        }
    }

    override fun getTitle(): Component {
        return mH.getLogMessage("GUI", "PunishType.title")
    }

    private fun createItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(mH.formatMixedTextToMiniMessage(name, TagResolver.empty()))
        item.itemMeta = meta
        return item
    }
}
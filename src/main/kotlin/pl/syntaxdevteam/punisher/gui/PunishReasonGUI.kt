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

class PunishReasonGUI(private val plugin: PunisherX) : GUI {

    private val mH = plugin.messageHandler

    private class Holder(val target: UUID, val type: String, val time: String) : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    fun open(player: Player, target: Player, type: String, time: String) {
        val holder = Holder(target.uniqueId, type, time)
        val reasons = plugin.config.getStringList("gui.punish.reasons")
        val size = ((reasons.size / 9) + 1) * 9
        val inventory = Bukkit.createInventory(holder, if (size < 27) 27 else size, getTitle())
        holder.inv = inventory
        reasons.forEachIndexed { index, reason ->
            inventory.setItem(index, createItem(Material.PAPER, "<yellow>$reason</yellow>"))
        }
        player.openInventory(inventory)
    }

    override fun open(player: Player) {}

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val holder = event.view.topInventory.holder as? Holder ?: return
        val player = event.whoClicked as? Player ?: return
        val target = Bukkit.getPlayer(holder.target) ?: return
        val reasons = plugin.config.getStringList("gui.punish.reasons")
        val force = plugin.config.getBoolean("gui.punish.use_force", false)

        val slot = event.rawSlot
        if (slot in 0 until reasons.size) {
            val reason = reasons[slot]
            player.closeInventory()

            val base = if (holder.time.equals("perm", true)) {
                "${holder.type} ${target.name} $reason"
            } else {
                "${holder.type} ${target.name} ${holder.time} $reason"
            }

            val command = base + if (force) " --force" else ""
            player.performCommand(command)
        }

    }

    override fun getTitle(): Component {
        return mH.getLogMessage("GUI", "PunishReason.title")
    }

    private fun createItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(mH.formatMixedTextToMiniMessage(name, TagResolver.empty()))
        item.itemMeta = meta
        return item
    }
}
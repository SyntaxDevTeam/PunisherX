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

class PunishReasonGUI(plugin: PunisherX) : BaseGUI(plugin) {

    private class Holder(val target: UUID, val type: String, val time: String) : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    fun open(player: Player, target: OfflinePlayer, type: String, time: String) {
        val holder = Holder(target.uniqueId, type, time)
        val reasons = plugin.config.getStringList("gui.punish.reasons")
        val size = ((reasons.size / 9) + 1) * 9
        val inventory = Bukkit.createInventory(holder, if (size < 27) 27 else size, getTitle())
        holder.inv = inventory
        inventory.fillWithFiller()
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
        val target = Bukkit.getOfflinePlayer(holder.target)
        val reasons = plugin.config.getStringList("gui.punish.reasons")
        val force = plugin.config.getBoolean("gui.punish.use_force", false)

        val slot = event.rawSlot
        if (slot in 0 until reasons.size) {
            val reason = reasons[slot]
            player.closeInventory()

            val command = if (holder.time.equals("perm", true)) {
                "${holder.type} ${target.name} $reason" + if (force) " --force" else ""
            } else {
                "${holder.type} ${target.name} ${holder.time} $reason" + if (force) " --force" else ""
            }
            player.performCommand(command)
        }

    }

    override fun getTitle(): Component {
        return mH.getLogMessage("GUI", "PunishReason.title")
    }
}
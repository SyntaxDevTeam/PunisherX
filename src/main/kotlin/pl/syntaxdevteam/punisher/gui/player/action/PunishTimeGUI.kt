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

class PunishTimeGUI(plugin: PunisherX) : BaseGUI(plugin) {

    private class Holder(val target: UUID, val type: String) : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    fun open(player: Player, target: OfflinePlayer, type: String) {
        val holder = Holder(target.uniqueId, type)
        val inventory = Bukkit.createInventory(holder, 27, getTitle())
        holder.inv = inventory
        inventory.fillWithFiller()
        val times = plugin.config.getStringList("gui.punish.times")
        times.forEachIndexed { index, time ->
            inventory.setItem(10 + index, createItem(Material.PAPER, "<yellow>$time</yellow>"))
        }
        player.openInventory(inventory)
    }

    override fun open(player: Player) {}

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val holder = event.view.topInventory.holder as? Holder ?: return
        val player = event.whoClicked as? Player ?: return
        val target = Bukkit.getOfflinePlayer(holder.target)
        val times = plugin.config.getStringList("gui.punish.times")
        val slot = event.rawSlot
        if (slot in 10 until 10 + times.size) {
            val time = times[slot - 10]
            PunishReasonGUI(plugin).open(player, target, holder.type, time)
        }
    }

    override fun getTitle(): Component {
        return mH.getLogMessage("GUI", "PunishTime.title")
    }
}
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
import pl.syntaxdevteam.punisher.gui.player.action.PlayerActionGUI
import java.util.UUID

class ConfirmDeleteGUI(plugin: PunisherX) : BaseGUI(plugin) {

    private class Holder(val target: UUID) : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    fun open(player: Player, target: OfflinePlayer) {
        val holder = Holder(target.uniqueId)
        val inventory = Bukkit.createInventory(holder, 27, getTitle())
        holder.inv = inventory

        inventory.fillWithFiller()

        inventory.setItem(11, createItem(Material.GREEN_WOOL, mH.getCleanMessage("GUI", "PlayerAction.confirmDelete.confirm")))
        inventory.setItem(15, createItem(Material.RED_WOOL, mH.getCleanMessage("GUI", "PlayerAction.confirmDelete.cancel")))

        player.openInventory(inventory)
    }

    override fun open(player: Player) {}

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val holder = event.view.topInventory.holder as? Holder ?: return
        val player = event.whoClicked as? Player ?: return
        val target = Bukkit.getOfflinePlayer(holder.target)

        when (event.rawSlot) {
            11 -> {
                player.closeInventory()
                target.player?.kick(mH.getLogMessage("GUI", "PlayerAction.deleteMessage"))
                plugin.databaseHandler.deletePlayerData(target.uniqueId.toString())
                plugin.playerIPManager.deletePlayerInfo(target.uniqueId)
            }
            15 -> {
                player.closeInventory()
                PlayerActionGUI(plugin).open(player, target)
            }
        }
    }

    override fun getTitle(): Component {
        return mH.getLogMessage("GUI", "PlayerAction.confirmDelete.title")
    }
}
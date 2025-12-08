package pl.syntaxdevteam.punisher.gui.report

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

/**
 * First-step selector for /report: choose between Online and Offline (last hour) players.
 */
class ReportSelectorGUI(plugin: PunisherX) : BaseGUI(plugin) {

    private class Holder : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    override fun open(player: Player) {
        val holder = Holder()
        val inv = Bukkit.createInventory(holder, 45, getTitle())
        holder.inv = inv

        inv.fillWithFiller()

        inv.setItem(20, createItem(
            Material.GREEN_DYE,
            mH.stringMessageToStringNoPrefix("GUI", "Report.menu.online")
        ))

        inv.setItem(24, createItem(
            Material.RED_DYE,
            mH.stringMessageToStringNoPrefix("GUI", "Report.menu.offline")
        ))

        inv.setItem(40, createNavItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")))

        player.openInventory(inv)
    }

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val holder = event.view.topInventory.holder as? Holder ?: return
        val player = event.whoClicked as? Player ?: return

        when (event.rawSlot) {
            20 -> ReportPlayerGUI(plugin).open(player)
            24 -> ReportOfflineGUI(plugin).open(player)
            40 -> player.closeInventory()
        }
    }

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "Report.menu.title")
    }
}

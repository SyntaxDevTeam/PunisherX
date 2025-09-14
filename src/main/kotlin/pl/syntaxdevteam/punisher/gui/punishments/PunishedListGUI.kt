package pl.syntaxdevteam.punisher.gui.punishments

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.PunisherMain
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

class PunishedListGUI(plugin: PunisherX) : BaseGUI(plugin) {

    override fun open(player: Player) {
        val inventory = Bukkit.createInventory(null, 45, getTitle())
        inventory.fillWithFiller()
        inventory.setItem(20, createItem(Material.IRON_SWORD, mH.getCleanMessage("GUI", "PunishedList.banned")))
        inventory.setItem(24, createItem(Material.IRON_BARS, mH.getCleanMessage("GUI", "PunishedList.jailed")))
        inventory.setItem(40, createNavItem(Material.BARRIER, mH.getCleanMessage("GUI", "Nav.back")))
        player.openInventory(inventory)
    }

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        when (event.rawSlot) {
            20 -> {
                BanListGUI(plugin).open(player)
            }
            24 -> {
                JailListGUI(plugin).open(player)
            }
            40 -> PunisherMain(plugin).open(player)
        }
    }

    override fun getTitle(): Component {
        return mH.getLogMessage("GUI", "PunishedList.title")
    }
}
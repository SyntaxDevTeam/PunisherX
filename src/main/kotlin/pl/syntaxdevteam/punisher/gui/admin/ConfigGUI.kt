package pl.syntaxdevteam.punisher.gui.admin

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.PunisherMain
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

class ConfigGUI(plugin: PunisherX) : BaseGUI(plugin) {

    override fun open(player: Player) {
        val inventory = Bukkit.createInventory(null, 45, getTitle())

        inventory.fillWithFiller()

        inventory.setItem(20, createItem(Material.COMPASS, mH.stringMessageToStringNoPrefix("GUI", "Config.setunjail")))
        inventory.setItem(
            24,
            createItem(
                plugin.versionCompatibility.resolveMaterial("IRON_CHAIN", "CHAIN", "IRON_BARS"),
                mH.stringMessageToStringNoPrefix("GUI", "Config.setjail")
            )
        )
        inventory.setItem(40, createNavItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")))
        player.openInventory(inventory)
    }

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        when (event.rawSlot) {
            20 -> {
                player.closeInventory()
                player.performCommand("setunjail")
            }
            24 -> {
                player.closeInventory()
                player.performCommand("setjail 5")
            }
            40 -> PunisherMain(plugin).open(player)
        }
    }

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "Config.title")
    }
}
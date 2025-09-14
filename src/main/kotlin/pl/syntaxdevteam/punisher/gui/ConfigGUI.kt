package pl.syntaxdevteam.punisher.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import pl.syntaxdevteam.punisher.PunisherX

class ConfigGUI(private val plugin: PunisherX) : GUI {

    private val mH = plugin.messageHandler

    override fun open(player: Player) {
        val inventory = Bukkit.createInventory(null, 27, getTitle())
        for (slot in 0 until 27) {
            inventory.setItem(slot, createFillerItem())
        }
        inventory.setItem(11, createItem(Material.COMPASS, mH.getCleanMessage("GUI", "Config.setspawn")))
        inventory.setItem(15, createItem(Material.CHAIN, mH.getCleanMessage("GUI", "Config.setjail")))
        player.openInventory(inventory)
    }

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        when (event.rawSlot) {
            11 -> {
                player.closeInventory()
                player.performCommand("setspawn")
            }
            15 -> {
                player.closeInventory()
                player.performCommand("setjail 5")
            }
        }
    }

    override fun getTitle(): Component {
        return mH.getLogMessage("GUI", "Config.title")
    }

    private fun createItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(mH.formatMixedTextToMiniMessage(name, TagResolver.empty()))
        item.itemMeta = meta
        return item
    }

    private fun createFillerItem(): ItemStack {
        val item = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val meta = item.itemMeta
        meta.displayName(Component.text(" "))
        item.itemMeta = meta
        return item
    }
}
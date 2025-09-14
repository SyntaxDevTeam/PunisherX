package pl.syntaxdevteam.punisher.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import pl.syntaxdevteam.punisher.PunisherX

class PunishedListGUI(private val plugin: PunisherX) : GUI {

    private val mH = plugin.messageHandler

    override fun open(player: Player) {
        val inventory = Bukkit.createInventory(null, 45, getTitle())
        for (slot in 0 until 45) {
            inventory.setItem(slot, createFillerItem())
        }
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

    private fun createNavItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(mH.formatMixedTextToMiniMessage(name, TagResolver.empty()))
        item.itemMeta = meta
        return item
    }
}
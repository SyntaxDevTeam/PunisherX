package pl.syntaxdevteam.punisher.gui.interfaces

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import pl.syntaxdevteam.punisher.PunisherX

abstract class BaseGUI(protected val plugin: PunisherX) : GUI {
    protected val mH = plugin.messageHandler

    protected fun createNavItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(mH.formatMixedTextToMiniMessage(name, TagResolver.empty()))
        item.itemMeta = meta
        return item
    }

    protected fun createItem(
        material: Material,
        name: String,
        lore: List<String> = emptyList()
    ): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(mH.formatMixedTextToMiniMessage(name, TagResolver.empty()))
        if (lore.isNotEmpty()) {
            meta.lore(lore.map { mH.formatMixedTextToMiniMessage(it, TagResolver.empty()) })
        }
        item.itemMeta = meta
        return item
    }

    protected fun createFillerItem(): ItemStack {
        val item = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val meta = item.itemMeta
        meta.displayName(Component.text(" "))
        item.itemMeta = meta
        return item
    }

    protected fun Inventory.fillWithFiller() {
        for (slot in 0 until size) {
            setItem(slot, createFillerItem())
        }
    }
}
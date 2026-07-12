package pl.syntaxdevteam.punisher.gui.interfaces
import pl.syntaxdevteam.punisher.compatibility.*

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import pl.syntaxdevteam.punisher.PunisherX

abstract class BaseGUI(protected val plugin: PunisherX) : GUI {
    protected val mH = plugin.messageHandler

    protected fun createGui(rows: Int, title: Component = getTitle()): Gui {
        return Gui.gui()
            .title(title)
            .rows(rows)
            .disableAllInteractions()
            .create()
            .also { gui -> gui.filler.fill(createGuiItem(createFillerItem())) }
    }

    protected fun createGuiItem(
        material: Material,
        name: String,
        lore: List<String> = emptyList(),
        onClick: ((Player) -> Unit)? = null
    ): GuiItem {
        return createGuiItem(createItem(material, name, lore), onClick)
    }

    protected fun createNavGuiItem(
        material: Material,
        name: String,
        onClick: ((Player) -> Unit)? = null
    ): GuiItem {
        return createGuiItem(createNavItem(material, name), onClick)
    }

    protected fun createGuiItem(
        item: ItemStack,
        onClick: ((Player) -> Unit)? = null
    ): GuiItem {
        return ItemBuilder.from(item).asGuiItem { event ->
            event.isCancelled = true
            val player = event.whoClicked as? Player ?: return@asGuiItem
            onClick?.invoke(player)
        }
    }

    protected fun createNavItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item
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
        val meta = item.itemMeta ?: return item
        meta.displayName(mH.formatMixedTextToMiniMessage(name, TagResolver.empty()))
        if (lore.isNotEmpty()) {
            meta.lore(lore.map { mH.formatMixedTextToMiniMessage(it, TagResolver.empty()) })
        }
        item.itemMeta = meta
        return item
    }

    protected fun createFillerItem(): ItemStack {
        val item = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val meta = item.itemMeta ?: return item
        meta.displayName(Component.text(" "))
        item.itemMeta = meta
        return item
    }

}

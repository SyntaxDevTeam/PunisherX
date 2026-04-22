package pl.syntaxdevteam.punisher.gui.paper

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * Reusable navigation controls matching the common PunisherX screen layout.
 */
object NavigationComponents {
    const val PREVIOUS_SLOT = 36
    const val BACK_SLOT = 40
    const val NEXT_SLOT = 44
    const val REDRAW_SLOT = 42

    fun previous(label: Component): ItemStack = navItem(Material.PAPER, label)

    fun back(label: Component): ItemStack = navItem(Material.BARRIER, label)

    fun next(label: Component): ItemStack = navItem(Material.BOOK, label)

    fun redraw(label: Component): ItemStack = navItem(Material.SUNFLOWER, label)

    fun place(
        inventory: Inventory,
        previous: ItemStack? = null,
        back: ItemStack,
        next: ItemStack? = null,
        redraw: ItemStack? = null
    ) {
        if (previous != null) inventory.setItem(PREVIOUS_SLOT, previous)
        inventory.setItem(BACK_SLOT, back)
        if (next != null) inventory.setItem(NEXT_SLOT, next)
        if (redraw != null) inventory.setItem(REDRAW_SLOT, redraw)
    }

    private fun navItem(material: Material, label: Component): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(label)
        item.itemMeta = meta
        return item
    }
}

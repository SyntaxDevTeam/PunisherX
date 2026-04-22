package pl.syntaxdevteam.punisher.gui.paper

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory

/**
 * Contract for Paper inventory screens managed by [PaperGuiAdapter].
 */
interface PaperScreen {
    val id: String

    fun title(viewer: Player): Component

    fun size(viewer: Player): Int = 45

    fun render(viewer: Player, inventory: Inventory)

    fun handleClick(event: InventoryClickEvent, adapter: PaperGuiAdapter) {
        event.isCancelled = true
    }
}

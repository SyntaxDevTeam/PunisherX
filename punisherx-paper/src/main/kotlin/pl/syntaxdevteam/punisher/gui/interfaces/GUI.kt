package pl.syntaxdevteam.punisher.gui.interfaces

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

interface GUI {
    fun open(player: Player)
    fun handleClick(event: InventoryClickEvent)
    fun getTitle(): Component
}
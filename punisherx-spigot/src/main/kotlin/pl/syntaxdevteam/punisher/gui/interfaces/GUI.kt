package pl.syntaxdevteam.punisher.gui.interfaces

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

interface GUI {
    fun open(player: Player)
    fun getTitle(): Component
}

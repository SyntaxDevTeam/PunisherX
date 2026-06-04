package pl.syntaxdevteam.punisher.gui

import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX

/**
 * Fasada zachowująca dotychczasowe API, ale renderująca GUI przez syntaxgui-api.
 */
class PunisherMain(private val plugin: PunisherX) {
    fun open(player: Player) {
        plugin.punisherMainGuiController.open(player)
    }
}

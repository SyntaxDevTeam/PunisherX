package pl.syntaxdevteam.punisher.gui.punishments

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.PunishmentData
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

class PunishmentDetailsGUI(plugin: PunisherX) : BaseGUI(plugin) {
    fun open(player: Player, punishment: PunishmentData, back: () -> Unit) {
        val gui = createGui(3, mH.miniMessageFormat("<dark_gray>${punishment.type} #${punishment.id}</dark_gray>"))
        gui.setItem(13, createGuiItem(Material.PAPER, "<yellow>${punishment.name}</yellow>", listOf(
            "<gray>Reason: <white>${punishment.reason}</white></gray>",
            "<gray>Operator: <white>${punishment.operator}</white></gray>",
            "<gray>Server: <white>${punishment.server}</white></gray>"
        )))
        gui.setItem(11, createGuiItem(Material.LIME_WOOL, "<green>Remove this punishment</green>") { clicker ->
            clicker.closeInventory()
            plugin.guiPunishmentService.revoke(clicker, punishment, back)
        })
        gui.setItem(15, createNavGuiItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")) { back() })
        gui.open(player)
    }

    override fun open(player: Player) = Unit
    override fun getTitle(): Component = Component.text("Punishment")
}

package pl.syntaxdevteam.punisher.gui.punishments

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.PunishmentData
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import java.util.concurrent.atomic.AtomicBoolean

class ConfirmUndoGUI(plugin: PunisherX) : BaseGUI(plugin) {
    fun open(player: Player, punishment: PunishmentData, back: () -> Unit) {
        val gui = createGui(3)
        val used = AtomicBoolean()
        gui.setItem(13, createGuiItem(Material.PAPER, "<yellow>${pl.syntaxdevteam.punisher.permissions.IpVisibility.displayField(plugin, player, punishment, punishment.name)}: ${punishment.type} #${punishment.id}</yellow>",
            listOf("<gray>${punishment.reason}</gray>")))
        gui.setItem(11, createGuiItem(Material.LIME_WOOL,
            mH.stringMessageToStringNoPrefix("GUI", "UndoConfirmation.confirm")) { clicker ->
            if (used.compareAndSet(false, true)) {
                clicker.closeInventory()
                plugin.guiPunishmentService.revoke(clicker, punishment, back)
            }
        })
        gui.setItem(15, createGuiItem(Material.RED_WOOL,
            mH.stringMessageToStringNoPrefix("GUI", "UndoConfirmation.cancel")) { clicker ->
            if (used.compareAndSet(false, true)) PunishmentDetailsGUI(plugin).open(clicker, punishment, back)
        })
        openGui(gui, player)
    }

    override fun open(player: Player) = Unit
    override fun getTitle(): Component = mH.stringMessageToComponentNoPrefix("GUI", "UndoConfirmation.title")
}

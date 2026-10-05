package pl.syntaxdevteam.punisher.gui.player.action

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.PunishmentData
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import pl.syntaxdevteam.punisher.gui.punishments.PunishmentDetailsGUI
import java.text.SimpleDateFormat
import java.util.Date

class PlayerPunishmentsGUI(plugin: PunisherX) : BaseGUI(plugin) {
    fun open(player: Player, target: OfflinePlayer, history: Boolean, page: Int = 0) {
        player.sendActionBar(mH.miniMessageFormat("<gray>Loading punishments...</gray>"))
        plugin.schedulerAdapter.runAsync(Runnable {
            val offset = page * 45
            val rows = if (history) plugin.databaseHandler.getPunishmentHistory(target.uniqueId.toString(), 46, offset)
            else plugin.databaseHandler.getPunishments(target.uniqueId.toString(), 46, offset)
            plugin.schedulerAdapter.runRegionally(player.location, Runnable {
                if (player.isOnline) show(player, target, history, page, rows.take(45), rows.size > 45)
            })
        })
    }

    private fun show(player: Player, target: OfflinePlayer, history: Boolean, page: Int, rows: List<PunishmentData>, hasNext: Boolean) {
        val gui = createGui(6, mH.miniMessageFormat(if (history) "<dark_gray>Punishment history</dark_gray>" else "<dark_gray>Active punishments</dark_gray>"))
        rows.forEachIndexed { index, punishment ->
            val lore = listOf(
                "<gray>Player: <white>${punishment.name}</white></gray>",
                "<gray>Reason: <white>${punishment.reason}</white></gray>",
                "<gray>Operator: <white>${punishment.operator}</white></gray>",
                "<gray>Date: <white>${SimpleDateFormat("yy-MM-dd HH:mm:ss").format(Date(punishment.start))}</white></gray>"
            )
            gui.setItem(index, createGuiItem(Material.PAPER, "<yellow>${punishment.type} #${punishment.id}</yellow>", lore) { clicker ->
                if (!history) PunishmentDetailsGUI(plugin).open(clicker, punishment) { open(clicker, target, false, page) }
            })
        }
        if (page > 0) gui.setItem(45, createNavGuiItem(Material.ARROW, mH.stringMessageToStringNoPrefix("GUI", "Nav.previous")) { open(it, target, history, page - 1) })
        gui.setItem(49, createNavGuiItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")) { PlayerActionGUI(plugin).open(it, target) })
        if (hasNext) gui.setItem(53, createNavGuiItem(Material.ARROW, mH.stringMessageToStringNoPrefix("GUI", "Nav.next")) { open(it, target, history, page + 1) })
        gui.open(player)
    }

    override fun open(player: Player) = Unit
    override fun getTitle(): Component = Component.text("Punishments")
}

package pl.syntaxdevteam.punisher.gui.report

import pl.syntaxdevteam.punisher.compatibility.*

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.commands.ReportsCommand
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI

/** Each click goes through the command's current permission and database checks. */
class ReportInboxGUI(plugin: PunisherX) : BaseGUI(plugin) {
    override fun getTitle(): Component = mH.stringMessageToComponentNoPrefix("reports", "gui-title")
    override fun open(player: Player) = open(player, 1)

    fun open(player: Player, page: Int) {
        val reports = plugin.databaseHandler.getReports(46, (page - 1) * 45)
        val gui = createGui(6)
        reports.take(45).forEachIndexed { slot, report ->
            val item = ItemStack(Material.WRITABLE_BOOK)
            val meta = item.itemMeta ?: return@forEachIndexed
            meta.displayName(Component.text("#${report.id} | ${plugin.server.getOfflinePlayer(report.suspect).name ?: report.suspect}"))
            meta.lore(listOf(Component.text(report.reason), Component.text(report.filedAt.toString()),
                mH.stringMessageToComponentNoPrefix("reports", "gui-details")))
            item.itemMeta = meta
            gui.setItem(slot, createGuiItem(item) { staff ->
                staff.closeInventory()
                ReportsCommand(plugin).execute(staff, arrayOf("view", report.id.toString()))
            })
        }
        if (page > 1) gui.setItem(45, createNavGuiItem(Material.ARROW,
            mH.stringMessageToStringNoPrefix("GUI", "Nav.previous")) { staff ->
            ReportsCommand(plugin).execute(staff, arrayOf("gui", (page - 1).toString()))
        })
        gui.setItem(49, createNavGuiItem(Material.BOOK,
            mH.stringMessageToStringNoPrefix("reports", "gui-history")) { staff ->
            staff.closeInventory()
            ReportsCommand(plugin).execute(staff, arrayOf("history"))
        })
        if (reports.size > 45) gui.setItem(53, createNavGuiItem(Material.ARROW,
            mH.stringMessageToStringNoPrefix("GUI", "Nav.next")) { staff ->
            ReportsCommand(plugin).execute(staff, arrayOf("gui", (page + 1).toString()))
        })
        openGui(gui, player)
    }
}

package pl.syntaxdevteam.punisher.gui.punishments

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.PunishmentData
import pl.syntaxdevteam.punisher.gui.punishments.PunishedListGUI
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import java.text.SimpleDateFormat
import java.util.Date

class JailListGUI(plugin: PunisherX) : BaseGUI(plugin) {
    private val dateFormat = SimpleDateFormat("yy-MM-dd HH:mm:ss")

    private class Holder(var page: Int) : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    override fun open(player: Player) {
        open(player, 0)
    }

    private fun open(player: Player, page: Int) {
        val limit = 27
        val offset = page * limit
        val punishments = plugin.databaseHandler.getJailedPlayers(limit, offset)
        val hasNext = plugin.databaseHandler.getJailedPlayers(1, offset + limit).isNotEmpty()

        val holder = Holder(page)
        val inventory = Bukkit.createInventory(holder, 45, getTitle())
        holder.inv = inventory

        inventory.fillWithFiller()

        punishments.forEachIndexed { index, punishment ->
            inventory.setItem(index, createHead(punishment))
        }

        if (page > 0) {
            inventory.setItem(36, createNavItem(Material.PAPER, mH.getCleanMessage("GUI", "Nav.previous")))
        }
        inventory.setItem(40, createNavItem(Material.BARRIER, mH.getCleanMessage("GUI", "Nav.back")))
        if (hasNext) {
            inventory.setItem(44, createNavItem(Material.BOOK, mH.getCleanMessage("GUI", "Nav.next")))
        }

        player.openInventory(inventory)
    }

    private fun createHead(punishment: PunishmentData): ItemStack {
        val head = ItemStack(Material.PLAYER_HEAD)
        val meta = head.itemMeta as SkullMeta
        val offline = Bukkit.getOfflinePlayer(punishment.name)
        meta.owningPlayer = offline
        meta.displayName(mH.formatMixedTextToMiniMessage("<yellow>${punishment.name}</yellow>", TagResolver.empty()))
        val formattedDate = dateFormat.format(Date(punishment.start))
        val remaining = if (punishment.end == -1L) {
            mH.getCleanMessage("GUI", "JailList.permanent")
        } else {
            plugin.timeHandler.formatTime(((punishment.end - System.currentTimeMillis()) / 1000).toString())
        }
        meta.lore(
            listOf(
                mH.getLogMessage("GUI", "JailList.hover.id", mapOf("id" to punishment.id.toString())),
                mH.getLogMessage("GUI", "JailList.hover.date", mapOf("date" to formattedDate)),
                mH.getLogMessage("GUI", "JailList.hover.remaining", mapOf("time" to remaining)),
                mH.getLogMessage("GUI", "JailList.hover.operator", mapOf("operator" to punishment.operator)),
                mH.getLogMessage("GUI", "JailList.hover.reason", mapOf("reason" to punishment.reason))
            )
        )
        head.itemMeta = meta
        return head
    }

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val holder = event.view.topInventory.holder as? Holder ?: return
        val player = event.whoClicked as? Player ?: return
        val limit = 27
        when (event.rawSlot) {
            36 -> if (holder.page > 0) open(player, holder.page - 1)
            40 -> PunishedListGUI(plugin).open(player)
            44 -> if (plugin.databaseHandler.getJailedPlayers(1, (holder.page + 1) * limit).isNotEmpty()) open(player, holder.page + 1)
        }
    }

    override fun getTitle(): Component {
        return mH.getLogMessage("GUI", "JailList.title")
    }
}
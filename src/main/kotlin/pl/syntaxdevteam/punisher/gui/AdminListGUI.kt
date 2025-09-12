package pl.syntaxdevteam.punisher.gui

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
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import pl.syntaxdevteam.punisher.gui.stats.PlayerStatsService

/**
 * GUI displaying currently online administrators.
 */
class AdminListGUI(private val plugin: PunisherX) : GUI {

    private val mH = plugin.messageHandler
    /**
     * Inventory holder used to store the current page of the GUI.
     */
    private class Holder(var page: Int) : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    override fun open(player: Player) {
        open(player, 0)
    }

    /**
     * Opens the admin list GUI for the given page.
     */
    private fun open(player: Player, page: Int) {
        val online = plugin.server.onlinePlayers.filter {
            PermissionChecker.hasPermissionStartingWith(it, "punisherx")
        }
        val playersPerPage = 27 // 3 rows of heads
        val totalPages = if (online.isEmpty()) 1 else (online.size - 1) / playersPerPage + 1
        val currentPage = page.coerceIn(0, totalPages - 1)

        val startIndex = currentPage * playersPerPage
        val playersPage = online.drop(startIndex).take(playersPerPage)

        val holder = Holder(currentPage)
        val inventory = Bukkit.createInventory(holder, 45, getTitle())
        holder.inv = inventory

        playersPage.forEachIndexed { index, target ->
            val head = ItemStack(Material.PLAYER_HEAD)
            val meta = head.itemMeta as SkullMeta
            meta.owningPlayer = target
            meta.displayName(
                mH.formatMixedTextToMiniMessage("<yellow>${target.name}</yellow>", TagResolver.empty())
            )
            val loadMsg = mH.getCleanMessage("GUI", "PlayerList.loading")
            meta.lore(
                listOf(
                    mH.getLogMessage("GUI", "PlayerList.hover.uuid", mapOf("uuid" to loadMsg)),
                    mH.getLogMessage("GUI", "PlayerList.hover.playerIP", mapOf("playerip" to loadMsg)),
                    mH.getLogMessage("GUI", "PlayerList.hover.onlineStr", mapOf("onlinestr" to loadMsg)),
                    mH.getLogMessage("GUI", "PlayerList.hover.totalStr", mapOf("totalstr" to loadMsg)),
                    mH.getLogMessage("GUI", "PlayerList.hover.lastActive", mapOf("lastactive" to loadMsg)),
                    mH.getLogMessage("GUI", "PlayerList.hover.punishments", mapOf("punishments" to loadMsg)),
                    mH.getLogMessage("GUI", "PlayerList.hover.punishStr", mapOf("punishstr" to loadMsg)),
                )
            )
            head.itemMeta = meta
            inventory.setItem(index, head)

            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                val uuid = target.uniqueId
                val onlineStr  = PlayerStatsService.getCurrentOnlineString(uuid) ?: "Brak danych"
                val totalStr   = PlayerStatsService.getTotalPlaytimeString(uuid) ?: "Brak danych"
                val punishStr  = plugin.databaseHandler.countPlayerAllPunishmentHistory(uuid).toString()
                val playerIP   = plugin.playerIPManager.getPlayerIPByName(target.name) ?: "Brak danych"
                val punishments = plugin.databaseHandler.getActivePunishmentsString(uuid) ?: mH.getCleanMessage("error", "no_data")
                val lastActive = PlayerStatsService.getLastActiveString(uuid) ?: mH.getCleanMessage("error", "no_data")

                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (!holder.inv.viewers.contains(player)) return@Runnable
                    val item = inventory.getItem(index) ?: return@Runnable
                    val im = item.itemMeta as SkullMeta
                    im.lore(
                        listOf(
                            mH.getLogMessage("GUI", "PlayerList.hover.uuid", mapOf("uuid" to target.uniqueId.toString())),
                            mH.getLogMessage("GUI", "PlayerList.hover.playerIP", mapOf("playerip" to playerIP)),
                            mH.getLogMessage("GUI", "PlayerList.hover.onlineStr", mapOf("onlinestr" to onlineStr)),
                            mH.getLogMessage("GUI", "PlayerList.hover.totalStr", mapOf("totalstr" to totalStr)),
                            mH.getLogMessage("GUI", "PlayerList.hover.lastActive", mapOf("lastactive" to lastActive)),
                            mH.getLogMessage("GUI", "PlayerList.hover.punishments", mapOf("punishments" to punishments)),
                            mH.getLogMessage("GUI", "PlayerList.hover.punishStr", mapOf("punishstr" to punishStr)),
                        )
                    )
                    item.itemMeta = im
                    inventory.setItem(index, item)
                })
            })
            for (slot in 27 until 36) {
                inventory.setItem(slot, createFillerItem())
            }
        }

        if (currentPage > 0) {
            inventory.setItem(36, createNavItem(Material.PAPER, "<yellow>Poprzednia strona</yellow>"))
        }

        inventory.setItem(40, createNavItem(Material.BARRIER, "<yellow>Powrót</yellow>"))

        if (currentPage < totalPages - 1) {
            inventory.setItem(44, createNavItem(Material.BOOK, "<yellow>Następna strona</yellow>"))
        }

        player.openInventory(inventory)
    }

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val holder = event.view.topInventory.holder as? Holder ?: return
        val player = event.whoClicked as? Player ?: return

        val online = plugin.server.onlinePlayers.filter {
            PermissionChecker.hasPermissionStartingWith(it, "punisherx")
        }
        val playersPerPage = 27
        val totalPages = if (online.isEmpty()) 1 else (online.size - 1) / playersPerPage + 1
        val slot = event.rawSlot

        when (slot) {
            36 -> if (holder.page > 0) open(player, holder.page - 1)
            40 -> PunisherMain(plugin).open(player)
            44 -> if (holder.page < totalPages - 1) open(player, holder.page + 1)
        }
    }

    override fun getTitle(): Component {
        return mH.getLogMessage("GUI", "PunisherMain.adminOnline.title")
    }
    private fun createNavItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(mH.formatMixedTextToMiniMessage(name, TagResolver.empty()))
        item.itemMeta = meta
        return item
    }

    private fun createFillerItem(): ItemStack {
        val item = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val meta = item.itemMeta
        meta.displayName(Component.text(" "))
        item.itemMeta = meta
        return item
    }
}
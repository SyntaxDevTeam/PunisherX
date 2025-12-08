package pl.syntaxdevteam.punisher.gui.player

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
import pl.syntaxdevteam.punisher.gui.PunisherMain
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import pl.syntaxdevteam.punisher.gui.player.action.PlayerActionGUI
import pl.syntaxdevteam.punisher.gui.stats.PlayerStatsService

/**
 * GUI displaying currently online players.
 */
class PlayerListGUI(plugin: PunisherX) : BaseGUI(plugin) {
    /**
     * Inventory holder used to store the current page of the GUI.
     */
    private class Holder(var page: Int, val players: List<Player>) : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    override fun open(player: Player) {
        val online = ArrayList(plugin.server.onlinePlayers)
        online.sortBy { it.name.lowercase() }
        open(player, 0, online)
    }

    /**
     * Opens the player list GUI for the given page.
     */
    private fun open(player: Player, page: Int, online: List<Player>) {
        val playersPerPage = 27
        val totalPages = if (online.isEmpty()) 1 else (online.size - 1) / playersPerPage + 1
        val currentPage = page.coerceIn(0, totalPages - 1)

        val startIndex = currentPage * playersPerPage
        val playersPage = online.drop(startIndex).take(playersPerPage)

        val holder = Holder(currentPage, online)
        val inventory = Bukkit.createInventory(holder, 45, getTitle())
        holder.inv = inventory

        inventory.fillWithFiller()

        playersPage.forEachIndexed { index, target ->
            val head = ItemStack(Material.PLAYER_HEAD)
            val meta = head.itemMeta as SkullMeta
            meta.owningPlayer = target
            meta.displayName(mH.formatMixedTextToMiniMessage("<yellow>${target.name}</yellow>", TagResolver.empty()))
            val loading = mH.stringMessageToStringNoPrefix("GUI", "PlayerList.loading")
            meta.lore(
                listOf(
                    mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.uuid", mapOf("uuid" to loading)),
                    mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.playerIP", mapOf("playerip" to loading)),
                    mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.onlineStr", mapOf("onlinestr" to loading)),
                    mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.totalStr", mapOf("totalstr" to loading)),
                    mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.lastActive", mapOf("lastactive" to loading)),
                    mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.punishments", mapOf("punishments" to loading)),
                    mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.punishStr", mapOf("punishstr" to loading))
                )
            )
            head.itemMeta = meta
            inventory.setItem(index, head)

            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                val uuid = target.uniqueId
                val onlineStr = PlayerStatsService.getCurrentOnlineString(uuid) ?: mH.stringMessageToStringNoPrefix("error", "no_data")
                val totalStr = PlayerStatsService.getTotalPlaytimeString(uuid) ?: mH.stringMessageToStringNoPrefix("error", "no_data")
                val punishStr = plugin.databaseHandler.countPlayerAllPunishmentHistory(uuid).toString()
                val playerIP = plugin.playerIPManager.getPlayerIPByName(target.name) ?: mH.stringMessageToStringNoPrefix("error", "no_data")
                val punishments = plugin.databaseHandler.getActivePunishmentsString(uuid) ?: mH.stringMessageToStringNoPrefix("error", "no_data")
                val lastActive = PlayerStatsService.getLastActiveString(uuid) ?: mH.stringMessageToStringNoPrefix("error", "no_data")

                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (!holder.inv.viewers.contains(player)) return@Runnable
                    val item = inventory.getItem(index) ?: return@Runnable
                    val im = item.itemMeta as SkullMeta
                    im.lore(
                        listOf(
                            mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.uuid", mapOf("uuid" to target.uniqueId.toString())),
                            mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.playerIP", mapOf("playerip" to playerIP)),
                            mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.onlineStr", mapOf("onlinestr" to onlineStr)),
                            mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.totalStr", mapOf("totalstr" to totalStr)),
                            mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.lastActive", mapOf("lastactive" to lastActive)),
                            mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.punishments", mapOf("punishments" to punishments)),
                            mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.punishStr", mapOf("punishstr" to punishStr))
                        )
                    )
                    item.itemMeta = im
                    inventory.setItem(index, item)
                })
            })
        }

        if (currentPage > 0)
            inventory.setItem(36, createNavItem(Material.PAPER, mH.stringMessageToStringNoPrefix("GUI", "Nav.previous")))

        inventory.setItem(40, createNavItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")))

        if (currentPage < totalPages - 1)
            inventory.setItem(44, createNavItem(Material.BOOK, mH.stringMessageToStringNoPrefix("GUI", "Nav.next")))

        player.openInventory(inventory)
    }

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val holder = event.view.topInventory.holder as? Holder ?: return
        val player = event.whoClicked as? Player ?: return

        val players = holder.players
        val playersPerPage = 27
        val slot = event.rawSlot

        if (slot in 0 until playersPerPage) {
            val index = holder.page * playersPerPage + slot
            if (index < players.size) PlayerActionGUI(plugin).open(player, players[index])
            return
        }

        when (slot) {
            36 -> if (holder.page > 0) open(player, holder.page - 1, players)
            40 -> PunisherMain(plugin).open(player)
            44 -> {
                val totalPages = if (players.isEmpty()) 1 else (players.size - 1) / playersPerPage + 1
                if (holder.page < totalPages - 1) open(player, holder.page + 1, players)
            }
        }
    }

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "PunisherMain.playerOnline.title")
    }
}
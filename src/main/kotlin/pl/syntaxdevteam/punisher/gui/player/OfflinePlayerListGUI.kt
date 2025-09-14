package pl.syntaxdevteam.punisher.gui.player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import pl.syntaxdevteam.punisher.gui.stats.PlayerStatsService
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import com.destroystokyo.paper.profile.PlayerProfile
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.PunisherMain
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import pl.syntaxdevteam.punisher.gui.player.action.PlayerActionGUI
import java.util.UUID

class OfflinePlayerListGUI(plugin: PunisherX) : BaseGUI(plugin) {

    private enum class SortMode {
        NAME_ASC, NAME_DESC, LAST_SEEN_DESC, LAST_SEEN_ASC;
        fun next(): SortMode = when (this) {
            NAME_ASC -> NAME_DESC
            NAME_DESC -> LAST_SEEN_DESC
            LAST_SEEN_DESC -> LAST_SEEN_ASC
            LAST_SEEN_ASC -> NAME_ASC
        }
    }

    private class Holder(var page: Int, var sort: SortMode) : InventoryHolder {
        lateinit var inv: Inventory
        override fun getInventory(): Inventory = inv
    }

    override fun open(player: Player) {
        open(player, 0, SortMode.NAME_ASC)
    }

    private fun open(player: Player, page: Int, sort: SortMode) {
        val records = plugin.playerIPManager.getAllDecryptedRecords()
        val players = records.mapNotNull { info ->
            val uuid = UUID.fromString(info.playerUUID)
            if (Bukkit.getPlayer(uuid) != null) null else info
        }

        val sorted = when (sort) {
            SortMode.NAME_ASC -> players.sortedBy { it.playerName.lowercase() }
            SortMode.NAME_DESC -> players.sortedByDescending { it.playerName.lowercase() }
            SortMode.LAST_SEEN_DESC -> players.sortedByDescending { plugin.timeHandler.parseDate(it.lastUpdated) ?: 0L }
            SortMode.LAST_SEEN_ASC -> players.sortedBy { plugin.timeHandler.parseDate(it.lastUpdated) ?: Long.MAX_VALUE }
        }

        val playersPerPage = 27
        val totalPages = if (sorted.isEmpty()) 1 else (sorted.size - 1) / playersPerPage + 1
        val currentPage = page.coerceIn(0, totalPages - 1)
        val startIndex = currentPage * playersPerPage
        val pageList = sorted.drop(startIndex).take(playersPerPage)

        val holder = Holder(currentPage, sort)
        val inventory = Bukkit.createInventory(holder, 45, getTitle())
        holder.inv = inventory

        inventory.fillWithFiller()

        pageList.forEachIndexed { index, info ->
            val uuid = UUID.fromString(info.playerUUID)
            val head = ItemStack(Material.PLAYER_HEAD)
            val meta = head.itemMeta as SkullMeta
            val profile: PlayerProfile = Bukkit.createProfile(uuid, info.playerName)
            meta.playerProfile = profile
            meta.displayName(mH.formatMixedTextToMiniMessage("<yellow>${info.playerName}</yellow>", TagResolver.empty()))
            val loadMsg = mH.getCleanMessage("GUI", "OfflineList.loading")
            meta.lore(
                listOf(
                    mH.getLogMessage("GUI", "OfflineList.hover.uuid", mapOf("uuid" to loadMsg)),
                    mH.getLogMessage("GUI", "OfflineList.hover.ip", mapOf("ip" to loadMsg)),
                    mH.getLogMessage("GUI", "OfflineList.hover.geo", mapOf("geo" to loadMsg)),
                    mH.getLogMessage("GUI", "OfflineList.hover.lastSeen", mapOf("lastseen" to loadMsg)),
                    mH.getLogMessage("GUI", "OfflineList.hover.lastLocation", mapOf("lastlocation" to loadMsg)),
                    mH.getLogMessage("GUI", "OfflineList.hover.logout", mapOf("logout" to loadMsg)),
                    mH.getLogMessage("GUI", "OfflineList.hover.offlineTime", mapOf("offlinetime" to loadMsg)),
                )
            )
            head.itemMeta = meta
            inventory.setItem(index, head)

            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                val offlineTime = plugin.timeHandler.getOfflineDuration(info.lastUpdated)
                val lastSeenDate = info.lastUpdated
                val ipHistory = records
                    .filter { it.playerUUID == info.playerUUID }
                    .sortedByDescending { plugin.timeHandler.parseDate(it.lastUpdated) ?: 0L }
                    .map { it.playerIP }
                    .distinct()
                    .take(3)
                val ipLine = ipHistory.joinToString(", ")
                val geo = info.geoLocation
                val lastLocation = PlayerStatsService.getLastLocationString(uuid)
                    ?: mH.getCleanMessage("error", "no_data")
                val punishments = plugin.databaseHandler
                    .getPunishmentHistory(info.playerUUID, limit = 3, offset = 0)
                val punishmentLines = punishments.map { "${it.type}: ${it.reason}" }
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (!holder.inv.viewers.contains(player)) return@Runnable
                    val item = inventory.getItem(index) ?: return@Runnable
                    val im = item.itemMeta as SkullMeta
                    val loreLines = mutableListOf(
                        mH.getLogMessage("GUI", "OfflineList.hover.uuid", mapOf("uuid" to info.playerUUID)),
                        mH.getLogMessage("GUI", "OfflineList.hover.ip", mapOf("ip" to ipLine)),
                        mH.getLogMessage("GUI", "OfflineList.hover.geo", mapOf("geo" to geo)),
                        mH.getLogMessage("GUI", "OfflineList.hover.lastSeen", mapOf("lastseen" to lastSeenDate)),
                        mH.getLogMessage("GUI", "OfflineList.hover.lastLocation", mapOf("lastlocation" to lastLocation)),
                        mH.getLogMessage("GUI", "OfflineList.hover.logout", mapOf("logout" to lastSeenDate)),
                        mH.getLogMessage("GUI", "OfflineList.hover.offlineTime", mapOf("offlinetime" to offlineTime)),
                    )
                    if (punishmentLines.isEmpty()) {
                        loreLines.add(mH.getLogMessage("GUI", "OfflineList.hover.noPunishments"))
                    } else {
                        punishmentLines.forEach { line ->
                            loreLines.add(
                                mH.getLogMessage(
                                    "GUI",
                                    "OfflineList.hover.punishment",
                                    mapOf("punishment" to line)
                                )
                            )
                        }
                    }
                    im.lore(loreLines)
                    item.itemMeta = im
                    inventory.setItem(index, item)
                })
            })
        }

        if (currentPage > 0) {
            inventory.setItem(36, createNavItem(Material.PAPER, mH.getCleanMessage("GUI", "Nav.previous")))
        }
        inventory.setItem(40, createNavItem(Material.BARRIER, mH.getCleanMessage("GUI", "Nav.back")))
        if (currentPage < totalPages - 1) {
            inventory.setItem(44, createNavItem(Material.BOOK, mH.getCleanMessage("GUI", "Nav.next")))
        }

        val sortNameKey = when (sort) {
            SortMode.NAME_ASC -> "nameAsc"
            SortMode.NAME_DESC -> "nameDesc"
            SortMode.LAST_SEEN_DESC -> "lastSeenDesc"
            SortMode.LAST_SEEN_ASC -> "lastSeenAsc"
        }
        inventory.setItem(38, createNavItem(Material.COMPASS, mH.getCleanMessage("GUI", "OfflineList.sort.$sortNameKey")))

        player.openInventory(inventory)
    }

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val holder = event.view.topInventory.holder as? Holder ?: return
        val player = event.whoClicked as? Player ?: return

        val records = plugin.playerIPManager.getAllDecryptedRecords()
        val players = records.mapNotNull { info ->
            val uuid = UUID.fromString(info.playerUUID)
            if (Bukkit.getPlayer(uuid) != null) null else info
        }.let { list ->
            when (holder.sort) {
                SortMode.NAME_ASC -> list.sortedBy { it.playerName.lowercase() }
                SortMode.NAME_DESC -> list.sortedByDescending { it.playerName.lowercase() }
                SortMode.LAST_SEEN_DESC -> list.sortedByDescending { plugin.timeHandler.parseDate(it.lastUpdated) ?: 0L }
                SortMode.LAST_SEEN_ASC -> list.sortedBy { plugin.timeHandler.parseDate(it.lastUpdated) ?: Long.MAX_VALUE }
            }
        }

        val playersPerPage = 27
        val slot = event.rawSlot
        if (slot in 0 until playersPerPage) {
            val index = holder.page * playersPerPage + slot
            if (index < players.size) {
                val info = players[index]
                val off = Bukkit.getOfflinePlayer(UUID.fromString(info.playerUUID))
                PlayerActionGUI(plugin).open(player, off)
            }
            return
        }
        when (slot) {
            36 -> if (holder.page > 0) open(player, holder.page - 1, holder.sort)
            38 -> open(player, 0, holder.sort.next())
            40 -> PunisherMain(plugin).open(player)
            44 -> if (holder.page < (players.size - 1) / playersPerPage) open(player, holder.page + 1, holder.sort)
        }
    }

    override fun getTitle(): Component {
        return mH.getLogMessage("GUI", "PunisherMain.playerOffline.title")
    }
}

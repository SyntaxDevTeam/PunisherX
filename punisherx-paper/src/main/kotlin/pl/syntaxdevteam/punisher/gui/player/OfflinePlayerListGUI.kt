package pl.syntaxdevteam.punisher.gui.player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import pl.syntaxdevteam.punisher.gui.stats.PlayerStatsService
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import com.destroystokyo.paper.profile.PlayerProfile
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.PunisherMain
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import pl.syntaxdevteam.punisher.gui.player.action.PlayerActionGUI
import java.util.UUID
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

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

    override fun open(player: Player) {
        open(player, 0, SortMode.NAME_ASC, "")
    }

    private fun open(player: Player, page: Int, sort: SortMode, query: String) {
        player.sendActionBar(mH.miniMessageFormat("<gray>Loading offline players...</gray>"))
        plugin.schedulerAdapter.runAsync(Runnable {
            val records = plugin.playerIPManager.getAllDecryptedRecords()
            plugin.schedulerAdapter.runForPlayer(player, Runnable {
                if (player.isOnline) show(player, page, sort, query, records)
            })
        })
    }

    private fun show(player: Player, page: Int, sort: SortMode, query: String, records: List<pl.syntaxdevteam.punisher.players.PlayerIPManager.PlayerInfo>) {
        val players = records
            .mapNotNull { info ->
                val uuid = UUID.fromString(info.playerUUID)
                if (Bukkit.getPlayer(uuid) != null) null else info
            }
            .distinctBy { it.playerUUID }
            .filter { it.playerName.contains(query, ignoreCase = true) }

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

        val gui = createGui(5)

        pageList.forEachIndexed { index, info ->
            val uuid = UUID.fromString(info.playerUUID)
            val head = ItemStack(Material.PLAYER_HEAD)
            val meta = head.itemMeta as SkullMeta
            val profile: PlayerProfile = Bukkit.createProfile(uuid, info.playerName)
            meta.playerProfile = profile
            meta.displayName(mH.formatMixedTextToMiniMessage("<yellow>${info.playerName}</yellow>", TagResolver.empty()))
            val loadMsg = mH.stringMessageToStringNoPrefix("GUI", "OfflineList.loading")
            meta.lore(
                listOf(
                    mH.stringMessageToComponentNoPrefix("GUI", "OfflineList.hover.uuid", mapOf("uuid" to loadMsg)),
                        if (PermissionChecker.hasWithLegacy(player, PermissionChecker.PermissionKey.VIEW_IP))
                            mH.stringMessageToComponentNoPrefix("GUI", "OfflineList.hover.ip", mapOf("ip" to loadMsg)) else Component.empty(),
                    if (PermissionChecker.hasWithLegacy(player, PermissionChecker.PermissionKey.VIEW_IP))
                        mH.stringMessageToComponentNoPrefix("GUI", "OfflineList.hover.geo", mapOf("geo" to loadMsg)) else Component.empty(),
                    mH.stringMessageToComponentNoPrefix("GUI", "OfflineList.hover.lastSeen", mapOf("lastseen" to loadMsg)),
                    mH.stringMessageToComponentNoPrefix("GUI", "OfflineList.hover.lastLocation", mapOf("lastlocation" to loadMsg)),
                    mH.stringMessageToComponentNoPrefix("GUI", "OfflineList.hover.logout", mapOf("logout" to loadMsg)),
                    mH.stringMessageToComponentNoPrefix("GUI", "OfflineList.hover.offlineTime", mapOf("offlinetime" to loadMsg))
                )
            )
            head.itemMeta = meta
            gui.setItem(index, createGuiItem(head) { clicker ->
                val off = Bukkit.getOfflinePlayer(UUID.fromString(info.playerUUID))
                PlayerActionGUI(plugin).open(clicker, off)
            })

            plugin.schedulerAdapter.runAsync(Runnable {
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
                    ?: mH.stringMessageToStringNoPrefix("error", "no_data")
                val punishments = plugin.databaseHandler
                    .getPunishmentHistory(info.playerUUID, limit = 3, offset = 0)
                val punishmentLines = punishments.map { "${it.type}: ${it.reason}" }
                plugin.schedulerAdapter.runForPlayer(player, Runnable {
                    if (!gui.inventory.viewers.contains(player)) return@Runnable
                    val item = gui.inventory.getItem(index) ?: return@Runnable
                    val im = item.itemMeta as SkullMeta
                    val loreLines = mutableListOf(
                        mH.stringMessageToComponentNoPrefix("GUI", "OfflineList.hover.uuid", mapOf("uuid" to info.playerUUID)),
                        if (PermissionChecker.hasWithLegacy(player, PermissionChecker.PermissionKey.VIEW_IP))
                            mH.stringMessageToComponentNoPrefix("GUI", "OfflineList.hover.ip", mapOf("ip" to ipLine)) else Component.empty(),
                        if (PermissionChecker.hasWithLegacy(player, PermissionChecker.PermissionKey.VIEW_IP))
                            mH.stringMessageToComponentNoPrefix("GUI", "OfflineList.hover.geo", mapOf("geo" to geo)) else Component.empty(),
                        mH.stringMessageToComponentNoPrefix("GUI", "OfflineList.hover.lastSeen", mapOf("lastseen" to lastSeenDate)),
                        mH.stringMessageToComponentNoPrefix("GUI", "OfflineList.hover.lastLocation", mapOf("lastlocation" to lastLocation)),
                        mH.stringMessageToComponentNoPrefix("GUI", "OfflineList.hover.logout", mapOf("logout" to lastSeenDate)),
                        mH.stringMessageToComponentNoPrefix("GUI", "OfflineList.hover.offlineTime", mapOf("offlinetime" to offlineTime))
                    )
                    if (punishmentLines.isEmpty()) {
                        loreLines.add(mH.stringMessageToComponentNoPrefix("GUI", "OfflineList.hover.noPunishments"))
                    } else {
                        punishmentLines.forEach { line ->
                            loreLines.add(
                                mH.stringMessageToComponentNoPrefix(
                                    "GUI",
                                    "OfflineList.hover.punishment",
                                    mapOf("punishment" to line)
                                )
                            )
                        }
                    }
                    im.lore(loreLines)
                    item.itemMeta = im
                    gui.updateItem(index, item)
                })
            })
        }

        if (currentPage > 0)
            gui.setItem(36, createNavGuiItem(Material.PAPER, mH.stringMessageToStringNoPrefix("GUI", "Nav.previous")) { clicker ->
                open(clicker, currentPage - 1, sort, query)
            })

        gui.setItem(40, createNavGuiItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")) { clicker ->
            PunisherMain(plugin).open(clicker)
        })
        gui.setItem(39, createNavGuiItem(Material.NAME_TAG, "<yellow>Search players</yellow>") { clicker ->
            plugin.guiSearchService.request(clicker, "<yellow>Enter player name in chat, or 'cancel'.</yellow>") { searcher, text ->
                open(searcher, 0, sort, text)
            }
        })

        if (currentPage < totalPages - 1)
            gui.setItem(44, createNavGuiItem(Material.BOOK, mH.stringMessageToStringNoPrefix("GUI", "Nav.next")) { clicker ->
                open(clicker, currentPage + 1, sort, query)
            })

        val sortNameKey = when (sort) {
            SortMode.NAME_ASC -> "nameAsc"
            SortMode.NAME_DESC -> "nameDesc"
            SortMode.LAST_SEEN_DESC -> "lastSeenDesc"
            SortMode.LAST_SEEN_ASC -> "lastSeenAsc"
        }
        gui.setItem(38, createNavGuiItem(Material.COMPASS, mH.stringMessageToStringNoPrefix("GUI", "OfflineList.sort.$sortNameKey")) { clicker ->
            open(clicker, 0, sort.next(), query)
        })

        gui.open(player)
    }

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "PunisherMain.playerOffline.title")
    }
}

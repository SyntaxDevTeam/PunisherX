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
import com.destroystokyo.paper.profile.PlayerProfile
import pl.syntaxdevteam.punisher.PunisherX
import java.text.SimpleDateFormat
import java.util.UUID

class OfflinePlayerListGUI(private val plugin: PunisherX) : GUI {

    private val mH = plugin.messageHandler

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

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    private fun parseDate(date: String): Long? = try {
        dateFormat.parse(date)?.time
    } catch (_: Exception) {
        null
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
            SortMode.LAST_SEEN_DESC -> players.sortedByDescending { parseDate(it.lastUpdated) ?: 0L }
            SortMode.LAST_SEEN_ASC -> players.sortedBy { parseDate(it.lastUpdated) ?: Long.MAX_VALUE }
        }

        val playersPerPage = 27
        val totalPages = if (sorted.isEmpty()) 1 else (sorted.size - 1) / playersPerPage + 1
        val currentPage = page.coerceIn(0, totalPages - 1)
        val startIndex = currentPage * playersPerPage
        val pageList = sorted.drop(startIndex).take(playersPerPage)

        val holder = Holder(currentPage, sort)
        val inventory = Bukkit.createInventory(holder, 45, getTitle())
        holder.inv = inventory

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
                val offlineTime = getOfflineDuration(info.lastUpdated)
                val lastSeenDate = info.lastUpdated
                val ip = info.playerIP
                val geo = info.geoLocation
                val lastLocation = getLastLocation(uuid) ?: mH.getCleanMessage("error", "no_data")
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (!holder.inv.viewers.contains(player)) return@Runnable
                    val item = inventory.getItem(index) ?: return@Runnable
                    val im = item.itemMeta as SkullMeta
                    im.lore(
                        listOf(
                            mH.getLogMessage("GUI", "OfflineList.hover.uuid", mapOf("uuid" to info.playerUUID)),
                            mH.getLogMessage("GUI", "OfflineList.hover.ip", mapOf("ip" to ip)),
                            mH.getLogMessage("GUI", "OfflineList.hover.geo", mapOf("geo" to geo)),
                            mH.getLogMessage("GUI", "OfflineList.hover.lastSeen", mapOf("lastseen" to lastSeenDate)),
                            mH.getLogMessage("GUI", "OfflineList.hover.lastLocation", mapOf("lastlocation" to lastLocation)),
                            mH.getLogMessage("GUI", "OfflineList.hover.logout", mapOf("logout" to lastSeenDate)),
                            mH.getLogMessage("GUI", "OfflineList.hover.offlineTime", mapOf("offlinetime" to offlineTime)),
                        )
                    )
                    item.itemMeta = im
                    inventory.setItem(index, item)
                })
            })
        }

        for (slot in 27 until 36) {
            inventory.setItem(slot, createFillerItem())
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
                SortMode.LAST_SEEN_DESC -> list.sortedByDescending { parseDate(it.lastUpdated) ?: 0L }
                SortMode.LAST_SEEN_ASC -> list.sortedBy { parseDate(it.lastUpdated) ?: Long.MAX_VALUE }
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
            40 -> PunisherMain(plugin).open(player)
            41 -> open(player, 0, holder.sort.next())
            44 -> if (holder.page < (players.size - 1) / playersPerPage) open(player, holder.page + 1, holder.sort)
        }
    }

    override fun getTitle(): Component {
        return mH.getLogMessage("GUI", "PunisherMain.playerOffline.title")
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

    private fun getOfflineDuration(lastUpdated: String): String {
        val ts = parseDate(lastUpdated) ?: return mH.getCleanMessage("error", "no_data")
        var seconds = (System.currentTimeMillis() - ts) / 1000
        val years = seconds / (60 * 60 * 24 * 365)
        seconds %= 60 * 60 * 24 * 365
        val months = seconds / (60 * 60 * 24 * 30)
        seconds %= 60 * 60 * 24 * 30
        val weeks = seconds / (60 * 60 * 24 * 7)
        seconds %= 60 * 60 * 24 * 7
        val days = seconds / (60 * 60 * 24)
        seconds %= 60 * 60 * 24
        val hours = seconds / (60 * 60)
        seconds %= 60 * 60
        val minutes = seconds / 60
        seconds %= 60
        val parts = mutableListOf<String>()
        if (years > 0) parts.add("${years}Y")
        if (months > 0) parts.add("${months}M")
        if (weeks > 0) parts.add("${weeks}W")
        if (days > 0) parts.add("${days}d")
        if (hours > 0) parts.add("${hours}h")
        if (minutes > 0) parts.add("${minutes}m")
        if (seconds > 0 || parts.isEmpty()) parts.add("${seconds}s")
        return parts.joinToString(" ")
    }

    private fun getLastLocation(uuid: UUID): String? {
        return null
    }
}

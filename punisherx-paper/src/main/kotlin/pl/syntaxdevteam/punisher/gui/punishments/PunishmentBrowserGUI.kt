package pl.syntaxdevteam.punisher.gui.punishments

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.PunishmentData
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import java.text.SimpleDateFormat
import java.util.Date

class PunishmentBrowserGUI(plugin: PunisherX) : BaseGUI(plugin) {
    private enum class Filter(val types: Set<String>) {
        ALL(emptySet()), BANS(setOf("BAN", "BANIP")), JAIL(setOf("JAIL")), MUTES(setOf("MUTE")), WARNS(setOf("WARN"));
        fun next(): Filter = entries[(ordinal + 1) % entries.size]
    }

    override fun open(player: Player) = load(player, 0, Filter.ALL, "")

    fun open(player: Player, initialFilter: String, query: String = "") {
        val filter = runCatching { Filter.valueOf(initialFilter.uppercase()) }.getOrDefault(Filter.ALL)
        load(player, 0, filter, query)
    }

    private fun load(player: Player, page: Int, filter: Filter, query: String) {
        player.sendActionBar(mH.miniMessageFormat("<gray>Loading punishments...</gray>"))
        plugin.schedulerAdapter.runAsync(Runnable {
            val rows = plugin.databaseHandler.getActivePunishmentsFiltered(filter.types, query, 28, page * 27)
            plugin.schedulerAdapter.runRegionally(player.location, Runnable {
                if (!player.isOnline) return@Runnable
                show(player, page, filter, query, rows.take(27), rows.size > 27)
            })
        })
    }

    private fun show(player: Player, page: Int, filter: Filter, query: String, rows: List<PunishmentData>, hasNext: Boolean) {
        val gui = createGui(6)
        rows.forEachIndexed { index, punishment ->
            gui.setItem(index, createGuiItem(createHead(punishment)) { clicker ->
                PunishmentDetailsGUI(plugin).open(clicker, punishment) { load(clicker, page, filter, query) }
            })
        }
        if (page > 0) gui.setItem(45, createNavGuiItem(Material.ARROW, mH.stringMessageToStringNoPrefix("GUI", "Nav.previous")) { load(it, page - 1, filter, query) })
        gui.setItem(47, createNavGuiItem(Material.HOPPER, "<yellow>Filter: ${filter.name.lowercase()}</yellow>") { load(it, 0, filter.next(), query) })
        gui.setItem(49, createNavGuiItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")) { PunishedListGUI(plugin).open(it) })
        gui.setItem(51, createNavGuiItem(Material.NAME_TAG, if (query.isBlank()) "<yellow>Search player</yellow>" else "<yellow>Search: $query</yellow>") { clicker ->
            plugin.guiSearchService.request(clicker, "<yellow>Enter player name in chat, or 'cancel'.</yellow>") { searcher, text -> load(searcher, 0, filter, text) }
        })
        if (hasNext) gui.setItem(53, createNavGuiItem(Material.ARROW, mH.stringMessageToStringNoPrefix("GUI", "Nav.next")) { load(it, page + 1, filter, query) })
        gui.open(player)
    }

    private fun createHead(punishment: PunishmentData): ItemStack {
        val head = ItemStack(Material.PLAYER_HEAD)
        val meta = head.itemMeta as SkullMeta
        meta.owningPlayer = Bukkit.getOfflinePlayer(punishment.name)
        meta.displayName(mH.formatMixedTextToMiniMessage("<yellow>${punishment.name}</yellow> <gray>#${punishment.id}</gray>", TagResolver.empty()))
        val end = if (punishment.end == -1L) "permanent" else plugin.timeHandler.formatTime(((punishment.end - System.currentTimeMillis()).coerceAtLeast(0) / 1000).toString())
        meta.lore(listOf(
            mH.miniMessageFormat("<gray>Type: <yellow>${punishment.type}</yellow></gray>"),
            mH.miniMessageFormat("<gray>Reason: <white>${punishment.reason}</white></gray>"),
            mH.miniMessageFormat("<gray>Operator: <white>${punishment.operator}</white></gray>"),
            mH.miniMessageFormat("<gray>Started: <white>${SimpleDateFormat("yy-MM-dd HH:mm:ss").format(Date(punishment.start))}</white></gray>"),
            mH.miniMessageFormat("<gray>Remaining: <white>$end</white></gray>"),
            mH.miniMessageFormat("<green>Click for actions</green>")
        ))
        head.itemMeta = meta
        return head
    }

    override fun getTitle(): Component = mH.stringMessageToComponentNoPrefix("GUI", "PunishedList.title")
}

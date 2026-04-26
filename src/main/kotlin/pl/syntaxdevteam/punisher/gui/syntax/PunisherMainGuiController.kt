package pl.syntaxdevteam.punisher.gui.syntax

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.admin.ConfigGUI
import pl.syntaxdevteam.punisher.gui.player.action.PlayerActionGUI
import pl.syntaxdevteam.punisher.gui.punishments.BanListGUI
import pl.syntaxdevteam.punisher.gui.punishments.JailListGUI
import pl.syntaxdevteam.punisher.gui.stats.PlayerStatsService
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import pl.syntaxdevteam.syntaxguiapi.components.NavigationComponents
import pl.syntaxdevteam.syntaxguiapi.core.GuiAction
import pl.syntaxdevteam.syntaxguiapi.core.GuiBase
import pl.syntaxdevteam.syntaxguiapi.core.GuiIcon
import pl.syntaxdevteam.syntaxguiapi.core.GuiLayout
import pl.syntaxdevteam.syntaxguiapi.core.GuiMaterial
import pl.syntaxdevteam.syntaxguiapi.paper.PaperGuiListener
import pl.syntaxdevteam.syntaxguiapi.paper.PaperGuiRenderer
import java.lang.management.ManagementFactory
import java.util.UUID

class PunisherMainGuiController(private val plugin: PunisherX) {

    private val renderer = PaperGuiRenderer()

    val listener = PaperGuiListener { player, holder, action ->
        val gui = holder.gui
        when (action) {
            is GuiAction.SelectElement -> (gui as? SelectableScreen)?.handleSelection(action.globalIndex, player)
            GuiAction.Back -> (gui as? BackNavigableScreen)?.onBack(player)
            else -> Unit
        }
    }

    fun open(player: Player) = renderer.open(player, MainScreen(plugin, this))
    fun openPlayerList(player: Player, page: Int = 0) = renderer.open(player, OnlinePlayersScreen(plugin, this, page))
    fun openOfflinePlayerList(player: Player, page: Int = 0, sortMode: SortMode = SortMode.NAME_ASC) =
        renderer.open(player, OfflinePlayersScreen(plugin, this, page, sortMode))
    fun openAdminList(player: Player, page: Int = 0) = renderer.open(player, AdminPlayersScreen(plugin, this, page))
    fun openPunishedMenu(player: Player) = renderer.open(player, PunishedMenuScreen(plugin, this))

    private interface SelectableScreen { fun handleSelection(index: Int, player: Player) }
    private interface BackNavigableScreen { fun onBack(player: Player) }

    private class MainScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController) :
        GuiBase(plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "PunisherMain.title")), 45),
        SelectableScreen {

        private val entries = listOf(
            MenuEntry(4, GuiMaterial.PAPER, message("PunisherMain.serwerInfo.title"), { listOf(serverNameLine(), dailyLine(), tpsLine()) }) { _, _ -> },
            MenuEntry(10, GuiMaterial.PLAYER_HEAD, message("PunisherMain.playerOnline.title"), { listOf(playerOnlineLine(), message("PunisherMain.playerOnline.clickToView")) }) { _, p -> controller.openPlayerList(p) },
            MenuEntry(16, GuiMaterial.BOOK, message("PunisherMain.playerOffline.title"), { listOf(playerOfflineLine(), message("PunisherMain.playerOffline.clickToView")) }) { _, p -> controller.openOfflinePlayerList(p) },
            MenuEntry(22, GuiMaterial.TRIAL_KEY, message("PunisherMain.adminOnline.title")) { _, p -> controller.openAdminList(p) },
            MenuEntry(29, GuiMaterial.MACE, message("PlayerAction.list")) { _, p -> controller.openPunishedMenu(p) },
            MenuEntry(33, GuiMaterial.BARRIER, message("PlayerAction.config")) { _, p -> ConfigGUI(plugin).open(p) },
        )

        override fun buildLayout(page: Int): GuiLayout {
            val slots = filledSlots(size)
            entries.forEach { slots[it.slot] = GuiIcon(it.key, it.title, it.loreProvider(), it.material) }
            return GuiLayout(title, size, slots)
        }

        override fun onClick(slot: Int): GuiAction {
            val idx = entries.indexOfFirst { it.slot == slot }
            return if (idx == -1) GuiAction.None else GuiAction.SelectElement(idx)
        }

        override fun handleSelection(index: Int, player: Player) { entries.getOrNull(index)?.onClick?.invoke(index, player) }

        private fun message(key: String, vars: Map<String, String> = emptyMap()): String = plain(
            plugin.messageHandler.formatMixedTextToMiniMessage(plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key, vars)),
        )
        private fun serverNameLine() = message("PunisherMain.serwerInfo.serverName", mapOf("servername" to plugin.getServerName()))
        private fun dailyLine() = message("PunisherMain.serwerInfo.daily", mapOf("daily" to plugin.databaseHandler.countTodayPunishments().toString()))
        private fun playerOnlineLine() = message("PunisherMain.playerOnline.online", mapOf("onlineplayers" to Bukkit.getOnlinePlayers().size.toString()))
        private fun playerOfflineLine() = message("PunisherMain.playerOffline.total", mapOf("totalplayers" to plugin.playerIPManager.getAllDecryptedRecords().size.toString()))
        private fun tpsLine(): String {
            val uptime = ManagementFactory.getRuntimeMXBean().uptime / 1000
            return message("PunisherMain.serwerInfo.tps", mapOf("time" to plugin.timeHandler.formatTime(uptime.toString()), "tps" to "N/A"))
        }
    }

    private class OnlinePlayersScreen(
        private val plugin: PunisherX,
        private val controller: PunisherMainGuiController,
        private val pageIndex: Int,
    ) : GuiBase(plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "PunisherMain.playerOnline.title")), 45), SelectableScreen, BackNavigableScreen {
        private val players = plugin.server.onlinePlayers.sortedBy { it.name.lowercase() }
        private val perPage = 27
        private val maxPage = if (players.isEmpty()) 0 else (players.size - 1) / perPage

        override fun buildLayout(page: Int): GuiLayout {
            val slots = filledSlots(size)
            players.drop(pageIndex * perPage).take(perPage).forEachIndexed { i, target ->
                slots[i] = GuiIcon("online_${target.uniqueId}", target.name, lore(target.uniqueId, target.name), GuiMaterial.PLAYER_HEAD, target.uniqueId)
            }
            if (pageIndex > 0) slots[36] = NavigationComponents.previous(message("Nav.previous"))
            slots[40] = NavigationComponents.back(message("Nav.back"))
            if (pageIndex < maxPage) slots[44] = NavigationComponents.next(message("Nav.next"))
            return GuiLayout(title, size, slots)
        }

        override fun onClick(slot: Int): GuiAction = when {
            slot in 0 until perPage -> GuiAction.SelectElement(pageIndex * perPage + slot)
            slot == 36 && pageIndex > 0 -> GuiAction.OpenPage(pageIndex - 1)
            slot == 44 && pageIndex < maxPage -> GuiAction.OpenPage(pageIndex + 1)
            slot == 40 -> GuiAction.Back
            else -> GuiAction.None
        }

        override fun handleSelection(index: Int, player: Player) { players.getOrNull(index)?.let { PlayerActionGUI(plugin).open(player, it) } }
        override fun onBack(player: Player) { controller.open(player) }

        private fun lore(uuid: UUID, name: String): List<String> {
            val noData = plugin.messageHandler.stringMessageToStringNoPrefix("error", "no_data")
            val punishments = plugin.databaseHandler.getActivePunishmentsString(uuid) ?: noData
            return listOf(
                line("PlayerList.hover.uuid", mapOf("uuid" to uuid.toString())),
                line("PlayerList.hover.playerIP", mapOf("playerip" to (plugin.playerIPManager.getPlayerIPByName(name) ?: noData))),
                line("PlayerList.hover.onlineStr", mapOf("onlinestr" to (PlayerStatsService.getCurrentOnlineString(uuid) ?: noData))),
                line("PlayerList.hover.totalStr", mapOf("totalstr" to (PlayerStatsService.getTotalPlaytimeString(uuid) ?: noData))),
                line("PlayerList.hover.lastActive", mapOf("lastactive" to (PlayerStatsService.getLastActiveString(uuid) ?: noData))),
                line("PlayerList.hover.punishments", mapOf("punishments" to punishments)),
                line("PlayerList.hover.punishStr", mapOf("punishstr" to plugin.databaseHandler.countPlayerAllPunishmentHistory(uuid).toString())),
            )
        }

        private fun message(key: String) = plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key)
        private fun line(key: String, vars: Map<String, String>) = plain(plugin.messageHandler.formatMixedTextToMiniMessage(plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key, vars)))
    }

    enum class SortMode { NAME_ASC, NAME_DESC, LAST_SEEN_DESC, LAST_SEEN_ASC;
        fun next(): SortMode = when (this) {
            NAME_ASC -> NAME_DESC
            NAME_DESC -> LAST_SEEN_DESC
            LAST_SEEN_DESC -> LAST_SEEN_ASC
            LAST_SEEN_ASC -> NAME_ASC
        }
    }

    private class OfflinePlayersScreen(
        private val plugin: PunisherX,
        private val controller: PunisherMainGuiController,
        private val pageIndex: Int,
        private val sortMode: SortMode,
    ) : GuiBase(plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "PunisherMain.playerOffline.title")), 45), SelectableScreen, BackNavigableScreen {
        private val perPage = 27
        private val allRecords = plugin.playerIPManager.getAllDecryptedRecords()
        private val players = allRecords
            .mapNotNull { info -> if (Bukkit.getPlayer(UUID.fromString(info.playerUUID)) == null) info else null }
            .distinctBy { it.playerUUID }
            .let { list ->
                when (sortMode) {
                    SortMode.NAME_ASC -> list.sortedBy { it.playerName.lowercase() }
                    SortMode.NAME_DESC -> list.sortedByDescending { it.playerName.lowercase() }
                    SortMode.LAST_SEEN_DESC -> list.sortedByDescending { plugin.timeHandler.parseDate(it.lastUpdated) ?: 0L }
                    SortMode.LAST_SEEN_ASC -> list.sortedBy { plugin.timeHandler.parseDate(it.lastUpdated) ?: Long.MAX_VALUE }
                }
            }
        private val maxPage = if (players.isEmpty()) 0 else (players.size - 1) / perPage
        private val sortSlot = 38

        override fun buildLayout(page: Int): GuiLayout {
            val slots = filledSlots(size)
            players.drop(pageIndex * perPage).take(perPage).forEachIndexed { i, info ->
                val uuid = UUID.fromString(info.playerUUID)
                slots[i] = GuiIcon("offline_${info.playerUUID}", info.playerName, offlineLore(info, uuid), GuiMaterial.PLAYER_HEAD, uuid)
            }
            if (pageIndex > 0) slots[36] = NavigationComponents.previous(message("Nav.previous"))
            slots[sortSlot] = GuiIcon("sort", sortLabel(), emptyList(), GuiMaterial.BOOK)
            slots[40] = NavigationComponents.back(message("Nav.back"))
            if (pageIndex < maxPage) slots[44] = NavigationComponents.next(message("Nav.next"))
            return GuiLayout(title, size, slots)
        }

        override fun onClick(slot: Int): GuiAction = when {
            slot in 0 until perPage -> GuiAction.SelectElement(pageIndex * perPage + slot)
            slot == 36 && pageIndex > 0 -> GuiAction.OpenPage(pageIndex - 1)
            slot == sortSlot -> GuiAction.SelectElement(Int.MAX_VALUE)
            slot == 44 && pageIndex < maxPage -> GuiAction.OpenPage(pageIndex + 1)
            slot == 40 -> GuiAction.Back
            else -> GuiAction.None
        }

        override fun handleSelection(index: Int, player: Player) {
            if (index == Int.MAX_VALUE) {
                controller.openOfflinePlayerList(player, 0, sortMode.next())
                return
            }
            players.getOrNull(index)?.let {
                val off: OfflinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(it.playerUUID))
                PlayerActionGUI(plugin).open(player, off)
            }
        }

        override fun onBack(player: Player) { controller.open(player) }

        private fun message(key: String) = plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key)
        private fun sortLabel(): String {
            val key = when (sortMode) {
                SortMode.NAME_ASC -> "nameAsc"
                SortMode.NAME_DESC -> "nameDesc"
                SortMode.LAST_SEEN_DESC -> "lastSeenDesc"
                SortMode.LAST_SEEN_ASC -> "lastSeenAsc"
            }
            return plain(plugin.messageHandler.formatMixedTextToMiniMessage(plugin.messageHandler.stringMessageToStringNoPrefix("GUI", "OfflineList.sort.$key")))
        }

        private fun offlineLore(info: pl.syntaxdevteam.punisher.players.PlayerIPManager.PlayerInfo, uuid: UUID): List<String> {
            val noData = plugin.messageHandler.stringMessageToStringNoPrefix("error", "no_data")
            val punishments = plugin.databaseHandler.getPunishmentHistory(info.playerUUID, 3, 0)
            val punishLines = if (punishments.isEmpty()) {
                listOf(line("OfflineList.hover.noPunishments", emptyMap()))
            } else {
                punishments.map { line("OfflineList.hover.punishment", mapOf("punishment" to "${it.type}: ${it.reason}")) }
            }
            return mutableListOf(
                line("OfflineList.hover.uuid", mapOf("uuid" to info.playerUUID)),
                line("OfflineList.hover.ip", mapOf("ip" to ipHistory(info.playerUUID))),
                line("OfflineList.hover.geo", mapOf("geo" to info.geoLocation)),
                line("OfflineList.hover.lastSeen", mapOf("lastseen" to info.lastUpdated)),
                line("OfflineList.hover.lastLocation", mapOf("lastlocation" to (PlayerStatsService.getLastLocationString(uuid) ?: noData))),
                line("OfflineList.hover.logout", mapOf("logout" to info.lastUpdated)),
                line("OfflineList.hover.offlineTime", mapOf("offlinetime" to plugin.timeHandler.getOfflineDuration(info.lastUpdated))),
            ).apply { addAll(punishLines) }
        }

        private fun ipHistory(playerUuid: String): String = allRecords
            .filter { it.playerUUID == playerUuid }
            .sortedByDescending { plugin.timeHandler.parseDate(it.lastUpdated) ?: 0L }
            .map { it.playerIP }
            .distinct().take(3)
            .joinToString(", ")

        private fun line(key: String, vars: Map<String, String>) = plain(plugin.messageHandler.formatMixedTextToMiniMessage(plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key, vars)))
    }

    private class AdminPlayersScreen(
        private val plugin: PunisherX,
        private val controller: PunisherMainGuiController,
        private val pageIndex: Int,
    ) : GuiBase(plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "PunisherMain.adminOnline.title")), 45), BackNavigableScreen {
        private val admins = plugin.server.onlinePlayers.filter { PermissionChecker.hasPermissionStartingWith(it, "punisherx") }.sortedBy { it.name.lowercase() }
        private val perPage = 27
        private val maxPage = if (admins.isEmpty()) 0 else (admins.size - 1) / perPage

        override fun buildLayout(page: Int): GuiLayout {
            val slots = filledSlots(size)
            admins.drop(pageIndex * perPage).take(perPage).forEachIndexed { i, admin ->
                slots[i] = GuiIcon("admin_${admin.uniqueId}", admin.name, emptyList(), GuiMaterial.PLAYER_HEAD, admin.uniqueId)
            }
            if (pageIndex > 0) slots[36] = NavigationComponents.previous(message("Nav.previous"))
            slots[40] = NavigationComponents.back(message("Nav.back"))
            if (pageIndex < maxPage) slots[44] = NavigationComponents.next(message("Nav.next"))
            return GuiLayout(title, size, slots)
        }

        override fun onClick(slot: Int): GuiAction = when {
            slot == 36 && pageIndex > 0 -> GuiAction.OpenPage(pageIndex - 1)
            slot == 44 && pageIndex < maxPage -> GuiAction.OpenPage(pageIndex + 1)
            slot == 40 -> GuiAction.Back
            else -> GuiAction.None
        }

        override fun onBack(player: Player) { controller.open(player) }
        private fun message(key: String) = plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key)
    }

    private class PunishedMenuScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController) :
        GuiBase(plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "PunishedList.title")), 45),
        SelectableScreen,
        BackNavigableScreen {

        private val entries = listOf(
            MenuEntry(20, GuiMaterial.MACE, message("PunishedList.banned")) { _, p -> BanListGUI(plugin).open(p) },
            MenuEntry(24, GuiMaterial.TRIAL_KEY, message("PunishedList.jailed")) { _, p -> JailListGUI(plugin).open(p) },
        )

        override fun buildLayout(page: Int): GuiLayout {
            val slots = filledSlots(size)
            entries.forEach { slots[it.slot] = GuiIcon(it.key, it.title, emptyList(), it.material) }
            slots[40] = NavigationComponents.back(message("Nav.back"))
            return GuiLayout(title, size, slots)
        }

        override fun onClick(slot: Int): GuiAction {
            val idx = entries.indexOfFirst { it.slot == slot }
            return when {
                idx >= 0 -> GuiAction.SelectElement(idx)
                slot == 40 -> GuiAction.Back
                else -> GuiAction.None
            }
        }

        override fun handleSelection(index: Int, player: Player) { entries.getOrNull(index)?.onClick?.invoke(index, player) }
        override fun onBack(player: Player) { controller.open(player) }
        private fun message(key: String) = plain(plugin.messageHandler.formatMixedTextToMiniMessage(plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key)))
    }

    private data class MenuEntry(
        val slot: Int,
        val material: GuiMaterial,
        val title: String,
        val loreProvider: () -> List<String> = { emptyList() },
        val onClick: (Int, Player) -> Unit,
    ) { val key: String = "entry_${slot}_${material.name.lowercase()}" }

    companion object {
        private val serializer = PlainTextComponentSerializer.plainText()
        private fun plain(component: Component): String = serializer.serialize(component)
        private fun filledSlots(size: Int): MutableMap<Int, GuiIcon> {
            val slots = mutableMapOf<Int, GuiIcon>()
            for (i in 0 until size) slots[i] = NavigationComponents.filler(" ")
            return slots
        }
    }
}

package pl.syntaxdevteam.punisher.gui.syntax

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.admin.ConfigGUI
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

    fun openPlayerAction(player: Player, target: OfflinePlayer) = renderer.open(player, PlayerActionScreen(plugin, this, target.uniqueId))
    fun openPunishType(player: Player, target: OfflinePlayer) = renderer.open(player, PunishTypeScreen(plugin, this, target.uniqueId))
    fun openPunishTime(player: Player, target: OfflinePlayer, type: String) = renderer.open(player, PunishTimeScreen(plugin, this, target.uniqueId, type))
    fun openPunishReason(player: Player, target: OfflinePlayer, type: String, time: String) = renderer.open(player, PunishReasonScreen(plugin, this, target.uniqueId, type, time))
    fun openConfirmDelete(player: Player, target: OfflinePlayer) = renderer.open(player, ConfirmDeleteScreen(plugin, this, target.uniqueId))
    fun openBanList(player: Player, page: Int = 0) = renderer.open(player, BanListScreen(plugin, this, page))
    fun openJailList(player: Player, page: Int = 0) = renderer.open(player, JailListScreen(plugin, this, page))
    fun openConfig(player: Player) = renderer.open(player, ConfigScreen(plugin, this))
    fun openReportSelector(player: Player) = renderer.open(player, ReportSelectorScreen(plugin, this))
    fun openReportPlayers(player: Player, page: Int = 0) = renderer.open(player, ReportPlayerScreen(plugin, this, player.uniqueId, page))
    fun openReportOffline(player: Player, page: Int = 0) = renderer.open(player, ReportOfflineScreen(plugin, this, player.uniqueId, page))
    fun openReportReason(player: Player, target: OfflinePlayer, page: Int = 0) = renderer.open(player, ReportReasonScreen(plugin, this, target.uniqueId, page))

    private interface SelectableScreen { fun handleSelection(index: Int, player: Player) }
    private interface BackNavigableScreen { fun onBack(player: Player) }

    private class MainScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController) : GuiBase(
        plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "PunisherMain.title")), 45,
    ), SelectableScreen {
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

    private class OnlinePlayersScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController, private val pageIndex: Int) : GuiBase(
        plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "PunisherMain.playerOnline.title")), 45,
    ), SelectableScreen, BackNavigableScreen {
        private val players = plugin.server.onlinePlayers.sortedBy { it.name.lowercase() }
        private val perPage = 27
        private val maxPage = if (players.isEmpty()) 0 else (players.size - 1) / perPage

        override fun buildLayout(page: Int): GuiLayout {
            val slots = filledSlots(size)
            players.drop(pageIndex * perPage).take(perPage).forEachIndexed { i, target ->
                slots[i] = GuiIcon("online_${target.uniqueId}", target.name, lore(target.uniqueId, target.name), GuiMaterial.PLAYER_HEAD, target.uniqueId)
            }
            applyPagedNavigation(slots, pageIndex, pageIndex < maxPage, ::message)
            return GuiLayout(title, size, slots)
        }

        override fun onClick(slot: Int): GuiAction = when (slot) {
            in 0 until perPage -> GuiAction.SelectElement(pageIndex * perPage + slot)
            36 if pageIndex > 0 -> GuiAction.OpenPage(pageIndex - 1)
            44 if pageIndex < maxPage -> GuiAction.OpenPage(pageIndex + 1)
            40 -> GuiAction.Back
            else -> GuiAction.None
        }

        override fun handleSelection(index: Int, player: Player) { players.getOrNull(index)?.let { controller.openPlayerAction(player, it) } }
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

    private class OfflinePlayersScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController, private val pageIndex: Int, private val sortMode: SortMode) : GuiBase(
        plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "PunisherMain.playerOffline.title")), 45,
    ), SelectableScreen, BackNavigableScreen {
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
            slots[sortSlot] = GuiIcon("sort", sortLabel(), emptyList(), GuiMaterial.BOOK)
            applyPagedNavigation(slots, pageIndex, pageIndex < maxPage, ::message)
            return GuiLayout(title, size, slots)
        }

        override fun onClick(slot: Int): GuiAction = when (slot) {
            in 0 until perPage -> GuiAction.SelectElement(pageIndex * perPage + slot)
            36 if pageIndex > 0 -> GuiAction.OpenPage(pageIndex - 1)
            sortSlot -> GuiAction.SelectElement(Int.MAX_VALUE)
            44 if pageIndex < maxPage -> GuiAction.OpenPage(pageIndex + 1)
            40 -> GuiAction.Back
            else -> GuiAction.None
        }

        override fun handleSelection(index: Int, player: Player) {
            if (index == Int.MAX_VALUE) {
                controller.openOfflinePlayerList(player, 0, sortMode.next())
                return
            }
            players.getOrNull(index)?.let {
                val off = Bukkit.getOfflinePlayer(UUID.fromString(it.playerUUID))
                controller.openPlayerAction(player, off)
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
            .asSequence()
            .filter { it.playerUUID == playerUuid }
            .sortedByDescending { plugin.timeHandler.parseDate(it.lastUpdated) ?: 0L }
            .map { it.playerIP }
            .distinct().take(3)
            .joinToString(", ")

        private fun line(key: String, vars: Map<String, String>) = plain(plugin.messageHandler.formatMixedTextToMiniMessage(plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key, vars)))
    }

    private class PlayerActionScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController, private val targetId: UUID) : GuiBase(
        plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "PlayerAction.title")), 45,
    ), SelectableScreen, BackNavigableScreen {
        private val target by lazy { Bukkit.getOfflinePlayer(targetId) }
        private val entries = listOf(
            MenuEntry(11, GuiMaterial.MACE, msg("PlayerAction.punish")) { _, p -> controller.openPunishType(p, target) },
            MenuEntry(13, GuiMaterial.TRIAL_KEY, msg("PlayerAction.undo")) { _, p -> undoPunishments(p) },
            MenuEntry(15, GuiMaterial.BOOK, msg("PlayerAction.history")) { _, p -> p.closeInventory(); p.performCommand("history ${target.name ?: return@MenuEntry}") },
            MenuEntry(29, GuiMaterial.PAPER, msg("PlayerAction.active")) { _, p -> p.closeInventory(); p.performCommand("check ${target.name ?: return@MenuEntry} all") },
            MenuEntry(31, GuiMaterial.WIND_CHARGE, msg("PlayerAction.teleport")) { _, p -> teleportToTarget(p) },
            MenuEntry(33, GuiMaterial.BARRIER, msg("PlayerAction.delete")) { _, p -> controller.openConfirmDelete(p, target) },
        )

        override fun buildLayout(page: Int): GuiLayout {
            val slots = filledSlots(size)
            entries.forEach { slots[it.slot] = GuiIcon(it.key, it.title, emptyList(), it.material) }
            slots[40] = NavigationComponents.back(msg("Nav.back"))
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
        override fun onBack(player: Player) {
            if (target.isOnline) controller.openPlayerList(player) else controller.openOfflinePlayerList(player)
        }

        private fun undoPunishments(player: Player) {
            player.closeInventory()
            val name = target.name ?: return
            val punishments = plugin.databaseHandler.getPunishments(target.uniqueId.toString())
            if (punishments.isEmpty()) {
                player.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_data"))
                return
            }
            punishments.forEach { punishment ->
                val command = when (punishment.type) {
                    "BAN", "BANIP" -> "unban $name"
                    "MUTE" -> "unmute $name"
                    "WARN" -> "unwarn $name"
                    "JAIL" -> "unjail $name"
                    else -> null
                }
                if (command != null) player.performCommand(command)
            }
        }

        private fun teleportToTarget(player: Player) {
            player.closeInventory()
            val online = target.player
            if (online != null) {
                plugin.safeTeleportService.teleportSafely(player, online.location)
                return
            }
            val loc = PlayerStatsService.getLastLocation(target.uniqueId)
            if (loc != null) {
                plugin.safeTeleportService.teleportSafely(player, loc)
            } else {
                player.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_data"))
            }
        }

        private fun msg(key: String): String = plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key)
    }

    private class PunishTypeScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController, private val targetId: UUID) : GuiBase(
        plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "PunishType.title")), 27,
    ), SelectableScreen, BackNavigableScreen {
        private val target by lazy { Bukkit.getOfflinePlayer(targetId) }
        private val force = plugin.config.getBoolean("gui.punish.use_force", false)
        private val entries = listOf(
            MenuEntry(10, GuiMaterial.MACE, msg("PunishType.ban")) { _, p -> controller.openPunishTime(p, target, "ban") },
            MenuEntry(11, GuiMaterial.BARRIER, msg("PunishType.banip")) { _, p -> p.closeInventory(); p.performCommand(build("banip", target.name, plugin.messageHandler.stringMessageToString("banip", "no_reasons"))) },
            MenuEntry(12, GuiMaterial.WIND_CHARGE, msg("PunishType.kick")) { _, p ->
                val online = target.player ?: return@MenuEntry
                p.closeInventory(); p.performCommand(build("kick", online.name, plugin.messageHandler.stringMessageToString("kick", "no_reasons")))
            },
            MenuEntry(14, GuiMaterial.TRIAL_KEY, msg("PunishType.jail")) { _, p -> controller.openPunishTime(p, target, "jail") },
            MenuEntry(15, GuiMaterial.BOOK, msg("PunishType.mute")) { _, p -> controller.openPunishTime(p, target, "mute") },
            MenuEntry(16, GuiMaterial.PAPER, msg("PunishType.warn")) { _, p -> controller.openPunishTime(p, target, "warn") },
        )

        override fun buildLayout(page: Int): GuiLayout {
            val slots = filledSlots(size)
            entries.forEach { slots[it.slot] = GuiIcon(it.key, it.title, emptyList(), it.material) }
            slots[22] = NavigationComponents.back(msg("Nav.back"))
            return GuiLayout(title, size, slots)
        }

        override fun onClick(slot: Int): GuiAction {
            val idx = entries.indexOfFirst { it.slot == slot }
            return when {
                idx >= 0 -> GuiAction.SelectElement(idx)
                slot == 22 -> GuiAction.Back
                else -> GuiAction.None
            }
        }

        override fun handleSelection(index: Int, player: Player) { entries.getOrNull(index)?.onClick?.invoke(index, player) }
        override fun onBack(player: Player) { controller.openPlayerAction(player, target) }

        private fun build(command: String, targetName: String?, reason: String): String {
            val targetValue = targetName ?: return command
            return buildString {
                append(command).append(' ').append(targetValue).append(' ').append(reason)
                if (force) append(" --force")
            }
        }

        private fun msg(key: String): String = plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key)
    }

    private class PunishTimeScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController, private val targetId: UUID, private val type: String) : GuiBase(
        plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "PunishTime.title")), 27,
    ), SelectableScreen, BackNavigableScreen {
        private val target by lazy { Bukkit.getOfflinePlayer(targetId) }
        private val times = plugin.config.getStringList("gui.punish.times")

        override fun buildLayout(page: Int): GuiLayout {
            val slots = filledSlots(size)
            times.take(9).forEachIndexed { idx, time ->
                slots[10 + idx] = GuiIcon("time_${idx}", time, emptyList(), GuiMaterial.PAPER)
            }
            slots[22] = NavigationComponents.back(msg("Nav.back"))
            return GuiLayout(title, size, slots)
        }

        override fun onClick(slot: Int): GuiAction = when (slot) {
            in 10 until 10 + times.take(9).size -> GuiAction.SelectElement(slot - 10)
            22 -> GuiAction.Back
            else -> GuiAction.None
        }

        override fun handleSelection(index: Int, player: Player) {
            times.getOrNull(index)?.let { controller.openPunishReason(player, target, type, it) }
        }

        override fun onBack(player: Player) { controller.openPunishType(player, target) }
        private fun msg(key: String): String = plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key)
    }

    private class PunishReasonScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController, private val targetId: UUID, private val type: String, private val time: String) : GuiBase(
        plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "PunishReason.title")), reasonSize(plugin),
    ), SelectableScreen, BackNavigableScreen {
        private val target by lazy { Bukkit.getOfflinePlayer(targetId) }
        private val reasons = plugin.config.getStringList("gui.punish.reasons")
        private val force = plugin.config.getBoolean("gui.punish.use_force", false)

        override fun buildLayout(page: Int): GuiLayout {
            val slots = filledSlots(size)
            reasons.take(size - 9).forEachIndexed { idx, reason ->
                slots[idx] = GuiIcon("reason_$idx", reason, emptyList(), GuiMaterial.PAPER)
            }
            slots[size - 5] = NavigationComponents.back(msg("Nav.back"))
            return GuiLayout(title, size, slots)
        }

        override fun onClick(slot: Int): GuiAction = when (slot) {
            in reasons.take(size - 9).indices -> GuiAction.SelectElement(slot)
            size - 5 -> GuiAction.Back
            else -> GuiAction.None
        }

        override fun handleSelection(index: Int, player: Player) {
            val reason = reasons.getOrNull(index) ?: return
            val name = target.name ?: return
            player.closeInventory()
            val command = if (time.equals("perm", true)) {
                "$type $name $reason"
            } else {
                "$type $name $time $reason"
            } + if (force) " --force" else ""
            player.performCommand(command)
        }

        override fun onBack(player: Player) { controller.openPunishTime(player, target, type) }

        private fun msg(key: String): String = plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key)

        companion object {
            private fun reasonSize(plugin: PunisherX): Int {
                val reasons = plugin.config.getStringList("gui.punish.reasons")
                val raw = ((reasons.size / 9) + 1) * 9
                return raw.coerceIn(27, 54)
            }
        }
    }

    private class ConfirmDeleteScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController, private val targetId: UUID) : GuiBase(
        plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "PlayerAction.confirmDelete.title")), 27,
    ), SelectableScreen, BackNavigableScreen {
        private val target by lazy { Bukkit.getOfflinePlayer(targetId) }

        override fun buildLayout(page: Int): GuiLayout {
            val slots = filledSlots(size)
            slots[11] = GuiIcon("confirm", msg("PlayerAction.confirmDelete.confirm"), emptyList(), GuiMaterial.TRIAL_KEY)
            slots[15] = GuiIcon("cancel", msg("PlayerAction.confirmDelete.cancel"), emptyList(), GuiMaterial.BARRIER)
            return GuiLayout(title, size, slots)
        }

        override fun onClick(slot: Int): GuiAction = when (slot) {
            11 -> GuiAction.SelectElement(0)
            15 -> GuiAction.Back
            else -> GuiAction.None
        }

        override fun handleSelection(index: Int, player: Player) {
            if (index != 0) return
            player.closeInventory()
            target.player?.kick(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "PlayerAction.deleteMessage"))
            plugin.databaseHandler.deletePlayerData(target.uniqueId.toString())
            plugin.playerIPManager.deletePlayerInfo(target.uniqueId)
        }

        override fun onBack(player: Player) { controller.openPlayerAction(player, target) }

        private fun msg(key: String): String = plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key)
    }

    private class AdminPlayersScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController, private val pageIndex: Int) : GuiBase(
        plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "PunisherMain.adminOnline.title")), 45,
    ), BackNavigableScreen {
        private val admins = plugin.server.onlinePlayers.filter { PermissionChecker.hasPermissionStartingWith(it, "punisherx") }.sortedBy { it.name.lowercase() }
        private val perPage = 27
        private val maxPage = if (admins.isEmpty()) 0 else (admins.size - 1) / perPage

        override fun buildLayout(page: Int): GuiLayout {
            val slots = filledSlots(size)
            admins.drop(pageIndex * perPage).take(perPage).forEachIndexed { i, admin ->
                slots[i] = GuiIcon("admin_${admin.uniqueId}", admin.name, emptyList(), GuiMaterial.PLAYER_HEAD, admin.uniqueId)
            }
            applyPagedNavigation(slots, pageIndex, pageIndex < maxPage, ::message)
            return GuiLayout(title, size, slots)
        }

        override fun onClick(slot: Int): GuiAction = when (slot) {
            36 if pageIndex > 0 -> GuiAction.OpenPage(pageIndex - 1)
            44 if pageIndex < maxPage -> GuiAction.OpenPage(pageIndex + 1)
            40 -> GuiAction.Back
            else -> GuiAction.None
        }

        override fun onBack(player: Player) { controller.open(player) }
        private fun message(key: String) = plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key)
    }

    private class PunishedMenuScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController) : GuiBase(
        plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "PunishedList.title")), 45,
    ), SelectableScreen, BackNavigableScreen {
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


    private class BanListScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController, private val pageIndex: Int) : GuiBase(
        plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "BanList.title")), 45,
    ), BackNavigableScreen {
        private val limit = 27
        private val offset = pageIndex * limit
        private val punishments = plugin.databaseHandler.getBannedPlayers(limit, offset)
        private val hasNext = plugin.databaseHandler.getBannedPlayers(1, offset + limit).isNotEmpty()

        override fun buildLayout(page: Int): GuiLayout {
            val slots = filledSlots(size)
            punishments.forEachIndexed { idx, punishment ->
                val remaining = if (punishment.end == -1L) msg("BanList.permanent") else plugin.timeHandler.formatTime(((punishment.end - System.currentTimeMillis()) / 1000).toString())
                slots[idx] = GuiIcon(
                    "ban_${punishment.id}",
                    punishment.name,
                    listOf(
                        line("BanList.hover.id", mapOf("id" to punishment.id.toString())),
                        line("BanList.hover.date", mapOf("date" to java.text.SimpleDateFormat("yy-MM-dd HH:mm:ss").format(java.util.Date(punishment.start)))),
                        line("BanList.hover.remaining", mapOf("time" to remaining)),
                        line("BanList.hover.operator", mapOf("operator" to punishment.operator)),
                        line("BanList.hover.reason", mapOf("reason" to punishment.reason)),
                    ),
                    GuiMaterial.PLAYER_HEAD,
                    Bukkit.getOfflinePlayer(punishment.name).uniqueId,
                )
            }
            applyPagedNavigation(slots, pageIndex, hasNext, ::msg)
            return GuiLayout(title, size, slots)
        }

        override fun onClick(slot: Int): GuiAction = when (slot) {
            36 if pageIndex > 0 -> GuiAction.OpenPage(pageIndex - 1)
            44 if hasNext -> GuiAction.OpenPage(pageIndex + 1)
            40 -> GuiAction.Back
            else -> GuiAction.None
        }

        override fun onBack(player: Player) { controller.openPunishedMenu(player) }
        private fun msg(key: String) = plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key)
        private fun line(key: String, vars: Map<String, String>) = plain(plugin.messageHandler.formatMixedTextToMiniMessage(plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key, vars)))
    }

    private class JailListScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController, private val pageIndex: Int) : GuiBase(
        plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "JailList.title")), 45,
    ), BackNavigableScreen {
        private val limit = 27
        private val offset = pageIndex * limit
        private val punishments = plugin.databaseHandler.getJailedPlayers(limit, offset)
        private val hasNext = plugin.databaseHandler.getJailedPlayers(1, offset + limit).isNotEmpty()

        override fun buildLayout(page: Int): GuiLayout {
            val slots = filledSlots(size)
            punishments.forEachIndexed { idx, punishment ->
                val remaining = if (punishment.end == -1L) msg("JailList.permanent") else plugin.timeHandler.formatTime(((punishment.end - System.currentTimeMillis()) / 1000).toString())
                slots[idx] = GuiIcon(
                    "jail_${punishment.id}",
                    punishment.name,
                    listOf(
                        line("JailList.hover.id", mapOf("id" to punishment.id.toString())),
                        line("JailList.hover.date", mapOf("date" to java.text.SimpleDateFormat("yy-MM-dd HH:mm:ss").format(java.util.Date(punishment.start)))),
                        line("JailList.hover.remaining", mapOf("time" to remaining)),
                        line("JailList.hover.operator", mapOf("operator" to punishment.operator)),
                        line("JailList.hover.reason", mapOf("reason" to punishment.reason)),
                    ),
                    GuiMaterial.PLAYER_HEAD,
                    Bukkit.getOfflinePlayer(punishment.name).uniqueId,
                )
            }
            applyPagedNavigation(slots, pageIndex, hasNext, ::msg)
            return GuiLayout(title, size, slots)
        }

        override fun onClick(slot: Int): GuiAction = when (slot) {
            36 if pageIndex > 0 -> GuiAction.OpenPage(pageIndex - 1)
            44 if hasNext -> GuiAction.OpenPage(pageIndex + 1)
            40 -> GuiAction.Back
            else -> GuiAction.None
        }

        override fun onBack(player: Player) { controller.openPunishedMenu(player) }
        private fun msg(key: String) = plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key)
        private fun line(key: String, vars: Map<String, String>) = plain(plugin.messageHandler.formatMixedTextToMiniMessage(plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key, vars)))
    }

    private class ConfigScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController) : GuiBase(
        plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "Config.title")), 45,
    ), SelectableScreen, BackNavigableScreen {
        override fun buildLayout(page: Int): GuiLayout {
            val slots = filledSlots(size)
            slots[20] = GuiIcon("setunjail", msg("Config.setunjail"), emptyList(), GuiMaterial.BOOK)
            slots[24] = GuiIcon("setjail", msg("Config.setjail"), emptyList(), GuiMaterial.TRIAL_KEY)
            slots[40] = NavigationComponents.back(msg("Nav.back"))
            return GuiLayout(title, size, slots)
        }

        override fun onClick(slot: Int): GuiAction = when (slot) {
            20 -> GuiAction.SelectElement(0)
            24 -> GuiAction.SelectElement(1)
            40 -> GuiAction.Back
            else -> GuiAction.None
        }

        override fun handleSelection(index: Int, player: Player) {
            when (index) {
                0 -> { player.closeInventory(); player.performCommand("setunjail") }
                1 -> { player.closeInventory(); player.performCommand("setjail 5") }
            }
        }

        override fun onBack(player: Player) { controller.open(player) }
        private fun msg(key: String) = plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key)
    }

    private class ReportSelectorScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController) : GuiBase(
        plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "Report.menu.title")), 45,
    ), SelectableScreen, BackNavigableScreen {
        override fun buildLayout(page: Int): GuiLayout {
            val slots = filledSlots(size)
            slots[20] = GuiIcon("online", msg("Report.menu.online"), emptyList(), GuiMaterial.TRIAL_KEY)
            slots[24] = GuiIcon("offline", msg("Report.menu.offline"), emptyList(), GuiMaterial.BARRIER)
            slots[40] = NavigationComponents.back(msg("Nav.back"))
            return GuiLayout(title, size, slots)
        }

        override fun onClick(slot: Int): GuiAction = when (slot) {
            20 -> GuiAction.SelectElement(0)
            24 -> GuiAction.SelectElement(1)
            40 -> GuiAction.Back
            else -> GuiAction.None
        }

        override fun handleSelection(index: Int, player: Player) {
            when (index) {
                0 -> controller.openReportPlayers(player)
                1 -> controller.openReportOffline(player)
            }
        }

        override fun onBack(player: Player) { player.closeInventory() }
        private fun msg(key: String) = plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key)
    }

    private class ReportPlayerScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController, private val reporterId: UUID, private val pageIndex: Int) : GuiBase(
        plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "Report.player.title")), 45,
    ), SelectableScreen, BackNavigableScreen {
        private val centerSlots = intArrayOf(10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34)
        private val players = plugin.server.onlinePlayers.filter { it.uniqueId != reporterId }.sortedBy { it.name.lowercase() }
        private val perPage = centerSlots.size
        private val maxPage = if (players.isEmpty()) 0 else (players.size - 1) / perPage

        override fun buildLayout(page: Int): GuiLayout {
            val slots = filledSlots(size)
            players.drop(pageIndex * perPage).take(perPage).forEachIndexed { i, target ->
                slots[centerSlots[i]] = GuiIcon("report_online_${target.uniqueId}", target.name, listOf(msg("Report.lore.clickToReport")), GuiMaterial.PLAYER_HEAD, target.uniqueId)
            }
            applyPagedNavigation(slots, pageIndex, pageIndex < maxPage, ::msg)
            return GuiLayout(title, size, slots)
        }

        override fun onClick(slot: Int): GuiAction = when (slot) {
            in centerSlots -> GuiAction.SelectElement(pageIndex * perPage + centerSlots.indexOf(slot))
            36 if pageIndex > 0 -> GuiAction.OpenPage(pageIndex - 1)
            44 if pageIndex < maxPage -> GuiAction.OpenPage(pageIndex + 1)
            40 -> GuiAction.Back
            else -> GuiAction.None
        }

        override fun handleSelection(index: Int, player: Player) {
            players.getOrNull(index)?.let { controller.openReportReason(player, it) }
        }

        override fun onBack(player: Player) { controller.openReportSelector(player) }
        private fun msg(key: String) = plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key)
    }

    private class ReportOfflineScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController, private val reporterId: UUID, private val pageIndex: Int) : GuiBase(
        plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "Report.offline.title")), 45,
    ), SelectableScreen, BackNavigableScreen {
        private val centerSlots = intArrayOf(10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34)
        private val perPage = centerSlots.size
        private val oneHourMs = 60 * 60 * 1000L
        private val now = System.currentTimeMillis()
        private val players = Bukkit.getOfflinePlayers().asSequence()
            .filter { it.uniqueId != reporterId && !it.isOnline && !it.name.isNullOrBlank() }
            .filter { it.lastSeen > 0L && (now - it.lastSeen) <= oneHourMs }
            .sortedBy { it.name!!.lowercase() }
            .toList()
        private val maxPage = if (players.isEmpty()) 0 else (players.size - 1) / perPage

        override fun buildLayout(page: Int): GuiLayout {
            val slots = filledSlots(size)
            if (players.isEmpty()) {
                slots[22] = GuiIcon("none", "No recent offline players", listOf("Only players from last hour are listed."), GuiMaterial.PAPER)
            } else {
                players.drop(pageIndex * perPage).take(perPage).forEachIndexed { i, target ->
                    slots[centerSlots[i]] = GuiIcon("report_offline_${target.uniqueId}", target.name ?: target.uniqueId.toString(), listOf(msg("Report.lore.clickToReport")), GuiMaterial.PLAYER_HEAD, target.uniqueId)
                }
            }
            applyPagedNavigation(slots, pageIndex, pageIndex < maxPage, ::msg)
            return GuiLayout(title, size, slots)
        }

        override fun onClick(slot: Int): GuiAction = when (slot) {
            in centerSlots -> GuiAction.SelectElement(pageIndex * perPage + centerSlots.indexOf(slot))
            36 if pageIndex > 0 -> GuiAction.OpenPage(pageIndex - 1)
            44 if pageIndex < maxPage -> GuiAction.OpenPage(pageIndex + 1)
            40 -> GuiAction.Back
            else -> GuiAction.None
        }

        override fun handleSelection(index: Int, player: Player) {
            players.getOrNull(index)?.let { controller.openReportReason(player, it) }
        }

        override fun onBack(player: Player) { controller.openReportSelector(player) }
        private fun msg(key: String) = plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key)
    }

    private class ReportReasonScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController, private val targetId: UUID, private val pageIndex: Int) : GuiBase(
        plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "Report.reason.title")), 45,
    ), SelectableScreen, BackNavigableScreen {
        private val centerSlots = intArrayOf(10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34)
        private val reasons = plugin.config.getStringList("gui.punish.reasons").ifEmpty { listOf("Cheating", "Griefing", "Spamming") }
        private val perPage = centerSlots.size
        private val maxPage = if (reasons.isEmpty()) 0 else (reasons.size - 1) / perPage
        private val target by lazy { Bukkit.getOfflinePlayer(targetId) }

        override fun buildLayout(page: Int): GuiLayout {
            val slots = filledSlots(size)
            reasons.drop(pageIndex * perPage).take(perPage).forEachIndexed { i, reason ->
                slots[centerSlots[i]] = GuiIcon("report_reason_$i", reason, listOf(msg("Report.lore.clickToChoose")), GuiMaterial.BOOK)
            }
            applyPagedNavigation(slots, pageIndex, pageIndex < maxPage, ::msg)
            return GuiLayout(title, size, slots)
        }

        override fun onClick(slot: Int): GuiAction = when (slot) {
            in centerSlots -> GuiAction.SelectElement(pageIndex * perPage + centerSlots.indexOf(slot))
            36 if pageIndex > 0 -> GuiAction.OpenPage(pageIndex - 1)
            44 if pageIndex < maxPage -> GuiAction.OpenPage(pageIndex + 1)
            40 -> GuiAction.Back
            else -> GuiAction.None
        }

        override fun handleSelection(index: Int, player: Player) {
            val reason = reasons.getOrNull(index) ?: return
            player.closeInventory()
            val success = plugin.databaseHandler.addReport(player.uniqueId, target.uniqueId, reason)
            if (success) {
                player.sendMessage(plugin.messageHandler.stringMessageToComponent("reports", "report-sent", mapOf("target" to (target.name ?: target.uniqueId.toString()), "reason" to reason)))
                plugin.server.onlinePlayers
                    .filter { PermissionChecker.hasWithSee(it, PermissionChecker.PermissionKey.SEE_REPORTS) }
                    .forEach { staff ->
                        staff.sendMessage(plugin.messageHandler.stringMessageToComponentNoPrefix("reports", "admin-notify", mapOf("reporter" to player.name, "target" to (target.name ?: target.uniqueId.toString()), "reason" to reason)))
                    }
            } else {
                player.sendMessage(plugin.messageHandler.stringMessageToComponentNoPrefix("error", "db_error"))
            }
        }

        override fun onBack(player: Player) { controller.openReportSelector(player) }
        private fun msg(key: String) = plugin.messageHandler.stringMessageToStringNoPrefix("GUI", key)
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
        private fun applyPagedNavigation(
            slots: MutableMap<Int, GuiIcon>,
            pageIndex: Int,
            hasNextPage: Boolean,
            message: (String) -> String,
        ) {
            if (pageIndex > 0) slots[36] = NavigationComponents.previous(message("Nav.previous"))
            slots[40] = NavigationComponents.back(message("Nav.back"))
            if (hasNextPage) slots[44] = NavigationComponents.next(message("Nav.next"))
        }

        private fun filledSlots(size: Int): MutableMap<Int, GuiIcon> {
            val slots = mutableMapOf<Int, GuiIcon>()
            for (i in 0 until size) slots[i] = NavigationComponents.filler(" ")
            return slots
        }
    }
}

package pl.syntaxdevteam.punisher.gui.syntax

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import pl.syntaxdevteam.syntaxguiapi.components.NavigationComponents
import pl.syntaxdevteam.syntaxguiapi.core.GuiAction
import pl.syntaxdevteam.syntaxguiapi.core.GuiBase
import pl.syntaxdevteam.syntaxguiapi.core.GuiIcon
import pl.syntaxdevteam.syntaxguiapi.core.GuiLayout
import pl.syntaxdevteam.syntaxguiapi.core.GuiMaterial
import java.util.UUID

internal interface SelectableScreen { fun handleSelection(index: Int, player: Player) }
internal interface BackNavigableScreen { fun onBack(player: Player) }

internal data class MenuEntry(
    val slot: Int,
    val material: GuiMaterial,
    val title: String,
    val loreProvider: () -> List<String> = { emptyList() },
    val onClick: (Int, Player) -> Unit,
) { val key: String = "entry_${slot}_${material.name.lowercase()}" }

private val serializer = PlainTextComponentSerializer.plainText()
internal fun plain(component: Component): String = serializer.serialize(component)

internal fun applyPagedNavigation(
    slots: MutableMap<Int, GuiIcon>,
    pageIndex: Int,
    hasNextPage: Boolean,
    message: (String) -> String,
) {
    if (pageIndex > 0) slots[36] = NavigationComponents.previous(message("Nav.previous"))
    slots[40] = NavigationComponents.back(message("Nav.back"))
    if (hasNextPage) slots[44] = NavigationComponents.next(message("Nav.next"))
}

internal fun filledSlots(size: Int): MutableMap<Int, GuiIcon> {
    val slots = mutableMapOf<Int, GuiIcon>()
    for (i in 0 until size) slots[i] = NavigationComponents.filler(" ")
    return slots
}

internal class PunishedMenuScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController) : GuiBase(
    plain(plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", "PunishedList.title")), 45,
), SelectableScreen, BackNavigableScreen {
    private val entries = listOf(
        MenuEntry(20, GuiMaterial.MACE, message("PunishedList.banned")) { _, p -> controller.openBanList(p) },
        MenuEntry(24, GuiMaterial.TRIAL_KEY, message("PunishedList.jailed")) { _, p -> controller.openJailList(p) },
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


internal class BanListScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController, private val pageIndex: Int) : GuiBase(
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

internal class JailListScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController, private val pageIndex: Int) : GuiBase(
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

internal class ConfigScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController) : GuiBase(
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

internal class ReportSelectorScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController) : GuiBase(
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

internal class ReportPlayerScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController, private val reporterId: UUID, private val pageIndex: Int) : GuiBase(
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

internal class ReportOfflineScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController, private val reporterId: UUID, private val pageIndex: Int) : GuiBase(
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

internal class ReportReasonScreen(private val plugin: PunisherX, private val controller: PunisherMainGuiController, private val targetId: UUID, private val pageIndex: Int) : GuiBase(
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

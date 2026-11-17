package pl.syntaxdevteam.punisher.books

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.util.UUID

/** Presentation only: report validation and persistence stay in ReportService and ReportsCommand. */
class ReportBookService(private val plugin: PunisherX) {
    private fun message(key: String, values: Map<String, String> = emptyMap()): Component =
        plugin.messageHandler.stringMessageToComponentNoPrefixLiteral("report-book", key, values)

    private fun open(player: Player, build: (BookManager.Menu) -> List<Component>): Unit =
        plugin.bookManager.open(player, message("title"), message("author"), build)

    private fun appendLine(page: Component, line: Component) = page.append(Component.newline()).append(line)
    private fun pageCount(size: Int, perPage: Int) = ((size - 1).coerceAtLeast(0) / perPage) + 1
    private fun name(uuid: UUID) = Bukkit.getOfflinePlayer(uuid).name ?: uuid.toString()
    private fun canRead(player: Player) = PermissionChecker.hasWithSee(player, PermissionChecker.PermissionKey.SEE_REPORTS) || canManage(player)
    private fun canManage(player: Player) = PermissionChecker.hasWithManage(player, PermissionChecker.PermissionKey.MANAGE_REPORTS)

    fun openReport(player: Player, target: OfflinePlayer? = null) {
        if (target != null) { openReasons(player, target); return }
        val now = System.currentTimeMillis()
        val targets = (plugin.server.onlinePlayers.filter { it.uniqueId != player.uniqueId && player.canSee(it) } +
            Bukkit.getOfflinePlayers().filter { !it.isOnline && it.uniqueId != player.uniqueId && it.name?.isNotBlank() == true && it.lastSeen > 0 && now - it.lastSeen <= 3_600_000 })
            .distinctBy { it.uniqueId }.sortedBy { it.name?.lowercase() }
        openTargets(player, targets, 0)
    }

    private fun openTargets(player: Player, targets: List<OfflinePlayer>, page: Int): Unit = open(player) { menu ->
        var body = message("players-heading")
        if (targets.isEmpty()) body = appendLine(body, message("empty"))
        targets.drop(page * 4).take(4).forEach { target ->
            body = appendLine(body, menu.button(message("player-entry", mapOf("target" to (target.name ?: target.uniqueId.toString())))) { openReasons(player, target) })
        }
        if (page > 0) body = appendLine(body, menu.button(message("previous")) { openTargets(player, targets, page - 1) })
        if (page + 1 < pageCount(targets.size, 4)) body = appendLine(body, menu.button(message("next")) { openTargets(player, targets, page + 1) })
        listOf(body)
    }

    private fun reasons() = plugin.config.getStringList("reports.reasons")
        .ifEmpty { plugin.config.getStringList("gui.punish.reasons") }
        .map(String::trim).filter { it.length in 3..255 && it.none(Char::isISOControl) }.distinct()
        .ifEmpty { listOf("Cheating", "Griefing", "Spamming") }

    private fun openReasons(player: Player, target: OfflinePlayer, page: Int = 0): Unit = open(player) { menu ->
        val reasons = reasons()
        var body = message("reasons-heading", mapOf("target" to (target.name ?: target.uniqueId.toString())))
        reasons.drop(page * 3).take(3).forEach { reason ->
            body = appendLine(body, menu.button(message("reason-entry", mapOf("reason" to if (reason.length > 22) reason.take(22) + "…" else reason))) { openConfirmation(player, target, reason) })
        }
        if (page > 0) body = appendLine(body, menu.button(message("previous")) { openReasons(player, target, page - 1) })
        if (page + 1 < pageCount(reasons.size, 3)) body = appendLine(body, menu.button(message("next")) { openReasons(player, target, page + 1) })
        body = appendLine(body, menu.button(message("back")) { openReport(player) })
        listOf(body)
    }

    private fun openConfirmation(player: Player, target: OfflinePlayer, reason: String): Unit = open(player) { menu ->
        val values = mapOf("target" to (target.name ?: target.uniqueId.toString()), "reason" to reason)
        val pages = reason.chunked(90).map { chunk -> message("confirmation", values + ("reason" to chunk)) }.toMutableList()
        var actions = message("confirm-heading")
        actions = appendLine(actions, menu.button(message("submit")) {
            if (!plugin.databaseHandler.isReady()) {
                player.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_not_ready"))
            } else {
                plugin.reportService.submitAndNotify(player, target, reason)
            }
        })
        actions = appendLine(actions, menu.button(message("back")) { openReasons(player, target) })
        pages + actions
    }

    fun openInbox(player: Player, page: Int = 1, closed: Boolean = false) {
        if (!canRead(player) || !plugin.databaseHandler.isReady()) return
        val safePage = page.coerceIn(1, Int.MAX_VALUE / 4)
        val reports = plugin.databaseHandler.getReports(5, (safePage - 1) * 4, closed)
        open(player) { menu ->
            var body = message(if (closed) "history-heading" else "inbox-heading", mapOf("page" to "$safePage"))
            if (reports.isEmpty()) body = appendLine(body, message("empty"))
            reports.take(4).forEach { report ->
                body = appendLine(body, menu.button(message("report-entry", mapOf("id" to "${report.id}", "target" to name(report.suspect).take(16)))) {
                    openDetails(player, report.id)
                })
            }
            if (safePage > 1) body = appendLine(body, menu.button(message("previous")) { openInbox(player, safePage - 1, closed) })
            if (reports.size > 4 && safePage < Int.MAX_VALUE / 4) body = appendLine(body, menu.button(message("next")) { openInbox(player, safePage + 1, closed) })
            body = appendLine(body, menu.button(message(if (closed) "inbox" else "history")) { openInbox(player, 1, !closed) })
            listOf(body)
        }
    }

    fun openDetails(player: Player, id: Int) {
        if (!canRead(player) || !plugin.databaseHandler.isReady()) return
        val report = plugin.databaseHandler.getReports(reportId = id).firstOrNull() ?: return
        open(player) { menu ->
            val values = mapOf("id" to "$id", "reporter" to name(report.player), "target" to name(report.suspect).take(16),
                "filed_at" to report.filedAt.toString(), "status" to report.status,
                "handled_by" to report.handledBy.orEmpty(), "handled_at" to report.handledAt?.toString().orEmpty())
            val pages = mutableListOf(message("details", values), message("filed-at", values))
            pages += report.reason.chunked(90).map { message("reason-page", mapOf("reason" to it)) }
            if (report.status != "OPEN") {
                pages += message("resolution", values)
                pages += report.note.orEmpty().chunked(90).map { message("note-page", mapOf("note" to it)) }
            }
            var actions = message("actions-heading")
            if (report.status == "OPEN" && canManage(player)) {
                actions = appendLine(actions, menu.button(message("resolve")) {
                    if (canManage(player)) player.sendMessage(message("decision-prompt").append(
                        message("resolve").clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand("/reports resolve $id "))))
                })
                actions = appendLine(actions, menu.button(message("reject")) {
                    if (canManage(player)) player.sendMessage(message("decision-prompt").append(
                        message("reject").clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand("/reports reject $id "))))
                })
            }
            actions = appendLine(actions, menu.button(message("back")) { openInbox(player) })
            pages + actions
        }
    }
}

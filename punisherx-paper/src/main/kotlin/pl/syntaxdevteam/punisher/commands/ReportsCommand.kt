package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.gui.report.ReportInboxGUI
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.compatibility.VersionCompatibility
import pl.syntaxdevteam.punisher.dialogs.ReportManagementDialogService
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.util.UUID

/** Staff inbox; all actions also work from the server console. */
class ReportsCommand(private val plugin: PunisherX) : BasicCommand {
    private fun canRead(sender: CommandSender) =
        PermissionChecker.hasWithSee(sender, PermissionChecker.PermissionKey.SEE_REPORTS) || canManage(sender)

    private fun canManage(sender: CommandSender) =
        PermissionChecker.hasWithManage(sender, PermissionChecker.PermissionKey.MANAGE_REPORTS)

    private fun message(sender: CommandSender, key: String, values: Map<String, String> = emptyMap()) =
        sender.sendMessage(plugin.messageHandler.stringMessageToComponent("reports", key, values))

    override fun execute(stack: CommandSourceStack, args: Array<String>) = execute(stack.sender, args)

    fun execute(sender: CommandSender, args: Array<String>) {
        if (!canRead(sender)) {
            sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }
        if (!plugin.databaseHandler.isReady()) {
            sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_not_ready"))
            return
        }
        try {
            when (args.firstOrNull()?.lowercase() ?: if (sender is Player) "gui" else "list") {
                "gui" -> {
                    val page = if (args.size < 2) 1 else args[1].toIntOrNull()
                    if (sender !is Player || args.size > 2 || page == null || page < 1 || page > Int.MAX_VALUE / 45) {
                        message(sender, "admin-usage")
                        return
                    }
                    if (!openDialogInbox(sender, page, false)) ReportInboxGUI(plugin).open(sender, page)
                }
                "list", "history" -> {
                    val page = if (args.size < 2) 1 else args[1].toIntOrNull()
                    val pageSize = plugin.config.getInt("reports.page-size", 10).coerceIn(1, 50)
                    if (args.size > 2 || page == null || page < 1 || page > Int.MAX_VALUE / pageSize) {
                        message(sender, "admin-usage")
                        return
                    }
                    val closed = args.firstOrNull()?.equals("history", true) == true
                    if (sender is Player && openDialogInbox(sender, page, closed)) {
                        return
                    }
                    val reports = plugin.databaseHandler.getReports(pageSize + 1, (page - 1) * pageSize, closed)
                    message(sender, if (closed) "history-title" else "inbox-title", mapOf("page" to "$page"))
                    if (reports.isEmpty()) message(sender, "empty")
                    reports.take(pageSize).forEach { report ->
                        // User-supplied text is a literal Component, never parsed as MiniMessage.
                        sender.sendMessage(Component.text("#${report.id} | ${name(report.player)} → ${name(report.suspect)} | ${report.reason}")
                            .clickEvent(ClickEvent.runCommand("/reports view ${report.id}")))
                    }
                    val command = if (closed) "history" else "list"
                    if (page > 1) sender.sendMessage(Component.text("← /reports $command ${page - 1}")
                        .clickEvent(ClickEvent.runCommand("/reports $command ${page - 1}")))
                    if (reports.size > pageSize) sender.sendMessage(Component.text("/reports $command ${page + 1} →")
                        .clickEvent(ClickEvent.runCommand("/reports $command ${page + 1}")))
                    message(sender, "admin-usage")
                }
                "view", "resolve", "reject" -> {
                    val action = args[0].lowercase()
                    if (action != "view" && !canManage(sender)) {
                        sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
                        return
                    }
                    val id = args.getOrNull(1)?.toIntOrNull()
                    val note = args.drop(2).joinToString(" ").trim()
                    if (id == null || id < 1 || (action == "view" && args.size != 2) ||
                        (action != "view" && (note.length !in 3..255 || note.any { it.isISOControl() }))) {
                        message(sender, "admin-usage")
                        return
                    }
                    val report = plugin.databaseHandler.getReports(reportId = id).firstOrNull()
                    if (report == null) {
                        message(sender, "not-found")
                        return
                    }
                    if (action == "view") {
                        if (sender is Player && openDialogDetails(sender, id)) {
                            return
                        }
                        message(sender, "details-title", mapOf("id" to "$id"))
                        sender.sendMessage(Component.text("${name(report.player)} (${report.player}) → ${name(report.suspect)} (${report.suspect})\n${report.filedAt}\n${report.reason}"))
                        message(sender, "status-${report.status.lowercase()}")
                        if (report.status != "OPEN") {
                            sender.sendMessage(Component.text("${report.handledBy} | ${report.handledAt}\n${report.note}"))
                        } else if (canManage(sender)) {
                            listOf("resolve", "reject").forEach { verb ->
                                sender.sendMessage(Component.text("/reports $verb $id ")
                                    .clickEvent(ClickEvent.suggestCommand("/reports $verb $id ")))
                            }
                        }
                        return
                    }
                    val status = if (action == "resolve") "RESOLVED" else "REJECTED"
                    if (!plugin.databaseHandler.resolveReport(id, status, sender.name, note)) {
                        message(sender, "already-closed")
                        return
                    }
                    message(sender, "handled", mapOf("id" to "$id"))
                }
                else -> message(sender, "admin-usage")
            }
        } catch (exception: Exception) {
            plugin.logger.warning("Report management failed: ${exception.message}")
            sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))
        }
    }

    override fun suggest(stack: CommandSourceStack, args: Array<String>): List<String> {
        if (!canRead(stack.sender) || args.size > 1) return emptyList()
        val actions = mutableListOf("gui", "list", "view", "history")
        if (canManage(stack.sender)) actions.addAll(listOf("resolve", "reject"))
        return actions.filter { it.startsWith(args.firstOrNull().orEmpty(), true) }
    }

    private fun name(uuid: UUID): String = Bukkit.getOfflinePlayer(uuid).name ?: uuid.toString()

    private fun openDialogInbox(player: Player, page: Int, closed: Boolean): Boolean =
        useDialogs() && runCatching { ReportManagementDialogService(plugin).openInbox(player, page, closed) }
            .onFailure { plugin.logger.warning("Could not open report management dialog, using inventory GUI: ${it.message}") }
            .getOrDefault(false)

    private fun openDialogDetails(player: Player, id: Int): Boolean =
        useDialogs() && runCatching { ReportManagementDialogService(plugin).openDetails(player, id) }
            .onFailure { plugin.logger.warning("Could not open report details dialog, using chat output: ${it.message}") }
            .getOrDefault(false)

    private fun useDialogs(): Boolean = plugin.config.getBoolean("reports.admin-use-dialogs", true) &&
        plugin.versionCompatibility.supports(VersionCompatibility.CompatibilityFlag.DIALOGS)
}

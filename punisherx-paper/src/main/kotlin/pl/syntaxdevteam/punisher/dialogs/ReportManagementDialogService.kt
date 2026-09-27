package pl.syntaxdevteam.punisher.dialogs

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput
import io.papermc.paper.registry.data.dialog.input.TextDialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.ReportData
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Native staff report browser available on Paper 1.21.6+. */
@Suppress("UnstableApiUsage")
class ReportManagementDialogService(private val plugin: PunisherX) {
    private companion object {
        private const val REPORT_KEY = "report_id"
        private const val NOTE_KEY = "resolution_note"
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    fun openInbox(player: Player, page: Int = 1, closed: Boolean = false): Boolean {
        if (!canRead(player) || !plugin.databaseHandler.isReady()) return false
        val pageSize = plugin.config.getInt("reports.page-size", 10).coerceIn(1, 50)
        val safePage = page.coerceAtLeast(1)
        val reports = plugin.databaseHandler.getReports(pageSize + 1, (safePage - 1) * pageSize, closed)
        val visible = reports.take(pageSize)
        val values = mapOf("page" to safePage.toString())
        val titleKey = if (closed) "history-title" else "inbox-title"
        val baseBuilder = DialogBase.builder(message(titleKey, values))
            .canCloseWithEscape(true)
            .pause(false)
            .afterAction(DialogBase.DialogAfterAction.NONE)

        if (visible.isEmpty()) {
            baseBuilder.body(listOf(DialogBody.plainMessage(message("empty"), 360)))
        } else {
            baseBuilder.inputs(listOf(DialogInput.singleOption(
                REPORT_KEY,
                message("gui-details"),
                visible.mapIndexed { index, report ->
                    SingleOptionDialogInput.OptionEntry.create(
                        report.id.toString(),
                        Component.text("#${report.id} • ${name(report.suspect)} • ${report.reason.take(80)}"),
                        index == 0
                    )
                }
            ).width(420).build()))
        }

        val actions = mutableListOf<ActionButton>()
        if (visible.isNotEmpty()) {
            actions += button(message("gui-details")) { response ->
                response.getText(REPORT_KEY)?.toIntOrNull()?.let { openDetails(player, it, safePage, closed) }
            }
        }
        if (safePage > 1) actions += button(guiMessage("Nav.previous")) { openInbox(player, safePage - 1, closed) }
        if (reports.size > pageSize) actions += button(guiMessage("Nav.next")) { openInbox(player, safePage + 1, closed) }
        actions += button(message(if (closed) "inbox-title" else "gui-history", values)) {
            openInbox(player, 1, !closed)
        }

        val close = ActionButton.create(message("dialog-cancel"), null, 120, null)
        val dialog = Dialog.create { builder ->
            builder.empty().base(baseBuilder.build()).type(DialogType.multiAction(actions, close, 2))
        }
        player.showDialog(dialog)
        return true
    }

    fun openDetails(player: Player, id: Int, returnPage: Int = 1, closedList: Boolean = false): Boolean {
        if (!canRead(player) || !plugin.databaseHandler.isReady()) return false
        val report = plugin.databaseHandler.getReports(reportId = id).firstOrNull() ?: return false
        val body = Component.text(
            "${name(report.player)} (${report.player}) → ${name(report.suspect)} (${report.suspect})\n" +
                "${TIME_FORMAT.format(report.filedAt.atZone(ZoneId.systemDefault()))}\n\n${report.reason}"
        ).append(Component.newline()).append(message("status-${report.status.lowercase()}"))
        val base = DialogBase.builder(message("details-title", mapOf("id" to id.toString())))
            .canCloseWithEscape(true)
            .pause(false)
            .afterAction(DialogBase.DialogAfterAction.NONE)
            .body(listOf(DialogBody.plainMessage(body, 440)))

        val actions = mutableListOf<ActionButton>()
        if (report.status == "OPEN" && canManage(player)) {
            base.inputs(listOf(DialogInput.text(NOTE_KEY, message("dialog-reason-label"))
                .width(420).maxLength(255)
                .multiline(TextDialogInput.MultilineOptions.create(3, 80)).build()))
            actions += decisionButton(player, report, "RESOLVED", "status-resolved")
            actions += decisionButton(player, report, "REJECTED", "status-rejected")
        } else if (report.status != "OPEN") {
            val resolution = Component.text("${report.handledBy} • ${report.handledAt}\n${report.note.orEmpty()}")
            base.body(listOf(DialogBody.plainMessage(body, 440), DialogBody.plainMessage(resolution, 440)))
        }
        actions += button(guiMessage("Nav.back")) { openInbox(player, returnPage, closedList) }
        val close = ActionButton.create(message("dialog-cancel"), null, 120, null)
        val dialog = Dialog.create { builder ->
            builder.empty().base(base.build()).type(DialogType.multiAction(actions, close, 2))
        }
        player.showDialog(dialog)
        return true
    }

    private fun decisionButton(player: Player, report: ReportData, status: String, labelKey: String): ActionButton =
        button(message(labelKey)) { response ->
            if (!canManage(player)) return@button
            val note = response.getText(NOTE_KEY)?.trim().orEmpty()
            if (note.length !in 3..255 || note.any(Char::isISOControl)) {
                player.sendMessage(plugin.messageHandler.stringMessageToComponent("reports", "invalid-reason"))
                openDetails(player, report.id)
                return@button
            }
            if (plugin.databaseHandler.resolveReport(report.id, status, player.name, note)) {
                player.sendMessage(plugin.messageHandler.stringMessageToComponent("reports", "handled", mapOf("id" to report.id.toString())))
                openInbox(player)
            } else {
                player.sendMessage(plugin.messageHandler.stringMessageToComponent("reports", "already-closed"))
                openInbox(player)
            }
        }

    private fun button(label: Component, callback: (io.papermc.paper.dialog.DialogResponseView) -> Unit): ActionButton =
        ActionButton.create(label, null, 180, DialogAction.customClick(
            { response, _ -> callback(response) },
            ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(10)).build()
        ))

    private fun canRead(player: Player) = PermissionChecker.hasWithSee(player, PermissionChecker.PermissionKey.SEE_REPORTS) || canManage(player)
    private fun canManage(player: Player) = PermissionChecker.hasWithManage(player, PermissionChecker.PermissionKey.MANAGE_REPORTS)
    private fun name(uuid: UUID) = Bukkit.getOfflinePlayer(uuid).name ?: uuid.toString()
    private fun message(key: String, values: Map<String, String> = emptyMap()) =
        plugin.messageHandler.stringMessageToComponentNoPrefix("reports", key, values)
    private fun guiMessage(key: String) = plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", key)
}

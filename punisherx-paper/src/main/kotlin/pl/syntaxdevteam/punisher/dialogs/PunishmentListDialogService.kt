package pl.syntaxdevteam.punisher.dialogs

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.PunishmentData
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Date

/** Shared native dialog renderer for punishment tables on Paper 1.21.7+. */
@Suppress("UnstableApiUsage")
class PunishmentListDialogService(private val plugin: PunisherX) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    fun open(
        player: Player,
        title: Component,
        subtitle: Component,
        entries: List<PunishmentData>,
        page: Int,
        hasNext: Boolean,
        loadPage: (Int) -> Unit
    ) {
        val safePage = page.coerceAtLeast(1)
        val actions = entries.map { punishment ->
            button(
                Component.text("#${punishment.id} • ${punishment.name} • ${punishment.type}"),
                Component.text(punishment.reason.take(120))
            ) { openDetails(player, title, punishment) { loadPage(safePage) } }
        }.toMutableList()

        if (safePage > 1) actions += button(gui("Nav.previous")) { loadPage(safePage - 1) }
        if (hasNext) actions += button(gui("Nav.next")) { loadPage(safePage + 1) }

        val body = if (entries.isEmpty()) {
            plugin.messageHandler.stringMessageToComponentNoPrefix("error", "no_data")
        } else subtitle
        val base = DialogBase.builder(title)
            .canCloseWithEscape(true).pause(false)
            .afterAction(DialogBase.DialogAfterAction.NONE)
            .body(listOf(DialogBody.plainMessage(body, 440))).build()
        val close = ActionButton.create(gui("Nav.back"), null, 140, null)
        val type = if (actions.isEmpty()) DialogType.notice(close) else DialogType.multiAction(actions, close, 2)
        player.showDialog(Dialog.create { it.empty().base(base).type(type) })
    }

    private fun openDetails(player: Player, title: Component, data: PunishmentData, back: () -> Unit) {
        val end = if (data.end == -1L) "∞" else dateFormat.format(Date(data.end))
        val details = Component.text(
            "${data.name} (${data.uuid})\n#${data.id} • ${data.type}\n\n${data.reason}\n\n" +
                "${dateFormat.format(Date(data.start))} → $end\n${data.operator} • ${data.server}"
        )
        val base = DialogBase.builder(title).canCloseWithEscape(true).pause(false)
            .afterAction(DialogBase.DialogAfterAction.NONE)
            .body(listOf(DialogBody.plainMessage(details, 440))).build()
        val backButton = button(gui("Nav.back")) { back() }
        player.showDialog(Dialog.create { it.empty().base(base).type(DialogType.notice(backButton)) })
    }

    private fun button(label: Component, tooltip: Component? = null, callback: () -> Unit): ActionButton =
        ActionButton.create(label, tooltip, 210, DialogAction.customClick(
            { _, _ -> callback() },
            ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(10)).build()
        ))

    private fun gui(key: String) = plugin.messageHandler.stringMessageToComponentNoPrefix("GUI", key)
}

package pl.syntaxdevteam.punisher.dialogs

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import java.time.Duration

/**
 * Native report form available on Paper 1.21.6 and newer.
 */
@Suppress("UnstableApiUsage")
class ReportDialogService(private val plugin: PunisherX) {
    private companion object {
        private const val TARGET_KEY = "report_target"
        private const val REASON_KEY = "report_reason"
        private const val RECENT_PLAYER_WINDOW_MS = 60 * 60 * 1000L
    }

    fun open(player: Player, preselectedTarget: OfflinePlayer? = null): Boolean {
        val targets = if (preselectedTarget == null) reportablePlayers(player) else listOf(preselectedTarget)
        if (targets.isEmpty()) {
            player.sendMessage(plugin.messageHandler.stringMessageToComponent("reports", "no-targets"))
            return false
        }

        val configuredReasons = plugin.config.getStringList("reports.reasons")
            .ifEmpty { plugin.config.getStringList("gui.punish.reasons") }
            .ifEmpty { listOf("Cheating", "Griefing", "Spamming") }
        val reasons = configuredReasons
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .ifEmpty { listOf("Cheating", "Griefing", "Spamming") }

        val targetById = targets.associateBy { it.uniqueId.toString() }
        val reasonById = reasons.mapIndexed { index, reason -> "reason_$index" to reason }.toMap()
        val inputs = buildList {
            if (preselectedTarget == null) {
                add(
                    DialogInput.singleOption(
                        TARGET_KEY,
                        plugin.messageHandler.stringMessageToComponentNoPrefix(
                            "reports",
                            "dialog-target-label"
                        ),
                        targets.mapIndexed { index, target ->
                            SingleOptionDialogInput.OptionEntry.create(
                                target.uniqueId.toString(),
                                Component.text(target.name ?: target.uniqueId.toString()),
                                index == 0
                            )
                        }
                    ).width(320).build()
                )
            }
            add(
                DialogInput.singleOption(
                    REASON_KEY,
                    plugin.messageHandler.stringMessageToComponentNoPrefix(
                        "reports",
                        "dialog-reason-label"
                    ),
                    reasonById.entries.mapIndexed { index, (id, reason) ->
                        SingleOptionDialogInput.OptionEntry.create(
                            id,
                            Component.text(reason),
                            index == 0
                        )
                    }
                ).width(320).build()
            )
        }

        val callback = DialogAction.customClick(
            { response, _ ->
                val target = preselectedTarget
                    ?: response.getText(TARGET_KEY)?.let(targetById::get)
                val reason = response.getText(REASON_KEY)?.let(reasonById::get)
                if (target == null || reason == null) {
                    player.sendMessage(
                        plugin.messageHandler.stringMessageToComponent("reports", "invalid-form")
                    )
                    return@customClick
                }
                plugin.reportService.submitAndNotify(player, target, reason)
            },
            ClickCallback.Options.builder()
                .uses(1)
                .lifetime(Duration.ofMinutes(5))
                .build()
        )

        val submit = ActionButton.create(
            plugin.messageHandler.stringMessageToComponentNoPrefix("reports", "dialog-submit"),
            null,
            150,
            callback
        )
        val cancel = ActionButton.create(
            plugin.messageHandler.stringMessageToComponentNoPrefix("reports", "dialog-cancel"),
            null,
            150,
            null
        )
        val targetName = preselectedTarget?.name ?: preselectedTarget?.uniqueId?.toString()
        val bodyText = if (targetName == null) {
            plugin.messageHandler.stringMessageToComponentNoPrefix("reports", "dialog-body")
        } else {
            plugin.messageHandler.stringMessageToComponentNoPrefix(
                "reports",
                "dialog-body-target",
                mapOf("target" to targetName)
            )
        }
        val base = DialogBase.builder(
            plugin.messageHandler.stringMessageToComponentNoPrefix("reports", "dialog-title")
        )
            .canCloseWithEscape(true)
            .pause(false)
            .afterAction(DialogBase.DialogAfterAction.CLOSE)
            .body(listOf(DialogBody.plainMessage(bodyText, 360)))
            .inputs(inputs)
            .build()
        val dialog = Dialog.create { builder ->
            builder.empty().base(base).type(DialogType.confirmation(submit, cancel))
        }

        player.showDialog(dialog)
        return true
    }

    private fun reportablePlayers(reporter: Player): List<OfflinePlayer> {
        val now = System.currentTimeMillis()
        val online = plugin.server.onlinePlayers
            .asSequence()
            .filter { it.uniqueId != reporter.uniqueId }
            .filter(reporter::canSee)
        val recentlyOffline = Bukkit.getOfflinePlayers()
            .asSequence()
            .filter { it.uniqueId != reporter.uniqueId && !it.isOnline }
            .filter { it.name?.isNotBlank() == true }
            .filter { it.lastSeen > 0L && now - it.lastSeen <= RECENT_PLAYER_WINDOW_MS }

        return (online + recentlyOffline)
            .distinctBy { it.uniqueId }
            .sortedBy { it.name?.lowercase() }
            .take(100)
            .toList()
    }
}

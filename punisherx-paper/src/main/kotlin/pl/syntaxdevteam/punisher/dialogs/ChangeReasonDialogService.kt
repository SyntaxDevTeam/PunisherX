package pl.syntaxdevteam.punisher.dialogs

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.TextDialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.time.Duration

/**
 * Native Paper form for editing punishment reasons on 1.21.6+.
 */
@Suppress("UnstableApiUsage")
class ChangeReasonDialogService(private val plugin: PunisherX) {
    private companion object {
        private const val ID_KEY = "punishment_id"
        private const val REASON_KEY = "new_reason"
    }

    fun open(player: Player, preselectedId: Int? = null) {
        val callback = DialogAction.customClick(
            { response, _ ->
                if (!PermissionChecker.hasWithLegacy(
                        player,
                        PermissionChecker.PermissionKey.CHANGE_REASON
                    )
                ) {
                    player.sendMessage(
                        plugin.messageHandler.stringMessageToComponent("error", "no_permission")
                    )
                    return@customClick
                }

                val id = response.getText(ID_KEY)?.trim()?.toIntOrNull()
                val newReason = response.getText(REASON_KEY)?.trim().orEmpty()
                if (id == null) {
                    player.sendMessage(
                        plugin.messageHandler.stringMessageToComponent("change-reason", "invalid_id")
                    )
                    return@customClick
                }
                if (newReason.length !in 3..255) {
                    player.sendMessage(
                        plugin.messageHandler.stringMessageToComponent("change-reason", "invalid_reason")
                    )
                    return@customClick
                }

                sendResult(player, id, newReason)
            },
            ClickCallback.Options.builder()
                .uses(1)
                .lifetime(Duration.ofMinutes(5))
                .build()
        )
        val submit = ActionButton.create(
            plugin.messageHandler.stringMessageToComponentNoPrefix(
                "change-reason",
                "dialog_submit"
            ),
            null,
            150,
            callback
        )
        val cancel = ActionButton.create(
            plugin.messageHandler.stringMessageToComponentNoPrefix(
                "change-reason",
                "dialog_cancel"
            ),
            null,
            150,
            null
        )
        val inputs = listOf(
            DialogInput.text(
                ID_KEY,
                plugin.messageHandler.stringMessageToComponentNoPrefix(
                    "change-reason",
                    "dialog_id_label"
                )
            )
                .width(320)
                .initial(preselectedId?.toString().orEmpty())
                .maxLength(10)
                .build(),
            DialogInput.text(
                REASON_KEY,
                plugin.messageHandler.stringMessageToComponentNoPrefix(
                    "change-reason",
                    "dialog_reason_label"
                )
            )
                .width(320)
                .maxLength(255)
                .multiline(TextDialogInput.MultilineOptions.create(4, 80))
                .build()
        )
        val base = DialogBase.builder(
            plugin.messageHandler.stringMessageToComponentNoPrefix(
                "change-reason",
                "dialog_title"
            )
        )
            .canCloseWithEscape(true)
            .pause(false)
            .afterAction(DialogBase.DialogAfterAction.CLOSE)
            .body(
                listOf(
                    DialogBody.plainMessage(
                        plugin.messageHandler.stringMessageToComponentNoPrefix(
                            "change-reason",
                            "dialog_body"
                        ),
                        360
                    )
                )
            )
            .inputs(inputs)
            .build()
        val dialog = Dialog.create { builder ->
            builder.empty().base(base).type(DialogType.confirmation(submit, cancel))
        }
        player.showDialog(dialog)
    }

    private fun sendResult(player: Player, id: Int, newReason: String) {
        val placeholders = mapOf("id" to id.toString(), "reason" to newReason)
        val key = if (plugin.databaseHandler.updatePunishmentReason(id, newReason)) {
            "success"
        } else {
            "failure"
        }
        player.sendMessage(
            plugin.messageHandler.stringMessageToComponent("change-reason", key, placeholders)
        )
    }
}

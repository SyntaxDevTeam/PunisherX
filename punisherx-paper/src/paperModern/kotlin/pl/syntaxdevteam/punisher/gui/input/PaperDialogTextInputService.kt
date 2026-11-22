package pl.syntaxdevteam.punisher.gui.input

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.TextDialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX

/**
 * Kept in a separate class so pre-1.21.7 Paper servers never have to resolve Dialog API classes.
 */
class PaperDialogTextInputService(private val plugin: PunisherX) : GuiTextInputService {

    override fun request(
        player: Player,
        title: Component,
        initialValue: String,
        maxLength: Int,
        multiline: Boolean,
        onSubmit: (Player, String) -> Unit,
        onCancel: (Player) -> Unit
    ) {
        val inputBuilder = DialogInput.text("value", Component.text("Value"))
            .width(300)
            .initial(initialValue)
            .maxLength(maxLength)

        if (multiline) {
            inputBuilder.multiline(TextDialogInput.MultilineOptions.create(4, 80))
        }

        val input = inputBuilder.build()
        val callbackOptions = ClickCallback.Options.builder().uses(1).build()

        val save = ActionButton.builder(Component.text("Save", NamedTextColor.GREEN))
            .width(100)
            .action(
                DialogAction.customClick(
                    { response, audience ->
                        if (audience is Player) {
                            val value = response.getText("value").orEmpty()
                            plugin.schedulerAdapter.runForPlayer(audience, Runnable {
                                onSubmit(audience, value)
                            })
                        }
                    },
                    callbackOptions
                )
            )
            .build()

        val cancel = ActionButton.builder(Component.text("Cancel", NamedTextColor.RED))
            .width(100)
            .action(
                DialogAction.customClick(
                    { _, audience ->
                        if (audience is Player) {
                            plugin.schedulerAdapter.runForPlayer(audience, Runnable {
                                onCancel(audience)
                            })
                        }
                    },
                    ClickCallback.Options.builder().uses(1).build()
                )
            )
            .build()

        val dialog = Dialog.create { builder ->
            builder.empty()
                .base(
                    DialogBase.builder(title)
                        .canCloseWithEscape(true)
                        .inputs(listOf(input))
                        .build()
                )
                .type(DialogType.confirmation(save, cancel))
        }

        player.showDialog(dialog)
    }
}

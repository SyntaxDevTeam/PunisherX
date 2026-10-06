package pl.syntaxdevteam.punisher.gui.input

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX

interface GuiTextInputService {
    fun request(
        player: Player,
        title: Component,
        initialValue: String = "",
        maxLength: Int = 128,
        multiline: Boolean = false,
        onSubmit: (Player, String) -> Unit,
        onCancel: (Player) -> Unit = {}
    )
}

object GuiTextInputServices {
    fun create(plugin: PunisherX): GuiTextInputService = ChatGuiTextInputService(plugin)

    private class ChatGuiTextInputService(private val plugin: PunisherX) : GuiTextInputService {
        override fun request(
            player: Player,
            title: Component,
            initialValue: String,
            maxLength: Int,
            multiline: Boolean,
            onSubmit: (Player, String) -> Unit,
            onCancel: (Player) -> Unit
        ) {
            plugin.guiSearchService.request(
                player,
                "<yellow>Enter the new value in chat or type <white>cancel</white>.</yellow>",
                onCancel
            ) { clicker, text ->
                onSubmit(clicker, text)
            }
        }
    }
}

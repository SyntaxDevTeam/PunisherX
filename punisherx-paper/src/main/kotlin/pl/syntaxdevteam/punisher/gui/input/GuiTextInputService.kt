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
    fun create(plugin: PunisherX): GuiTextInputService {
        val supportsDialog = runCatching { plugin.versionChecker.isAtLeast("1.21.7") }.getOrDefault(false) &&
            hasDialogApi(plugin)

        if (supportsDialog) {
            val dialogService = runCatching {
                val clazz = Class.forName(
                    "pl.syntaxdevteam.punisher.gui.input.PaperDialogTextInputService",
                    true,
                    plugin.javaClass.classLoader
                )
                clazz.getDeclaredConstructor(PunisherX::class.java)
                    .newInstance(plugin) as GuiTextInputService
            }.onFailure {
                plugin.logger.warning("Unable to initialize Minecraft Dialog text input; falling back to chat: ${it.message}")
            }.getOrNull()

            if (dialogService != null) return dialogService
        }

        return ChatGuiTextInputService(plugin)
    }

    private fun hasDialogApi(plugin: PunisherX): Boolean = runCatching {
        Class.forName("io.papermc.paper.dialog.Dialog", false, plugin.javaClass.classLoader)
    }.isSuccess

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

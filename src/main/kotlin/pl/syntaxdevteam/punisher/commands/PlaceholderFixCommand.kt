package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.io.File
import java.util.Locale

/**
 * Command that converts legacy placeholders formatted with curly braces (e.g. {player})
 * to the new angle bracket style (<player>) in the language file that matches the
 * language selected in the plugin configuration.
 */
class PlaceholderFixCommand(private val plugin: PunisherX) : BasicCommand {
    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.PUNISHERX_COMMAND)) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
            return
        }

        val lang = plugin.config.getString("language")?.lowercase(Locale.getDefault()) ?: "en"
        val langDir = File(plugin.dataFolder, "lang")
        val candidates = listOf(
            File(langDir, "messages_$lang.yml"),
            File(langDir, "message_$lang.yml")
        )
        val langFile = candidates.firstOrNull { it.exists() }

        if (langFile == null) {
            stack.sender.sendMessage("Language file for $lang not found.")
            return
        }
        val prefix = plugin.messageHandler.getPrefix()
        try {
            val content = langFile.readText(Charsets.UTF_8)
            val updated = content.replace(Regex("\\{(\\w+)}"), "<$1>")
            langFile.writeText(updated, Charsets.UTF_8)

            stack.sender.sendMessage(plugin.messageHandler.formatMixedTextToMiniMessage("$prefix Converted placeholders in ${langFile.name}."))
        } catch (e: Exception) {
            stack.sender.sendMessage(plugin.messageHandler.formatMixedTextToMiniMessage("$prefix Failed to convert placeholders: ${e.message}"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> = emptyList()
}
package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.io.File
import java.util.Locale

/**
 * Command that converts legacy placeholders formatted with curly braces (e.g. {player})
 * to the new angle bracket style (<player>) in the language file that matches the
 * language selected in the plugin configuration.
 */
class PlaceholderFixCommand(private val plugin: PunisherX) : BrigadierCommand {
    override fun execute(stack: CommandSourceStack, args: List<String>) {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.PUNISHERX_COMMAND)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
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

    override fun suggest(stack: CommandSourceStack, args: List<String>): List<String> = emptyList()

    override fun build(name: String): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresPermission(PermissionChecker.PermissionKey.PUNISHERX_COMMAND))
            .executes { context ->
                execute(context.source, emptyList())
                1
            }
            .build()
    }
}

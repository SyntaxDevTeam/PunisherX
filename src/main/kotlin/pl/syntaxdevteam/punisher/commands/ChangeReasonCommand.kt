package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.configuration.PluginMeta
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.common.MessageHandler

@Suppress("UnstableApiUsage")
class ChangeReasonCommand(private val plugin: PunisherX, pluginMetas: PluginMeta) : BasicCommand {

    private val messageHandler = MessageHandler(plugin, pluginMetas)

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (args.isNotEmpty()) {
            if (stack.sender.hasPermission("punisherx.change_reason")) {
                if (args.size < 2) {
                    stack.sender.sendRichMessage(messageHandler.getMessage("change-reason", "usage"))
                    return
                }
                val id = args[0].toIntOrNull()
                val newReason = args.drop(1).joinToString(" ")
                if (id == null) {
                    stack.sender.sendRichMessage(messageHandler.getMessage("change-reason", "invalid_id"))
                    return
                }
                val success = plugin.databaseHandler.updatePunishmentReason(id, newReason)
                if (success) {
                    stack.sender.sendRichMessage(
                        messageHandler.getMessage(
                            "change-reason",
                            "success",
                            mapOf("id" to id.toString(), "reason" to newReason)
                        )
                    )
                } else {
                    stack.sender.sendRichMessage(
                        messageHandler.getMessage(
                            "change-reason",
                            "failure",
                            mapOf("id" to id.toString())
                        )
                    )
                }
            } else {
                stack.sender.sendRichMessage(messageHandler.getMessage("error", "no_permission"))
            }
        } else {
            stack.sender.sendRichMessage(messageHandler.getMessage("ban", "usage"))
        }
    }
}

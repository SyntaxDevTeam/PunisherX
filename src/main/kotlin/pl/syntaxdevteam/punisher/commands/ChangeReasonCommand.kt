package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class ChangeReasonCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.CHANGE_REASON)) {
            if (args.isNotEmpty()) {
                if (args.size < 2) {
                    stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("change-reason", "usage"))
                    return
                }
                val id = args[0].toIntOrNull()
                val newReason = args.drop(1).joinToString(" ")
                if (id == null) {
                    stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("change-reason", "invalid_id"))
                    return
                }
                val success = plugin.databaseHandler.updatePunishmentReason(id, newReason)
                if (success) {
                    stack.sender.sendMessage(
                        plugin.messageHandler.stringMessageToComponent(
                            "change-reason",
                            "success",
                            mapOf("id" to id.toString(), "reason" to newReason)
                        )
                    )
                } else {
                    stack.sender.sendMessage(
                        plugin.messageHandler.stringMessageToComponent(
                            "change-reason",
                            "failure",
                            mapOf("id" to id.toString())
                        )
                    )
                }
            } else {
                stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("ban", "usage"))
            }
        } else {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
        }

    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.CHANGE_REASON)) {
            return emptyList()
        }
        return when (args.size) {
            0, 1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> TimeSuggestionProvider.generateTimeSuggestions()
            else -> emptyList()
        }
    }
}

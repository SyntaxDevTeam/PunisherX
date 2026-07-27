package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.jetbrains.annotations.NotNull
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.compatibility.VersionCompatibility
import pl.syntaxdevteam.punisher.dialogs.ChangeReasonDialogService
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class ChangeReasonCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.CHANGE_REASON)) {
            val useDialog = stack.sender is Player &&
                args.size <= 1 &&
                plugin.config.getBoolean("dialogs.use-change-reason", true) &&
                plugin.versionCompatibility.supports(VersionCompatibility.CompatibilityFlag.DIALOGS)
            if (useDialog) {
                val preselectedId = args.firstOrNull()?.toIntOrNull()
                if (args.isNotEmpty() && preselectedId == null) {
                    stack.sender.sendMessage(
                        plugin.messageHandler.stringMessageToComponent("change-reason", "invalid_id")
                    )
                    return
                }
                val opened = runCatching {
                    ChangeReasonDialogService(plugin).open(stack.sender as Player, preselectedId)
                }.onFailure {
                    plugin.logger.warning("Could not open change-reason dialog: ${it.message}")
                }.isSuccess
                if (opened) return
            }

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
                if (newReason.length !in 3..255) {
                    stack.sender.sendMessage(
                        plugin.messageHandler.stringMessageToComponent("change-reason", "invalid_reason")
                    )
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
                stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("change-reason", "usage"))
            }
        } else {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
        }

    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.CHANGE_REASON)) {
            return emptyList()
        }
        val input = args.lastOrNull().orEmpty()
        return when (args.size) {
            0, 1 -> plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(input, ignoreCase = true) }
            2 -> TimeSuggestionProvider.generateTimeSuggestions().filter { it.startsWith(input, ignoreCase = true) }
            else -> emptyList()
        }
    }
}

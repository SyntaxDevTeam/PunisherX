package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class ChangeReasonCommand(private val plugin: PunisherX) : BrigadierCommand {

    override fun execute(stack: CommandSourceStack, args: List<String>) {
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

    override fun suggest(stack: CommandSourceStack, args: List<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.CHANGE_REASON)) {
            return emptyList()
        }
        return when (args.size) {
            0, 1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> TimeSuggestionProvider.generateTimeSuggestions()
            else -> emptyList()
        }
    }

    override fun build(name: String): LiteralCommandNode<CommandSourceStack> {
        val reasonArg = Commands.argument("reason", StringArgumentType.greedyString())
            .executes { context ->
                val id = IntegerArgumentType.getInteger(context, "id")
                val reason = StringArgumentType.getString(context, "reason")
                execute(context.source, BrigadierCommandUtils.greedyArgs(listOf(id.toString()), reason))
                1
            }

        val idArg = Commands.argument("id", IntegerArgumentType.integer(1))
            .executes { context ->
                val id = IntegerArgumentType.getInteger(context, "id")
                execute(context.source, listOf(id.toString()))
                1
            }
            .then(reasonArg)

        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresPermission(PermissionChecker.PermissionKey.CHANGE_REASON))
            .executes { context ->
                execute(context.source, emptyList())
                1
            }
            .then(idArg)
            .build()
    }
}

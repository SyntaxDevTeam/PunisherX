package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class UnWarnCommand(private val plugin: PunisherX) : BrigadierCommand {

    override fun execute(stack: CommandSourceStack, args: List<String>) {
        if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNWARN)) {
            if (args.isNotEmpty()) {
                val player = args[0]
                val uuid = plugin.resolvePlayerUuid(player).toString()
                val punishments = plugin.databaseHandler.getPunishments(uuid)
                val warnPunishments = punishments.filter { it.type == "WARN" }
                if (warnPunishments.isNotEmpty()) {
                    plugin.databaseHandler.removePunishment(uuid, "WARN")
                    stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("unwarn", "unwarn", mapOf("player" to player)))
                    plugin.logger.info("Player $player ($uuid) has been unwarned")
                } else {
                    stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "player_not_found", mapOf("player" to player)))
                }
            } else {
                stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("unwarn", "usage"))
            }
        } else {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
        }
    }

    override fun suggest(stack: CommandSourceStack, args: List<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNWARN)) {
            return emptyList()
        }
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            else -> emptyList()
        }
    }

    override fun build(name: String): LiteralCommandNode<CommandSourceStack> {
        val playerArg = Commands.argument("player", StringArgumentType.word())
            .suggests(BrigadierCommandUtils.suggestions(this) { emptyList() })
            .executes { context ->
                val player = StringArgumentType.getString(context, "player")
                execute(context.source, listOf(player))
                1
            }

        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresPermission(PermissionChecker.PermissionKey.UNWARN))
            .executes { context ->
                execute(context.source, emptyList())
                1
            }
            .then(playerArg)
            .build()
    }
}

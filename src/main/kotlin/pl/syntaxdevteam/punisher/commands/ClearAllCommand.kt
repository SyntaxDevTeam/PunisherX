package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Bukkit
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class ClearAllCommand(private val plugin: PunisherX) : BrigadierCommand {

    override fun execute(stack: CommandSourceStack, args: List<String>) {
        if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.CLEAR_ALL)) {
            if (args.isNotEmpty()) {
                val player = args[0]
                val uuid = plugin.resolvePlayerUuid(player).toString()
                val punishments = plugin.databaseHandler.getPunishments(uuid)
                if (punishments.isNotEmpty()) {
                    punishments.forEach { punishment ->
                        if (punishment.type == "MUTE" || punishment.type == "BAN" || punishment.type == "WARN") {
                            plugin.databaseHandler.removePunishment(uuid, punishment.type, true)
                        }
                    }
                    stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("clear", "clearall", mapOf("player" to player)))
                    val targetPlayer = Bukkit.getPlayer(player)
                    val getMessage = plugin.messageHandler.stringMessageToComponent("clear", "clear_message")
                    targetPlayer?.sendMessage(getMessage)
                    plugin.logger.success("Player $player ($uuid) has been cleared of all punishments")
                } else {
                    stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "player_not_found", mapOf("player" to player)))
                }
            } else {
                stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("clear", "usage"))
            }
        } else {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
        }
    }

    override fun suggest(stack: CommandSourceStack, args: List<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.CLEAR_ALL)) {
            return emptyList()
        }
        return when (args.size) {
            0, 1 -> plugin.server.onlinePlayers.map { it.name }
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
            .requires(BrigadierCommandUtils.requiresPermission(PermissionChecker.PermissionKey.CLEAR_ALL))
            .executes { context ->
                execute(context.source, emptyList())
                1
            }
            .then(playerArg)
            .build()
    }
}

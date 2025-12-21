package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import org.bukkit.Bukkit
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class UnMuteCommand(private val plugin: PunisherX) : BrigadierCommand {

    override fun execute(stack: CommandSourceStack, args: List<String>) {
        if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNMUTE)) {
            if (args.isNotEmpty()) {
                val player = args[0]
                val uuid = plugin.resolvePlayerUuid(player).toString()
                val punishments = plugin.databaseHandler.getPunishments(uuid)
                if (punishments.isNotEmpty()) {
                    punishments.forEach { punishment ->
                        if (punishment.type == "MUTE") {
                            plugin.databaseHandler.removePunishment(uuid, punishment.type)
                        }
                    }
                    stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("unmute", "unmute", mapOf("player" to player)))
                    val targetPlayer = Bukkit.getPlayer(player)
                    val muteMessage = plugin.messageHandler.stringMessageToComponent("unmute", "unmute_message")
                    targetPlayer?.sendMessage(muteMessage)
                    plugin.logger.info("Player $player ($uuid) has been unmuted")
                } else {
                    stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "player_not_found", mapOf("player" to player)))
                }
            } else {
                stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("unmute", "usage"))
            }
        } else {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
        }
    }

    override fun suggest(stack: CommandSourceStack, args: List<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNMUTE)) {
            return emptyList()
        }
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            else -> emptyList()
        }
    }

    override fun build(name: String): LiteralCommandNode<CommandSourceStack> {
        val playerArg = Commands.argument("player", ArgumentTypes.playerProfiles())
            .executes { context ->
                BrigadierCommandUtils.resolvePlayerProfileNames(context, "player").forEach { player ->
                    execute(context.source, listOf(player))
                }
                1
            }

        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresPermission(PermissionChecker.PermissionKey.UNMUTE))
            .executes { context ->
                execute(context.source, emptyList())
                1
            }
            .then(playerArg)
            .build()
    }
}

package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.common.PunishmentCommandUtils
import pl.syntaxdevteam.punisher.commands.arguments.ReasonArgumentType
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class KickCommand(private val plugin: PunisherX) : BrigadierCommand {

    override fun execute(stack: CommandSourceStack, args: List<String>) {
        executeLegacy(stack, args)
    }

    private fun executeLegacy(stack: CommandSourceStack, args: List<String>) {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.KICK)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }

        if (args.isEmpty()) {
            sendUsage(stack)
            return
        }

        val targetArg = args[0]
        val isForce = args.contains("--force")
        val reason = args.slice(1 until args.size).filterNot { it == "--force" }.joinToString(" ")
        if (targetArg.equals("all", ignoreCase = true)) {
            executeKickAll(stack, reason, isForce)
            return
        }

        val uuid = plugin.resolvePlayerUuid(targetArg)
        val targetPlayer = Bukkit.getPlayer(uuid)
        executeKickResolved(stack, targetArg, uuid, targetPlayer, reason, isForce)
    }

    private fun executeKickAll(stack: CommandSourceStack, reason: String, isForce: Boolean) {
        val history: Boolean = plugin.config.getBoolean("kick.history", false)
        val punishmentType = "KICK"
        val start = System.currentTimeMillis()
        val formattedTime = plugin.timeHandler.formatTime(null)

        Bukkit.getOnlinePlayers().forEach { target ->
            if (target.name == stack.sender.name) return@forEach

            val uuid = target.uniqueId
            if (!isForce && PermissionChecker.hasWithBypass(target, PermissionChecker.PermissionKey.BYPASS_KICK)) {
                stack.sender.sendMessage(
                    plugin.messageHandler.stringMessageToComponent(
                        "error", "bypass", mapOf("player" to target.name)
                    )
                )
                return@forEach
            }
            val prefix = plugin.messageHandler.getPrefix()
            if (PermissionChecker.isAuthor(uuid)) {
                stack.sender.sendMessage(plugin.messageHandler.formatMixedTextToMiniMessage("$prefix <red>You can't punish the plugin author</red>",
                    TagResolver.empty()))
                return@forEach
            }

            if (history) {
                plugin.databaseHandler.addPunishmentHistory(
                    target.name,
                    uuid.toString(),
                    reason,
                    stack.sender.name,
                    punishmentType,
                    start,
                    start
                )
            }

            val placeholders = PunishmentCommandUtils.buildPlaceholders(
                player = target.name,
                operator = stack.sender.name,
                reason = reason,
                time = formattedTime,
                type = punishmentType
            )
            PunishmentCommandUtils.sendKickMessage(plugin, target, "kick", "kick_message", placeholders)
            PunishmentCommandUtils.sendSenderMessages(plugin, stack, "kick", "kick", placeholders)
            plugin.actionExecutor.executeAction("kicked", target.name, placeholders)
        }

        val allPlaceholders = PunishmentCommandUtils.buildPlaceholders(
            player = "all",
            operator = stack.sender.name,
            reason = reason,
            time = formattedTime,
            type = punishmentType
        )
        PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_KICK, "kick", "broadcast", allPlaceholders)
    }

    private fun executeKickResolved(
        stack: CommandSourceStack,
        targetName: String,
        uuid: java.util.UUID,
        targetPlayer: Player?,
        reason: String,
        isForce: Boolean
    ) {
        val history: Boolean = plugin.config.getBoolean("kick.history", false)
        val punishmentType = "KICK"
        val start = System.currentTimeMillis()
        val formattedTime = plugin.timeHandler.formatTime(null)

        if (targetPlayer != null && !isForce && PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_KICK)) {
            stack.sender.sendMessage(
                plugin.messageHandler.stringMessageToComponent(
                    "error", "bypass", mapOf("player" to targetName)
                )
            )
            return
        }
        val prefix = plugin.messageHandler.getPrefix()
        if (PermissionChecker.isAuthor(uuid)) {
            stack.sender.sendMessage(
                plugin.messageHandler.formatMixedTextToMiniMessage("$prefix <red>You can't punish the plugin author</red>",
                    TagResolver.empty())
            )
            return
        }

        if (history) {
            plugin.databaseHandler.addPunishmentHistory(
                targetName,
                uuid.toString(),
                reason,
                stack.sender.name,
                punishmentType,
                start,
                start
            )
        }

        val placeholders = PunishmentCommandUtils.buildPlaceholders(
            player = targetName,
            operator = stack.sender.name,
            reason = reason,
            time = formattedTime,
            type = punishmentType
        )
        PunishmentCommandUtils.sendKickMessage(plugin, targetPlayer, "kick", "kick_message", placeholders)
        PunishmentCommandUtils.sendSenderMessages(plugin, stack, "kick", "kick", placeholders)
        plugin.actionExecutor.executeAction("kicked", targetName, placeholders)
        PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_KICK, "kick", "broadcast", placeholders)
    }

    override fun suggest(stack: CommandSourceStack, args: List<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.KICK)) {
            return emptyList()
        }
        return when (args.size) {
            0, 1 -> listOf("all") + plugin.server.onlinePlayers.map { it.name }
            2 -> plugin.messageHandler.getMessageStringList("kick", "reasons")
            else -> emptyList()
        }
    }

    override fun build(name: String): LiteralCommandNode<CommandSourceStack> {
        val forceLiteral = Commands.literal("--force")
            .executes { context ->
                BrigadierCommandUtils.resolvePlayers(context, "target")
                    .forEach { target -> executeKickResolved(context.source, target.name, target.uniqueId, target, "", true) }
                1
            }
            .then(
                Commands.argument("reason", ReasonArgumentType.reason())
                    .executes { context ->
                        val reason = ReasonArgumentType.getReason(context, "reason")
                        BrigadierCommandUtils.resolvePlayers(context, "target").forEach { target ->
                            executeKickResolved(context.source, target.name, target.uniqueId, target, reason, true)
                        }
                        1
                    }
            )

        val reasonArg = Commands.argument("reason", ReasonArgumentType.reason())
            .executes { context ->
                val reason = ReasonArgumentType.getReason(context, "reason")
                BrigadierCommandUtils.resolvePlayers(context, "target").forEach { target ->
                    executeKickResolved(context.source, target.name, target.uniqueId, target, reason, false)
                }
                1
            }

        val targetArg = Commands.argument("target", ArgumentTypes.player())
            .executes { context ->
                BrigadierCommandUtils.resolvePlayers(context, "target")
                    .forEach { target -> executeKickResolved(context.source, target.name, target.uniqueId, target, "", false) }
                1
            }
            .then(forceLiteral)
            .then(reasonArg)

        val allArg = Commands.literal("all")
            .executes { context ->
                executeKickAll(context.source, "", false)
                1
            }
            .then(
                Commands.literal("--force")
                    .executes { context ->
                        executeKickAll(context.source, "", true)
                        1
                    }
                    .then(
                        Commands.argument("reason", ReasonArgumentType.reason())
                            .executes { context ->
                                val reason = ReasonArgumentType.getReason(context, "reason")
                                executeKickAll(context.source, reason, true)
                                1
                            }
                    )
            )
            .then(
                Commands.argument("reason", ReasonArgumentType.reason())
                    .executes { context ->
                        val reason = ReasonArgumentType.getReason(context, "reason")
                        executeKickAll(context.source, reason, false)
                        1
                    }
            )

        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresPermission(PermissionChecker.PermissionKey.KICK))
            .executes { context ->
                sendUsage(context.source)
                1
            }
            .then(targetArg)
            .then(allArg)
            .build()
    }

    private fun sendUsage(stack: CommandSourceStack) {
        stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("kick", "usage"))
    }
}

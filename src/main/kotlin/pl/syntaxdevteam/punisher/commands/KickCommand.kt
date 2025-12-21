package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.common.PunishmentCommandUtils
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class KickCommand(private val plugin: PunisherX) : BrigadierCommand {

    override fun execute(stack: CommandSourceStack, args: List<String>) {
        val history: Boolean = plugin.config.getBoolean("kick.history", false)
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.KICK)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }

        if (args.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("kick", "usage"))
            return
        }

        val targetArg = args[0]
        val isForce = args.contains("--force")
        val reason = args.slice(1 until args.size).filterNot { it == "--force" }.joinToString(" ")
        val punishmentType = "KICK"
        val start = System.currentTimeMillis()
        val formattedTime = plugin.timeHandler.formatTime(null)

        if (targetArg.equals("all", ignoreCase = true)) {
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

                if(history) {
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
            return
        }

        val uuid = plugin.resolvePlayerUuid(targetArg)
        val targetPlayer = Bukkit.getPlayer(uuid)

        if (targetPlayer != null) {
            if (!isForce && PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_KICK)) {
                stack.sender.sendMessage(
                    plugin.messageHandler.stringMessageToComponent(
                        "error", "bypass", mapOf("player" to targetArg)
                    )
                )
                return
            }
        }
      val prefix = plugin.messageHandler.getPrefix()
        if (PermissionChecker.isAuthor(uuid)) {
            stack.sender.sendMessage(
                plugin.messageHandler.formatMixedTextToMiniMessage("$prefix <red>You can't punish the plugin author</red>",
                    TagResolver.empty())
            )
            return
        }

        if(history) {
            plugin.databaseHandler.addPunishmentHistory(
                targetArg,
                uuid.toString(),
                reason,
                stack.sender.name,
                punishmentType,
                start,
                start
            )
        }

        val placeholders = PunishmentCommandUtils.buildPlaceholders(
            player = targetArg,
            operator = stack.sender.name,
            reason = reason,
            time = formattedTime,
            type = punishmentType
        )
        PunishmentCommandUtils.sendKickMessage(plugin, targetPlayer, "kick", "kick_message", placeholders)
        PunishmentCommandUtils.sendSenderMessages(plugin, stack, "kick", "kick", placeholders)
        plugin.actionExecutor.executeAction("kicked", targetArg, placeholders)
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
                BrigadierCommandUtils.resolvePlayerNames(context, "target")
                    .forEach { target -> execute(context.source, listOf(target, "--force")) }
                1
            }
            .then(
                Commands.argument("reason", StringArgumentType.greedyString())
                    .executes { context ->
                        val reason = StringArgumentType.getString(context, "reason")
                        BrigadierCommandUtils.resolvePlayerNames(context, "target").forEach { target ->
                            val args = BrigadierCommandUtils.greedyArgs(listOf(target, "--force"), reason)
                            execute(context.source, args)
                        }
                        1
                    }
            )

        val reasonArg = Commands.argument("reason", StringArgumentType.greedyString())
            .executes { context ->
                val reason = StringArgumentType.getString(context, "reason")
                BrigadierCommandUtils.resolvePlayerNames(context, "target").forEach { target ->
                    execute(context.source, BrigadierCommandUtils.greedyArgs(listOf(target), reason))
                }
                1
            }

        val targetArg = Commands.argument("target", ArgumentTypes.player())
            .executes { context ->
                BrigadierCommandUtils.resolvePlayerNames(context, "target")
                    .forEach { target -> execute(context.source, listOf(target)) }
                1
            }
            .then(forceLiteral)
            .then(reasonArg)

        val allArg = Commands.literal("all")
            .executes { context ->
                execute(context.source, listOf("all"))
                1
            }
            .then(
                Commands.literal("--force")
                    .executes { context ->
                        execute(context.source, listOf("all", "--force"))
                        1
                    }
                    .then(
                        Commands.argument("reason", StringArgumentType.greedyString())
                            .executes { context ->
                                val reason = StringArgumentType.getString(context, "reason")
                                execute(context.source, BrigadierCommandUtils.greedyArgs(listOf("all", "--force"), reason))
                                1
                            }
                    )
            )
            .then(
                Commands.argument("reason", StringArgumentType.greedyString())
                    .executes { context ->
                        val reason = StringArgumentType.getString(context, "reason")
                        execute(context.source, BrigadierCommandUtils.greedyArgs(listOf("all"), reason))
                        1
                    }
            )

        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresPermission(PermissionChecker.PermissionKey.KICK))
            .executes { context ->
                execute(context.source, emptyList())
                1
            }
            .then(targetArg)
            .then(allArg)
            .build()
    }
}

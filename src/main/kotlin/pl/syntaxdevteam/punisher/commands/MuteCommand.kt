package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import org.bukkit.Bukkit
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.common.PunishmentCommandUtils
import pl.syntaxdevteam.punisher.common.TimeSuggestionProvider
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class MuteCommand(private val plugin: PunisherX) : BrigadierCommand {

    override fun execute(stack: CommandSourceStack, args: List<String>) {
        if(PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.MUTE)) {
            if (args.isNotEmpty()) {
                if (args.size < 2) {
                    stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("mute", "usage"))
                } else {
                    val player = args[0]
                    val targetPlayer = Bukkit.getPlayer(player)
                    val isForce = args.contains("--force")
                    if (targetPlayer != null) {
                        if (!isForce && PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_MUTE)) {
                            stack.sender.sendMessage(
                                plugin.messageHandler.stringMessageToComponent(
                                    "error",
                                    "bypass",
                                    mapOf("player" to player)
                                )
                            )
                            return
                        }
                    }
                    val uuid = plugin.resolvePlayerUuid(player).toString()

                    val (gtime, reason) = PunishmentCommandUtils.parseTimeAndReason(plugin, args, 1)

                    val punishmentType = "MUTE"
                    val start = System.currentTimeMillis()
                    val end: Long? = if (gtime != null) (System.currentTimeMillis() + plugin.timeHandler.parseTime(gtime) * 1000) else null

                    val punishmentId = plugin.databaseHandler.addPunishment(player, uuid, reason, stack.sender.name, punishmentType, start, end ?: -1)
                    if (punishmentId == null) {
                        stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))
                        return
                    }
                    plugin.databaseHandler.addPunishmentHistory(player, uuid, reason, stack.sender.name, punishmentType, start, end ?: -1)

                    val formattedTime = plugin.timeHandler.formatTime(gtime)
                    val placeholders = PunishmentCommandUtils.buildPlaceholders(
                        player = player,
                        operator = stack.sender.name,
                        reason = reason,
                        time = formattedTime,
                        type = punishmentType,
                        extra = mapOf("id" to punishmentId.toString())
                    )
                    PunishmentCommandUtils.sendSenderMessages(plugin, stack, "mute", "mute", placeholders)
                    plugin.actionExecutor.executeAction("muted", player, placeholders)
                    PunishmentCommandUtils.sendTargetMessages(plugin, targetPlayer, "mute", "mute_message", placeholders)
                    PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_MUTE, "mute", "broadcast", placeholders)
                }
            } else {
                stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("mute", "usage"))
            }
        } else {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
        }
    }

    override fun suggest(stack: CommandSourceStack, args: List<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.MUTE)) {
            return emptyList()
        }
        return when (args.size) {
            0, 1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> TimeSuggestionProvider.generateTimeSuggestions()
            3 -> plugin.messageHandler.getMessageStringList("mute", "reasons")
            else -> emptyList()
        }
    }

    override fun build(name: String): LiteralCommandNode<CommandSourceStack> {
        val targetArg = Commands.argument("target", ArgumentTypes.playerProfiles())
            .executes { context ->
                BrigadierCommandUtils.resolvePlayerProfileNames(context, "target").forEach { target ->
                    execute(context.source, listOf(target))
                }
                1
            }
            .then(
                Commands.argument("time", StringArgumentType.word())
                    .suggests(BrigadierCommandUtils.suggestions(this) { context ->
                        val target = BrigadierCommandUtils.resolvePlayerProfileNames(context, "target")
                            .firstOrNull()
                            .orEmpty()
                        listOf(target, "")
                    })
                    .executes { context ->
                        val time = StringArgumentType.getString(context, "time")
                        BrigadierCommandUtils.resolvePlayerProfileNames(context, "target").forEach { target ->
                            execute(context.source, listOf(target, time))
                        }
                        1
                    }
                    .then(
                        Commands.argument("reason", StringArgumentType.greedyString())
                            .executes { context ->
                                val time = StringArgumentType.getString(context, "time")
                                val reason = StringArgumentType.getString(context, "reason")
                                BrigadierCommandUtils.resolvePlayerProfileNames(context, "target").forEach { target ->
                                    execute(context.source, BrigadierCommandUtils.greedyArgs(listOf(target, time), reason))
                                }
                                1
                            }
                    )
            )
            .then(
                Commands.argument("reason", StringArgumentType.greedyString())
                    .executes { context ->
                        val reason = StringArgumentType.getString(context, "reason")
                        BrigadierCommandUtils.resolvePlayerProfileNames(context, "target").forEach { target ->
                            execute(context.source, BrigadierCommandUtils.greedyArgs(listOf(target), reason))
                        }
                        1
                    }
            )

        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresPermission(PermissionChecker.PermissionKey.MUTE))
            .executes { context ->
                execute(context.source, emptyList())
                1
            }
            .then(targetArg)
            .build()
    }
}

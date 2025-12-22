package pl.syntaxdevteam.punisher.commands

import com.destroystokyo.paper.profile.PlayerProfile
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import org.bukkit.Bukkit
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.common.PunishmentCommandUtils
import pl.syntaxdevteam.punisher.common.TimeSuggestionProvider
import pl.syntaxdevteam.punisher.commands.arguments.PunishmentDuration
import pl.syntaxdevteam.punisher.commands.arguments.PunishmentDurationArgumentType
import pl.syntaxdevteam.punisher.commands.arguments.ReasonArgumentType
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class MuteCommand(private val plugin: PunisherX) : BrigadierCommand {

    override fun execute(stack: CommandSourceStack, args: List<String>) {
        executeLegacy(stack, args)
    }

    private fun executeLegacy(stack: CommandSourceStack, args: List<String>) {
        if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.MUTE)) {
            if (args.isNotEmpty()) {
                if (args.size < 2) {
                    sendUsage(stack)
                } else {
                    val player = args[0]
                    val isForce = args.contains("--force")
                    val (gtime, reason) = PunishmentCommandUtils.parseTimeAndReason(plugin, args, 1)
                    val duration = gtime?.let { PunishmentDurationArgumentType.parseRaw(it) }
                    val uuid = plugin.resolvePlayerUuid(player)
                    executeMute(stack, Bukkit.createProfile(uuid, player), duration, reason, isForce)
                }
            } else {
                sendUsage(stack)
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
                sendUsage(context.source)
                1
            }
            .then(
                Commands.argument("time", PunishmentDurationArgumentType.duration())
                    .suggests(BrigadierCommandUtils.suggestions(this) { context ->
                        val target = BrigadierCommandUtils.resolvePlayerProfileNames(context, "target")
                            .firstOrNull()
                            .orEmpty()
                        listOf(target, "")
                    })
                    .executes { context ->
                        val time = PunishmentDurationArgumentType.getDuration(context, "time")
                        BrigadierCommandUtils.resolvePlayerProfiles(context, "target").forEach { target ->
                            executeMute(context.source, target, time, "", false)
                        }
                        1
                    }
                    .then(
                        Commands.argument("reason", ReasonArgumentType.reason())
                            .executes { context ->
                                val time = PunishmentDurationArgumentType.getDuration(context, "time")
                                val reason = ReasonArgumentType.getReason(context, "reason")
                                BrigadierCommandUtils.resolvePlayerProfiles(context, "target").forEach { target ->
                                    executeMute(context.source, target, time, reason, false)
                                }
                                1
                            }
                    )
            )
            .then(
                Commands.argument("reason", ReasonArgumentType.reason())
                    .executes { context ->
                        val reason = ReasonArgumentType.getReason(context, "reason")
                        BrigadierCommandUtils.resolvePlayerProfiles(context, "target").forEach { target ->
                            executeMute(context.source, target, null, reason, false)
                        }
                        1
                    }
            )

        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresPermission(PermissionChecker.PermissionKey.MUTE))
            .executes { context ->
                sendUsage(context.source)
                1
            }
            .then(targetArg)
            .build()
    }

    private fun executeMute(
        stack: CommandSourceStack,
        profile: PlayerProfile,
        duration: PunishmentDuration?,
        reason: String,
        isForce: Boolean
    ) {
        val player = profile.name ?: profile.id?.toString()
        if (player == null) {
            sendUsage(stack)
            return
        }
        val uuid = profile.id ?: plugin.resolvePlayerUuid(player)
        val targetPlayer = Bukkit.getPlayer(uuid)
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
        val uuidString = uuid.toString()

        val punishmentType = "MUTE"
        val start = System.currentTimeMillis()
        val end: Long? = duration?.let { start + it.seconds * 1000 }

        val punishmentId = plugin.databaseHandler.addPunishment(player, uuidString, reason, stack.sender.name, punishmentType, start, end ?: -1)
        if (punishmentId == null) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))
            return
        }
        plugin.databaseHandler.addPunishmentHistory(player, uuidString, reason, stack.sender.name, punishmentType, start, end ?: -1)

        val formattedTime = plugin.timeHandler.formatTime(duration?.raw)
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

    private fun sendUsage(stack: CommandSourceStack) {
        stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("mute", "usage"))
    }
}

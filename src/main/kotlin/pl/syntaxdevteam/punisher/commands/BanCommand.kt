package pl.syntaxdevteam.punisher.commands

import com.destroystokyo.paper.profile.PlayerProfile
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.ban.BanListType
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.ban.ProfileBanList
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.common.PunishmentCommandUtils
import pl.syntaxdevteam.punisher.common.TimeSuggestionProvider
import pl.syntaxdevteam.punisher.commands.arguments.PunishmentDuration
import pl.syntaxdevteam.punisher.commands.arguments.PunishmentDurationArgumentType
import pl.syntaxdevteam.punisher.commands.arguments.ReasonArgumentType
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.util.*

class BanCommand(private var plugin: PunisherX) : BrigadierCommand {
    private val clp = plugin.commandLoggerPlugin

    override fun execute(stack: CommandSourceStack, args: List<String>) {
        executeLegacy(stack, args)
    }

    private fun executeLegacy(stack: CommandSourceStack, args: List<String>) {
        if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.BAN)) {
            if (args.isNotEmpty()) {
                if (args.size < 2) {
                    sendUsage(stack)
                } else {
                    val player = args[0]
                    val isForce = args.contains("--force")
                    val (gtime, reason) = PunishmentCommandUtils.parseTimeAndReason(plugin, args, 1)
                    val duration = gtime?.let { PunishmentDurationArgumentType.parseRaw(it) }

                    val uuid = plugin.resolvePlayerUuid(player)
                    executeBan(stack, Bukkit.createProfile(uuid, player), duration, reason, isForce)

                }
            } else {
                sendUsage(stack)
            }
        } else {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
        }
    }

    override fun suggest(stack: CommandSourceStack, args: List<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.BAN)) {
            return emptyList()
        }
        return when (args.size) {
            0, 1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> TimeSuggestionProvider.generateTimeSuggestions()
            3 -> plugin.messageHandler.getMessageStringList("ban", "reasons")
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
                            executeBan(context.source, target, time, "", false)
                        }
                        1
                    }
                    .then(
                        Commands.argument("reason", ReasonArgumentType.reason())
                            .executes { context ->
                                val time = PunishmentDurationArgumentType.getDuration(context, "time")
                                val reason = ReasonArgumentType.getReason(context, "reason")
                                BrigadierCommandUtils.resolvePlayerProfiles(context, "target").forEach { target ->
                                    executeBan(context.source, target, time, reason, false)
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
                            executeBan(context.source, target, null, reason, false)
                        }
                        1
                    }
            )

        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresPermission(PermissionChecker.PermissionKey.BAN))
            .executes { context ->
                sendUsage(context.source)
                1
            }
            .then(targetArg)
            .build()
    }

    private fun executeBan(
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
            if (!isForce && PermissionChecker.hasWithLegacy(targetPlayer, PermissionChecker.PermissionKey.BYPASS_BAN)) {
                stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "bypass", mapOf("player" to player)))
                return
            }
        }
        if (PermissionChecker.isAuthor(uuid)) {
            stack.sender.sendMessage(
                plugin.messageHandler.formatMixedTextToMiniMessage(
                    "<red>You can't punish the plugin author</red>",
                    TagResolver.empty()
                )
            )
            return
        }

        val punishmentType = "BAN"
        val start = System.currentTimeMillis()
        val end: Long? = duration?.let { start + it.seconds * 1000 }
        val normalizedEnd = end ?: -1

        val punishmentId = plugin.databaseHandler.addPunishment(player, uuid.toString(), reason, stack.sender.name, punishmentType, start, normalizedEnd)
        if (punishmentId == null) {
            plugin.logger.err("Failed to add ban to database for player $player. Using fallback method.")
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))
            val playerProfile = Bukkit.createProfile(uuid, player)
            val banList: ProfileBanList = Bukkit.getBanList(BanListType.PROFILE)
            val banEndDate = end?.let { Date(it) }
            banList.addBan(playerProfile, reason, banEndDate, stack.sender.name)
        }
        clp.logCommand(stack.sender.name, punishmentType, player, reason)
        plugin.databaseHandler.addPunishmentHistory(player, uuid.toString(), reason, stack.sender.name, punishmentType, start, normalizedEnd)
        plugin.proxyBridgeMessenger.notifyBan(uuid, reason, normalizedEnd)

        val formattedTime = plugin.timeHandler.formatTime(duration?.raw)
        val placeholders = PunishmentCommandUtils.buildPlaceholders(
            player = player,
            operator = stack.sender.name,
            reason = reason,
            time = formattedTime,
            type = punishmentType,
            extra = mapOf("id" to (punishmentId?.toString() ?: "?"))
        )

        PunishmentCommandUtils.sendKickMessage(plugin, targetPlayer, "ban", "kick_message", placeholders)
        PunishmentCommandUtils.sendSenderMessages(plugin, stack, "ban", "ban", placeholders)
        plugin.actionExecutor.executeAction("banned", player, placeholders)
        PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_BAN, "ban", "broadcast", placeholders)
        if (isForce) {
            plugin.logger.warning("Force-banned by ${stack.sender.name} on $player")
        }
    }

    private fun sendUsage(stack: CommandSourceStack) {
        stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("ban", "usage"))
    }
}

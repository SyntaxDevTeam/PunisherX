package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.arguments.StringArgumentType
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
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.util.*

class BanCommand(private var plugin: PunisherX) : BrigadierCommand {
    private val clp = plugin.commandLoggerPlugin

    override fun execute(stack: CommandSourceStack, args: List<String>) {
        if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.BAN)) {
            if (args.isNotEmpty()) {
                if (args.size < 2) {
                    stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("ban", "usage"))
                } else {
                    val player = args[0]
                    val uuid = plugin.resolvePlayerUuid(player)
                    val targetPlayer = Bukkit.getPlayer(uuid)
                    val isForce = args.contains("--force")
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
                    val (gtime, reason) = PunishmentCommandUtils.parseTimeAndReason(plugin, args, 1)

                    val punishmentType = "BAN"
                    val start = System.currentTimeMillis()
                    val end: Long? = if (gtime != null) (System.currentTimeMillis() + plugin.timeHandler.parseTime(gtime) * 1000) else null
                    val normalizedEnd = end ?: -1

                    val punishmentId = plugin.databaseHandler.addPunishment(player, uuid.toString(), reason, stack.sender.name, punishmentType, start, normalizedEnd)
                    if (punishmentId == null) {
                        plugin.logger.err("Failed to add ban to database for player $player. Using fallback method.")
                        stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))
                        val playerProfile = Bukkit.createProfile(uuid, player)
                        val banList: ProfileBanList = Bukkit.getBanList(BanListType.PROFILE)
                        val banEndDate = if (gtime != null) Date(System.currentTimeMillis() + plugin.timeHandler.parseTime(gtime) * 1000) else null
                        banList.addBan(playerProfile, reason, banEndDate, stack.sender.name)
                    }
                    clp.logCommand(stack.sender.name, punishmentType, player, reason)
                    plugin.databaseHandler.addPunishmentHistory(player, uuid.toString(), reason, stack.sender.name, punishmentType, start, normalizedEnd)
                    plugin.proxyBridgeMessenger.notifyBan(uuid, reason, normalizedEnd)

                    val formattedTime = plugin.timeHandler.formatTime(gtime)
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
            } else {
                stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("ban", "usage"))
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
            .requires(BrigadierCommandUtils.requiresPermission(PermissionChecker.PermissionKey.BAN))
            .executes { context ->
                execute(context.source, emptyList())
                1
            }
            .then(targetArg)
            .build()
    }
}

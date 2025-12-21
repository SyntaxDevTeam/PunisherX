package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import org.bukkit.Bukkit
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.GameMode
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.basic.JailUtils
import pl.syntaxdevteam.punisher.common.TimeSuggestionProvider
import pl.syntaxdevteam.punisher.commands.arguments.PunishmentDuration
import pl.syntaxdevteam.punisher.commands.arguments.PunishmentDurationArgumentType
import pl.syntaxdevteam.punisher.commands.arguments.ReasonArgumentType
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class JailCommand(private val plugin: PunisherX) : BrigadierCommand {

    override fun execute(stack: CommandSourceStack, args: List<String>) {

        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.JAIL)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }
        if (args.isEmpty() || args.size < 2) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("jail", "usage"))
            return
        }

        val playerName = args[0]
        val isForce = args.contains("--force")

        val parsedDuration = PunishmentDurationArgumentType.parseRaw(args[1])
        val (duration, reason) = if (parsedDuration != null) {
            parsedDuration to args.drop(2).filterNot { it == "--force" }.joinToString(" ")
        } else {
            null to args.drop(1).filterNot { it == "--force" }.joinToString(" ")
        }

        executeJail(stack, playerName, duration, reason, isForce)
    }

    override fun suggest(stack: CommandSourceStack, args: List<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.JAIL)) {
            return emptyList()
        }
        return when (args.size) {
            0, 1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> TimeSuggestionProvider.generateTimeSuggestions()
            3 -> plugin.messageHandler.getMessageStringList("jail", "reasons")
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
                Commands.argument("time", PunishmentDurationArgumentType.duration())
                    .suggests(BrigadierCommandUtils.suggestions(this) { context ->
                        val target = BrigadierCommandUtils.resolvePlayerProfileNames(context, "target")
                            .firstOrNull()
                            .orEmpty()
                        listOf(target, "")
                    })
                    .executes { context ->
                        val time = PunishmentDurationArgumentType.getDuration(context, "time")
                        BrigadierCommandUtils.resolvePlayerProfileNames(context, "target").forEach { target ->
                            executeJail(context.source, target, time, "", false)
                        }
                        1
                    }
                    .then(
                        Commands.argument("reason", ReasonArgumentType.reason())
                            .executes { context ->
                                val time = PunishmentDurationArgumentType.getDuration(context, "time")
                                val reason = ReasonArgumentType.getReason(context, "reason")
                                BrigadierCommandUtils.resolvePlayerProfileNames(context, "target").forEach { target ->
                                    executeJail(context.source, target, time, reason, false)
                                }
                                1
                            }
                    )
            )
            .then(
                Commands.argument("reason", ReasonArgumentType.reason())
                    .executes { context ->
                        val reason = ReasonArgumentType.getReason(context, "reason")
                        BrigadierCommandUtils.resolvePlayerProfileNames(context, "target").forEach { target ->
                            executeJail(context.source, target, null, reason, false)
                        }
                        1
                    }
            )

        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresPermission(PermissionChecker.PermissionKey.JAIL))
            .executes { context ->
                execute(context.source, emptyList())
                1
            }
            .then(targetArg)
            .build()
    }

    private fun executeJail(
        stack: CommandSourceStack,
        playerName: String,
        duration: PunishmentDuration?,
        reason: String,
        isForce: Boolean
    ) {
        val uuid = plugin.resolvePlayerUuid(playerName)
        val targetPlayer = Bukkit.getPlayer(uuid)

        if (targetPlayer != null && !isForce && PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_JAIL)) {
            stack.sender.sendMessage(
                plugin.messageHandler.stringMessageToComponent("error", "bypass", mapOf("player" to playerName))
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

        val punishmentType = "JAIL"
        val start = System.currentTimeMillis()
        val end: Long? = duration?.let { start + it.seconds * 1000 }
        val previousLocation = targetPlayer?.location?.clone()

        val jailLocation = JailUtils.getJailLocation(plugin.config)
        if (jailLocation == null) {
            plugin.logger.debug("<red>No jail location found! Teleportation aborted.</red>")
            return
        }
        plugin.logger.debug("<yellow>Jail location: ${jailLocation}</yellow>")

        val formattedTime = plugin.timeHandler.formatTime(duration?.raw)
        val basePlaceholders = mapOf(
            "player" to playerName,
            "operator" to stack.sender.name,
            "reason" to reason,
            "time" to formattedTime,
            "type" to punishmentType
        )

        fun finalizePunishment() {
            val punishmentId = plugin.databaseHandler.addPunishment(playerName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)
            if (punishmentId == null) {
                stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))
                return
            }
            plugin.databaseHandler.addPunishmentHistory(playerName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)
            plugin.cache.addOrUpdatePunishment(uuid, end ?: -1, previousLocation)
            val placeholders = basePlaceholders + ("id" to punishmentId.toString())

            targetPlayer?.sendMessage(
                plugin.messageHandler.stringMessageToComponent(
                    "jail", "jail_message",
                    placeholders
                )
            )

            val broadcastMessages = plugin.messageHandler.getSmartMessage(
                "jail", "broadcast",
                placeholders
            )
            plugin.server.onlinePlayers.forEach { onlinePlayer ->
                if (PermissionChecker.hasWithSee(onlinePlayer, PermissionChecker.PermissionKey.SEE_JAIL)) {
                    broadcastMessages.forEach { message ->
                        onlinePlayer.sendMessage(message)
                    }
                }
            }

            plugin.messageHandler.getSmartMessage(
                "jail",
                "jail",
                placeholders
            ).forEach { stack.sender.sendMessage(it) }
            plugin.actionExecutor.executeAction("jailed", playerName, placeholders)
        }

        if (targetPlayer != null) {
            plugin.safeTeleportService.teleportSafely(targetPlayer, jailLocation) { success ->
                if (success) {
                    targetPlayer.gameMode = GameMode.ADVENTURE
                    plugin.logger.debug("<green>Player successfully teleported to jail.</green>")
                    plugin.logger.debug("Changing gamemode (${targetPlayer.gameMode}) and teleporting ${targetPlayer.name} to $jailLocation")
                    finalizePunishment()
                } else {
                    stack.sender.sendMessage(
                        plugin.messageHandler.stringMessageToComponent(
                            "jail",
                            "teleport_failed",
                            mapOf("player" to playerName)
                        )
                    )
                    plugin.logger.debug("<red>Failed to teleport player to jail. Command aborted.</red>")
                }
            }
        } else {
            finalizePunishment()
        }
    }
}

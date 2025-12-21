package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.util.UUID

class BanIpCommand(private val plugin: PunisherX) : BrigadierCommand {
    companion object {
        private val IP_REGEX = Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")
        private val UUID_REGEX = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    }

    private val clp = plugin.commandLoggerPlugin

    override fun execute(stack: CommandSourceStack, args: List<String>) {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.BANIP)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }
        if (args.size < 2) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("banip", "usage"))
            return
        }

        val rawTarget = args[0]
        val isForce = args.contains("--force")
        val playerIPs: List<String> = when {
            IP_REGEX.matches(rawTarget) -> listOf(rawTarget)
            UUID_REGEX.matches(rawTarget) -> plugin.playerIPManager.getPlayerIPsByUUID(rawTarget)
            else -> plugin.playerIPManager.getPlayerIPsByName(rawTarget.lowercase())
        }

        if (playerIPs.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("banip", "not_found"))
            return
        }

        val targetUUID: UUID? = when {
            IP_REGEX.matches(rawTarget) -> plugin.playerIPManager.getAllDecryptedRecords()
                .find { it.playerIP == rawTarget }
                ?.let { UUID.fromString(it.playerUUID) }
            else -> plugin.resolvePlayerUuid(rawTarget)
        }
        val targetPlayer: Player? = targetUUID?.let { Bukkit.getPlayer(it) }

        if (targetPlayer != null && !isForce && PermissionChecker.hasWithLegacy(targetPlayer, PermissionChecker.PermissionKey.BYPASS_BANIP)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "bypass", mapOf("player" to rawTarget)))
            return
        }

        val filtered = args.filterNot { it == "--force" }
        val timeArg = filtered.getOrNull(1)
        val durationSec = timeArg?.let {
            try { plugin.timeHandler.parseTime(it) } catch (_: Exception) { null }
        }
        val reason = if (durationSec != null) filtered.drop(2).joinToString(" ") else filtered.drop(1).joinToString(" ")

        val start = System.currentTimeMillis()
        val end = durationSec?.let { start + it * 1000 }
        val formattedTime = plugin.timeHandler.formatTime(timeArg)
        var dbError = false
        val normalizedEnd = end ?: -1
        val punishmentIds = mutableListOf<Long>()
        playerIPs.distinct().forEach { ip ->
            val punishmentId = plugin.databaseHandler.addPunishment(rawTarget, ip, reason, stack.sender.name,
                "BANIP", start, normalizedEnd)
            if (punishmentId == null) {
                plugin.logger.err("DB error ban-ip $ip")
                dbError = true
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban-ip $ip")
            } else {
                punishmentIds.add(punishmentId)
            }
            plugin.databaseHandler.addPunishmentHistory(rawTarget, ip, reason, stack.sender.name,
                "BANIP", start, normalizedEnd)
            plugin.proxyBridgeMessenger.notifyIpBan(ip, reason, normalizedEnd)
        }
        val placeholders = mapOf(
            "player" to rawTarget,
            "operator" to stack.sender.name,
            "reason" to reason,
            "time" to formattedTime,
            "type" to "BANIP",
            "id" to when {
                punishmentIds.isEmpty() -> "?"
                punishmentIds.size == 1 -> punishmentIds.first().toString()
                else -> punishmentIds.joinToString(",")
            }
        )
        clp.logCommand(stack.sender.name, "BANIP", rawTarget, reason)
        if (dbError) stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))

        if (targetPlayer != null) {
            val lines = plugin.messageHandler.getSmartMessage(
                "banip",
                "kick_message",
                placeholders
            )
            val builder = Component.text()
            lines.forEach { builder.append(it).append(Component.newline()) }
            targetPlayer.kick(builder.build())
        }

        val msgLines = plugin.messageHandler.getSmartMessage(
            "banip",
            "ban",
            placeholders
        )
        msgLines.forEach { stack.sender.sendMessage(it) }

        plugin.actionExecutor.executeAction("ip_banned", rawTarget, placeholders)

        plugin.server.onlinePlayers.filter { PermissionChecker.hasWithSee(it, PermissionChecker.PermissionKey.SEE_BANIP) }
            .forEach { player -> msgLines.forEach { player.sendMessage(it) } }

        if (isForce) plugin.logger.warning("Force by ${stack.sender.name} on $rawTarget")
    }

    override fun suggest(stack: CommandSourceStack, args: List<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender as Player, PermissionChecker.PermissionKey.BANIP)) return emptyList()
        return when (args.size) {
            0, 1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> TimeSuggestionProvider.generateTimeSuggestions()
            3 -> plugin.messageHandler.getMessageStringList("banip", "reasons")
            else -> emptyList()
        }
    }

    override fun build(name: String): LiteralCommandNode<CommandSourceStack> {
        val targetArg = Commands.argument("target", StringArgumentType.word())
            .suggests(BrigadierCommandUtils.suggestions(this) { emptyList() })
            .executes { context ->
                val target = StringArgumentType.getString(context, "target")
                execute(context.source, listOf(target))
                1
            }
            .then(
                Commands.argument("time", StringArgumentType.word())
                    .suggests(BrigadierCommandUtils.suggestions(this) { context ->
                        listOf(StringArgumentType.getString(context, "target"), "")
                    })
                    .executes { context ->
                        val target = StringArgumentType.getString(context, "target")
                        val time = StringArgumentType.getString(context, "time")
                        execute(context.source, listOf(target, time))
                        1
                    }
                    .then(
                        Commands.argument("reason", StringArgumentType.greedyString())
                            .executes { context ->
                                val target = StringArgumentType.getString(context, "target")
                                val time = StringArgumentType.getString(context, "time")
                                val reason = StringArgumentType.getString(context, "reason")
                                execute(context.source, BrigadierCommandUtils.greedyArgs(listOf(target, time), reason))
                                1
                            }
                    )
            )
            .then(
                Commands.argument("reason", StringArgumentType.greedyString())
                    .executes { context ->
                        val target = StringArgumentType.getString(context, "target")
                        val reason = StringArgumentType.getString(context, "reason")
                        execute(context.source, BrigadierCommandUtils.greedyArgs(listOf(target), reason))
                        1
                    }
            )

        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresPermission(PermissionChecker.PermissionKey.BANIP))
            .executes { context ->
                execute(context.source, emptyList())
                1
            }
            .then(targetArg)
            .build()
    }
}

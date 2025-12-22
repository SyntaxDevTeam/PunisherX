package pl.syntaxdevteam.punisher.commands

import com.destroystokyo.paper.profile.PlayerProfile
import com.google.common.net.InetAddresses
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.common.TimeSuggestionProvider
import pl.syntaxdevteam.punisher.commands.arguments.IpAddressArgumentType
import pl.syntaxdevteam.punisher.commands.arguments.PunishmentDuration
import pl.syntaxdevteam.punisher.commands.arguments.PunishmentDurationArgumentType
import pl.syntaxdevteam.punisher.commands.arguments.ReasonArgumentType
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.util.UUID

class BanIpCommand(private val plugin: PunisherX) : BrigadierCommand {
    companion object {
        private val UUID_REGEX = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    }

    private val clp = plugin.commandLoggerPlugin

    override fun execute(stack: CommandSourceStack, args: List<String>) {
        executeLegacy(stack, args)
    }

    private fun executeLegacy(stack: CommandSourceStack, args: List<String>) {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.BANIP)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }
        if (args.size < 2) {
            sendUsage(stack)
            return
        }

        val rawTarget = args[0]
        val isForce = args.contains("--force")
        val filtered = args.filterNot { it == "--force" }
        val timeArg = filtered.getOrNull(1)
        val duration = timeArg?.let { PunishmentDurationArgumentType.parseRaw(it) }
        val reason = if (duration != null) filtered.drop(2).joinToString(" ") else filtered.drop(1).joinToString(" ")

        when {
            InetAddresses.isInetAddress(rawTarget) -> executeBanIpAddress(stack, rawTarget, duration, reason, isForce)
            UUID_REGEX.matches(rawTarget) -> {
                val uuid = UUID.fromString(rawTarget)
                executeBanIpProfile(stack, Bukkit.createProfile(uuid, null), duration, reason, isForce)
            }
            else -> {
                val uuid = plugin.resolvePlayerUuid(rawTarget)
                executeBanIpProfile(stack, Bukkit.createProfile(uuid, rawTarget), duration, reason, isForce)
            }
        }
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
                        val targets = BrigadierCommandUtils.resolvePlayerProfiles(context, "target")
                        if (rejectIpTargets(context.source, targets)) return@executes 1
                        targets.forEach { target ->
                            executeBanIpProfile(context.source, target, time, "", false)
                        }
                        1
                    }
                    .then(
                        Commands.argument("reason", ReasonArgumentType.reason())
                            .executes { context ->
                                val time = PunishmentDurationArgumentType.getDuration(context, "time")
                                val reason = ReasonArgumentType.getReason(context, "reason")
                                val targets = BrigadierCommandUtils.resolvePlayerProfiles(context, "target")
                                if (rejectIpTargets(context.source, targets)) return@executes 1
                                targets.forEach { target ->
                                    executeBanIpProfile(context.source, target, time, reason, false)
                                }
                                1
                            }
                    )
            )
            .then(
                Commands.argument("reason", ReasonArgumentType.reason())
                    .executes { context ->
                        val reason = ReasonArgumentType.getReason(context, "reason")
                        val targets = BrigadierCommandUtils.resolvePlayerProfiles(context, "target")
                        if (rejectIpTargets(context.source, targets)) return@executes 1
                        targets.forEach { target ->
                            executeBanIpProfile(context.source, target, null, reason, false)
                        }
                        1
                    }
            )

        val ipArg = Commands.literal("ip")
            .executes { context ->
                sendUsage(context.source)
                1
            }
            .then(
                Commands.argument("address", IpAddressArgumentType.ipAddress())
                    .executes { context ->
                        val address = IpAddressArgumentType.getAddress(context, "address")
                        executeBanIpAddress(context.source, address, null, "", false)
                        1
                    }
                    .then(
                        Commands.argument("time", PunishmentDurationArgumentType.duration())
                            .executes { context ->
                                val address = IpAddressArgumentType.getAddress(context, "address")
                                val time = PunishmentDurationArgumentType.getDuration(context, "time")
                                executeBanIpAddress(context.source, address, time, "", false)
                                1
                            }
                            .then(
                                Commands.argument("reason", ReasonArgumentType.reason())
                                    .executes { context ->
                                        val address = IpAddressArgumentType.getAddress(context, "address")
                                        val time = PunishmentDurationArgumentType.getDuration(context, "time")
                                        val reason = ReasonArgumentType.getReason(context, "reason")
                                        executeBanIpAddress(context.source, address, time, reason, false)
                                        1
                                    }
                            )
                    )
                    .then(
                        Commands.argument("reason", ReasonArgumentType.reason())
                            .executes { context ->
                                val address = IpAddressArgumentType.getAddress(context, "address")
                                val reason = ReasonArgumentType.getReason(context, "reason")
                                executeBanIpAddress(context.source, address, null, reason, false)
                                1
                            }
                    )
            )
            .then(
                Commands.argument("raw", StringArgumentType.word())
                    .executes { context ->
                        sendUsage(context.source)
                        1
                    }
            )

        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresPermission(PermissionChecker.PermissionKey.BANIP))
            .executes { context ->
                sendUsage(context.source)
                1
            }
            .then(targetArg)
            .then(ipArg)
            .build()
    }

    private fun executeBanIpProfile(
        stack: CommandSourceStack,
        profile: PlayerProfile,
        duration: PunishmentDuration?,
        reason: String,
        isForce: Boolean
    ) {
        val rawTarget = profile.name ?: profile.id?.toString()
        if (rawTarget == null) {
            sendUsage(stack)
            return
        }
        val playerUUID = profile.id
        val playerIPs = if (playerUUID != null) {
            plugin.playerIPManager.getPlayerIPsByUUID(playerUUID.toString())
        } else {
            plugin.playerIPManager.getPlayerIPsByName(rawTarget.lowercase())
        }

        if (playerIPs.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("banip", "not_found"))
            return
        }

        val targetUUID = playerUUID ?: plugin.resolvePlayerUuid(rawTarget)
        val targetPlayer: Player? = Bukkit.getPlayer(targetUUID)

        if (targetPlayer != null && !isForce && PermissionChecker.hasWithLegacy(targetPlayer, PermissionChecker.PermissionKey.BYPASS_BANIP)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "bypass", mapOf("player" to rawTarget)))
            return
        }

        val start = System.currentTimeMillis()
        val end = duration?.let { start + it.seconds * 1000 }
        val formattedTime = plugin.timeHandler.formatTime(duration?.raw)
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

    private fun executeBanIpAddress(
        stack: CommandSourceStack,
        address: String,
        duration: PunishmentDuration?,
        reason: String,
        isForce: Boolean
    ) {
        val playerIPs = listOf(address)
        val targetUUID = plugin.playerIPManager.getAllDecryptedRecords()
            .find { it.playerIP == address }
            ?.let { UUID.fromString(it.playerUUID) }
        val targetPlayer = targetUUID?.let { Bukkit.getPlayer(it) }

        if (targetPlayer != null && !isForce && PermissionChecker.hasWithLegacy(targetPlayer, PermissionChecker.PermissionKey.BYPASS_BANIP)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "bypass", mapOf("player" to address)))
            return
        }

        val start = System.currentTimeMillis()
        val end = duration?.let { start + it.seconds * 1000 }
        val formattedTime = plugin.timeHandler.formatTime(duration?.raw)
        var dbError = false
        val normalizedEnd = end ?: -1
        val punishmentIds = mutableListOf<Long>()
        playerIPs.forEach { ip ->
            val punishmentId = plugin.databaseHandler.addPunishment(address, ip, reason, stack.sender.name,
                "BANIP", start, normalizedEnd)
            if (punishmentId == null) {
                plugin.logger.err("DB error ban-ip $ip")
                dbError = true
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban-ip $ip")
            } else {
                punishmentIds.add(punishmentId)
            }
            plugin.databaseHandler.addPunishmentHistory(address, ip, reason, stack.sender.name,
                "BANIP", start, normalizedEnd)
            plugin.proxyBridgeMessenger.notifyIpBan(ip, reason, normalizedEnd)
        }
        val placeholders = mapOf(
            "player" to address,
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
        clp.logCommand(stack.sender.name, "BANIP", address, reason)
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

        plugin.actionExecutor.executeAction("ip_banned", address, placeholders)

        plugin.server.onlinePlayers.filter { PermissionChecker.hasWithSee(it, PermissionChecker.PermissionKey.SEE_BANIP) }
            .forEach { player -> msgLines.forEach { player.sendMessage(it) } }

        if (isForce) plugin.logger.warning("Force by ${stack.sender.name} on $address")
    }

    private fun sendUsage(stack: CommandSourceStack) {
        stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("banip", "usage"))
    }

    private fun rejectIpTargets(stack: CommandSourceStack, targets: Collection<PlayerProfile>): Boolean {
        val ipTarget = targets.firstOrNull { target ->
            target.name?.let(InetAddresses::isInetAddress) == true
        }
        if (ipTarget != null) {
            sendUsage(stack)
            return true
        }
        return false
    }
}
